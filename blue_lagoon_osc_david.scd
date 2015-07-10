
/* OSC Communication:

['/pitch/current', scale_degree, pitch_name]
Sent every 1/20 second to update the current pitch

['/buffer/new', name, numFrames, numChannels, bufnum]
Sent when I upload a buffer

['/bus/new', name, rate, index, numChannels]

*/

(
OSCdef.new(\bus, {
  arg msg, time, addr, recvPort;
  // e.buses is a Dictionary of buses. This adds a bus to that Dictionary, referring to the remote server.
  e.buses[msg[1]] = Bus.new(msg[2], msg[3], msg[4], e.sremote);
}, '/bus/new');

OSCdef.new(\currentpitch, {
  arg msg, time, addr, recvPort;
  // Sets the value of the text box to the received pitch
  AppClock.sched(0, {
    e['text_field'].value_(msg[2]);
  });
}, '/pitch/current');

OSCdef.new(\bufferreceived, {
  arg msg, time, addr, recvPort;
  // e.buffers is a Dictionary of buffers. This is me telling you I added a buffer on your server.
  e.buffers[msg[1]] = Buffer.new(Server.local, msg[2], msg[3], msg[4]);
  ("New buffer at " ++ msg[1]).postln;
}, '/buffer/new');
)


// Process all the things, executes once and creates GUI
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

// This will loop til you do AppClock.clear or cmd-.
AppClock.sched(0, {
  ~analysis_driver.bus.get({ |val|
    var loc;
    // see where we are in the sample
    loc = val;
    e.pitch_contour_location = (loc * (e.pitch_contour_plot.bounds.width - 26)) + 13;
  });
  e.pitch_contour_plot.refresh;
  0.05;
});
