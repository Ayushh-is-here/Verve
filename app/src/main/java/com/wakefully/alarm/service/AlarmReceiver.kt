package com.wakefully.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.wakefully.alarm.data.VerveDatabaseRepository
import com.wakefully.alarm.ui.ring.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RING = "com.wakefully.alarm.ACTION_RING"
        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
        const val EXTRA_ALARM_LABEL = "EXTRA_ALARM_LABEL"
        const val EXTRA_ALARM_TONE = "EXTRA_ALARM_TONE"
        const val EXTRA_ALARM_URI = "EXTRA_ALARM_URI"
        const val EXTRA_MISSION_TYPE = "EXTRA_MISSION_TYPE"
        const val EXTRA_BARCODE_ID = "EXTRA_BARCODE_ID"
        const val EXTRA_ALARM_HOUR = "EXTRA_ALARM_HOUR"
        const val EXTRA_ALARM_MINUTE = "EXTRA_ALARM_MINUTE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RING && intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Phone rebooted: reschedule all enabled alarms from persistent SQLite storage
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = VerveDatabaseRepository.getInstance(context)
                    val scheduler = AndroidAlarmScheduler(context)
                    val alarms = repo.getAllAlarmsDirect()
                    alarms.filter { it.enabled }.forEach { alarm ->
                        scheduler.schedule(alarm)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val repo = VerveDatabaseRepository.getInstance(context)
        val persistedAlarm = if (alarmId > 0L) repo.getAlarmByIdDirect(alarmId) else null

        val alarmLabel = persistedAlarm?.label
            ?: intent.getStringExtra(EXTRA_ALARM_LABEL)
            ?: "Morning Awakening"
        val toneName = persistedAlarm?.toneName
            ?: intent.getStringExtra(EXTRA_ALARM_TONE)
            ?: "Radiant Dawn"
        val toneUri = persistedAlarm?.toneUri
            ?: intent.getStringExtra(EXTRA_ALARM_URI)
        val missionType = persistedAlarm?.missionType?.name
            ?: intent.getStringExtra(EXTRA_MISSION_TYPE)
            ?: "MATH"
        val barcodeId = persistedAlarm?.barcodeId
            ?: intent.getLongExtra(EXTRA_BARCODE_ID, -1L)
        val hour = persistedAlarm?.hourOfDay
            ?: intent.getIntExtra(EXTRA_ALARM_HOUR, 7)
        val minute = persistedAlarm?.minute
            ?: intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)

        // 1. Physically illuminate screen and keep CPU active when device is sleeping/dark
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        try {
            val cpuLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "verve:receiver_cpu_wake"
            )
            cpuLock?.acquire(10_000L)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            val screenLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "verve:receiver_screen_wake"
            )
            screenLock?.acquire(10_000L)
        } catch (_: Exception) {}

        // 2. Start AlarmService (Foreground Service) for audio playback, continuous wakelock, and high-priority notification
        AlarmService.startAlarm(
            context = context,
            alarmId = alarmId,
            label = alarmLabel,
            toneName = toneName,
            toneUri = toneUri,
            missionType = missionType,
            barcodeId = barcodeId,
            hour = hour,
            minute = minute
        )

        // 3. Launch full-screen AlarmRingingActivity
        val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            action = ACTION_RING
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(EXTRA_ALARM_TONE, toneName)
            putExtra(EXTRA_ALARM_URI, toneUri)
            putExtra(EXTRA_MISSION_TYPE, missionType)
            putExtra(EXTRA_BARCODE_ID, barcodeId)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
        }

        try {
            context.startActivity(activityIntent)
        } catch (_: Exception) {}
    }
}
