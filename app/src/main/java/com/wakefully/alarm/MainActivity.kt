package com.wakefully.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wakefully.alarm.data.VerveDatabaseRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.service.AlarmReceiver
import com.wakefully.alarm.theme.ThemeManager
import com.wakefully.alarm.theme.VerveTheme
import com.wakefully.alarm.ui.VerveApp
import com.wakefully.alarm.ui.ring.AlarmRingingActivity

class MainActivity : ComponentActivity() {

    private var initialRingingAlarm by mutableStateOf<Alarm?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(applicationContext)
        enableEdgeToEdge()
        setupLockscreenAndWake()

        val repository = VerveDatabaseRepository.getInstance(applicationContext)
        parseRingingIntent(intent, repository)

        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            VerveTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val repo = remember { repository }
                    VerveApp(
                        repository = repo,
                        initialRingingAlarm = initialRingingAlarm
                    )
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupLockscreenAndWake()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupLockscreenAndWake()
        parseRingingIntent(intent, VerveDatabaseRepository.getInstance(applicationContext))
    }

    private fun setupLockscreenAndWake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun parseRingingIntent(intent: Intent?, repository: VerveDatabaseRepository) {
        if (intent?.action == AlarmReceiver.ACTION_RING) {
            val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
            val persistedAlarm = if (alarmId > 0L) repository.getAlarmByIdDirect(alarmId) else null

            if (persistedAlarm != null) {
                initialRingingAlarm = persistedAlarm
            } else {
                val alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Morning Awakening"
                val toneName = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TONE) ?: "Radiant Dawn"
                val toneUri = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_URI)
                val missionTypeName = intent.getStringExtra(AlarmReceiver.EXTRA_MISSION_TYPE) ?: "MATH"
                val barcodeId = intent.getLongExtra(AlarmReceiver.EXTRA_BARCODE_ID, -1L).takeIf { it >= 0 }
                val hour = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 7)
                val minute = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)

                val missionType = try {
                    MissionType.valueOf(missionTypeName)
                } catch (_: Exception) {
                    MissionType.MATH
                }

                initialRingingAlarm = Alarm(
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

            // Launch dedicated full-screen AlarmRingingActivity for optimal full-screen presentation
            try {
                val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                    action = AlarmReceiver.ACTION_RING
                    putExtras(intent)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(ringingIntent)
            } catch (_: Exception) {}
        }
    }
}
