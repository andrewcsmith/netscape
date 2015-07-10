(
e.sampler_group = Group.new;
e.source_group = Group.new(e.sampler_group, \addAfter);
e.analysis_group = Group.new(e.sampler_group, \addAfter);
e.resynth_group = Group.new(e.analysis_group, \addAfter);

/*e.buf.voice.loadToFloatArray(action: {
  arg data;
  e.record_plot_data = data;
});*/

NodeProxy.defaultNumAudio = 2;

~micIn = { SoundIn.ar };
// ~micIn.play(vol: 0.5);
~recordInDriver.ar(1);

~recordInDriver.ar(1);
~recordInDriver.add({ Phasor.ar(\trigger.kr, 1 / BufFrames.kr(e.buf.voice)) }, now: false);
~recordInDriver.set(\trigger, 0);
~recordInDriver.send;

~recordInDriverMonitor = { A2K.kr(\in.ar) };
~recordInDriverMonitor <<> ~recordInDriver;

~recordIn.ar(1);
~recordIn.add({
  BufWr.ar(\in.ar, \buf.kr, InFeedback.ar(\phase.ar) * BufFrames.kr(\buf.kr), 1);
}, 0, now: false);
~recordIn <<> ~micIn;
~recordIn.set(\phase, ~recordInDriver.bus);
~recordIn.set(\buf, e.buf.voice);
~recordIn.send;

// ~recordIn.set(\buf, e.buf.voice);
// ~recordIn.set(\phase, ~recordInDriver.bus);

e.buf.voice.fill(0, e.buf.voice.numFrames, 0);
// ~recordIn.set(\phase, ~recordInDriver.bus);
// ~recordInDriver.set(\trigger, 1);
// ~recordIn.set(\phase, e.buf.voice.numFrames);
// ~recordInDriver.set(\trigger, 0);

// ~recordIn.play;

~micDelay.ar(2);
~micDelay = { [CombC.ar(\in.ar, 0.2, 0.15), CombC.ar(\in.ar, 0.2, 0.18)] };
~micDelay <<> ~micIn;
~micDelay.play(0, 2, vol: 1);
// ~micIn.play(0, 2, vol: 0.2);

// ~sampler.pause;

e.record_window = Window.new('button', Rect(300, 300, 800, 200));
e.record_window.front;
e.record_button = Button.new(e.record_window, Rect(10, 10, 180, 180))
.states_([["ready", Color.black, Color.green], ["recording", Color.black, Color.red]])
.action_({
  arg button;
  case
  { button.value == 0 } {
    var outpath;
    // "ready".postln;
    ~recordIn.set(\phase, e.buf.voice.numFrames);
    ~recordInDriver.set(\trigger, 0);
    outpath = PathName.tmp +/+ UniqueID.next ++ ".aiff";
    e.buf.voice.write(outpath, "aiff", "float", completionMessage: {
      e.obn_file = outpath;
      // e.obn_file.postln;
      e.analyze;
      AppClock.sched(5, e['lets_go']);
    });
  }

  { button.value == 1 } {
    // "recording".postln;
    e.buf.voice.fill(0, e.buf.voice.numFrames, 0);
    ~recordIn.set(\phase, ~recordInDriver.bus);
    ~recordInDriver.set(\trigger, 1);
  };
});

e.record_plot = Plotter('voice', Rect(210, 10, 580, 180), e.record_window);
e.record_plot.editMode = false;

e.record_playhead_loc = 13;
e.record_plot.normalized = true;
e.record_plot.drawFunc = { |v|
  var x_val;
  x_val = e.record_playhead_loc;
  Pen.width = 3;
  Pen.strokeColor = Color.red;
  Pen.moveTo(x_val@0);
  Pen.lineTo(x_val@e.record_plot.bounds.height);
  Pen.fillStroke;
  Pen.width = 1;
};

e.record_plot_data = Array.fill(e.buf.voice.numFrames, 0);
e.record_plot.value = e.record_plot_data;

// Assigns new data and playhead location every 1/10 second
e.record_update_rate = 0.25;
e.record_update = {
  AppClock.sched(0, {
    ~recordInDriverMonitor.bus.get({ |val|
      // see where we are in the sample
      e.record_playhead_loc = (val * (e.record_plot.bounds.width - 26)) + 13;
    });
    e.buf.voice.loadToFloatArray(action: {
      arg data, buf;
      e.record_plot_data = data;
    });
    e.record_plot.setValue(e.record_plot_data, refresh: true);
    e.record_update_rate;
  });
};
e.record_update.value;

e.record_window.onClose = { e.record_update_rate = nil; };

/*OSCdef(\monome, {
  arg msg, time, addr, recvPort;
  AppClock.sched(0, { e.record_button.valueAction = msg[3] });
}, "/monome/grid/key", recvPort: NetAddr.langPort);*/
)

e.proxy_space.gui;

// ~micIn.bus.scope