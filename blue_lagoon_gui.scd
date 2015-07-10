e.pitch_contour_array;
e.pitch_contour;

(
e.pitch_contour.loadToFloatArray(action: {|fl|
  e.pitch_contour_array = fl;
});
)

(
w = Window("plotwindow", Rect(0, 0, 800, 300));
w.front;
p = Plotter("plot", Rect(10, 10, 780, 180), w);
p.editMode = false;
p.value = e.pitch_contour_array;

e.current_pitch = TextField(w, Rect(10, 200, 100, 40));
x = 0;

p.drawFunc = { |v|
  var x_val;
  x_val = x;
  Pen.width = 3;
  Pen.strokeColor = Color.red;
  Pen.moveTo(x_val@0);
  Pen.lineTo(x_val@p.bounds.height);
  Pen.fillStroke;
};
)

(
AppClock.clear;

AppClock.sched(0, {
  e.pitch_driver_bus.get({ |val|
    var loc;
    // see where we are in the sample
    loc = val / e.obn.numFrames;
    x = (loc * (p.bounds.width - 26)) + 13;
  });
  p.refresh;
  0.05;
});

AppClock.sched(0, {
  var new_pitch, cond, freq, base_index, freq_array;
  freq = e.pitch_bus.getSynchronous;
  base_index = e.quantizePitch(freq, e.piano_study);
  new_pitch = e.notes.wrapAt(base_index);
  e.klang_buses.setn(([0, 12, 19, 24, 28, 31, 36, 40, 43, 48] + base_index)
    .collect({|i| e.chromatic.degreeToFreq(i, 36.midicps, 0) }));
  e.current_pitch.value_(new_pitch);
  0.05;
});
)
