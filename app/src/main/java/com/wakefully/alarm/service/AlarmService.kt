package com.wakefully.alarm.service

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.wakefully.alarm.R
import com.wakefully.alarm.audio.VerveAudioPlayer
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.ui.ring.AlarmRingingActivity

/**
 * Foreground Service running while an alarm is ringing.
 * Ensures the app process is prioritized by Android, maintains the wake lock,
 * guarantees continuous acoustic playback, and exposes the high-priority full-screen notification.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.wakefully.alarm.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.wakefully.alarm.ACTION_STOP_ALARM"

        const val CHANNEL_ID = "verve_alarm_ringing_channel_v8"
        const val NOTIFICATION_ID = 4242

        private var wakeLock: PowerManager.WakeLock? = null

        fun startAlarm(
            context: Context,
            alarmId: Long,
            label: String,
            toneName: String,
            toneUri: String?,
            missionType: String,
            barcodeId: Long,
            hour: Int,
            minute: Int
        ) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
                putExtra(AlarmReceiver.EXTRA_ALARM_TONE, toneName)
                putExtra(AlarmReceiver.EXTRA_ALARM_URI, toneUri)
                putExtra(AlarmReceiver.EXTRA_MISSION_TYPE, missionType)
                putExtra(AlarmReceiver.EXTRA_BARCODE_ID, barcodeId)
                putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
                putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startAlarm(context: Context, alarm: Alarm) {
            startAlarm(
                context = context,
                alarmId = alarm.id,
                label = alarm.label,
                toneName = alarm.toneName,
                toneUri = alarm.toneUri,
                missionType = alarm.missionType.name,
                barcodeId = alarm.barcodeId ?: -1L,
                hour = alarm.hourOfDay,
                minute = alarm.minute
            )
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopRinging()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_START_ALARM || intent?.action == AlarmReceiver.ACTION_RING) {
            val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
            val alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Morning Awakening"
            val toneName = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TONE) ?: "Radiant Dawn"
            val toneUri = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_URI)
            val missionType = intent.getStringExtra(AlarmReceiver.EXTRA_MISSION_TYPE) ?: "MATH"
            val barcodeId = intent.getLongExtra(AlarmReceiver.EXTRA_BARCODE_ID, -1L)
            val hour = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 7)
            val minute = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)

            startRinging(
                alarmId = alarmId,
                label = alarmLabel,
                toneName = toneName,
                toneUri = toneUri,
                missionType = missionType,
                barcodeId = barcodeId,
                hour = hour,
                minute = minute
            )
        }

        return START_STICKY
    }

    private fun startRinging(
        alarmId: Long,
        label: String,
        toneName: String,
        toneUri: String?,
        missionType: String,
        barcodeId: Long,
        hour: Int,
        minute: Int
    ) {
        // 1. Acquire WakeLock to keep CPU alive throughout ringing duration
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "verve:alarm_ringing_wakelock"
            )?.apply {
                acquire(15 * 60 * 1000L) // 15 minute timeout safety net
            }
        }

        // Physically turn on the device display from sleep/black screen
        try {
            @Suppress("DEPRECATION")
            val screenLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "verve:screen_wake_alarm"
            )
            screenLock?.acquire(10_000L) // 10 second window for AlarmRingingActivity to draw and hold FLAG_KEEP_SCREEN_ON
        } catch (_: Exception) {}

        // 2. Create full-screen intent targeting AlarmRingingActivity
        val activityIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            action = AlarmReceiver.ACTION_RING
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_TONE, toneName)
            putExtra(AlarmReceiver.EXTRA_ALARM_URI, toneUri)
            putExtra(AlarmReceiver.EXTRA_MISSION_TYPE, missionType)
            putExtra(AlarmReceiver.EXTRA_BARCODE_ID, barcodeId)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
        }

        val activityOptionsBundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            (alarmId % 100000).toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            activityOptionsBundle
        )

        // 3. Notification Channel with High Importance & Silent PCM Sound Resource
        // Android requires an alerting sound/importance to dispatch fullScreenIntent over lockscreen.
        // By supplying a 200ms silent WAV, Android treats the notification as alerting,
        // launching fullScreenIntent 100% reliably while producing zero audible chime to conflict with VerveAudioPlayer.
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val silentSoundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.verve_silent}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            listOf("v1", "v2", "v3", "v4", "v5", "v6", "v7").forEach { ver ->
                try {
                    notificationManager.deleteNotificationChannel("verve_alarm_ringing_channel_$ver")
                } catch (_: Exception) {}
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Verve Alarms (Full Screen)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical Alarm Notifications"
                setBypassDnd(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(silentSoundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 4. Build high priority notification with full-screen intent
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("VERVE // ALARM RINGING")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setSound(silentSoundUri, AudioManager.STREAM_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .addAction(R.mipmap.ic_launcher, "DISMISS / CHALLENGE", fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        // 5. Start foreground immediately so Android does not throttle or delay presentation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Explicitly trigger notify to ensure notification manager dispatches fullScreenIntent
        notificationManager?.notify(NOTIFICATION_ID, notification)

        // 6. Direct start activity to ensure immediate full-screen presentation
        try {
            startActivity(activityIntent)
        } catch (_: Exception) {}

        // 7. Play acoustic tone via VerveAudioPlayer (sole source of audible alarm sound)
        VerveAudioPlayer.startAlarmRinging(this, toneName, toneUri)
    }

    private fun stopRinging() {
        VerveAudioPlayer.stopAll()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}
