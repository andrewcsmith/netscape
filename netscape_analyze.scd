/* Proof of concept.

  Reads the following:

   e.slocal - the local server
   e.obn_file - path to the saved sound file. opened by SoundFile and allocated as a buffer
   e.obn - allocated as a buffer of the sample

  Writes the following:

   e.pitch_contour - buffer of the pitch contour, normalized -1.0, 1.0
   e.amp_contour - buffer of the amp contour, normalized, -1.0, 1.0

*/

/* Test data: */

/* Execute: */
e.analyze = {
  arg self;

  fork {
    var resultbuf, resultpath, oscpath, score, dur, sf, cond, size, data, bufreadMsg, source_bus, pitch_bus, amp_bus, driver_bus, sawpath;

    // sf = SoundFile.openRead(e.obn_file);
    // dur = sf.duration;
    // sf.close;

    dur = e.buf.voice.numFrames / e.slocal.sampleRate;

    e.obn = Buffer.new(e.slocal, dur * e.slocal.sampleRate, 1);
    bufreadMsg = e.obn.allocReadMsg(e.obn_file);

    resultbuf = Buffer.new(e.slocal, e.obn.numFrames, 2);
    // resultbuf.postln;

    resultpath = PathName.tmp +/+ UniqueID.next ++ ".aiff";
    oscpath = PathName.tmp +/+ UniqueID.next ++ ".osc";
    sawpath = PathName.tmp +/+ UniqueID.next ++ ".aiff";

    source_bus = Bus.audio(e.slocal);
    pitch_bus = Bus.control(e.slocal);
    amp_bus = Bus.control(e.slocal);
    driver_bus = Bus.audio(e.slocal);

    score = Score([
      [0, bufreadMsg],
      [0, resultbuf.allocMsg],
      [0, [\d_recv, SynthDef(\driver, {
        arg dur, out;
        Out.ar(out, Line.ar(1, dur * SampleRate.ir, dur));
      }).asBytes]],
      [0, [\d_recv, SynthDef(\player, {
        | out, buf, phase |
        Out.ar(out, BufRd.ar(1, buf, InFeedback.ar(phase)));
      }).asBytes]],
      [0, [\d_recv, SynthDef(\pitchTracker, {
        arg in, out, min = 60, max = 2000;
        var tracker, source;
        source = InFeedback.ar(in, 1);
        tracker = LPF.kr(Pitch.kr(source, (max + min) / 2, minFreq: min, maxFreq: max), 100);
        Out.kr(out, tracker);
      }).asBytes]],
      [0, [\d_recv, SynthDef(\ampTracker, {
        arg in, out;
        var tracker, source;
        source = InFeedback.ar(in, 1);
        tracker = LPF.kr(Amplitude.kr(source), 100);
        Out.kr(out, tracker);
      }).asBytes]],
      [0, [\d_recv, SynthDef(\stereoBufWrScale, {
        arg in1, in2, buf, phase, in1Min, in1Max, out1Min, out1Max, in2Min, in2Max, out2Min, out2Max;
        BufWr.ar([LinLin.ar(K2A.ar(In.kr(in1)), in1Min, in1Max, out1Min, out1Max), LinLin.ar(K2A.ar(In.kr(in2)), in2Min, in2Max, out2Min, out2Max)], buf, InFeedback.ar(phase));
      }).asBytes]],
      [0, Synth.basicNew(\driver, e.slocal)
        .newMsg(1, [\dur, dur, \out, driver_bus])],
      [0, Synth.basicNew(\player, e.slocal)
        .newMsg(1, [\out, source_bus, \buf, e.obn, \phase, driver_bus])],
      [0, Synth.basicNew(\pitchTracker, e.slocal).newMsg(1, [\in, source_bus, \out, pitch_bus, \min, e.min, \max, e.max], \addToTail)],
      [0, Synth.basicNew(\ampTracker, e.slocal).newMsg(1, [\in, source_bus, \out, amp_bus], \addToTail)],
      [0, Synth.basicNew(\stereoBufWrScale, e.slocal)
        .newMsg(1, [\in1, pitch_bus, \in2, amp_bus, \buf, resultbuf, \phase, driver_bus, \in1Min, e.min, \in1Max, e.max, \out1Min, -1.0, \out1Max, 1.0, \in2Min, 0, \in2Max, 1, \out2Min, -1, \out2Max, 1], \addToTail)],
      [dur, resultbuf.writeMsg(resultpath, headerFormat: "AIFF", sampleFormat: "float")]
      // [dur, [\c_set, 0, 0]]
    ]);

    cond = Condition.new;

    // Record the sample
    score.recordNRT(oscpath, "/dev/null", sampleRate: e.slocal.sampleRate,
      options: ServerOptions.new
      .verbosity_(-1)
      .numInputBusChannels_(1)
      .numOutputBusChannels_(2)
      .sampleRate_(e.slocal.sampleRate),
      action: { cond.unhang }
    );

    cond.hang;

    // Add each channel of the sound file to its own buffer
    e.buf.pitch_contour = Buffer.readChannel(e.slocal, resultpath, channels: 0);
    e.buf.amp_contour = Buffer.readChannel(e.slocal, resultpath, channels: 1);

    // Process all the things
    AppClock.sched(0, {
      e.pitch_contour_window = Window("plotwindow", Rect(0, 0, 800, 300));
      e.pitch_contour_window.front;
      e.pitch_contour_plot = Plotter("plot", Rect(10, 10, 780, 180), e.pitch_contour_window);
      e.pitch_contour_plot.editMode = false;

      e.current_pitch = TextField(e.pitch_contour_window, Rect(10, 200, 100, 40));
      e.pitch_contour_location = 13;

      e.pitch_contour_plot.drawFunc = { |v|
        var x_val;
        x_val = e.pitch_contour_location;
        Pen.width = 3;
        Pen.strokeColor = Color.red;
        Pen.moveTo(x_val@0);
        Pen.lineTo(x_val@e.pitch_contour_plot.bounds.height);
        Pen.fillStroke;
        Pen.width = 1;
      };

      e.buf.pitch_contour.loadToFloatArray(action: {|pitch|
        e.pitch_contour_array = pitch;
        e.buf.amp_contour.loadToFloatArray(action: {|amp|
          e.amp_contour_array = amp;
          AppClock.sched(0, { e.pitch_contour_plot.value = [e.pitch_contour_array, e.amp_contour_array]; });
        });
      });
    });

    if (e.networked == true, {
      e.buf.voice.loadToFloatArray(action: {
        Buffer.sendCollection(e.sremote, e.record_plot_data, action: {
          |buf|
          "sent voice".postln;
          e.lremote.sendMsg('/buffer/new', "voice", buf.numFrames, buf.numChannels, buf.bufnum);
        });
      });
      e.buf.pitch_contour.loadToFloatArray(action: {
        |ar|
        Buffer.sendCollection(e.sremote, ar, action: {
          |buf|
          "sent pitch_contour".postln;
          e.lremote.sendMsg('/buffer/new', "pitch_contour", buf.numFrames, buf.numChannels, buf.bufnum);
        });
      });
      e.buf.amp_contour.loadToFloatArray(action: {
        Buffer.sendCollection(e.sremote, e.amp_contour_array, action: {
          |buf|
          "sent amp_contour".postln;
          e.lremote.sendMsg('/buffer/new', "amp_contour", buf.numFrames, buf.numChannels, buf.bufnum);
        });
      });
    });
  };
};
