package com.wakefully.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakefully.alarm.MainActivity
import com.wakefully.alarm.domain.Alarm
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Calculates the exact upcoming trigger [Calendar] for an alarm.
 * Handles one-shot (today or tomorrow) and repeating days of week.
 */
fun getNextAlarmTime(hour: Int, minute: Int, repeatDays: Set<Int>): Calendar {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (repeatDays.isEmpty()) {
        // One-shot alarm: if target is in the past today, schedule for tomorrow
        if (target.before(now) || target == now) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target
    }

    // Repeating alarm: repeatDays contains ISO 8601 day numbers (1 = Mon ... 7 = Sun)
    // Java Calendar days: Sun = 1, Mon = 2, Tue = 3, Wed = 4, Thu = 5, Fri = 6, Sat = 7
    val isoToCalDay = mapOf(1 to Calendar.MONDAY, 2 to Calendar.TUESDAY, 3 to Calendar.WEDNESDAY,
        4 to Calendar.THURSDAY, 5 to Calendar.FRIDAY, 6 to Calendar.SATURDAY, 7 to Calendar.SUNDAY)
    val calDays = repeatDays.mapNotNull { isoToCalDay[it] }.toSet()

    for (offset in 0..7) {
        val candidate = (target.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offset)
        }
        val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
        if (calDays.contains(dayOfWeek)) {
            if (candidate.after(now)) {
                return candidate
            }
        }
    }

    target.add(Calendar.DAY_OF_YEAR, 1)
    return target
}

/**
 * Formats a user-friendly toast message for how long until the alarm rings:
 * - "Alarm set for 7 hours and 15 minutes from now"
 * - "Alarm set for 45 minutes from now"
 * - "Alarm set for Monday (2 days from now)"
 */
fun formatTimeRemainingToast(nextCal: Calendar): String {
    val nowMs = System.currentTimeMillis()
    val targetMs = nextCal.timeInMillis
    val diffMs = maxOf(0L, targetMs - nowMs)

    val totalMinutes = (diffMs + 30000L) / 60000L // round to nearest minute
    val days = (totalMinutes / (24 * 60)).toInt()
    val hours = ((totalMinutes % (24 * 60)) / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()

    return when {
        days > 0 -> {
            val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(nextCal.time)
            if (hours > 0) {
                "Alarm set for $dayName ($days d $hours h from now)"
            } else {
                "Alarm set for $dayName ($days d from now)"
            }
        }
        hours > 0 -> {
            val hStr = if (hours == 1) "1 hour" else "$hours hours"
            if (minutes > 0) {
                val mStr = if (minutes == 1) "1 minute" else "$minutes minutes"
                "Alarm set for $hStr and $mStr from now"
            } else {
                "Alarm set for $hStr from now"
            }
        }
        minutes > 0 -> {
            val mStr = if (minutes == 1) "1 minute" else "$minutes minutes"
            "Alarm set for $mStr from now"
        }
        else -> "Alarm set for less than 1 minute from now"
    }
}

/**
 * Android system [AlarmManager] coordinator for exact hardware wakeups.
 */
class AndroidAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled || alarmManager == null) return

        val targetCal = getNextAlarmTime(alarm.hourOfDay, alarm.minute, alarm.repeatDays)
        val triggerMs = targetCal.timeInMillis

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_RING
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_ALARM_TONE, alarm.toneName)
            putExtra(AlarmReceiver.EXTRA_ALARM_URI, alarm.toneUri)
            putExtra(AlarmReceiver.EXTRA_MISSION_TYPE, alarm.missionType.name)
            putExtra(AlarmReceiver.EXTRA_BARCODE_ID, alarm.barcodeId ?: -1L)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hourOfDay)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, com.wakefully.alarm.ui.ring.AlarmRingingActivity::class.java).apply {
                action = AlarmReceiver.ACTION_RING
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setAlarmClock gives highest priority system wakeup, bypasses Doze, shows clock in status bar
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMs, showIntent),
                pendingIntent
            )
        } catch (_: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    fun cancel(alarmId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_RING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
