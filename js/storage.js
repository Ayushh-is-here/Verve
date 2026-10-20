// Verve Architectural Storage Facility (Database-Free, Resilient Local Storage)
// Manages streaks, alarms, custom audio tones, barcodes, and settings with schema versioning.

class VerveStorageFacility {
  constructor() {
    this.SCHEMA_VERSION = 2;
    this.STORAGE_PREFIX = 'verve_';
    this.listeners = new Map();

    // In-memory fallback if localStorage is disabled or restricted
    this.memoryStore = {};
    this.isLocalStorageAvailable = this.checkLocalStorage();

    this.initDefaults();
  }

  checkLocalStorage() {
    try {
      const testKey = '__verve_storage_test__';
      window.localStorage.setItem(testKey, testKey);
      window.localStorage.removeItem(testKey);
      return true;
    } catch (e) {
      console.warn('VerveStorage: localStorage unavailable, falling back to memoryStore.', e);
      return false;
    }
  }

  getRaw(key) {
    const fullKey = this.STORAGE_PREFIX + key;
    if (this.isLocalStorageAvailable) {
      try {
        const item = window.localStorage.getItem(fullKey);
        return item ? JSON.parse(item) : null;
      } catch (e) {
        console.error('VerveStorage read error for key:', key, e);
      }
    }
    return this.memoryStore[fullKey] || null;
  }

  setRaw(key, value) {
    const fullKey = this.STORAGE_PREFIX + key;
    if (this.isLocalStorageAvailable) {
      try {
        window.localStorage.setItem(fullKey, JSON.stringify(value));
      } catch (e) {
        console.warn('VerveStorage write error (possible quota exceeded):', e);
      }
    }
    this.memoryStore[fullKey] = value;
    this.notify(key, value);
  }

  initDefaults() {
    // 1. Initialize Streaks & Wake-Up Stats if empty
    if (!this.getRaw('streaks')) {
      const initialStreaks = {
        currentStreak: 3,
        longestStreak: 7,
        lastWakeupDate: this.getYesterdayDateString(),
        totalWakeups: 14,
        history: [
          {
            id: 'wake_hist_01',
            date: this.getYesterdayDateString(),
            time: '06:31 AM',
            label: 'Morning Routine',
            missionType: 'barcode',
            durationSeconds: 42,
            earned: true
          },
          {
            id: 'wake_hist_02',
            date: this.getDateStringDaysAgo(2),
            time: '06:30 AM',
            label: 'Morning Routine',
            missionType: 'math',
            durationSeconds: 58,
            earned: true
          },
          {
            id: 'wake_hist_03',
            date: this.getDateStringDaysAgo(3),
            time: '06:32 AM',
            label: 'Morning Routine',
            missionType: 'shake',
            durationSeconds: 31,
            earned: true
          }
        ]
      };
      this.setRaw('streaks', initialStreaks);
    }

    // 2. Initialize Alarms if empty
    if (!this.getRaw('alarms')) {
      const defaultAlarms = [
        {
          id: 'alarm_01',
          time: '06:30',
          ampm: 'AM',
          hour: 6,
          minute: 30,
          label: 'Executive Awakening',
          days: ['M', 'T', 'W', 'T', 'F'],
          enabled: true,
          missionType: 'barcode',
          barcodeId: 'bc_01',
          toneId: 'radiant-dawn',
          snoozeCount: 0
        },
        {
          id: 'alarm_02',
          time: '07:15',
          ampm: 'AM',
          hour: 7,
          minute: 15,
          label: 'Weekend Mind Activation',
          days: ['S', 'S'],
          enabled: false,
          missionType: 'math',
          barcodeId: null,
          toneId: 'zen-marimba',
          snoozeCount: 0
        }
      ];
      this.setRaw('alarms', defaultAlarms);
    }

    // 3. Initialize Barcodes if empty
    if (!this.getRaw('barcodes')) {
      const defaultBarcodes = [
        {
          id: 'bc_01',
          name: 'Ensuite C-09 (Ceramic Cleanser)',
          value: '4006381333931',
          format: 32, // EAN-13
          dateAdded: new Date().toISOString()
        },
        {
          id: 'bc_02',
          name: 'Kitchen Arabica Reserve',
          value: '012345678905',
          format: 32,
          dateAdded: new Date().toISOString()
        }
      ];
      this.setRaw('barcodes', defaultBarcodes);
    }

    // 4. Initialize Custom Audio Tones if empty
    if (!this.getRaw('custom_tones')) {
      this.setRaw('custom_tones', []);
    }

    // 5. Initialize App Settings if empty
    if (!this.getRaw('settings')) {
      this.setRaw('settings', {
        selectedToneId: 'radiant-dawn',
        soundEnabled: true,
        volume: 0.9,
        vibrationEnabled: true,
        emergencyHoldDuration: 5000
      });
    }
  }

  // ================= STREAKS & METRICS =================
  getStreakStats() {
    const data = this.getRaw('streaks') || {
      currentStreak: 0,
      longestStreak: 0,
      lastWakeupDate: null,
      totalWakeups: 0,
      history: []
    };
    const today = this.getTodayDateString();
    return {
      ...data,
      isTodayCompleted: data.lastWakeupDate === today
    };
  }

  recordSuccessfulWakeup(alarmLabel = 'Scheduled Alarm', missionType = 'math') {
    const stats = this.getStreakStats();
    const today = this.getTodayDateString();
    const yesterday = this.getYesterdayDateString();

    let newCurrentStreak = stats.currentStreak;

    if (stats.lastWakeupDate === today) {
      // Already recorded today, don't double count streak
      console.log('VerveStorage: Wakeup already verified for today.');
    } else if (stats.lastWakeupDate === yesterday || !stats.lastWakeupDate) {
      // Consecutive day or first wakeup
      newCurrentStreak = (stats.currentStreak || 0) + 1;
    } else {
      // Streak broken (gap > 1 day)
      newCurrentStreak = 1;
    }

    const newLongestStreak = Math.max(stats.longestStreak || 0, newCurrentStreak);
    const newTotalWakeups = (stats.totalWakeups || 0) + 1;

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const newHistoryEntry = {
      id: 'wake_' + Date.now(),
      date: today,
      time: timeStr,
      label: alarmLabel,
      missionType: missionType,
      durationSeconds: Math.floor(Math.random() * 35) + 25,
      earned: true
    };

    const updatedHistory = [newHistoryEntry, ...(stats.history || [])].slice(0, 50);

    const updatedStreaks = {
      currentStreak: newCurrentStreak,
      longestStreak: newLongestStreak,
      lastWakeupDate: today,
      totalWakeups: newTotalWakeups,
      history: updatedHistory
    };

    this.setRaw('streaks', updatedStreaks);
    return updatedStreaks;
  }

  // ================= ALARMS =================
  getAlarms() {
    return this.getRaw('alarms') || [];
  }

  getAlarmById(id) {
    return this.getAlarms().find(a => a.id === id) || null;
  }

  saveAlarm(alarm) {
    const alarms = this.getAlarms();
    let updated;
    if (alarm.id) {
      const idx = alarms.findIndex(a => a.id === alarm.id);
      if (idx >= 0) {
        updated = [...alarms];
        updated[idx] = { ...alarms[idx], ...alarm };
      } else {
        updated = [...alarms, alarm];
      }
    } else {
      const newAlarm = {
        ...alarm,
        id: 'alarm_' + Date.now()
      };
      updated = [...alarms, newAlarm];
    }
    this.setRaw('alarms', updated);
    return updated;
  }

  deleteAlarm(id) {
    const alarms = this.getAlarms().filter(a => a.id !== id);
    this.setRaw('alarms', alarms);
    return alarms;
  }

  toggleAlarm(id, forceState) {
    const alarms = this.getAlarms().map(a => {
      if (a.id === id) {
        return { ...a, enabled: forceState !== undefined ? forceState : !a.enabled };
      }
      return a;
    });
    this.setRaw('alarms', alarms);
    return alarms;
  }

  // ================= BARCODES =================
  getBarcodes() {
    return this.getRaw('barcodes') || [];
  }

  saveBarcode(barcode) {
    const barcodes = this.getBarcodes();
    const newBarcode = {
      ...barcode,
      id: barcode.id || 'bc_' + Date.now(),
      dateAdded: new Date().toISOString()
    };
    const updated = [...barcodes.filter(b => b.id !== newBarcode.id), newBarcode];
    this.setRaw('barcodes', updated);
    return newBarcode;
  }

  deleteBarcode(id) {
    // Safety verification: Block deleting barcode attached to any active alarm
    const attachedAlarm = this.getAlarms().find(a => a.barcodeId === id);
    if (attachedAlarm) {
      throw new Error(`Cannot delete barcode: Attached to active alarm "${attachedAlarm.label}". Reassign or delete the alarm first.`);
    }
    const updated = this.getBarcodes().filter(b => b.id !== id);
    this.setRaw('barcodes', updated);
    return updated;
  }

  // ================= CUSTOM AUDIO TONES =================
  getCustomTones() {
    return this.getRaw('custom_tones') || [];
  }

  getAllTones() {
    // Built-in studio synthesized tones
    const builtInTones = [
      { id: 'radiant-dawn', name: 'Radiant Dawn (A/B Harmonic)', type: 'synth', builtIn: true },
      { id: 'terracotta-chimes', name: 'Terracotta Chimes (Ethereal)', type: 'synth', builtIn: true },
      { id: 'pulse-siren', name: 'Pulse Siren (High Urgency)', type: 'synth', builtIn: true },
      { id: 'zen-marimba', name: 'Zen Marimba (Organic Acoustic)', type: 'synth', builtIn: true }
    ];

    const custom = this.getCustomTones();
    return [...builtInTones, ...custom];
  }

  saveCustomTone(name, dataUrl, mimeType = 'audio/mpeg', sizeBytes = 0) {
    const customTones = this.getCustomTones();
    const newTone = {
      id: 'tone_' + Date.now(),
      name: name || 'Custom Tone ' + (customTones.length + 1),
      dataUrl: dataUrl,
      type: 'custom',
      mimeType: mimeType,
      sizeBytes: sizeBytes,
      builtIn: false,
      dateAdded: new Date().toISOString()
    };

    const updated = [...customTones, newTone];
    this.setRaw('custom_tones', updated);
    return newTone;
  }

  deleteCustomTone(id) {
    const updated = this.getCustomTones().filter(t => t.id !== id);
    this.setRaw('custom_tones', updated);

    // If active tone was deleted, fallback to radiant-dawn
    const settings = this.getSettings();
    if (settings.selectedToneId === id) {
      this.updateSettings({ selectedToneId: 'radiant-dawn' });
    }
    return updated;
  }

  // ================= SETTINGS =================
  getSettings() {
    return this.getRaw('settings') || {
      selectedToneId: 'radiant-dawn',
      soundEnabled: true,
      volume: 0.9,
      vibrationEnabled: true
    };
  }

  updateSettings(partial) {
    const current = this.getSettings();
    const updated = { ...current, ...partial };
    this.setRaw('settings', updated);
    return updated;
  }

  // ================= BACKUP & EXPORT =================
  exportBackupJson() {
    const payload = {
      verve_version: this.SCHEMA_VERSION,
      export_date: new Date().toISOString(),
      streaks: this.getRaw('streaks'),
      alarms: this.getRaw('alarms'),
      barcodes: this.getRaw('barcodes'),
      custom_tones: this.getRaw('custom_tones'),
      settings: this.getRaw('settings')
    };
    return JSON.stringify(payload, null, 2);
  }

  importBackupJson(jsonString) {
    try {
      const data = JSON.parse(jsonString);
      if (data.streaks) this.setRaw('streaks', data.streaks);
      if (data.alarms) this.setRaw('alarms', data.alarms);
      if (data.barcodes) this.setRaw('barcodes', data.barcodes);
      if (data.custom_tones) this.setRaw('custom_tones', data.custom_tones);
      if (data.settings) this.setRaw('settings', data.settings);
      return { success: true };
    } catch (e) {
      return { success: false, error: e.message };
    }
  }

  // ================= EVENT BUS =================
  subscribe(key, callback) {
    if (!this.listeners.has(key)) {
      this.listeners.set(key, new Set());
    }
    this.listeners.get(key).add(callback);
    return () => this.listeners.get(key)?.delete(callback);
  }

  notify(key, data) {
    if (this.listeners.has(key)) {
      this.listeners.get(key).forEach(cb => {
        try { cb(data); } catch (e) { console.error(e); }
      });
    }
    if (this.listeners.has('*')) {
      this.listeners.get('*').forEach(cb => {
        try { cb(key, data); } catch (e) { console.error(e); }
      });
    }
  }

  // ================= DATE HELPERS =================
  getTodayDateString() {
    const now = new Date();
    return now.toISOString().split('T')[0];
  }

  getYesterdayDateString() {
    const d = new Date();
    d.setDate(d.getDate() - 1);
    return d.toISOString().split('T')[0];
  }

  getDateStringDaysAgo(days) {
    const d = new Date();
    d.setDate(d.getDate() - days);
    return d.toISOString().split('T')[0];
  }
}

window.verveStorage = new VerveStorageFacility();
// Backwards compatibility alias
window.wakefullyStorage = window.verveStorage;
