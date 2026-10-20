package com.wakefully.alarm.ui.mission.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakefully.alarm.data.BarcodeRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.domain.barcode.BarcodeVerificationEngine
import com.wakefully.alarm.domain.barcode.ScanVerificationResult
import com.wakefully.alarm.service.AlarmController
import com.wakefully.alarm.service.AlarmDismissalReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BarcodeMissionUiState(
    val targetBarcode: RegisteredBarcode? = null,
    val isLoading: Boolean = true,
    val hasCameraPermission: Boolean = true,
    val isTorchOn: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isDismissed: Boolean = false,
    val isMissingTargetFallback: Boolean = false,
    val scanCountDebounce: Int = 0
)

/**
 * ViewModel for the Barcode Mission RINGING flow.
 *
 * NOTE ON SEPARATION OF CONCERNS:
 * This class is strictly for RINGING verification.
 * The setup screen (BarcodeSetupViewModel) uses an independent scanner flow to REGISTER a barcode.
 * The setup code path is never reused during alarm ringing.
 */
class BarcodeMissionViewModel(
    val alarm: Alarm,
    private val barcodeRepository: BarcodeRepository,
    private val alarmController: AlarmController,
    private val errorDebounceMs: Long = 500L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    initialTarget: RegisteredBarcode? = null,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeMissionUiState())
    val uiState: StateFlow<BarcodeMissionUiState> = _uiState.asStateFlow()

    private var verificationEngine: BarcodeVerificationEngine? = null

    init {
        if (initialTarget != null) {
            verificationEngine = BarcodeVerificationEngine(
                targetBarcode = initialTarget,
                errorDebounceMs = errorDebounceMs,
                timeProvider = timeProvider
            )
            _uiState.update {
                it.copy(
                    targetBarcode = initialTarget,
                    isLoading = false
                )
            }
        } else {
            loadTargetBarcode()
        }
    }

    private fun loadTargetBarcode() {
        viewModelScope.launch(dispatcher) {
            val barcodeId = alarm.barcodeId
            if (barcodeId == null) {
                // Safe fallback: Alarm has no attached barcode ID
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isMissingTargetFallback = true,
                        errorMessage = "No registered barcode linked to this alarm. Emergency hold active."
                    )
                }
                return@launch
            }

            val target = barcodeRepository.getBarcodeById(barcodeId)
            if (target != null) {
                verificationEngine = BarcodeVerificationEngine(
                    targetBarcode = target,
                    errorDebounceMs = errorDebounceMs,
                    timeProvider = timeProvider
                )
                _uiState.update {
                    it.copy(
                        targetBarcode = target,
                        isLoading = false
                    )
                }
            } else {
                // Safe fallback: Target barcode was deleted or missing.
                // Never trap user, but never auto-dismiss!
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isMissingTargetFallback = true,
                        errorMessage = "Registered barcode was removed. Use emergency hold to dismiss."
                    )
                }
            }
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false, errorMessage = null) }
    }

    /**
     * Ingests a scanned frame from the camera analyzer (or test injector).
     */
    fun onScanFrame(rawValue: String?, format: Int) {
        val engine = verificationEngine ?: return
        if (_uiState.value.isDismissed) return

        when (val result = engine.processFrame(rawValue, format)) {
            ScanVerificationResult.Ignored -> {
                // Ignore null / empty / whitespace
            }
            is ScanVerificationResult.Debouncing -> {
                _uiState.update { it.copy(scanCountDebounce = result.consecutiveFrames) }
            }
            is ScanVerificationResult.MatchSuccess -> {
                // RIGHT BARCODE! Dismiss through the single guarded code path
                _uiState.update {
                    it.copy(
                        isDismissed = true,
                        isError = false,
                        errorMessage = null
                    )
                }
                alarmController.dismissAlarm(
                    AlarmDismissalReason.MissionVerified(
                        missionType = MissionType.BARCODE,
                        metadata = result.barcode.name
                    )
                )
            }
            is ScanVerificationResult.MismatchError -> {
                // WRONG BARCODE: Show error, shake, do NOT dismiss, keep alarm ringing
                _uiState.update {
                    it.copy(
                        isError = true,
                        errorMessage = result.errorMessage
                    )
                }
            }
        }
    }

    /**
     * Intentional emergency hold completion (fallback path if barcode missing or emergency)
     */
    fun onEmergencyHoldComplete() {
        _uiState.update { it.copy(isDismissed = true) }
        alarmController.dismissAlarm(AlarmDismissalReason.EmergencyHoldCompleted)
    }
}
