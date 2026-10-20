package com.wakefully.alarm.service

import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AlarmDismissalReason {
    /** Dismissal earned through verified completion of the assigned mission. */
    data class MissionVerified(val missionType: MissionType, val metadata: String = "") : AlarmDismissalReason

    /** Dismissal through intentional 5-second uninterrupted emergency press. */
    data object EmergencyHoldCompleted : AlarmDismissalReason

    /** Temporary silence permitted by scheduled snooze interval. */
    data class PermittedSnooze(val durationMinutes: Int = 5) : AlarmDismissalReason
}

/**
 * Centralized, guarded controller managing alarm ringing state and sound continuity.
 *
 * CRITICAL AUDIT GUARANTEE:
 * [dismissAlarm] is the SINGLE authorized dismiss function in the entire application.
 * It will throw or reject any dismissal attempt that is not guarded by a valid [AlarmDismissalReason].
 */
interface AlarmController {
    val isRinging: StateFlow<Boolean>
    val activeAlarm: StateFlow<Alarm?>
    val soundPlaying: StateFlow<Boolean>

    fun triggerAlarm(alarm: Alarm)
    fun dismissAlarm(reason: AlarmDismissalReason): Boolean
}

class DefaultAlarmController : AlarmController {

    private val _isRinging = MutableStateFlow(false)
    override val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    private val _activeAlarm = MutableStateFlow<Alarm?>(null)
    override val activeAlarm: StateFlow<Alarm?> = _activeAlarm.asStateFlow()

    private val _soundPlaying = MutableStateFlow(false)
    override val soundPlaying: StateFlow<Boolean> = _soundPlaying.asStateFlow()

    override fun triggerAlarm(alarm: Alarm) {
        _activeAlarm.value = alarm
        _isRinging.value = true
        _soundPlaying.value = true
    }

    override fun dismissAlarm(reason: AlarmDismissalReason): Boolean {
        // Enforce that dismissal can ONLY occur when an alarm is actually active
        if (!_isRinging.value) {
            return false
        }

        // Validate that reason is legitimately permitted
        when (reason) {
            is AlarmDismissalReason.MissionVerified -> {
                // Ensure the completed mission matches the active alarm's required mission
                val current = _activeAlarm.value
                if (current != null && current.missionType != MissionType.NONE) {
                    if (current.missionType != reason.missionType) {
                        // Attempting to dismiss with wrong mission type (e.g. math when barcode is required)
                        return false
                    }
                }
            }
            AlarmDismissalReason.EmergencyHoldCompleted -> {
                // Explicit 5-second emergency hold bypass permitted
            }
            is AlarmDismissalReason.PermittedSnooze -> {
                // Permitted snooze
            }
        }

        // Single atomic point of stoppage
        _isRinging.value = false
        _soundPlaying.value = false
        _activeAlarm.value = null
        return true
    }
}
