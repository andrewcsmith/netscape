e.lets_go = {
  Routine {
    e.buf.ringBuf = Buffer.alloc(s, 4096);

    ~rev <<> ~micIn;
    ~klangs.clear;
    ~klangs.vol = 0.001;
    ~klangs.add(\klangs, 0, [\freq_bus, ~klang_freq.bus, \amp_bus, ~klang_amp.bus]);

    /* ~lpc.clear; */
    /* ~lpc.ar(2); */
    /* ~lpc = { Resonz.ar(~klangs.ar, LPF.kr(~lpc_analysis.kr, 10), 1, mul: Amplitude.kr(~micIn.ar, 0.01, 0.5)); }; */
    /* ~rev <<> ~lpc; */

    ~bufRecorder = { RecordBuf.ar(~micIn.ar, e.buf.ringBuf); };
    ~bufRecorder.play(1, 1, vol: 0.0);

    ~lpc_analysis = { VoxRes.kr(e.buf.ringBuf, 13, 12000.0 / s.options.sampleRate ); };

    ~horribleSounds = { 
      var filters = Resonz.ar(\in.ar, LPF.kr(~lpc_analysis.kr, 10), 1);
      Splay.ar(filters * Amplitude.kr(~micIn.ar, 0.01, 0.5), 1);
    };
    ~horribleSounds <<> ~klangs;
    ~horribleSounds.play(vol: 0.001);

    ~moreHorribleSounds = { 
      var filters = Resonz.ar(\in.ar, LPF.kr(~lpc_analysis.kr, 100), 1);
      Splay.ar(filters, 1);
    };
    ~moreHorribleSounds <<> ~klangs;
    ~moreHorribleSounds.play(0, 2, vol: 0.003);

    ~yetmoreHorribleSounds = { 
      var filters = Resonz.ar(\in.ar, LPF.kr(~lpc_analysis.kr, 100), 1);
      Splay.ar(filters, 1);
    };
    ~yetmoreHorribleSounds <<> ~klangs;
    ~yetmoreHorribleSounds.play(1, 2, vol: 0.01);

    ~horribleSines = { 
      Splay.ar(SinOsc.ar(LPF.kr(~lpc_analysis.kr, 100))); 
    };
    ~horribleSines.play(0, 1, vol: 0.005);

    ~rev = { FreeVerb.ar(~lpc.ar) };
    ~rev.play(1, 1, vol: 0.03);

    ~comp = { Compander.ar(~klangs.ar + ~shaper.ar, ~lpc.ar, 0.7, 1, 1/4, 0.01, 1.0) };
    ~comp.play(vol: 0.01);

    ~echo_phase.kr(1);
    ~echo_phase = { |rate, mul, add| SinOsc.kr(rate, 0, mul, add) };
    ~echo_phase.set(\rate, 1);
    ~echo_phase.set(\mul, 0.001);
    ~echo_phase.set(\add, 0.2);

    ~sampler.ar(2);
    ~sampler.add({Pan2.ar(PlayBuf.ar(1, e.buf.voice, 0.25, loop: 1, doneAction: 2), 0)});
    ~sampler.play(0, 2, fadeTime: 1, vol: 0.25);

    ~samp_delay.ar(2);
    ~samp_delay = { [CombC.ar(\in.ar, 3.0, ~echo_phase.kr, 2.0), CombC.ar(\in.ar, 3.0, ~echo_phase.kr+0.1, 2.0)] };
    ~samp_delay <<> ~sampler;
    ~samp_delay.play(vol: 1);

    e.buf.cheby.cheby(Array.geom(15, 0.5, 0.25));
    ~shaper = { Shaper.ar(e.buf.cheby, ~klangs.ar, 1, 0); };
    ~shaper_comp = { Compander.ar(~shaper.ar, ~shaper.ar, 0.1, 0.1, 0.1) };
    ~shaper_comp.fadeTime = 2.0;
    ~shaper_comp.play(0, 2, fadeTime: 1, vol: 0.05);
    // ~shaper = { Shaper.ar(e.buf.cheby, ~micIn.ar, 1, 0); };

    // shaper playing
    ~shaper.play(0, 2, fadeTime: 1, vol: 0.009);
    ~shaper.stop;

    ~analysis_driver.clear;
    ~analysis_driver.kr;
    ~analysis_driver.add(\driver_kr, 0, [\dur, e.buf.pitch_contour.numFrames /
      e.slocal.sampleRate, \step, 1.0/(4 * e.buf.pitch_contour.numFrames) ]);

    ~pitch_sample_player.clear;
    ~pitch_sample_player.kr; 
    ~pitch_sample_player.add(\sample_player_kr, 0, [\buf, e.buf.pitch_contour,
      \phase, ~analysis_driver.bus.asMap]);

    ~amp_sample_player.clear;
    ~amp_sample_player.kr;
    ~amp_sample_player.add(\sample_player_kr, 0, [\buf, e.buf.amp_contour,
      \phase, ~analysis_driver.bus.asMap]);

    ~voice_sample_player.clear;
    ~voice_sample_player.kr;
    ~voice_sample_player.add(\sample_player_kr, 0, [\buf, e.buf.voice, \phase,
      ~analysis_driver.bus.asMap]);

    ~zero_crosser.clear;
    ~zero_crosser.ar(1);
    ~zero_crosser.add(\zero_crosser, 0, [\amp, ~amp_sample_player.bus.asMap, \in, ~voice_sample_player.bus]);
    ~zero_crosser.play(0, 1, fadeTime: 1, vol: 0.15);

    ~amp_scaler.clear;
    ~amp_scaler.kr;
    ~amp_scaler.add(\linear_scale_kr, 0, [\in, ~amp_sample_player.bus.asMap,
      \inMin, -1.0, \inMax, 1, \outMin, 0, \outMax, 1]);

    ~pitch_scaler.clear;
    ~pitch_scaler.kr;
    ~pitch_scaler.add(\linear_scale_kr, 0, [\in, ~pitch_sample_player.bus.asMap,
      \inMin, -1.0, \inMax, 1.0, \outMin, e.min, \outMax, e.max]);

    ~synth_driver.clear;
    ~synth_driver.add(\driver, 0, [\dur, e.buf.voice.numFrames /
      e.slocal.sampleRate]);

    // ~driver_monitor.kr;
    // ~driver_monitor.add(\a2k_patch, [\in, ~.bus]);

    // Register the relevant OSC functions for TouchOSC
    OSCdef.new(\micDelayVol, {
      arg msg, time, addr, recvPort;
      // msg[1].asFloat.postln;
      ~micDelay.vol = msg[1].asFloat;
    }, '/1/fader1');

    OSCdef.new(\shaperCompVol, {
      arg msg, time, addr, recvPort;
      // msg[1].asFloat.postln;
      ~shaper_comp.vol = msg[1].asFloat;
    }, '/1/fader2');

    OSCdef.new(\compVol, {
      arg msg, time, addr, recvPort;
      // msg[1].asFloat.postln;
      ~comp.vol = msg[1].asFloat;
    }, '/1/fader3');

    OSCdef.new(\lpcVol, {
      arg msg, time, addr, recvPort;
      // msg[1].asFloat.postln;
      ~lpc.vol = msg[1].asFloat * 0.04;
    }, '/1/fader4');

    OSCdef.new(\xyval, {
      arg msg, time, addr, recvPort;
      e.samplerRate = msg[2].asFloat * 4;
      e.samplerPan = msg[1].asFloat * 2 - 1;
    }, '/1/xy1');

    OSCdef.new(\addSampler, {
      arg msg, time, addr, recvPort;
      if(msg[1] == 1, {
        ~sampler.add(
          {
            Pan2.ar(
              PlayBuf.ar(1, e.buf.voice, e.samplerRate, loop: 1, doneAction: 2), e.samplerPan);

        });
      });
    }, '/1/push2');

     OSCdef.new(\removeSampler, {
      arg msg, time, addr, recvPort;
      if(msg[1] == 1, {
        ~sampler.removeLast;
      });
    }, '/1/push3');

    OSCdef.new(\removeAllSampler, {
      arg msg, time, addr, recvPort;
      if(msg[1] == 1, {
        "removing all".postln;
        ~sampler.removeAll;
      });
    }, '/1/push4');


  }.value;

  /* ==== Update the vertical line ==== */
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

  /* ==== Update pitch and send to David ==== */
  AppClock.sched(0, {
    var new_pitch, cond, freq, base_index, freq_array, amp;
    freq = ~pitch_scaler.bus.getSynchronous;
    amp = ~amp_scaler.bus.getSynchronous;
    base_index = e.quantizePitch(freq, e.current_scale);
    ~klang_amp = Array.geom(7, 1, amp / 1);
    new_pitch = e.notes.wrapAt(base_index);
    e.current_pitch.value_(new_pitch);
    ~klang_freq = ((e.chord.wrapAt(base_index))
      .collect({|i| e.current_scale.degreeToFreq(i, e.root, 0) }));

    // Enable this if we're sending to David
    if (e.networked == true, {
      e.lremote.sendMsg('/pitch/current', base_index, e.notes_david.wrapAt(base_index));
    });
    0.05;
  });

  if (e.networked == true, {
    e.lremote.sendMsg('/bus/new', "analysis_driver", ~analysis_driver.bus.rate, ~analysis_driver.bus.index, ~analysis_driver.bus.numChannels);
  });
  e.proxy_space.gui;
};
