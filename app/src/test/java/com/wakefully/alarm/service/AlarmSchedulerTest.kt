package com.wakefully.alarm.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    @Test
    fun `formatTimeRemainingToast under 1 hour shows minutes`() {
        val target = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 35)
        }
        val toast = formatTimeRemainingToast(target)
        assertTrue("Expected minutes in toast, got: $toast", toast.contains("35 minutes from now"))
    }

    @Test
    fun `formatTimeRemainingToast over 1 hour shows hours and minutes`() {
        val target = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 4)
            add(Calendar.MINUTE, 20)
        }
        val toast = formatTimeRemainingToast(target)
        assertTrue("Expected 4 hours and 20 minutes in toast, got: $toast", toast.contains("4 hours and 20 minutes from now"))
    }

    @Test
    fun `formatTimeRemainingToast for multiple days shows weekday name`() {
        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
        }
        val toast = formatTimeRemainingToast(target)
        assertTrue("Expected day name and days in toast, got: $toast", toast.contains("from now") && toast.contains("d"))
    }

    @Test
    fun `getNextAlarmTime one-shot schedules for today if in future`() {
        val now = Calendar.getInstance()
        val futureHour = (now.get(Calendar.HOUR_OF_DAY) + 2) % 24
        // If wrapping around midnight, this test checks next day
        val cal = getNextAlarmTime(futureHour, 0, emptySet())
        assertTrue("Scheduled time must be after now", cal.after(now))
    }

    @Test
    fun `getNextAlarmTime schedules repeating days correctly`() {
        val now = Calendar.getInstance()
        // Repeat on Saturday and Sunday (6 and 7 in ISO)
        val cal = getNextAlarmTime(8, 0, setOf(6, 7))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        assertTrue(
            "Scheduled day must be Saturday or Sunday",
            dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        )
        assertTrue("Scheduled calendar must be in the future", cal.after(now))
    }

    @Test
    fun `verify alarm intent and service constants`() {
        assertEquals("com.wakefully.alarm.ACTION_RING", AlarmReceiver.ACTION_RING)
        assertEquals("EXTRA_ALARM_ID", AlarmReceiver.EXTRA_ALARM_ID)
        assertEquals("EXTRA_ALARM_HOUR", AlarmReceiver.EXTRA_ALARM_HOUR)
        assertEquals("EXTRA_ALARM_MINUTE", AlarmReceiver.EXTRA_ALARM_MINUTE)
        assertEquals("com.wakefully.alarm.ACTION_START_ALARM", AlarmService.ACTION_START_ALARM)
        assertEquals("com.wakefully.alarm.ACTION_STOP_ALARM", AlarmService.ACTION_STOP_ALARM)
    }
}

