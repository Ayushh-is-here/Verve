package com.wakefully.alarm.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Thread-safe, persistent SQLite repository implementing [VerveRepository].
 * Guarantees that alarms, barcodes, and streak statistics are durably stored on disk,
 * surviving app process death (e.g. swiping app from recents) and device reboots.
 */
class VerveDatabaseRepository(context: Context) : VerveRepository {

    private val dbHelper = VerveDatabaseHelper(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    private val _barcodes = MutableStateFlow<List<RegisteredBarcode>>(emptyList())
    private val _streakStats = MutableStateFlow(StreakStats())

    init {
        refreshAll()
    }

    private fun refreshAll() {
        val loadedBarcodes = queryAllBarcodesInternal()
        _barcodes.value = loadedBarcodes
        val loadedAlarms = queryAllAlarmsInternal()
        _alarms.value = loadedAlarms
        val loadedStreaks = queryStreakStatsInternal()
        _streakStats.value = loadedStreaks
    }

    private fun refreshAlarms() {
        _alarms.value = queryAllAlarmsInternal()
    }

    private fun refreshBarcodes() {
        _barcodes.value = queryAllBarcodesInternal()
    }

    private fun refreshStreaks() {
        _streakStats.value = queryStreakStatsInternal()
    }

    // ==================== BARCODE REPOSITORY ====================

    override fun getAllBarcodes(): Flow<List<RegisteredBarcode>> = _barcodes.asStateFlow()

    override suspend fun getBarcodeById(id: Long): RegisteredBarcode? = withContext(Dispatchers.IO) {
        queryBarcodeByIdInternal(id)
    }

    override suspend fun saveBarcode(barcode: RegisteredBarcode): Long = withContext(Dispatchers.IO) {
        saveBarcodeDirect(barcode)
    }

    fun saveBarcodeDirect(barcode: RegisteredBarcode): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", barcode.name)
            put("value", barcode.value)
            put("format", barcode.format)
        }

        val id = if (barcode.id > 0L) {
            val existing = queryBarcodeByIdInternal(barcode.id)
            if (existing != null) {
                db.update(VerveDatabaseHelper.TABLE_BARCODES, values, "id = ?", arrayOf(barcode.id.toString()))
                barcode.id
            } else {
                values.put("id", barcode.id)
                db.insertWithOnConflict(
                    VerveDatabaseHelper.TABLE_BARCODES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        } else {
            db.insert(VerveDatabaseHelper.TABLE_BARCODES, null, values)
        }

        refreshBarcodes()
        return id
    }

    override suspend fun deleteBarcode(id: Long) = withContext(Dispatchers.IO) {
        val alarms = queryAllAlarmsInternal()
        val attachedAlarm = alarms.find { it.barcodeId == id }
        if (attachedAlarm != null) {
            val barcode = queryBarcodeByIdInternal(id)
            throw IllegalStateException(
                "Cannot delete barcode ID $id ('${barcode?.name ?: ""}'): " +
                    "It is attached to Alarm '${attachedAlarm.label}' (ID ${attachedAlarm.id}). " +
                    "Please reassign or delete the alarm first."
            )
        }

        val db = dbHelper.writableDatabase
        db.delete(VerveDatabaseHelper.TABLE_BARCODES, "id = ?", arrayOf(id.toString()))
        refreshBarcodes()
    }

    fun deleteBarcodeDirect(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            putNull("barcode_id")
        }
        db.update(VerveDatabaseHelper.TABLE_ALARMS, values, "barcode_id = ?", arrayOf(id.toString()))
        db.delete(VerveDatabaseHelper.TABLE_BARCODES, "id = ?", arrayOf(id.toString()))
        refreshBarcodes()
        refreshAlarms()
    }

    fun getAllBarcodesDirect(): List<RegisteredBarcode> = queryAllBarcodesInternal()

    private fun queryAllBarcodesInternal(): List<RegisteredBarcode> {
        val list = mutableListOf<RegisteredBarcode>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            VerveDatabaseHelper.TABLE_BARCODES,
            null,
            null,
            null,
            null,
            null,
            "id ASC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("id")
            val nameCol = it.getColumnIndexOrThrow("name")
            val valCol = it.getColumnIndexOrThrow("value")
            val fmtCol = it.getColumnIndexOrThrow("format")

            while (it.moveToNext()) {
                list.add(
                    RegisteredBarcode(
                        id = it.getLong(idCol),
                        name = it.getString(nameCol),
                        value = it.getString(valCol),
                        format = it.getInt(fmtCol)
                    )
                )
            }
        }
        return list
    }

    private fun queryBarcodeByIdInternal(id: Long): RegisteredBarcode? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            VerveDatabaseHelper.TABLE_BARCODES,
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return RegisteredBarcode(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    value = it.getString(it.getColumnIndexOrThrow("value")),
                    format = it.getInt(it.getColumnIndexOrThrow("format"))
                )
            }
        }
        return null
    }

    // ==================== ALARM REPOSITORY ====================

    override fun getAllAlarms(): Flow<List<Alarm>> = _alarms.asStateFlow()

    override suspend fun getAlarmById(id: Long): Alarm? = withContext(Dispatchers.IO) {
        queryAlarmByIdInternal(id)
    }

    fun getAlarmByIdDirect(id: Long): Alarm? = queryAlarmByIdInternal(id)

    fun getAllAlarmsDirect(): List<Alarm> = queryAllAlarmsInternal()

    override suspend fun saveAlarm(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase

        if (alarm.missionType == MissionType.BARCODE && alarm.barcodeId != null) {
            val exists = queryBarcodeByIdInternal(alarm.barcodeId) != null
            require(exists) {
                "Foreign key constraint violation: Barcode ID ${alarm.barcodeId} does not exist in database"
            }
        }

        val values = ContentValues().apply {
            put("hour_of_day", alarm.hourOfDay)
            put("minute", alarm.minute)
            put("repeat_days", alarm.repeatDays.sorted().joinToString(","))
            put("mission_type", alarm.missionType.name)
            if (alarm.barcodeId != null) {
                put("barcode_id", alarm.barcodeId)
            } else {
                putNull("barcode_id")
            }
            put("enabled", if (alarm.enabled) 1 else 0)
            put("label", alarm.label)
            put("tone_name", alarm.toneName)
            if (alarm.toneUri != null) {
                put("tone_uri", alarm.toneUri)
            } else {
                putNull("tone_uri")
            }
        }

        val id = if (alarm.id > 0L) {
            val existing = queryAlarmByIdInternal(alarm.id)
            if (existing != null) {
                db.update(VerveDatabaseHelper.TABLE_ALARMS, values, "id = ?", arrayOf(alarm.id.toString()))
                alarm.id
            } else {
                values.put("id", alarm.id)
                db.insertWithOnConflict(
                    VerveDatabaseHelper.TABLE_ALARMS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        } else {
            db.insert(VerveDatabaseHelper.TABLE_ALARMS, null, values)
        }

        refreshAlarms()
        id
    }

    override suspend fun deleteAlarm(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(VerveDatabaseHelper.TABLE_ALARMS, "id = ?", arrayOf(id.toString()))
        refreshAlarms()
    }

    override suspend fun setAlarmEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("enabled", if (enabled) 1 else 0)
        }
        db.update(VerveDatabaseHelper.TABLE_ALARMS, values, "id = ?", arrayOf(id.toString()))
        refreshAlarms()
    }

    private fun queryAllAlarmsInternal(): List<Alarm> {
        val list = mutableListOf<Alarm>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            VerveDatabaseHelper.TABLE_ALARMS,
            null,
            null,
            null,
            null,
            null,
            "hour_of_day ASC, minute ASC, id ASC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("id")
            val hourCol = it.getColumnIndexOrThrow("hour_of_day")
            val minCol = it.getColumnIndexOrThrow("minute")
            val repeatCol = it.getColumnIndexOrThrow("repeat_days")
            val missionCol = it.getColumnIndexOrThrow("mission_type")
            val barcodeCol = it.getColumnIndexOrThrow("barcode_id")
            val enabledCol = it.getColumnIndexOrThrow("enabled")
            val labelCol = it.getColumnIndexOrThrow("label")
            val toneNameCol = it.getColumnIndexOrThrow("tone_name")
            val toneUriCol = it.getColumnIndexOrThrow("tone_uri")

            while (it.moveToNext()) {
                val repeatStr = it.getString(repeatCol) ?: ""
                val repeatDays = if (repeatStr.isBlank()) {
                    emptySet()
                } else {
                    repeatStr.split(",").mapNotNull { str -> str.trim().toIntOrNull() }.toSet()
                }

                val missionStr = it.getString(missionCol) ?: "MATH"
                val missionType = try {
                    MissionType.valueOf(missionStr)
                } catch (_: Exception) {
                    MissionType.MATH
                }

                val barcodeId = if (it.isNull(barcodeCol)) null else it.getLong(barcodeCol)

                list.add(
                    Alarm(
                        id = it.getLong(idCol),
                        hourOfDay = it.getInt(hourCol),
                        minute = it.getInt(minCol),
                        repeatDays = repeatDays,
                        missionType = missionType,
                        barcodeId = barcodeId,
                        enabled = it.getInt(enabledCol) == 1,
                        label = it.getString(labelCol) ?: "",
                        toneName = it.getString(toneNameCol) ?: "Radiant Dawn",
                        toneUri = if (it.isNull(toneUriCol)) null else it.getString(toneUriCol)
                    )
                )
            }
        }
        return list
    }

    private fun queryAlarmByIdInternal(id: Long): Alarm? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            VerveDatabaseHelper.TABLE_ALARMS,
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val repeatStr = it.getString(it.getColumnIndexOrThrow("repeat_days")) ?: ""
                val repeatDays = if (repeatStr.isBlank()) {
                    emptySet()
                } else {
                    repeatStr.split(",").mapNotNull { str -> str.trim().toIntOrNull() }.toSet()
                }

                val missionStr = it.getString(it.getColumnIndexOrThrow("mission_type")) ?: "MATH"
                val missionType = try {
                    MissionType.valueOf(missionStr)
                } catch (_: Exception) {
                    MissionType.MATH
                }

                val barcodeCol = it.getColumnIndexOrThrow("barcode_id")
                val barcodeId = if (it.isNull(barcodeCol)) null else it.getLong(barcodeCol)

                val toneUriCol = it.getColumnIndexOrThrow("tone_uri")
                val toneUri = if (it.isNull(toneUriCol)) null else it.getString(toneUriCol)

                return Alarm(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    hourOfDay = it.getInt(it.getColumnIndexOrThrow("hour_of_day")),
                    minute = it.getInt(it.getColumnIndexOrThrow("minute")),
                    repeatDays = repeatDays,
                    missionType = missionType,
                    barcodeId = barcodeId,
                    enabled = it.getInt(it.getColumnIndexOrThrow("enabled")) == 1,
                    label = it.getString(it.getColumnIndexOrThrow("label")) ?: "",
                    toneName = it.getString(it.getColumnIndexOrThrow("tone_name")) ?: "Radiant Dawn",
                    toneUri = toneUri
                )
            }
        }
        return null
    }

    // ==================== STREAK MANAGEMENT ====================

    override fun getStreakStats(): Flow<StreakStats> = _streakStats.asStateFlow()

    override fun recordSuccessfulWakeup(missionType: MissionType) {
        scope.launch {
            val current = queryStreakStatsInternal()
            val newCurrent = current.currentStreak + 1
            val newLongest = maxOf(current.longestStreak, newCurrent)
            val newTotal = current.totalWakeups + 1

            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("current_streak", newCurrent)
                put("longest_streak", newLongest)
                put("total_wakeups", newTotal)
                put("is_today_verified", 1)
            }
            db.update(VerveDatabaseHelper.TABLE_STREAKS, values, "id = 1", null)
            refreshStreaks()
        }
    }

    private fun queryStreakStatsInternal(): StreakStats {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            VerveDatabaseHelper.TABLE_STREAKS,
            null,
            "id = 1",
            null,
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return StreakStats(
                    currentStreak = it.getInt(it.getColumnIndexOrThrow("current_streak")),
                    longestStreak = it.getInt(it.getColumnIndexOrThrow("longest_streak")),
                    totalWakeups = it.getInt(it.getColumnIndexOrThrow("total_wakeups")),
                    isTodayVerified = it.getInt(it.getColumnIndexOrThrow("is_today_verified")) == 1
                )
            }
        }
        return StreakStats()
    }

    companion object {
        @Volatile
        private var INSTANCE: VerveDatabaseRepository? = null

        fun getInstance(context: Context): VerveDatabaseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VerveDatabaseRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
