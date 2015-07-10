// Add a low-pass filter to the pitch tracker

(
// Boot local server
o = ServerOptions.new;
o.maxLogins = 2;
o.device = "PreSonus FireStudio";

~alice = Server(\Alice, NetAddr.new("alice.local", 57115), o, 0);
~david = NetAddr.new("croquemonsieur.local", 57110);
~croque = Server(\David, ~david);
~monsieur = NetAddr.new("croquemonsieur.local", 57120);



Server.default = ~alice;
~alice.boot;
)

~me = NetAddr("localhost", NetAddr.langPort);
~me.sendMsg("/time", TempoClock.beats);

~timer = OSCFunc({|msg, time, addr, recvPort| (msg[1] - TempoClock.beats).postln;}, "/time");
~timer.disable;


(
// Load lang
~pianoStudyOctaves = [1/1, 21/20, 9/8, 6/5, 5/4, 4/3, 7/5, 3/2, 8/5, 5/3, 7/4, 15/8] ++
  ([1/1, 21/20, 9/8, 6/5, 5/4, 4/3, 7/5, 3/2, 8/5, 5/3, 7/4, 15/8] * 2) ++
  ([1/1, 33/32, 9/8, 6/5, 5/4, 21/16, 11/8, 3/2, 8/5, 13/8, 7/4, 15/8] * 4) ++
  ([1/1, 21/20, 9/8, 7/6, 5/4, 4/3, 11/8, 3/2, 8/5, 27/16, 7/4, 15/8] * 8);

// Converts the ratio value to the number of semitones
~toCents = {
  | val |
  val.log2 * 12.0;
};

t = Tuning.new(~pianoStudyOctaves.collect(~toCents), 16.0, "Piano Study #5");
~scale = Scale.new((0..47), 48, t);

~quantizePitch = {
  | freq, tuning |
  var semitone;
  // Deviation in semitones from c,
  semitone = (freq.cpsmidi - 36);
  (tuning.semitones - semitone).abs.minIndex;
};

SynthDef.new(\pitchTracker, {
  | bus = 0, in = 0 |
  Out.kr(bus, (Pitch.kr(SoundIn.ar(in), 200, 40, 500)));
}).load(~served);

SynthDef.new(\sine, {
  | freq, amp = 0.2, out = 0 |
  Out.ar(out, SinOsc.ar(Ramp.kr(freq, 0.01), 0, amp));
}).load(~davidServer);
)

~of_being_numerous = Buffer.read(~alice, "/Users/acsmith/workspaces/music/blue_lagoon/of_being_numerous_cropped.wav");
~of_being_numerous.play;

~cheby = Buffer.alloc(~alice, 4096 * 4, 1);
~cheby.cheby([0.1, 0.4, 0.3, 0.6, 0.5, 0.2]);
~cheby.plot;

(
SynthDef(\shaper, {
  | in_buf, shaper_buf, out = 0 |
  var driver, shaper, multiplier;
  driver = PlayBuf.ar(1, in_buf, 1, loop: 1);
  multiplier = BufFrames.ir(shaper_buf);
  // driver = SinOsc.ar(200);
  Out.ar(out, BufRd.ar(1, shaper_buf, multiplier * driver, 1, 4));
}).play(~alice, [\in_buf, ~of_being_numerous, \shaper_buf, ~cheby]);
)

~lolBuffer = Buffer.alloc(~served, 512, 1);
~davidBuffer.copyData(~lolBuffer, 0, 0, -1);
~lolBuffer.plot;

~davidLOL = Buffer.sendCollection(~davidServer, [0, 0.1, 0.3, 0.2, 0.0], 1);
~davidLOL.bufnum;
~davidLOL.query;

~davidLOL.getToFloatArray(0, -1, wait: 0.02, timeout: 10, action: {|array| array.postln;});

~lolBuffer.query;
~davidBuffer.query;

~cheby.plot;

Buffer.new(~served, 512, 1, 0).plot;
~cheby.getToFloatArray(0, 0.01, 0.1, 3, {|i| i.plot;});

~another = Buffer.alloc(~served, 512, 1);
~cheby.copyData(~another, 0, 0, -1);

~davidBuffer = Buffer.alloc(~davidServer, 512, 1);
~davidBuffer.plot;
~davidBuffer.bufnum;

b = Buffer.new(~served, 512, 1, 0);
b.plot;

(

~bus = Bus.control(s, 1).set(300);
~freqBus = Bus.control(s, 1).set(200);

~pitchTracker = Synth(\pitchTracker, [\bus, ~bus], s);
~synthFollower = Synth(\sine, [\freq, ~freqBus.asMap, \out, 0], s);
~otherSynth = Synth(\sine, [\freq, ~freqBus.asMap, \out, 1], s);

// This reads in the tracked pitch, quantizes it, and sets another bus to it
x = Task({
  var newFreq;
  loop {
    ~bus.get({
      | freq |
      newFreq = 4 * ~scale.degreeToFreq(~quantizePitch.value(freq, t), 12.midicps, 0);
      if (newFreq > 150, {
        ~synthFollower.set(\freq, newFreq);
        ~otherSynth.set(\freq, freq);
        // ~freqBus.set(newFreq);
      }, {});
    });
    0.02.wait;
  }
}).play;
)

(
x.stop;
~synthFollower.free;
~otherSynth.free;
)

MIDIIn.connectAll;
(
~notes = Array.fill(127, {nil});
MIDIFunc.noteOn({
  |val, num, chan, src|
   ~notes[num] = Synth(\default, [\freq, ~scale.degreeToFreq(num, 12.midicps, 0)], ~served);
});
MIDIFunc.noteOff({
  |val, num, chan, src|
  ~notes[num].set(\gate, 0);
});
)

e.obn = Buffer.read(e.slocal, e.obn_file);
e.source_bus = Bus.audio(e.slocal, 1);
e.pitch_bus = Bus.control(e.slocal, 1);
e.amp_bus = Bus.control(e.slocal, 1);

e.source_bus.asMap;
e.pitch_bus.asMap;
e.slocal.options;

e.player = { Out.ar(e.source_bus, PlayBuf.ar(1, e.obn, loop: 1)); }.play;
e.player.free;

SynthDef(\player, {
  | out, buf |
  Out.ar(out, PlayBuf.ar(1, buf, loop: 1));
}).asBytes;

e.pitch_tracker = Synth(\pitchTracker, [\in, e.source_bus, \out, e.pitch_bus, \max, 140], e.slocal, \addToTail);
e.amp_tracker = Synth(\ampTracker, [\in, e.source_bus, \out, e.amp_bus], e.pitch_tracker, \addAfter);
e.pitch_tracker.free; e.amp_tracker.free;

e.saw = Synth(\saw, [\freq, e.pitch_bus.asMap, \amp, e.amp_bus.asMap], e.pitch_tracker, \addAfter);
e.saw.free;

e.patch = { arg amp = 1.0; Out.ar([0, 1], amp * In.ar(e.source_bus)); }.play(addAction: \addToTail);
e.patch.free;
e.patch.set(\amp, 0.5);

Synth.basicNew(\pitchTracker, e.slocal).addToTailMsg(args: [\in, e.source_bus, \out, e.pitch_bus, \max, 140]);
e.slocal.queryAllNodes;

SynthDef(\driverplayer, {
  Out.ar([0, 1], BufRd.ar(1, e.obn, Line.ar(1, 10, 10)));
}).play

e.obn;
e.obn = Buffer.new(e.slocal, e.obn_file);
e.obn.readMsg;
e.obn.bufnum;
e.slocal.boot;

(e.obn = Buffer.new(e.slocal, 17 * 44100));
e.obn;

(
SynthDef(\driver, {
  arg dur, out;
  Out.ar(out, Line.ar(1, dur * 44100, dur));
}).add;
)
e.obn;
e.obn.plot;
e.obn_out = Buffer.new(e.slocal, e.obn.numFrames, e.obn.numChannels);
(
Routine({
  var dur = 16.434, bufreadMsg, obn, driver_bus;

  e.obn = Buffer.new(e.slocal, (dur * 44100), 1);
  e.out_buffer = Buffer.new(e.server, e.obn.numFrames, e.obn.numChannels);
  driver_bus = Bus.audio(e.slocal, 1);

  e.slocal.listSendMsg(e.obn.allocReadMsg(e.obn_file));
  e.slocal.listSendMsg(e.out_buffer.allocMsg);

  Synth(\driver, [\dur, dur, \out, driver_bus]);
  // { arg freq; Out.ar(0, Saw.ar(InFeedback.ar(freq) / 200)); }.play(args: [\freq, driver_bus]);
  // { arg phase; Out.ar(0, BufRd.ar(1, e.obn, InFeedback.ar(phase))); }.play(e.slocal, args: [\phase, driver_bus]);
  {
    arg phase, out_buf;
    BufWr.ar(EnvGen.kr(Env.new([0, 1, 0], [dur / 2, dur / 2], 'exp'), doneAction: 2) *
      BufRd.ar(1, e.obn, InFeedback.ar(phase)), out_buf, InFeedback.ar(phase));
    Out.ar([0, 1], SinOsc.ar(440));
  }.play(e.slocal, args: [\phase, driver_bus, \out_buf, e.out_buffer]);


}).value;
)

e.out_buffer.plot;

e.slocal.boot;
x = Buffer.read(e.slocal, e.obn_file);
x;
x.plot;
e.obn.path;
e.obn;
e.obn.plot;

{ Out.ar(0, PlayBuf.ar(1, e.out_buffer, 1)); }.play(e.slocal);



(e.obn = Buffer.new(e.slocal, e.obn_file)).readMsg(e.obn_file);

e.obn.readMsg(e.obn_file);
(e.obn = Buffer.new(e.slocal, SoundFile.openRead(e.obn_file).duration * 44100, 1)).obn.readMsg(e.obn_file);
~bufreadMsg = (e.obn = Buffer.new(e.slocal)).readMsg(e.obn_file);
~bufreadMsg;

{ Out.ar(0, BufRd.ar(1, e.obn, Line.ar(1, 44100, 1))); }.play;

e.slocal.queryAllNodes;

// Proof of concept.
(
fork {
  var resultbuf, resultpath, oscpath, score, dur, sf, cond, size, data, bufreadMsg, source_bus, pitch_bus, amp_bus, driver_bus, sawpath;

  sf = SoundFile.openRead(e.obn_file);
  dur = sf.duration;
  dur.postln;
  sf.close;

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
      Out.ar(out, Line.ar(1, dur * e.slocal.sampleRate, dur));
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
  score.recordNRT(oscpath, "/dev/null", sampleRate: sf.sampleRate,
    options: ServerOptions.new
    .verbosity_(-1)
    .numInputBusChannels_(sf.numChannels)
    .numOutputBusChannels_(sf.numChannels)
    .sampleRate_(sf.sampleRate),
    action: { cond.unhang }
  );

  cond.hang;

  // Add each channel of the sound file to its own buffer
  e.pitch_contour = Buffer.readChannel(e.slocal, resultpath, channels: 0);
  e.amp_contour = Buffer.readChannel(e.slocal, resultpath, channels: 1);
};
)

(
{
  var pitch_control, amp_control;
  pitch_control = Bus.control(e.slocal, 1);
  amp_control = Bus.control(e.slocal, 1);
  { Out.kr(amp_control, PlayBuf.ar(1, e.amp_contour, doneAction: 14) * 0.5 + 1.0); }.play;
  { Out.kr(pitch_control, e.min + ((e.max - e.min) * PlayBuf.ar(1, e.pitch_contour))) }.play;
  Synth(\quantSaw, [\freq, pitch_control.asMap, \min, e.min, \max, e.max, \lookupBuf, e.lookup_buf, \amp, amp_control.asMap]);
}.fork;
)

(
{
  var sf, data;
  sf = SoundFile.openRead("/tmp/1112.aiff");
  sf.readData(data = FloatArray.newClear(sf.numFrames));
  sf.close;
}.fork
)

(
~now = SystemClock.seconds;
e.obn.loadToFloatArray(action: {
  |ar|
  "loaded".postln;
  (SystemClock.seconds - ~now).postln;
  Buffer.sendCollection(~croque, ar, 1, action: {|bu| bu.postln; (SystemClock.seconds - ~now).postln;});
});
)