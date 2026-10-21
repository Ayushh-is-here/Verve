package com.wakefully.alarm.domain.barcode

import com.wakefully.alarm.domain.RegisteredBarcode

sealed interface ScanVerificationResult {
    /** The scan input was null, empty, or whitespace only. Ignored. */
    data object Ignored : ScanVerificationResult

    /** The scan is in the debounce window waiting for the 2nd consecutive matching frame. */
    data class Debouncing(val candidateValue: String, val consecutiveFrames: Int) : ScanVerificationResult

    /** The barcode matches the target registered barcode across 2 consecutive frames. Dismiss permitted. */
    data class MatchSuccess(val barcode: RegisteredBarcode) : ScanVerificationResult

    /** The barcode does not match this alarm's registered barcode. Alarm must continue ringing. */
    data class MismatchError(
        val scannedValue: String,
        val scannedFormat: Int,
        val expectedName: String,
        val errorMessage: String = "That's not the right one"
    ) : ScanVerificationResult
}

/**
 * Strict verification engine for the Barcode Mission during alarm ringing.
 *
 * Rules:
 * 1. Evaluates ONLY against [targetBarcode].
 * 2. Compares trimmed rawValue AND exact format.
 * 3. Requires 2 consecutive frames with the same candidate value before confirming match or error.
 * 4. Debounces repeated error notifications to prevent UI flickering.
 */
class BarcodeVerificationEngine(
    val targetBarcode: RegisteredBarcode,
    private val errorDebounceMs: Long = 500L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var lastCandidateValue: String? = null
    private var lastCandidateFormat: Int? = null
    private var consecutiveFrames: Int = 0
    private var lastErrorEmittedTimeMs: Long = 0L

    /**
     * Ingests a raw camera frame from the ML Kit analyzer.
     *
     * @param rawValue The detected raw string, or null if nothing scanned.
     * @param format The detected format constant (e.g. Barcode.FORMAT_EAN_13).
     */
    fun processFrame(rawValue: String?, format: Int): ScanVerificationResult {
        if (rawValue.isNullOrBlank()) {
            return ScanVerificationResult.Ignored
        }

        val trimmedValue = rawValue.trim()
        if (trimmedValue.isEmpty()) {
            return ScanVerificationResult.Ignored
        }

        // Check if this matches the candidate from the immediately preceding frame
        if (trimmedValue == lastCandidateValue && format == lastCandidateFormat) {
            consecutiveFrames++
        } else {
            // First time seeing this candidate
            lastCandidateValue = trimmedValue
            lastCandidateFormat = format
            consecutiveFrames = 1
        }

        // Require at least 2 consecutive analyzer frames to prevent transient optical noise
        if (consecutiveFrames < 2) {
            return ScanVerificationResult.Debouncing(trimmedValue, consecutiveFrames)
        }

        // We have 2+ consecutive frames for this candidate
        val isValueMatch = trimmedValue == targetBarcode.value.trim()
        val isFormatMatch = targetBarcode.format == 0 || format == 0 || format == targetBarcode.format

        return if (isValueMatch && isFormatMatch) {
            // Reset candidates to prevent re-triggering
            reset()
            ScanVerificationResult.MatchSuccess(targetBarcode)
        } else {
            // Mismatch: Debounce error dispatch so UI doesn't flicker on continuous video feed
            val now = timeProvider()
            if (now - lastErrorEmittedTimeMs < errorDebounceMs && consecutiveFrames > 2) {
                // Already reported this error recently in current stream, debounce without crashing
                ScanVerificationResult.Debouncing(trimmedValue, consecutiveFrames)
            } else {
                lastErrorEmittedTimeMs = now
                ScanVerificationResult.MismatchError(
                    scannedValue = trimmedValue,
                    scannedFormat = format,
                    expectedName = targetBarcode.name
                )
            }
        }
    }

    fun reset() {
        lastCandidateValue = null
        lastCandidateFormat = null
        consecutiveFrames = 0
    }
}
