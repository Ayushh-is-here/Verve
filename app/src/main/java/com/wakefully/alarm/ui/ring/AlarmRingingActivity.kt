package com.wakefully.alarm.ui.ring

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wakefully.alarm.data.VerveDatabaseRepository
import com.wakefully.alarm.data.VerveRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.AlarmController
import com.wakefully.alarm.service.AlarmDismissalReason
import com.wakefully.alarm.service.AlarmReceiver
import com.wakefully.alarm.service.AlarmService
import com.wakefully.alarm.service.DefaultAlarmController
import com.wakefully.alarm.theme.ThemeManager
import com.wakefully.alarm.theme.VerveTheme
import com.wakefully.alarm.ui.mission.barcode.BarcodeMissionScreen
import com.wakefully.alarm.ui.mission.barcode.BarcodeMissionViewModel
import com.wakefully.alarm.ui.mission.math.MathMissionScreen
import com.wakefully.alarm.ui.mission.math.MathMissionViewModel
import com.wakefully.alarm.ui.success.SuccessScreen

sealed interface RingingHostScreen {
    data object Ringing : RingingHostScreen
    data object MathMission : RingingHostScreen
    data class BarcodeMission(val targetBarcode: RegisteredBarcode?) : RingingHostScreen
    data class Success(val summary: String) : RingingHostScreen
}

/**
 * Dedicated full-screen activity that covers the entire physical screen when an alarm rings,
 * exactly like a traditional system clock alarm.
 *
 * It turns on the screen, displays above keyguard/lockscreen, dismisses keyguard,
 * hides system status and navigation bars for total immersive focus, and disables
 * back button dismissal until the assigned wake-up mission or 5-second emergency hold is verified.
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Configure lockscreen bypass & wake flags BEFORE super.onCreate so Window is prepared with full-screen attributes
        setupLockscreenAndWake()
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_ALARM
        ThemeManager.init(applicationContext)

        // 2. Hide system status & navigation bars for true full-screen immersion
        makeImmersiveFullScreen()

        val repository = VerveDatabaseRepository.getInstance(applicationContext)
        val alarm = resolveAlarmFromIntent(intent, repository)

        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            VerveTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingHost(
                        alarm = alarm,
                        repository = repository,
                        onDismissAlarm = {
                            AlarmService.stopAlarm(this@AlarmRingingActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Intercept all hardware volume buttons to completely disallow lowering or muting the alarm volume
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> super.dispatchKeyEvent(event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupLockscreenAndWake()
        makeImmersiveFullScreen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupLockscreenAndWake()
        makeImmersiveFullScreen()
    }

    override fun onResume() {
        super.onResume()
        setupLockscreenAndWake()
        makeImmersiveFullScreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupLockscreenAndWake()
            makeImmersiveFullScreen()
        }
    }

    private fun setupLockscreenAndWake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        // Apply window flags in addition to O_MR1 methods for complete OEM lockscreen bypass (Samsung, Xiaomi, OnePlus, etc.)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Allow drawing into camera cutout / notch area for 100% full screen coverage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun makeImmersiveFullScreen() {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun resolveAlarmFromIntent(intent: Intent?, repository: VerveDatabaseRepository): Alarm {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L) ?: 0L
        if (alarmId > 0L) {
            val persistedAlarm = repository.getAlarmByIdDirect(alarmId)
            if (persistedAlarm != null) {
                return persistedAlarm
            }
        }

        val alarmLabel = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Morning Awakening"
        val toneName = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_TONE) ?: "Radiant Dawn"
        val toneUri = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_URI)
        val missionTypeName = intent?.getStringExtra(AlarmReceiver.EXTRA_MISSION_TYPE) ?: "MATH"
        val barcodeId = intent?.getLongExtra(AlarmReceiver.EXTRA_BARCODE_ID, -1L)?.takeIf { it >= 0 }
        val hour = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 7) ?: 7
        val minute = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0) ?: 0

        val missionType = try {
            MissionType.valueOf(missionTypeName)
        } catch (_: Exception) {
            MissionType.MATH
        }

        return Alarm(
            id = alarmId,
            hourOfDay = hour,
            minute = minute,
            missionType = missionType,
            barcodeId = barcodeId,
            enabled = true,
            label = alarmLabel,
            toneName = toneName,
            toneUri = toneUri
        )
    }
}

@Composable
fun AlarmRingingHost(
    alarm: Alarm,
    repository: VerveRepository,
    onDismissAlarm: () -> Unit
) {
    // CRITICAL: Block Android system back button while alarm is ringing
    BackHandler(enabled = true) {}

    val alarmController: AlarmController = remember { DefaultAlarmController() }
    var currentScreen by remember { mutableStateOf<RingingHostScreen>(RingingHostScreen.Ringing) }
    val barcodes by repository.getAllBarcodes().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        alarmController.triggerAlarm(alarm)
    }

    when (val screen = currentScreen) {
        RingingHostScreen.Ringing -> {
            RingingScreen(
                alarm = alarm,
                onStartMission = {
                    when (alarm.missionType) {
                        MissionType.BARCODE -> {
                            val target = barcodes.firstOrNull { it.id == alarm.barcodeId }
                                ?: barcodes.firstOrNull()
                            currentScreen = RingingHostScreen.BarcodeMission(target)
                        }
                        MissionType.MATH -> {
                            currentScreen = RingingHostScreen.MathMission
                        }
                        MissionType.SHAKE, MissionType.NONE -> {
                            repository.recordSuccessfulWakeup(alarm.missionType)
                            alarmController.dismissAlarm(
                                AlarmDismissalReason.MissionVerified(alarm.missionType)
                            )
                            currentScreen = RingingHostScreen.Success("Standard Dismissal Completed")
                        }
                    }
                },
                onEmergencyDismiss = {
                    alarmController.dismissAlarm(AlarmDismissalReason.EmergencyHoldCompleted)
                    currentScreen = RingingHostScreen.Success("Emergency 5-Second Override Engaged")
                }
            )
        }

        RingingHostScreen.MathMission -> {
            val mathVm = remember(alarm.id) { MathMissionViewModel() }
            MathMissionScreen(
                viewModel = mathVm,
                onDismissAlarm = {
                    repository.recordSuccessfulWakeup(MissionType.MATH)
                    alarmController.dismissAlarm(
                        AlarmDismissalReason.MissionVerified(MissionType.MATH, "3 Problems Solved")
                    )
                    currentScreen = RingingHostScreen.Success("Mental Math Verified // 3 Problems Solved")
                }
            )
        }

        is RingingHostScreen.BarcodeMission -> {
            val barcodeVm = remember(alarm.id) {
                BarcodeMissionViewModel(
                    alarm = alarm,
                    barcodeRepository = repository,
                    alarmController = alarmController,
                    initialTarget = screen.targetBarcode
                )
            }
            BarcodeMissionScreen(
                viewModel = barcodeVm,
                onFallbackEmergency = {
                    currentScreen = RingingHostScreen.MathMission
                }
            )

            val vmState by barcodeVm.uiState.collectAsState()
            if (vmState.isDismissed) {
                repository.recordSuccessfulWakeup(MissionType.BARCODE)
                currentScreen = RingingHostScreen.Success(
                    "Optical Signature Verified // ${screen.targetBarcode?.name ?: "Barcoded Item"}"
                )
            }
        }

        is RingingHostScreen.Success -> {
            SuccessScreen(
                missionSummary = screen.summary,
                onReturnHome = onDismissAlarm
            )
        }
    }
}
