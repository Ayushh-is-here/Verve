package com.wakefully.alarm.ui.mission.math

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wakefully.alarm.domain.MathProblem
import com.wakefully.alarm.domain.MathProblemGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MathMissionUiState(
    val problems: List<MathProblem> = emptyList(),
    val currentIndex: Int = 0,
    val currentInput: String = "",
    val isError: Boolean = false,
    val isCompleted: Boolean = false,
) {
    val currentProblem: MathProblem?
        get() = problems.getOrNull(currentIndex)

    val progressText: String
        get() = if (problems.isEmpty()) "" else "${currentIndex + 1} of ${problems.size}"

    val totalProblems: Int
        get() = problems.size
}

class MathMissionViewModel(
    private val generator: MathProblemGenerator = MathProblemGenerator(),
    previousLastProblem: MathProblem? = null,
    totalCount: Int = 3
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MathMissionUiState(
            problems = generator.generateSession(
                count = totalCount,
                previousAlarmLastProblem = previousLastProblem
            )
        )
    )
    val uiState: StateFlow<MathMissionUiState> = _uiState.asStateFlow()

    fun onNumberInput(digit: String) {
        if (_uiState.value.isCompleted) return
        _uiState.update { current ->
            // Limit input to 4 digits (answers are under 200, but allow typing up to 4 chars)
            if (current.currentInput.length < 4) {
                current.copy(
                    currentInput = current.currentInput + digit,
                    isError = false
                )
            } else {
                current
            }
        }
    }

    fun onBackspace() {
        if (_uiState.value.isCompleted) return
        _uiState.update { current ->
            current.copy(
                currentInput = current.currentInput.dropLast(1),
                isError = false
            )
        }
    }

    fun onClear() {
        if (_uiState.value.isCompleted) return
        _uiState.update { it.copy(currentInput = "", isError = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false) }
    }

    /**
     * Submits the current input.
     *
     * [onDismissAlarm] is strictly guaranteed to be called ONLY after the FINAL problem
     * is answered correctly.
     */
    fun submitAnswer(onDismissAlarm: () -> Unit) {
        val state = _uiState.value
        if (state.isCompleted) return

        val problem = state.currentProblem ?: return
        val userAnswer = state.currentInput.toIntOrNull()

        if (userAnswer == null) {
            // Empty or invalid input triggers error, does NOT advance or dismiss
            _uiState.update { it.copy(isError = true, currentInput = "") }
            return
        }

        if (userAnswer == problem.correctAnswer) {
            // Correct answer
            if (state.currentIndex + 1 < state.totalProblems) {
                // Advance to next problem; DO NOT dismiss alarm
                _uiState.update { current ->
                    current.copy(
                        currentIndex = current.currentIndex + 1,
                        currentInput = "",
                        isError = false
                    )
                }
            } else {
                // Final problem solved!
                _uiState.update { current ->
                    current.copy(
                        isCompleted = true,
                        currentInput = "",
                        isError = false
                    )
                }
                onDismissAlarm()
            }
        } else {
            // Wrong answer: shake/red error feedback, clear input, keep SAME problem, alarm keeps ringing
            _uiState.update { current ->
                current.copy(
                    isError = true,
                    currentInput = ""
                )
            }
        }
    }
}
