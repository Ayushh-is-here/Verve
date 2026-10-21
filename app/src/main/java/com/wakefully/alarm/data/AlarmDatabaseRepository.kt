package com.wakefully.alarm.data

import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

interface BarcodeRepository {
    fun getAllBarcodes(): Flow<List<RegisteredBarcode>>
    suspend fun getBarcodeById(id: Long): RegisteredBarcode?
    suspend fun saveBarcode(barcode: RegisteredBarcode): Long
    /**
     * Attempts to delete the barcode with [id].
     * @throws IllegalStateException if the barcode is attached to an active or configured alarm.
     */
    suspend fun deleteBarcode(id: Long)
}

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun saveAlarm(alarm: Alarm): Long
    suspend fun deleteAlarm(id: Long)
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean)
}

data class StreakStats(
    val currentStreak: Int = 3,
    val longestStreak: Int = 7,
    val totalWakeups: Int = 14,
    val isTodayVerified: Boolean = false
)
interface VerveRepository : BarcodeRepository, AlarmRepository {
    fun getStreakStats(): Flow<StreakStats>
    fun recordSuccessfulWakeup(missionType: MissionType)
}

/**
 * Thread-safe relational in-memory database repository for testing.
 * Enforces foreign key integrity between [Alarm] and [RegisteredBarcode].
 */
class InMemoryWakefullyRepository : VerveRepository {

    private val barcodeIdGen = AtomicLong(1L)
    private val alarmIdGen = AtomicLong(1L)

    private val barcodesState = MutableStateFlow<Map<Long, RegisteredBarcode>>(emptyMap())
    private val alarmsState = MutableStateFlow<Map<Long, Alarm>>(emptyMap())

    init {
        // Pre-populate with default sample data so the app has working samples on launch
        val shampooId = barcodeIdGen.getAndIncrement()
        val defaultBarcode = RegisteredBarcode(
            id = shampooId,
            name = "Ensuite C-09 (Ceramic Cleanser)",
            value = "4006381333931",
            format = 32 // FORMAT_EAN_13
        )
        barcodesState.value = mapOf(shampooId to defaultBarcode)

        val alarm1Id = alarmIdGen.getAndIncrement()
        val alarm2Id = alarmIdGen.getAndIncrement()
        val defaultAlarms = mapOf(
            alarm1Id to Alarm(
                id = alarm1Id,
                hourOfDay = 6,
                minute = 30,
                repeatDays = setOf(1, 2, 3, 4, 5),
                missionType = MissionType.BARCODE,
                barcodeId = shampooId,
                enabled = true,
                label = "Morning Routine"
            ),
            alarm2Id to Alarm(
                id = alarm2Id,
                hourOfDay = 7,
                minute = 15,
                repeatDays = setOf(6, 7),
                missionType = MissionType.MATH,
                barcodeId = null,
                enabled = false,
                label = "Weekend Awakening"
            )
        )
        alarmsState.value = defaultAlarms
    }

    override fun getAllBarcodes(): Flow<List<RegisteredBarcode>> =
        barcodesState.map { it.values.toList() }

    override suspend fun getBarcodeById(id: Long): RegisteredBarcode? =
        barcodesState.value[id]

    override suspend fun saveBarcode(barcode: RegisteredBarcode): Long {
        val assignedId = if (barcode.id <= 0L) barcodeIdGen.getAndIncrement() else barcode.id
        val item = barcode.copy(id = assignedId)
        barcodesState.update { current ->
            current + (assignedId to item)
        }
        return assignedId
    }

    override suspend fun deleteBarcode(id: Long) {
        // Enforce Foreign Key Integrity: Block deleting a barcode that is attached to an alarm
        val attachedAlarm = alarmsState.value.values.find { it.barcodeId == id }
        if (attachedAlarm != null) {
            throw IllegalStateException(
                "Cannot delete barcode ID $id ('${barcodesState.value[id]?.name ?: ""}'): " +
                    "It is attached to Alarm '${attachedAlarm.label}' (ID ${attachedAlarm.id}). " +
                    "Please reassign or delete the alarm first."
            )
        }
        barcodesState.update { it - id }
    }

    override fun getAllAlarms(): Flow<List<Alarm>> =
        alarmsState.map { it.values.toList() }

    override suspend fun getAlarmById(id: Long): Alarm? =
        alarmsState.value[id]

    override suspend fun saveAlarm(alarm: Alarm): Long {
        // If alarm specifies a barcodeId, verify it exists in the database
        if (alarm.missionType == MissionType.BARCODE && alarm.barcodeId != null) {
            require(barcodesState.value.containsKey(alarm.barcodeId)) {
                "Foreign key constraint violation: Barcode ID ${alarm.barcodeId} does not exist in database"
            }
        }
        val assignedId = if (alarm.id <= 0L) alarmIdGen.getAndIncrement() else alarm.id
        val item = alarm.copy(id = assignedId)
        alarmsState.update { current ->
            current + (assignedId to item)
        }
        return assignedId
    }

    override suspend fun deleteAlarm(id: Long) {
        alarmsState.update { it - id }
    }

    override suspend fun setAlarmEnabled(id: Long, enabled: Boolean) {
        alarmsState.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.copy(enabled = enabled))
        }
    }

    // Database-Free Streak Management
    private val streakState = MutableStateFlow(
        StreakStats(
            currentStreak = 3,
            longestStreak = 7,
            totalWakeups = 14,
            isTodayVerified = false
        )
    )

    override fun getStreakStats(): Flow<StreakStats> = streakState.asStateFlow()

    override fun recordSuccessfulWakeup(missionType: MissionType) {
        streakState.update { current ->
            val newCurrent = current.currentStreak + 1
            current.copy(
                currentStreak = newCurrent,
                longestStreak = maxOf(current.longestStreak, newCurrent),
                totalWakeups = current.totalWakeups + 1,
                isTodayVerified = true
            )
        }
    }
}
