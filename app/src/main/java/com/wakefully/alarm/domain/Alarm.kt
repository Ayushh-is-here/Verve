package com.wakefully.alarm.domain

import kotlinx.serialization.Serializable

/** The type of challenge required to dismiss this alarm. */
enum class MissionType {
    MATH,    // solve 3 arithmetic problems
    BARCODE, // scan a specific registered barcode from Room DB
    SHAKE,   // shake the device N times
    NONE,    // no challenge – alarm dismisses immediately
}

/**
 * A scheduled alarm entry.
 * Stored in Room/Database.
 */
@Serializable
data class Alarm(
    val id: Long = 0L,
    /** Minutes from midnight (0..1439). */
    val hourOfDay: Int,
    val minute: Int,
    /** Days of the week this alarm repeats on (Mon=1 … Sun=7, ISO). Empty = one-shot. */
    val repeatDays: Set<Int> = emptySet(),
    val missionType: MissionType = MissionType.MATH,
    /** Foreign Key referencing RegisteredBarcode.id in Room DB. Only used when missionType == BARCODE. */
    val barcodeId: Long? = null,
    val enabled: Boolean = true,
    val label: String = "",
    val toneName: String = "Radiant Dawn",
    val toneUri: String? = null
)
