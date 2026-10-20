// Mission Controller & Compatibility Layer for Wakefully
// All primary verification handlers, sound hooks, and screen state are unified in window.app (js/app.js)

class MissionManager {
  constructor() {
    this.mathQuestions = [
      { expr: '(14 × 6) + 37', ans: '121' },
      { expr: '(8 × 9) - 27', ans: '45' },
      { expr: '(15 × 5) + 38', ans: '113' }
    ];
  }

  init() {
    // Delegates to unified WakefullyApp controller
  }

  get currentMathIdx() {
    return window.app ? window.app.currentMathIndex : 0;
  }

  set currentMathIdx(val) {
    if (window.app) window.app.currentMathIndex = val;
  }

  get mathInput() {
    return window.app ? window.app.currentMathInput : '';
  }

  set mathInput(val) {
    if (window.app) window.app.currentMathInput = val;
  }

  renderMathQuestion() {
    if (window.app) window.app.renderMathProblem();
  }

  handleMathKey(val) {
    if (!window.app) return;
    if (val === 'clear') {
      window.app.currentMathInput = '';
      const display = document.getElementById('math-input-display');
      if (display) display.textContent = '_';
    } else if (val === 'submit') {
      document.getElementById('btn-submit-math')?.click();
    } else {
      if (window.app.currentMathInput.length < 5) {
        window.app.currentMathInput += val;
      }
      const display = document.getElementById('math-input-display');
      if (display) display.textContent = window.app.currentMathInput || '_';
    }
  }
}

window.missionManager = new MissionManager();
