/*
Steps:

1. Boot server:
*/

o = Server.local.options;
ServerOptions.inDevices;
Server.local.options.sampleRate = 44100;
Server.local.options.numAudioBusChannels = 256;
Server.local.options.numOutputBusChannels = 8;
Server.local.options.numInputBusChannels = 8;
/* Server.local.options.outDevice = "EDIROL UA-25EX"; */
/* Server.local.options.inDevice = "EDIROL UA-25EX"; */
Server.local.options.inDevice = "Built-in Input";
Server.local.options.outDevice = "Built-in Output";
// Server.local.options.maxLogins = 2;
Server.local.boot;
Server.local.quit;
Server.local.meter;

/*
2. Execute the following code:
*/

(

// Load the important globals into an environment
e = Environment[
  // Initialize things that need to refer to self
  'init' -> {
    arg self;
    self['llocal'] = NetAddr.new("alice.local", NetAddr.langPort);

    if (self.networked == true, {
      self['david'] = NetAddr.new("croquemonsieur.local", 57110);
      self['sremote'] = Server(\David, self.david);
      self['lremote'] = NetAddr.new("croquemonsieur.local", 57120);
      self['lremote'].sendMsg('/test/hellodavid', "This is a message, hi");
    });

    self['chromatic'] = Scale.new(self.chromatic_degrees, 48, self.piano_study);
    self['triads'] = Scale.new(self.triads_degrees, 48, self.piano_study);
    self['major'] = Scale.new(self.major_degrees, 48, self.piano_study);
    self['current_scale'] = self['chromatic'];

    self['buf'].put(\pitch_transfer, self.fill_transfer);
    self['buf'].put(\cheby, Buffer.alloc(self.slocal, 512, 1).cheby(Array.geom(8, 1, 1)));
    self['buf'].put(\voice, Buffer.alloc(self.slocal, self.slocal.sampleRate * 20, completionMessage: {
      |buf|
      buf.fill(0, self.slocal.sampleRate * 20, 0);
    }));
    self['buf'].know = true;

    self['proxy_space'] = ProxySpace.push(self.slocal, "Blue Lagoon");
  },

  'slocal' -> Server.local,

  'buf' -> (),
  'root' -> 36.midicps,
  'notes' -> ["C7", "Gbmaj7/Db", "G7/D", "Cm/Eb", "C7/E", "F", "Gbmaj7", "G7", "Ab7", "F/A", "Gm7/Bb", "G7/B"],
  'notes_david' -> ["D7", "Abmaj7/Eb", "A7/E", "Dm/F", "D7/F#", "G", "Abmaj7", "A7", "Bb7", "G/B", "Am7/C", "A7/C#"],
  'chord' -> [
    [0, 7, 12, 16, 19, 22, 24],
    [1, 6, 10, 13, 18, 22, 29],
    [2, 7, 11, 14, 19, 26, 29],
    [3, 7, 12, 15, 19, 24, 27],
    [4, 7, 12, 16, 19, 22, 28],
    [5, 12, 17, 21, 24, 28, 31],
    [6, 10, 13, 18, 22, 29, 34],
    [7, 14, 19, 23, 26, 29, 31],
    [8, 15, 20, 24, 27, 30, 32],
    [9, 12, 17, 21, 24, 28, 31],
    [10, 19, 22, 26, 29, 31, 34],
    [11, 14, 19, 23, 26, 29, 31, 35]],
  // 'chord' -> [0, 7, 12, 16, 17, 19, 24, 28, 31, 36],
  // 'chord' -> [0, 2, 4, 7, 9, 11, 14, 16, 18, 21],
  // 'notes' -> ["C", "D", "E", "F", "G", "A", "B"],
  // 'notes_david' -> ["A", "B", "C#", "D", "E", "F#", "G#"],

  // 4-octave tuning for the Piano Study #5
  'piano_study' ->
    Tuning(([1/1, 21/20, 9/8, 6/5, 5/4, 4/3, 7/5, 3/2, 8/5, 5/3, 7/4, 15/8] ++
    ([1/1, 21/20, 9/8, 6/5, 5/4, 4/3, 7/5, 3/2, 8/5, 5/3, 7/4, 15/8] * 2) ++
    ([1/1, 33/32, 9/8, 6/5, 5/4, 21/16, 11/8, 3/2, 8/5, 13/8, 7/4, 15/8] * 4) ++
    ([1/1, 21/20, 9/8, 7/6, 5/4, 4/3, 11/8, 3/2, 8/5, 27/16, 7/4, 15/8] * 8)).ratiomidi,
    16, "Larry Polansky, Piano Study #5"),
  // Every note in the scale
  'chromatic_degrees' -> (0..47),
  // Diatonic thirds all the way up the tuning
  'triads_degrees' -> [ 0, 4, 7, 11, 14, 17, 21, 24, 28, 31, 35, 38, 41, 45 ],
  // A major scale all the way up the tuning
  'major_degrees' -> [0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35, 36, 38, 40, 41, 43, 45, 47],

  'quantizePitch' -> {
    arg self, freq, tuning;
    (tuning.semitones - (freq / self.root).ratiomidi).abs.minIndex;
  },

  'min' -> 60,
  'max' -> 200,

  // Fills the quantizing buffer function
  'fill_transfer' -> {
    arg self, min = self.min, max = self.max, base = 48, scale = self.chromatic, tuning = self.piano_study, server = self.slocal;
    var lookup, transfer, range, minQuant, maxQuant;

    range = max - min;
    minQuant = scale.degreeToFreq(self.quantizePitch(min, tuning), base, 0);
    maxQuant = scale.degreeToFreq(self.quantizePitch(max, tuning), base, 0);

    lookup = Array.fill(range, {
      arg i;
      scale.degreeToFreq(self.quantizePitch(i + min, tuning), base, 0);
    });

    transfer = Buffer.alloc(server, range, 1).loadCollection(lookup.normalize(-1.0, 1.0));
    transfer;
  }
];

// Allows the e.whatever syntax
e.know = true;
e.networked = false;
e.init;

this.executeFile("/Users/acsmith/workspaces/music/blue_lagoon/blue_lagoon_analyze.scd");
this.executeFile("/Users/acsmith/workspaces/music/blue_lagoon/blue_lagoon_synthdefs.sc");
this.executeFile("/Users/acsmith/workspaces/music/blue_lagoon/blue_lagoon_scratch.scd");

// // Audio buses
// ~source = NodeProxy.audio(e.slocal, 1);
// ~sample_driver = NodeProxy.audio(e.slocal, 1);
// ~sines = NodeProxy.audio(e.slocal, 1);
// ~shaped = NodeProxy.audio(e.slocal, 1);
// ~out = NodeProxy.audio(e.slocal, 2);
//
// // Data tracking buses
// ~pitch = NodeProxy.control(e.slocal, 1);
// ~pitch_unscaled = NodeProxy.control(e.slocal, 1);
// ~amp = NodeProxy.control(e.slocal, 1);
// ~amp_unscaled = NodeProxy.control(e.slocal, 1);
// ~driver_monitor = NodeProxy.control(e.slocal, 1);
// ~pitch_driver = NodeProxy.control(e.slocal, 1);
~klang_freq = NodeProxy.control(e.slocal, 7);
~klang_amp = NodeProxy.control(e.slocal, 7);

// Fill with default values
~klang_amp = Array.geom(10, 1, 0.7);
~klang_freq = Array.geom(10, 50, 1.5);

)
e.chord.wrapAt(3)
e.slocal.prepareForRecord;
e.slocal.record;
e.slocal.stopRecording;

e.lets_go.value;
