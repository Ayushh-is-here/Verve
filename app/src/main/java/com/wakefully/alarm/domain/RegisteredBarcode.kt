package com.wakefully.alarm.domain

import kotlinx.serialization.Serializable

/**
 * A physical barcode registered by the user (e.g. bathroom cleanser, book, tea box).
 * Stored in Room/Database.
 */
@Serializable
data class RegisteredBarcode(
    val id: Long = 0L,
    val name: String,
    val value: String,
    /** Standard ML Kit / ZXing barcode format constant, e.g. Barcode.FORMAT_EAN_13 (32) or Barcode.FORMAT_QR_CODE (256). */
    val format: Int = 32
)
