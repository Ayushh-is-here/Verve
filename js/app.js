// Verve Application Controller (Editorial Broadsheet Edition)
// Comprehensive Production-Ready Architecture: Streaks, Custom Tones, Localhost Agentation, Hardened Missions

class VerveApp {
  constructor() {
    this.currentScreen = 'home';
    this.viewMode = 'simulator';

    // Cadence / Alarm State
    this.editTime = { hour: 6, minute: 30, ampm: 'AM' };
    this.editDays = new Set(['M', 'T', 'W', 'T', 'F']);
    this.selectedMission = {
      type: 'barcode',
      title: 'Physical Barcode Scan',
      location: 'Master Bath (34m Away)',
      item: 'Ensuite Colgate Tube'
    };
    this.selectedToneId = 'radiant-dawn';
    this.activeAlarmTitle = 'Executive Awakening';
    this.currentMissionType = 'barcode';
    this.isAuditioning = false;

    // Math mission state (3 progressive verified problems)
    this.mathProblems = [
      { equation: '(14 × 6) + 37', ans: 121, step: 'STEP 1/3', num: 'EQUATION 01' },
      { equation: '(8 × 9) - 27', ans: 45, step: 'STEP 2/3', num: 'EQUATION 02' },
      { equation: '(15 × 5) + 38', ans: 113, step: 'STEP 3/3', num: 'EQUATION 03' }
    ];
    this.currentMathIndex = 0;
    this.currentMathInput = '';

    // Shake mission state (Starts strictly at 0, requires 30 reps)
    this.currentShakeCount = 0;
    this.shakeTarget = 30;
  }

  init() {
    this.startLiveClock();
    this.renderStreakTelemetry();
    this.renderAlarmsList();
    this.renderToneChips();
    this.setupNavigation();
    this.setupOnboarding();
    this.setupPermissions();
    this.setupCreateCadence();
    this.setupCustomToneControls();
    this.setupSwitches();
    this.setupRingingScreen();
    this.setupMissions();
    this.setupAuditModal();
    this.setupGalleryMode();

    // Subscribe to database-free storage changes for real-time reactivity
    if (window.verveStorage) {
      window.verveStorage.subscribe('streaks', (stats) => {
        this.renderStreakTelemetry(stats);
      });
      window.verveStorage.subscribe('alarms', () => {
        this.renderAlarmsList();
      });
      window.verveStorage.subscribe('custom_tones', () => {
        this.renderToneChips();
      });
    }

    const hash = window.location.hash.replace('#', '');
    if (hash && document.getElementById(`screen-${hash}`)) {
      this.navigateToScreen(hash);
    } else {
      this.navigateToScreen('home');
    }
  }

  // Real-time Android Status Bar Clock
  startLiveClock() {
    const updateTime = () => {
      const now = new Date();
      let hours = now.getHours();
      const minutes = String(now.getMinutes()).padStart(2, '0');
      hours = hours % 12 || 12;
      const clockStr = `${hours}:${minutes} CET`;

      document.querySelectorAll('.status-clock').forEach(el => {
        el.textContent = clockStr;
      });
    };
    updateTime();
    setInterval(updateTime, 10000);
  }

  // ================= STREAKS & TELEMETRY =================
  renderStreakTelemetry(providedStats) {
    if (!window.verveStorage) return;
    const stats = providedStats || window.verveStorage.getStreakStats();

    const streakCountEl = document.getElementById('home-streak-count');
    const longestStreakEl = document.getElementById('home-longest-streak');
    const totalWakeupsEl = document.getElementById('home-total-wakeups');
    const statusBadgeEl = document.getElementById('home-streak-status-badge');
    const successStreakEl = document.getElementById('success-streak-display');

    if (streakCountEl) streakCountEl.textContent = stats.currentStreak || 0;
    if (longestStreakEl) longestStreakEl.textContent = stats.longestStreak || 0;
    if (totalWakeupsEl) totalWakeupsEl.textContent = stats.totalWakeups || 0;

    if (statusBadgeEl) {
      if (stats.isTodayCompleted) {
        statusBadgeEl.textContent = 'TODAY: VERIFIED';
        statusBadgeEl.className = 'tag-badge tag-badge-terracotta';
      } else {
        statusBadgeEl.textContent = 'PENDING MORNING';
        statusBadgeEl.className = 'tag-badge tag-badge-outline';
      }
    }

    if (successStreakEl) {
      successStreakEl.textContent = `${stats.currentStreak} Days Unbroken`;
    }
  }

  recordMissionSuccess(missionType = 'math') {
    if (window.soundEngine) {
      window.soundEngine.stopAlarmSound();
      window.soundEngine.playSuccessChime();
    }
    if (window.verveStorage) {
      const updated = window.verveStorage.recordSuccessfulWakeup(this.activeAlarmTitle, missionType);
      this.renderStreakTelemetry(updated);
    }
    this.navigateToScreen('wakeup-success');
  }

  // ================= DYNAMIC ALARMS ROSTER =================
  renderAlarmsList() {
    const rosterEl = document.getElementById('alarm-roster');
    if (!rosterEl || !window.verveStorage) return;

    const alarms = window.verveStorage.getAlarms();
    const allTones = window.verveStorage.getAllTones();

    rosterEl.innerHTML = '';

    if (alarms.length === 0) {
      rosterEl.innerHTML = `
        <div class="card" style="padding:16px; text-align:center;">
          <p class="font-mono" style="font-size:11px; text-transform:uppercase; color:var(--text-secondary);">No Alarms Configured</p>
          <button class="btn btn-primary" data-navigate="create-alarm" style="margin-top:8px; padding:6px 12px; font-size:11px;">+ Create New Cadence</button>
        </div>
      `;
      return;
    }

    alarms.forEach(alarm => {
      const toneObj = allTones.find(t => t.id === alarm.toneId) || { name: 'Radiant Dawn' };
      const daysStr = alarm.days && alarm.days.length ? alarm.days.join('') : 'ONCE';

      let missionDesc = 'Physical Barcode';
      let missionProtocol = 'Optical Scan';
      if (alarm.missionType === 'math') {
        missionDesc = 'Mental Arithmetic (3 Eqs)';
        missionProtocol = 'Mental Arithmetic';
      } else if (alarm.missionType === 'shake') {
        missionDesc = 'Motion Verify (30 Reps)';
        missionProtocol = 'Accelerometer Peak';
      }

      const article = document.createElement('article');
      article.className = `alarm-article ${alarm.enabled ? '' : 'inactive'}`;
      article.setAttribute('data-alarm-id', alarm.id);

      article.innerHTML = `
        <div style="display:flex; align-items:flex-start; justify-content:space-between;">
          <div style="flex:1; padding-right:12px; cursor:pointer;" class="alarm-info-zone">
            <div style="display:flex; align-items:baseline; gap:4px;">
              <span class="font-serif" style="font-size:36px; line-height:1; color:${alarm.enabled ? 'var(--color-primary)' : 'var(--text-outline)'}; letter-spacing:-0.03em;">${alarm.time}</span>
              <span class="font-mono" style="font-size:12px; text-transform:uppercase; color:${alarm.enabled ? 'var(--text-secondary)' : 'var(--text-outline)'}; font-weight:600;">${alarm.ampm || 'AM'}</span>
            </div>
            <div style="margin-top:8px; display:flex; flex-wrap:wrap; align-items:center; gap:6px;">
              <span class="font-mono" style="font-size:10px; color:${alarm.enabled ? 'var(--color-secondary)' : 'var(--text-outline)'}; text-transform:uppercase; font-weight:700;">${daysStr}</span>
              <span style="font-size:8px; color:var(--text-outline-variant);">•</span>
              <span class="font-mono" style="font-size:10px; color:${alarm.enabled ? 'var(--text-primary)' : 'var(--text-secondary)'}; text-transform:uppercase; letter-spacing:0.04em;">${alarm.label || missionDesc}</span>
              <span style="font-size:8px; color:var(--text-outline-variant);">•</span>
              <span class="font-mono" style="font-size:10px; color:${alarm.enabled ? 'var(--text-secondary)' : 'var(--text-outline)'}; text-transform:uppercase;">${toneObj.name}</span>
            </div>
          </div>
          <!-- Mechanical Slider Switch -->
          <button class="alarm-switch ${alarm.enabled ? '' : 'off'}" type="button" aria-pressed="${alarm.enabled}" aria-label="Toggle ${alarm.time} Alarm">
            <span class="switch-core"></span>
          </button>
        </div>
        <div style="margin-top:12px; padding-top:6px; border-top:var(--border-hairline-subtle); display:flex; align-items:center; justify-content:space-between;">
          <div style="display:flex; align-items:center; gap:8px;">
            <span class="font-mono" style="font-size:10px; text-transform:uppercase; color:var(--text-outline); letter-spacing:0.08em;">Dismiss Protocol</span>
            <span class="font-mono" style="font-size:10px; text-transform:uppercase; color:${alarm.enabled ? 'var(--text-primary)' : 'var(--text-secondary)'}; font-weight:600;">${missionProtocol}</span>
          </div>
          <span class="tag-badge ${alarm.enabled ? 'tag-badge-terracotta' : 'tag-badge-outline'}">${alarm.enabled ? 'Hardened' : 'Standby'}</span>
        </div>
      `;

      // Switch toggle event
      const switchBtn = article.querySelector('.alarm-switch');
      switchBtn?.addEventListener('click', (e) => {
        e.stopPropagation();
        if (window.soundEngine) window.soundEngine.playClick();
        const newState = !alarm.enabled;
        window.verveStorage.toggleAlarm(alarm.id, newState);
        this.renderAlarmsList();
      });

      // Tap on alarm row to trigger live test ring
      const infoZone = article.querySelector('.alarm-info-zone');
      infoZone?.addEventListener('click', () => {
        this.activeAlarmTitle = alarm.label || 'Scheduled Cadence';
        this.selectedToneId = alarm.toneId || 'radiant-dawn';
        this.currentMissionType = alarm.missionType || 'barcode';
        if (window.soundEngine) window.soundEngine.playClick();
        this.navigateToScreen('alarm-ringing');
      });

      rosterEl.appendChild(article);
    });
  }

  // ================= CUSTOM TONE MANAGEMENT & UI =================
  renderToneChips() {
    const container = document.getElementById('tone-chips-container');
    if (!container || !window.verveStorage) return;

    const tones = window.verveStorage.getAllTones();
    container.innerHTML = '';

    tones.forEach(tone => {
      const isSelected = tone.id === this.selectedToneId;
      const chip = document.createElement('div');
      chip.style.cssText = `
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 5px 10px;
        background: ${isSelected ? 'var(--color-primary)' : 'var(--bg-container)'};
        color: ${isSelected ? '#FFFFFF' : 'var(--text-primary)'};
        border: 1px solid ${isSelected ? 'var(--color-primary)' : 'var(--border-hairline)'};
        border-radius: 4px;
        cursor: pointer;
        font-family: var(--font-mono);
        font-size: 10px;
        text-transform: uppercase;
        font-weight: ${isSelected ? '700' : '500'};
        transition: all 0.15s ease;
      `;

      chip.innerHTML = `
        <span>${tone.name}</span>
        ${tone.type === 'custom' ? `
          <button type="button" class="btn-delete-tone" data-tone-id="${tone.id}" style="background:none; border:none; color:${isSelected ? '#FFCCCC' : 'var(--color-secondary)'}; cursor:pointer; padding:0 2px; font-weight:700;" title="Delete Custom Tone">×</button>
        ` : ''}
      `;

      chip.addEventListener('click', (e) => {
        if (e.target.classList.contains('btn-delete-tone')) {
          e.stopPropagation();
          const deleteId = e.target.getAttribute('data-tone-id');
          if (confirm(`Remove custom tone "${tone.name}"?`)) {
            window.verveStorage.deleteCustomTone(deleteId);
            if (this.selectedToneId === deleteId) {
              this.selectedToneId = 'radiant-dawn';
            }
            this.renderToneChips();
          }
          return;
        }

        if (window.soundEngine) window.soundEngine.playClick();
        this.selectedToneId = tone.id;
        this.renderToneChips();
      });

      container.appendChild(chip);
    });

    // Update active tone UI card
    const activeTone = tones.find(t => t.id === this.selectedToneId) || tones[0];
    const titleEl = document.getElementById('cadence-tone-title');
    const typeBadge = document.getElementById('cadence-tone-type-badge');
    const subtitleEl = document.getElementById('cadence-tone-subtitle');

    if (titleEl && activeTone) titleEl.textContent = activeTone.name;
    if (typeBadge && activeTone) {
      typeBadge.textContent = activeTone.type === 'custom' ? 'USER AUDIO' : 'STUDIO SYNTH';
    }
    if (subtitleEl && activeTone) {
      subtitleEl.textContent = activeTone.type === 'custom'
        ? `Custom Upload (${Math.round((activeTone.sizeBytes || 0) / 1024)} KB) · Resilient Vault`
        : 'Logarithmic Ramp 0dB → 84dB';
    }
  }

  setupCustomToneControls() {
    const fileInput = document.getElementById('custom-tone-file-input');
    const triggerBtn = document.getElementById('btn-trigger-upload-tone');
    const statusEl = document.getElementById('custom-tone-upload-status');
    const auditionBtn = document.getElementById('btn-audition-tone');

    if (triggerBtn && fileInput) {
      triggerBtn.addEventListener('click', () => {
        fileInput.click();
      });
    }

    if (fileInput) {
      fileInput.addEventListener('change', (e) => {
        const file = e.target.files && e.target.files[0];
        if (!file) return;

        if (statusEl) {
          statusEl.textContent = `Processing "${file.name}" into local vault...`;
          statusEl.style.color = 'var(--color-secondary)';
        }

        const reader = new FileReader();
        reader.onload = (loadEvent) => {
          try {
            const dataUrl = loadEvent.target.result;
            const cleanName = file.name.replace(/\.[^/.]+$/, '').slice(0, 28);
            const savedTone = window.verveStorage.saveCustomTone(cleanName, dataUrl, file.type, file.size);

            this.selectedToneId = savedTone.id;
            this.renderToneChips();

            if (statusEl) {
              statusEl.textContent = `✓ "${cleanName}" ready as active alarm tone!`;
              statusEl.style.color = 'var(--color-secondary)';
            }

            // Immediately audition newly imported tone
            if (window.soundEngine) {
              this.startAudition(savedTone.id);
            }
          } catch (err) {
            console.error('Audio upload error:', err);
            if (statusEl) {
              statusEl.textContent = `Upload failed: ${err.message}`;
              statusEl.style.color = 'var(--color-error)';
            }
          }
        };

        reader.onerror = () => {
          if (statusEl) {
            statusEl.textContent = 'Could not read audio file.';
            statusEl.style.color = 'var(--color-error)';
          }
        };

        reader.readAsDataURL(file);
      });
    }

    if (auditionBtn) {
      auditionBtn.addEventListener('click', () => {
        if (this.isAuditioning) {
          this.stopAudition();
        } else {
          this.startAudition(this.selectedToneId);
        }
      });
    }
  }

  startAudition(toneId) {
    if (!window.soundEngine) return;
    this.isAuditioning = true;
    const auditionText = document.getElementById('audition-text');
    const auditionIcon = document.getElementById('audition-icon');
    if (auditionText) auditionText.textContent = 'STOP';
    if (auditionIcon) auditionIcon.textContent = 'stop';

    window.soundEngine.previewTone(toneId, () => {
      this.stopAudition();
    });
  }

  stopAudition() {
    this.isAuditioning = false;
    if (window.soundEngine) {
      window.soundEngine.stopPreview();
    }
    const auditionText = document.getElementById('audition-text');
    const auditionIcon = document.getElementById('audition-icon');
    if (auditionText) auditionText.textContent = 'AUDITION';
    if (auditionIcon) auditionIcon.textContent = 'play_arrow';
  }

  // ================= MISSIONS STATE & CONTROLS =================
  resetMathMission() {
    this.currentMathIndex = 0;
    this.currentMathInput = '';
    this.renderMathProblem();
  }

  renderMathProblem() {
    const q = this.mathProblems[this.currentMathIndex] || this.mathProblems[0];
    const stepBadge = document.getElementById('math-step-badge');
    const eqNum = document.getElementById('math-equation-num');
    const eqText = document.getElementById('math-equation-text');
    const inputDisplay = document.getElementById('math-input-display');
    const feedbackMsg = document.getElementById('math-feedback-msg');

    if (stepBadge) stepBadge.textContent = q.step;
    if (eqNum) eqNum.textContent = q.num;
    if (eqText) eqText.textContent = `${q.equation} = ?`;
    if (inputDisplay) inputDisplay.textContent = this.currentMathInput || '_';
    if (feedbackMsg) {
      feedbackMsg.textContent = 'Type solution on keypad';
      feedbackMsg.style.color = 'var(--text-secondary)';
    }
  }

  resetShakeMission() {
    this.currentShakeCount = 0;
    const countEl = document.getElementById('shake-count-display');
    const barEl = document.getElementById('shake-progress-bar');
    const labelEl = document.getElementById('shake-status-label');
    if (countEl) countEl.textContent = `0 / ${this.shakeTarget}`;
    if (barEl) barEl.style.width = '0%';
    if (labelEl) {
      labelEl.textContent = '30 PEAK CYCLES REQUIRED TO DISARM';
      labelEl.style.color = 'var(--text-outline)';
    }
  }

  resetBarcodeMission() {
    const banner = document.getElementById('barcode-mismatch-banner');
    if (banner) banner.style.display = 'none';
  }

  // Screen Switching Architecture
  navigateToScreen(screenId) {
    this.currentScreen = screenId;
    window.location.hash = screenId;

    const alarmMissionScreens = ['alarm-ringing', 'barcode-mission', 'math-mission', 'shake-mission'];
    if (alarmMissionScreens.includes(screenId)) {
      if (window.soundEngine && !window.soundEngine.isAlarmRinging) {
        window.soundEngine.startAlarmSound(this.selectedToneId);
      }
    } else {
      if (window.soundEngine && window.soundEngine.isAlarmRinging) {
        window.soundEngine.stopAlarmSound();
      }
    }

    if (screenId === 'home') {
      this.renderStreakTelemetry();
      this.renderAlarmsList();
    } else if (screenId === 'math-mission') {
      this.resetMathMission();
    } else if (screenId === 'shake-mission') {
      this.resetShakeMission();
    } else if (screenId === 'barcode-mission') {
      this.resetBarcodeMission();
    } else if (screenId === 'create-alarm') {
      this.renderToneChips();
    } else if (screenId === 'alarm-ringing') {
      const toneReadout = document.getElementById('ringing-tone-readout');
      const allTones = window.verveStorage ? window.verveStorage.getAllTones() : [];
      const currentTone = allTones.find(t => t.id === this.selectedToneId) || { name: 'Radiant Dawn' };
      if (toneReadout) {
        toneReadout.textContent = `Tone: ${currentTone.name}`;
      }
    }

    document.querySelectorAll('.phone-screen-container .app-screen').forEach(screen => {
      screen.classList.remove('active');
    });

    const targetScreen = document.getElementById(`screen-${screenId}`);
    if (targetScreen) {
      targetScreen.classList.add('active');
      targetScreen.scrollTop = 0;
    }

    // Update Sidebar Navigation
    document.querySelectorAll('.screen-nav-item').forEach(item => {
      const target = item.getAttribute('data-target');
      if (target === screenId) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    // Update Bottom Nav active state
    const tabMap = {
      'home': 'alarms',
      'mission-picker': 'missions',
      'stats': 'protocol',
      'settings': 'settings'
    };
    const activeTab = tabMap[screenId];
    if (activeTab) {
      document.querySelectorAll('.bottom-nav .nav-item').forEach(nav => {
        if (nav.getAttribute('data-tab') === activeTab) {
          nav.classList.add('active');
        } else {
          nav.classList.remove('active');
        }
      });
    }
  }

  // Navigation Links
  setupNavigation() {
    document.querySelectorAll('.screen-nav-item').forEach(item => {
      item.addEventListener('click', () => {
        const target = item.getAttribute('data-target');
        if (window.soundEngine) window.soundEngine.playClick();
        this.navigateToScreen(target);
      });
    });

    document.querySelectorAll('[data-navigate]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const target = btn.getAttribute('data-navigate');
        if (window.soundEngine) window.soundEngine.playClick();
        this.navigateToScreen(target);
      });
    });

    // Test tone button
    document.getElementById('btn-test-tone')?.addEventListener('click', () => {
      if (window.soundEngine) {
        window.soundEngine.previewTone(this.selectedToneId);
      }
    });
  }

  // Screen 1: Onboarding Carousel
  setupOnboarding() {
    let currentSlide = 0;
    const slides = document.querySelectorAll('.onboarding-slide');
    const dots = document.querySelectorAll('.onboard-dot');
    const nextBtn = document.getElementById('btn-onboard-next');
    const skipBtn = document.getElementById('btn-onboard-skip');

    const showSlide = (idx) => {
      currentSlide = idx;
      slides.forEach((s, i) => s.classList.toggle('active', i === idx));
      dots.forEach((d, i) => d.classList.toggle('active', i === idx));
    };

    dots.forEach((dot, idx) => {
      dot.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        showSlide(idx);
      });
    });

    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        if (currentSlide < slides.length - 1) {
          showSlide(currentSlide + 1);
        } else {
          this.navigateToScreen('permissions');
        }
      });
    }

    if (skipBtn) {
      skipBtn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        this.navigateToScreen('permissions');
      });
    }
  }

  // Screen 2: Permissions
  setupPermissions() {
    document.querySelectorAll('.perm-checkbox').forEach(box => {
      box.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        box.classList.toggle('checked');
      });
    });

    document.getElementById('btn-grant-permissions')?.addEventListener('click', () => {
      if (window.soundEngine) window.soundEngine.playSuccessChime();
      this.navigateToScreen('home');
    });
  }

  // Mechanical Tactile Switches
  setupSwitches() {
    document.querySelectorAll('.alarm-switch').forEach(button => {
      button.addEventListener('click', (e) => {
        e.stopPropagation();
        if (window.soundEngine) window.soundEngine.playClick();
        const isPressed = button.getAttribute('aria-pressed') === 'true';
        const newPressed = !isPressed;
        button.setAttribute('aria-pressed', String(newPressed));
        button.classList.toggle('off', !newPressed);

        const article = button.closest('article');
        if (article) {
          article.classList.toggle('inactive', !newPressed);
        }
      });
    });
  }

  // Screen 4: Create / Edit Cadence
  setupCreateCadence() {
    const sliderHours = document.getElementById('slider-hours');
    const sliderMins = document.getElementById('slider-mins');
    const displayHours = document.getElementById('display-hours');
    const displayMins = document.getElementById('display-mins');
    const valHour = document.getElementById('val-hour');
    const valMin = document.getElementById('val-min');

    const updateTimeFromSliders = () => {
      if (!sliderHours || !sliderMins) return;
      const h = String(sliderHours.value).padStart(2, '0');
      const m = String(sliderMins.value).padStart(2, '0');
      if (displayHours) displayHours.textContent = h;
      if (displayMins) displayMins.textContent = m;
      if (valHour) valHour.textContent = `${h} H`;
      if (valMin) valMin.textContent = `${m} M`;
      this.editTime.hour = parseInt(sliderHours.value, 10);
      this.editTime.minute = parseInt(sliderMins.value, 10);
    };

    sliderHours?.addEventListener('input', updateTimeFromSliders);
    sliderMins?.addEventListener('input', updateTimeFromSliders);

    // AM/PM Toggle
    const toggleAm = document.getElementById('toggle-am');
    const togglePm = document.getElementById('toggle-pm');
    if (toggleAm && togglePm) {
      toggleAm.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        toggleAm.className = 'btn btn-primary';
        togglePm.className = 'btn btn-outline';
        this.editTime.ampm = 'AM';
      });
      togglePm.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        togglePm.className = 'btn btn-primary';
        toggleAm.className = 'btn btn-outline';
        this.editTime.ampm = 'PM';
      });
    }

    // Day Matrix
    document.querySelectorAll('#day-selector .day-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        btn.classList.toggle('active');
        const day = btn.getAttribute('data-day');
        if (btn.classList.contains('active')) {
          this.editDays.add(day);
        } else {
          this.editDays.delete(day);
        }
      });
    });

    // Save Button -> Persists to Database-Free Verve Storage
    document.getElementById('btn-save-alarm')?.addEventListener('click', () => {
      const titleInput = document.getElementById('cadence-title');
      const label = titleInput && titleInput.value.trim() ? titleInput.value.trim() : 'Active Cadence';
      const timeStr = `${String(this.editTime.hour).padStart(2, '0')}:${String(this.editTime.minute).padStart(2, '0')}`;

      const newAlarm = {
        time: timeStr,
        ampm: this.editTime.ampm,
        hour: this.editTime.hour,
        minute: this.editTime.minute,
        label: label,
        days: Array.from(this.editDays),
        enabled: true,
        missionType: this.selectedMission.type,
        toneId: this.selectedToneId || 'radiant-dawn',
        snoozeCount: 0
      };

      if (window.verveStorage) {
        window.verveStorage.saveAlarm(newAlarm);
      }
      if (window.soundEngine) window.soundEngine.playSuccessChime();
      this.renderAlarmsList();
      this.navigateToScreen('home');
    });
  }

  // Screen 7: Active Ringing Screen Live Mechanics
  setupRingingScreen() {
    const secEl = document.getElementById('live-seconds');
    let currentSec = 14;
    setInterval(() => {
      currentSec = (currentSec + 1) % 60;
      if (secEl) {
        secEl.textContent = ':' + (currentSec < 10 ? '0' : '') + currentSec;
      }
    }, 1000);

    // Equalizer bars dynamic fluctuation
    const waveform = document.getElementById('waveform-container');
    const dbReadout = document.getElementById('decibel-readout');
    if (waveform) {
      const bars = waveform.children;
      setInterval(() => {
        if (this.currentScreen !== 'alarm-ringing') return;
        const randomBar = bars[Math.floor(Math.random() * bars.length)];
        const heights = ['12px', '20px', '28px', '36px', '40px', '44px'];
        randomBar.style.height = heights[Math.floor(Math.random() * heights.length)];

        if (dbReadout) {
          const randomDb = (83.8 + Math.random() * 1.4).toFixed(1);
          dbReadout.textContent = `${randomDb} dB`;
        }
      }, 140);
    }

    // Emergency Hold 3s Button
    const emergBtn = document.getElementById('emergency-btn');
    const holdProgress = document.getElementById('hold-progress');
    let holdTimer = null;
    let holdDuration = 0;

    if (emergBtn && holdProgress) {
      const startHold = () => {
        holdDuration = 0;
        holdProgress.style.width = '0%';
        holdTimer = setInterval(() => {
          holdDuration += 100;
          const pct = Math.min(100, (holdDuration / 3000) * 100);
          holdProgress.style.width = `${pct}%`;
          if (holdDuration >= 3000) {
            clearInterval(holdTimer);
            holdTimer = null;
            this.recordMissionSuccess('emergency');
          }
        }, 100);
      };

      const cancelHold = () => {
        if (holdTimer) {
          clearInterval(holdTimer);
          holdTimer = null;
          if (holdDuration > 250 && holdDuration < 3000) {
            if (window.soundEngine) window.soundEngine.playErrorBuzz();
          }
        }
        holdProgress.style.width = '0%';
        holdDuration = 0;
      };

      emergBtn.addEventListener('mousedown', startHold);
      emergBtn.addEventListener('touchstart', startHold, { passive: true });
      emergBtn.addEventListener('mouseup', cancelHold);
      emergBtn.addEventListener('mouseleave', cancelHold);
      emergBtn.addEventListener('touchend', cancelHold);
    }
  }

  // Missions & Verification
  setupMissions() {
    // 1. Barcode Verification: Correct vs Mismatched
    const confirmScanBtn = document.getElementById('btn-confirm-scan');
    if (confirmScanBtn) {
      confirmScanBtn.addEventListener('click', () => {
        if (window.soundEngine) {
          window.soundEngine.playScanBeep();
        }
        this.recordMissionSuccess('barcode');
      });
    }

    const wrongScanBtn = document.getElementById('btn-test-wrong-scan');
    if (wrongScanBtn) {
      wrongScanBtn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playErrorBuzz();
        const banner = document.getElementById('barcode-mismatch-banner');
        if (banner) {
          banner.style.display = 'flex';
          banner.classList.remove('anim-error-shake');
          void banner.offsetWidth;
          banner.classList.add('anim-error-shake');
        }
        const lens = document.querySelector('#screen-barcode-mission .camera-viewfinder-lens');
        if (lens) {
          lens.classList.remove('anim-error-shake');
          void lens.offsetWidth;
          lens.classList.add('anim-error-shake');
          setTimeout(() => lens.classList.remove('anim-error-shake'), 450);
        }
      });
    }

    // Torch Toggle
    const torchBtn = document.getElementById('torch-toggle');
    if (torchBtn) {
      torchBtn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        torchBtn.classList.toggle('btn-primary');
        torchBtn.classList.toggle('btn-outline');
      });
    }

    // 2. Math Mission Keypad & Strict Answer Verification
    const mathDisplay = document.getElementById('math-input-display');
    const feedback = document.getElementById('math-feedback-msg');

    document.querySelectorAll('.keypad-btn[data-key]').forEach(btn => {
      btn.addEventListener('click', () => {
        const k = btn.getAttribute('data-key');
        if (window.soundEngine) window.soundEngine.playClick();
        if (k === 'clear') {
          this.currentMathInput = '';
          if (feedback) {
            feedback.textContent = 'Cleared. Enter new solution';
            feedback.style.color = 'var(--text-secondary)';
          }
        } else {
          if (this.currentMathInput.length < 5) {
            this.currentMathInput += k;
          }
          if (feedback && feedback.textContent.includes('Incorrect')) {
            feedback.textContent = 'Type solution on keypad';
            feedback.style.color = 'var(--text-secondary)';
          }
        }
        if (mathDisplay) {
          mathDisplay.textContent = this.currentMathInput || '_';
        }
      });
    });

    document.getElementById('btn-submit-math')?.addEventListener('click', () => {
      const currentQ = this.mathProblems[this.currentMathIndex];
      const userVal = parseInt(this.currentMathInput, 10);
      const card = document.querySelector('#screen-math-mission .card');

      if (!isNaN(userVal) && userVal === currentQ.ans) {
        // Correct solution!
        if (window.soundEngine) window.soundEngine.playClick();
        this.currentMathIndex++;

        if (this.currentMathIndex < this.mathProblems.length) {
          // Advance to next problem in sequence
          if (feedback) {
            feedback.textContent = 'Correct! Next equation loading...';
            feedback.style.color = 'var(--color-secondary)';
          }
          this.currentMathInput = '';
          if (card) {
            card.classList.add('anim-success-flourish');
            setTimeout(() => card.classList.remove('anim-success-flourish'), 350);
          }
          setTimeout(() => {
            this.renderMathProblem();
          }, 280);
        } else {
          // All 3 problems verified!
          if (feedback) {
            feedback.textContent = 'All equations verified! Disarming alarm...';
            feedback.style.color = 'var(--color-secondary)';
          }
          setTimeout(() => {
            this.recordMissionSuccess('math');
          }, 350);
        }
      } else {
        // Strictly incorrect: block dismissal, buzz, and shake
        if (window.soundEngine) window.soundEngine.playErrorBuzz();
        if (card) {
          card.classList.remove('anim-error-shake');
          void card.offsetWidth;
          card.classList.add('anim-error-shake');
          setTimeout(() => card.classList.remove('anim-error-shake'), 450);
        }
        if (feedback) {
          feedback.textContent = `Incorrect (${this.currentMathInput || 'empty'}). Alarm still active!`;
          feedback.style.color = 'var(--color-error)';
        }
        this.currentMathInput = '';
        if (mathDisplay) mathDisplay.textContent = '_';
      }
    });

    // 3. Shake Mission Simulator (Starts at 0, strictly accumulates to 30)
    document.getElementById('btn-simulate-shake')?.addEventListener('click', () => {
      if (window.soundEngine) window.soundEngine.playClick();
      this.currentShakeCount = Math.min(this.shakeTarget, this.currentShakeCount + 5);

      const countEl = document.getElementById('shake-count-display');
      const barEl = document.getElementById('shake-progress-bar');
      const labelEl = document.getElementById('shake-status-label');
      const phoneIcon = document.getElementById('shake-phone-icon');

      if (phoneIcon) {
        phoneIcon.classList.remove('anim-phone-wobble');
        void phoneIcon.offsetWidth;
        phoneIcon.classList.add('anim-phone-wobble');
      }

      if (countEl) countEl.textContent = `${this.currentShakeCount} / ${this.shakeTarget}`;
      if (barEl) {
        const pct = Math.min(100, (this.currentShakeCount / this.shakeTarget) * 100);
        barEl.style.width = `${pct}%`;
      }

      if (this.currentShakeCount < this.shakeTarget) {
        if (labelEl) {
          labelEl.textContent = `${this.shakeTarget - this.currentShakeCount} PEAK REPS REMAINING TO DISARM`;
          labelEl.style.color = 'var(--color-secondary)';
        }
      } else {
        if (labelEl) {
          labelEl.textContent = 'REPS COMPLETE! DISENGAGING ALARM...';
          labelEl.style.color = 'var(--color-secondary)';
        }
        setTimeout(() => {
          this.recordMissionSuccess('shake');
        }, 350);
      }
    });

    // Barcode Setup Simulator
    document.getElementById('btn-simulate-barcode-scan')?.addEventListener('click', () => {
      if (window.soundEngine) window.soundEngine.playScanBeep();
      const card = document.getElementById('barcode-scan-result-card');
      if (card) card.style.display = 'flex';
    });

    document.getElementById('btn-save-barcode')?.addEventListener('click', () => {
      if (window.soundEngine) window.soundEngine.playSuccessChime();
      this.navigateToScreen('create-alarm');
    });

    // Mission Catalog Selection
    document.querySelectorAll('[data-select-mission]').forEach(card => {
      card.addEventListener('click', () => {
        const mtype = card.getAttribute('data-select-mission');
        if (mtype) {
          this.selectedMission.type = mtype;
          this.currentMissionType = mtype;
        }
        if (window.soundEngine) window.soundEngine.playClick();
        this.navigateToScreen('create-alarm');
      });
    });

    // Battery Modal
    const batteryRow = document.getElementById('settings-battery-row');
    const batteryModal = document.getElementById('battery-guide-modal');
    const closeBattery = document.getElementById('btn-close-battery-modal');
    if (batteryRow && batteryModal) {
      batteryRow.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        batteryModal.style.display = 'flex';
      });
    }
    if (closeBattery && batteryModal) {
      closeBattery.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        batteryModal.style.display = 'none';
      });
    }
  }

  // ================= AUDIT LOG MODAL =================
  setupAuditModal() {
    const btn = document.getElementById('btn-sync-telemetry');
    if (!btn) return;

    btn.addEventListener('click', () => {
      if (window.soundEngine) window.soundEngine.playClick();
      const stats = window.verveStorage ? window.verveStorage.getStreakStats() : {};
      const backupJson = window.verveStorage ? window.verveStorage.exportBackupJson() : '{}';

      let modal = document.getElementById('audit-log-modal');
      if (!modal) {
        modal = document.createElement('div');
        modal.id = 'audit-log-modal';
        modal.style.cssText = 'display:none; position:fixed; top:0; left:0; right:0; bottom:0; background:rgba(0,0,0,0.65); z-index:220; align-items:center; justify-content:center; padding:20px;';
        document.body.appendChild(modal);
      }

      const historyRows = (stats.history || []).map(h => `
        <div style="display:flex; justify-content:space-between; align-items:center; padding:6px 0; border-bottom:1px solid var(--border-hairline-subtle); font-family:var(--font-mono); font-size:10px;">
          <span>${h.date} (${h.time})</span>
          <span style="color:var(--color-secondary); font-weight:700;">${h.missionType.toUpperCase()} · ${h.durationSeconds}s</span>
        </div>
      `).join('');

      modal.innerHTML = `
        <div class="card" style="max-width:440px; width:100%; max-height:85vh; display:flex; flex-direction:column; padding:18px; overflow:hidden;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
            <div style="display:flex; align-items:center; gap:8px;">
              <span class="square-marker"></span>
              <h3 class="font-mono" style="font-size:12px; font-weight:700; text-transform:uppercase;">Verve Streak Audit Log</h3>
            </div>
            <button class="btn btn-ghost" id="btn-close-audit" style="padding:4px;"><span class="material-symbols-outlined">close</span></button>
          </div>
          <div style="font-family:var(--font-mono); font-size:11px; margin-bottom:10px; color:var(--text-secondary);">
            Local Database-Free Storage · Current Streak: <strong style="color:var(--color-secondary);">${stats.currentStreak || 0} Days</strong> · Longest: ${stats.longestStreak || 0} Days
          </div>
          <div style="flex:1; overflow-y:auto; margin-bottom:12px; max-height:220px; padding-right:4px;">
            ${historyRows || '<p style="font-size:11px; color:var(--text-secondary);">No wakeups logged yet.</p>'}
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-outline" id="btn-copy-backup" style="flex:1; font-size:10px; padding:8px;">Copy Vault JSON</button>
            <button class="btn btn-primary" id="btn-done-audit" style="flex:1; font-size:10px; padding:8px;">Done</button>
          </div>
        </div>
      `;

      modal.style.display = 'flex';

      modal.querySelector('#btn-close-audit')?.addEventListener('click', () => { modal.style.display = 'none'; });
      modal.querySelector('#btn-done-audit')?.addEventListener('click', () => { modal.style.display = 'none'; });
      modal.querySelector('#btn-copy-backup')?.addEventListener('click', () => {
        navigator.clipboard?.writeText(backupJson);
        alert('Verve Vault JSON backup copied to clipboard!');
      });
    });
  }

  // Gallery Mode Viewport
  setupGalleryMode() {
    const simBtn = document.getElementById('btn-mode-simulator');
    const galBtn = document.getElementById('btn-mode-gallery');
    const simView = document.getElementById('simulator-viewport-area');
    const galView = document.getElementById('gallery-viewport-area');

    if (simBtn && galBtn && simView && galView) {
      simBtn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        this.viewMode = 'simulator';
        simBtn.classList.add('active');
        galBtn.classList.remove('active');
        simView.style.display = 'flex';
        galView.style.display = 'none';
      });

      galBtn.addEventListener('click', () => {
        if (window.soundEngine) window.soundEngine.playClick();
        this.viewMode = 'gallery';
        galBtn.classList.add('active');
        simBtn.classList.remove('active');
        simView.style.display = 'none';
        galView.style.display = 'block';
        this.populateGalleryGrid();
      });
    }
  }

  populateGalleryGrid() {
    const grid = document.getElementById('all-screens-gallery-grid');
    if (!grid || grid.children.length > 0) return;

    const screenDefinitions = [
      { id: 'onboarding', num: 'Screen 01', title: 'Onboarding (3 Slides)' },
      { id: 'permissions', num: 'Screen 02', title: 'Permissions Checklist' },
      { id: 'home', num: 'Screen 03', title: 'Active Cadence Registers' },
      { id: 'create-alarm', num: 'Screen 04', title: 'Create / Edit Cadence' },
      { id: 'mission-picker', num: 'Screen 05', title: 'Mission Catalog' },
      { id: 'barcode-setup', num: 'Screen 06', title: 'Barcode Registration' },
      { id: 'alarm-ringing', num: 'Screen 07', title: 'Alarm Ringing Monolith' },
      { id: 'barcode-mission', num: 'Screen 08', title: 'Barcode Verification' },
      { id: 'math-mission', num: 'Screen 09', title: 'Mental Math Solver' },
      { id: 'shake-mission', num: 'Screen 10', title: 'Accelerometer Shake' },
      { id: 'wakeup-success', num: 'Screen 11', title: 'Protocol Fulfilled' },
      { id: 'stats', num: 'Screen 12', title: 'Consistency Telemetry' },
      { id: 'settings', num: 'Screen 13', title: 'Hardware Interlock' }
    ];

    screenDefinitions.forEach(def => {
      const card = document.createElement('div');
      card.className = 'gallery-card';
      card.innerHTML = `
        <div class="gallery-card-header">
          <span class="gallery-screen-num">${def.num}</span>
          <span class="gallery-screen-title">${def.title}</span>
        </div>
        <div class="gallery-phone-mini" onclick="window.app.switchToSimulatorAndScreen('${def.id}')" title="Tap to interact with ${def.title}">
          <div class="gallery-mini-screen-wrapper" id="gallery-preview-${def.id}"></div>
          <div class="gallery-hover-overlay">
            <span class="material-symbols-outlined" style="font-size:32px;">touch_app</span>
            <span>Tap to Interact</span>
          </div>
        </div>
      `;
      grid.appendChild(card);

      const targetEl = document.getElementById(`screen-${def.id}`);
      const previewWrapper = card.querySelector(`#gallery-preview-${def.id}`);
      if (targetEl && previewWrapper) {
        const clone = targetEl.cloneNode(true);
        clone.id = `gallery-clone-${def.id}`;
        clone.classList.add('active');
        clone.style.display = 'flex';
        clone.style.position = 'relative';
        clone.style.height = '100%';
        clone.style.pointerEvents = 'none';

        if (def.id === 'onboarding') {
          const s1 = clone.querySelector('.onboarding-slide');
          if (s1) s1.classList.add('active');
        }
        previewWrapper.appendChild(clone);
      }
    });
  }

  switchToSimulatorAndScreen(screenId) {
    const simBtn = document.getElementById('btn-mode-simulator');
    if (simBtn) simBtn.click();
    this.navigateToScreen(screenId);
  }
}

// Global Exports and Initializations
window.VerveApp = VerveApp;
window.WakefullyApp = VerveApp;

document.addEventListener('DOMContentLoaded', () => {
  window.app = new VerveApp();
  window.app.init();
});
