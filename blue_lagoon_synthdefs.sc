(
// Instantiate the lib if necessary
// SynthDescLib.new(\blue_lagoon, e.slocal);
SynthDescLib.new(\blue_lagoon, Server.local);

// Quantizes the frequency and outputs a saw wave
SynthDef(\quantSaw, {
  | freq, lookupBuf, min, max, amp = 0.3, out = 0 |
  var scaledFreq, quantFreq, range;
  range = max - min;
  scaledFreq = LinLin.ar(K2A.ar(freq), min, max, 0, range);
  quantFreq = LinLin.ar(BufRd.ar(1, lookupBuf, K2A.ar(scaledFreq), 0, 4), -1, 1, min, max);
  Out.ar(out, BlitB3.ar(quantFreq, amp));
}).add(\blue_lagoon);

SynthDef(\quantVoice, {
  | out, freq, lookupBuf, min, max, amp = 0.3 |
  var scaledFreq, quantFreq, range;
  range = max - min;
  scaledFreq = LinLin.kr(freq, min, max, 0, range);
  quantFreq = LinLin.kr(BufRd.kr(1, lookupBuf, scaledFreq, 0, 4), -1, 1, min, max);
  Out.kr(out, quantFreq);
}).add(\blue_lagoon);

SynthDef(\pitchTracker, {
  arg in, out, min = 60, max = 2000;
  var tracker, source;
  source = InFeedback.ar(in, 1);
  tracker = LPF.kr(Pitch.kr(source, (max + min) / 2, min, max), 100);
  Out.kr(out, tracker);
}).add(\blue_lagoon);

SynthDef(\ampTracker, {
  arg in, out;
  var tracker, source;
  source = In.ar(in, 1);
  tracker = LPF.kr(Amplitude.kr(source), 100);
  Out.kr(out, tracker);
}).add(\blue_lagoon);

SynthDef(\pitchShifter, {
  arg in, out, pitchRatio;
  var source, shifted;
  source = In.ar(in, 1);
  shifted = FreqShift.ar(source, pitchRatio, 0);
  Out.ar(out, shifted);
}).add(\blue_lagoon);

SynthDef(\samplePlayer, {
  arg buf, out = 0, loop = 1;
  Out.ar(out, PlayBuf.ar(1, buf, loop: loop));
}).add(\blue_lagoon);

SynthDef(\saw, {
  arg freq, amp = 0.3;
  Out.ar(1, LPF.ar(Saw.ar(freq, amp), 4000));
}).add(\blue_lagoon);

SynthDef(\shaper, {
  arg in, shaper_buf, out = 0, amp = 1.0;
/*  var driver, shaper, multiplier;
  driver = In.ar(in);
  multiplier = BufFrames.ir(shaper_buf);
  Out.ar(out, BufRd.ar(1, shaper_buf, multiplier * driver, 1, 4));*/
  var driver, shaper, multiplier;
  driver = In.ar(in);
  Out.ar(out, Shaper.ar(shaper_buf, driver, amp));
}).add(\blue_lagoon);

SynthDef(\reverb, {
  arg in, out;
  var verb;
  verb = FreeVerb.ar(In.ar(in));
  Out.ar(out, verb);
}).add(\blue_lagoon);

SynthDef(\patch, {
  arg in, amp = 1;
  Out.ar(0, In.ar(in) * amp);
}).add(\blue_lagoon);

SynthDef(\driver, {
  arg dur, out, trig = 1;
  Out.ar(out, Phasor.ar(trig, 1, 0, dur * SampleRate.ir));
}).add(\blue_lagoon);

SynthDef(\driver_kr, {
  arg dur, out, step = 1, trig = 1;
  Out.kr(out, Phasor.kr(trig, step, 0, dur * SampleRate.ir));
}).add(\blue_lagoon);

SynthDef(\linear_scale_ar, {
  arg in, out, inMin, inMax, outMin, outMax;
  Out.ar(out, LinLin.ar(In.ar(in), inMin, inMax, outMin, outMax));
}).add(\blue_lagoon);

SynthDef(\linear_scale_kr, {
  arg in, out = 0, inMin, inMax, outMin, outMax;
  Out.kr(out, LinLin.kr(in, inMin, inMax, outMin, outMax));
}).add(\blue_lagoon);

SynthDef(\sample_player_ar, {
  arg buf, scale = 1, out = 0, trig = 1;
  Out.kr(out, BufRd.ar(1, buf, Phasor.ar(trig, BufRateScale.kr(buf) * scale, 0, BufFrames.kr(buf))));
}).add(\blue_lagoon);

SynthDef(\sample_player_kr, {
  arg buf, phase, out = 0, trig = 1;
  Out.kr(out, BufRd.kr(1, buf, phase * BufFrames.kr(buf)));
}).add(\blue_lagoon);

SynthDef(\player, {
  arg out, buf, phase;
  Out.ar(out, BufRd.ar(1, buf, InFeedback.ar(phase)));
}).add(\blue_lagoon);

SynthDef(\lpc, {
  arg in, source, out, amp = 1.0, gate = 1, fadeTime = 1;
  var env, lpc;
  env = EnvGen.kr(Env.asr(fadeTime, 1, fadeTime), gate, amp, doneAction: 2);
  lpc = LPCAnalyzer.ar(In.ar(in), In.ar(source), windowtype: 1);
  Out.ar(out, env * lpc);
}).add(\blue_lagoon);

SynthDef(\a2k_patch, {
  arg in, out;
  Out.kr(out, A2K.kr(In.ar(in)));
}).add(\blue_lagoon);

SynthDef(\klangs, {
  arg freq_bus, amp_bus, phases = #[0, 0, 0], out, gate = 1, fadeTime = 1;
  var env, klang;
  env = EnvGen.ar(Env.asr(fadeTime, 1, fadeTime), gate, doneAction: 2);
  klang = DynKlang.ar(`[In.kr(freq_bus, 7), LPF.kr(In.kr(amp_bus, 7), 0.1), phases]);
  Out.ar(out, env * klang);
}).add(\blue_lagoon);
)
