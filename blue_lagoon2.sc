(
// Load the important globals into an environment
e = Environment[
  // Initialize things that need to refer to self
  'init' -> {
    arg self;
    self['chromatic'] = Scale.new(self.chromatic_degrees, 48, self.piano_study);
    self['triads'] = Scale.new(self.triads_degrees, 48, self.piano_study);
    self['major'] = Scale.new(self.major_degrees, 48, self.piano_study);
  },

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
    var semitone;
    semitone = (freq.cpsmidi - 36);
    (tuning.semitones - semitone).abs.minIndex;
  },

  'min' -> 60,
  'max' -> 130,
  'slocal' -> Server.default,
  'sremote' -> Server.default,

  // Fills the buffer function
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
  },

  'obn_file' -> "/Users/acsmith/workspaces/music/blue_lagoon/of_being_numerous_cropped.wav"
];

// Allows the e.whatever syntax
e.know = true;
e.init;
)
