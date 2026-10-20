// Verve Audio Engine (Multi-Tone Synthesizer & Custom Audio Player)
class VerveAudioEngine {
  constructor() {
    this.audioCtx = null;
    this.soundEnabled = true;
    this.alarmLoopTimer = null;
    this.isAlarmRinging = false;
    this.activeToneId = 'radiant-dawn';

    // Custom Audio Element for user uploaded tones
    this.customAudioPlayer = null;
    this.previewAudioPlayer = null;
    this.previewTimer = null;
    this.isPreviewing = false;
  }

  init() {
    if (!this.audioCtx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (AudioContext) {
        this.audioCtx = new AudioContext();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
  }

  toggleSound(enable) {
    this.soundEnabled = enable !== undefined ? enable : !this.soundEnabled;
    if (!this.soundEnabled) {
      this.stopAlarmSound();
      this.stopPreview();
    }
    if (window.verveStorage) {
      window.verveStorage.updateSettings({ soundEnabled: this.soundEnabled });
    }
    return this.soundEnabled;
  }

  // Soft tactile UI click feedback
  playClick() {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.audioCtx) return;

    try {
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(800, this.audioCtx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(300, this.audioCtx.currentTime + 0.04);

      gain.gain.setValueAtTime(0.08, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + 0.04);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start();
      osc.stop(this.audioCtx.currentTime + 0.04);
    } catch (e) {}
  }

  // Barcode laser scan confirmation beep
  playScanBeep() {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.audioCtx) return;

    try {
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(1760, this.audioCtx.currentTime);

      gain.gain.setValueAtTime(0.12, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + 0.12);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start();
      osc.stop(this.audioCtx.currentTime + 0.12);
    } catch (e) {}
  }

  // Error buzz for incorrect answer or wrong barcode scan
  playErrorBuzz() {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.audioCtx) return;

    try {
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();

      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(140, this.audioCtx.currentTime);
      osc.frequency.linearRampToValueAtTime(110, this.audioCtx.currentTime + 0.22);

      gain.gain.setValueAtTime(0.15, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + 0.25);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start();
      osc.stop(this.audioCtx.currentTime + 0.25);
    } catch (e) {}
  }

  // Success fanfare chime for verified wakefulness
  playSuccessChime() {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.audioCtx) return;

    try {
      const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
      notes.forEach((freq, idx) => {
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, this.audioCtx.currentTime + idx * 0.08);

        gain.gain.setValueAtTime(0, this.audioCtx.currentTime + idx * 0.08);
        gain.gain.linearRampToValueAtTime(0.12, this.audioCtx.currentTime + idx * 0.08 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + idx * 0.08 + 0.35);

        osc.connect(gain);
        gain.connect(this.audioCtx.destination);

        osc.start(this.audioCtx.currentTime + idx * 0.08);
        osc.stop(this.audioCtx.currentTime + idx * 0.08 + 0.35);
      });
    } catch (e) {}
  }

  // ================= TONE SELECTION & ALARM PLAYBACK =================
  setTone(toneId) {
    this.activeToneId = toneId;
    if (window.verveStorage) {
      window.verveStorage.updateSettings({ selectedToneId: toneId });
    }
  }

  getActiveTone() {
    if (window.verveStorage) {
      const settings = window.verveStorage.getSettings();
      return settings.selectedToneId || this.activeToneId;
    }
    return this.activeToneId;
  }

  // Start continuous alarm ringing using chosen built-in or custom user tone
  startAlarmSound(specificToneId = null) {
    if (!this.soundEnabled || this.isAlarmRinging) return;
    this.isAlarmRinging = true;

    const toneId = specificToneId || this.getActiveTone();
    const customTones = window.verveStorage ? window.verveStorage.getCustomTones() : [];
    const customTone = customTones.find(t => t.id === toneId);

    if (customTone && customTone.dataUrl) {
      // Play user's uploaded custom audio file
      this.playCustomAudioLoop(customTone.dataUrl);
    } else {
      // Play built-in studio synthesized tone
      this.startSynthesizedLoop(toneId);
    }
  }

  // Play custom audio data URL with seamless looping
  playCustomAudioLoop(dataUrl) {
    try {
      if (this.customAudioPlayer) {
        this.customAudioPlayer.pause();
        this.customAudioPlayer = null;
      }
      this.customAudioPlayer = new Audio(dataUrl);
      this.customAudioPlayer.loop = true;
      this.customAudioPlayer.volume = 0.95;
      const playPromise = this.customAudioPlayer.play();
      if (playPromise !== undefined) {
        playPromise.catch(err => {
          console.warn('Custom tone autoplay restricted, falling back to synthesizer:', err);
          this.startSynthesizedLoop('radiant-dawn');
        });
      }
    } catch (err) {
      console.error('Error playing custom audio tone:', err);
      this.startSynthesizedLoop('radiant-dawn');
    }
  }

  // Synthesizer pattern selector
  startSynthesizedLoop(toneId) {
    const playPulse = () => {
      if (!this.isAlarmRinging || !this.soundEnabled) return;
      this.init();
      if (!this.audioCtx) return;

      try {
        switch (toneId) {
          case 'terracotta-chimes':
            this.synthTerracottaChimes();
            break;
          case 'pulse-siren':
            this.synthPulseSiren();
            break;
          case 'zen-marimba':
            this.synthZenMarimba();
            break;
          case 'radiant-dawn':
          default:
            this.synthRadiantDawn();
            break;
        }
      } catch (e) {}

      const interval = toneId === 'pulse-siren' ? 700 : 1300;
      this.alarmLoopTimer = setTimeout(playPulse, interval);
    };

    playPulse();
  }

  synthRadiantDawn() {
    const chords = [
      [440, 554.37, 659.25], // A major
      [493.88, 622.25, 739.99] // B major
    ];
    const chord = chords[Math.floor(Date.now() / 1500) % chords.length];
    chord.forEach(freq => {
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, this.audioCtx.currentTime);
      gain.gain.setValueAtTime(0.045, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + 0.85);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start();
      osc.stop(this.audioCtx.currentTime + 0.85);
    });
  }

  synthTerracottaChimes() {
    const notes = [659.25, 830.61, 987.77, 1318.51];
    notes.forEach((freq, idx) => {
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, this.audioCtx.currentTime + idx * 0.1);
      gain.gain.setValueAtTime(0.04, this.audioCtx.currentTime + idx * 0.1);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + idx * 0.1 + 0.6);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start(this.audioCtx.currentTime + idx * 0.1);
      osc.stop(this.audioCtx.currentTime + idx * 0.1 + 0.6);
    });
  }

  synthPulseSiren() {
    const osc = this.audioCtx.createOscillator();
    const gain = this.audioCtx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(400, this.audioCtx.currentTime);
    osc.frequency.linearRampToValueAtTime(800, this.audioCtx.currentTime + 0.25);
    osc.frequency.linearRampToValueAtTime(400, this.audioCtx.currentTime + 0.5);
    gain.gain.setValueAtTime(0.06, this.audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + 0.55);
    osc.connect(gain);
    gain.connect(this.audioCtx.destination);
    osc.start();
    osc.stop(this.audioCtx.currentTime + 0.55);
  }

  synthZenMarimba() {
    const chords = [
      [261.63, 329.63, 392.00], // C4, E4, G4
      [293.66, 369.99, 440.00]  // D4, F#4, A4
    ];
    const chord = chords[Math.floor(Date.now() / 2000) % chords.length];
    chord.forEach((freq, idx) => {
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, this.audioCtx.currentTime + idx * 0.08);
      gain.gain.setValueAtTime(0.05, this.audioCtx.currentTime + idx * 0.08);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + idx * 0.08 + 0.45);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start(this.audioCtx.currentTime + idx * 0.08);
      osc.stop(this.audioCtx.currentTime + idx * 0.08 + 0.45);
    });
  }

  // Stop alarm sound (both custom player and synth timer)
  stopAlarmSound() {
    this.isAlarmRinging = false;
    if (this.alarmLoopTimer) {
      clearTimeout(this.alarmLoopTimer);
      this.alarmLoopTimer = null;
    }
    if (this.customAudioPlayer) {
      this.customAudioPlayer.pause();
      this.customAudioPlayer.currentTime = 0;
      this.customAudioPlayer = null;
    }
  }

  // ================= TONE AUDITION & PREVIEW =================
  previewTone(toneId, onEnded) {
    this.stopPreview();
    this.isPreviewing = true;

    const customTones = window.verveStorage ? window.verveStorage.getCustomTones() : [];
    const customTone = customTones.find(t => t.id === toneId);

    if (customTone && customTone.dataUrl) {
      try {
        this.previewAudioPlayer = new Audio(customTone.dataUrl);
        this.previewAudioPlayer.volume = 0.85;
        this.previewAudioPlayer.play();
        this.previewTimer = setTimeout(() => {
          this.stopPreview();
          if (onEnded) onEnded();
        }, 5000);
        this.previewAudioPlayer.onended = () => {
          this.stopPreview();
          if (onEnded) onEnded();
        };
      } catch (e) {
        console.error('Preview error:', e);
      }
    } else {
      // Play 1 cycle of built-in synth
      this.init();
      if (!this.audioCtx) return;
      try {
        switch (toneId) {
          case 'terracotta-chimes':
            this.synthTerracottaChimes();
            break;
          case 'pulse-siren':
            this.synthPulseSiren();
            break;
          case 'zen-marimba':
            this.synthZenMarimba();
            break;
          case 'radiant-dawn':
          default:
            this.synthRadiantDawn();
            break;
        }
      } catch (e) {}

      this.previewTimer = setTimeout(() => {
        this.stopPreview();
        if (onEnded) onEnded();
      }, 2000);
    }
  }

  stopPreview() {
    this.isPreviewing = false;
    if (this.previewTimer) {
      clearTimeout(this.previewTimer);
      this.previewTimer = null;
    }
    if (this.previewAudioPlayer) {
      this.previewAudioPlayer.pause();
      this.previewAudioPlayer.currentTime = 0;
      this.previewAudioPlayer = null;
    }
  }
}

window.verveAudio = new VerveAudioEngine();
// Compatibility aliases
window.soundEngine = window.verveAudio;
