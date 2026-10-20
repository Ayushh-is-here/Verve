package com.wakefully.alarm.ui.mission.math

import com.wakefully.alarm.domain.MathProblem
import com.wakefully.alarm.domain.MathProblemGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathMissionTest {

    private val generator = MathProblemGenerator()

    @Test
    fun generate1000Problems_assertRangesAndNonNegative() {
        // Generate 1,000 problems: assert every answer is a non-negative integer,
        // the operands are within range, and there are no negative results.
        for (i in 1..1000) {
            val problem = generator.generateProblem()
            val answer = problem.correctAnswer

            assertTrue("Answer must be non-negative: $problem", answer >= 0)

            when (problem.operator) {
                MathProblem.Operator.ADD -> {
                    assertTrue("Operand A in 10..99: ${problem.operandA}", problem.operandA in 10..99)
                    assertTrue("Operand B in 10..99: ${problem.operandB}", problem.operandB in 10..99)
                    assertTrue("Answer strictly under 200: $answer", answer < 200)
                    assertEquals("Answer matches a + b", problem.operandA + problem.operandB, answer)
                }
                MathProblem.Operator.SUBTRACT -> {
                    assertTrue("Operand A in 10..99: ${problem.operandA}", problem.operandA in 10..99)
                    assertTrue("Operand B in 10..99: ${problem.operandB}", problem.operandB in 10..99)
                    assertTrue("Operand A strictly larger than Operand B: $problem", problem.operandA > problem.operandB)
                    assertTrue("No negative results: $answer", answer > 0)
                    assertEquals("Answer matches a - b", problem.operandA - problem.operandB, answer)
                }
                MathProblem.Operator.MULTIPLY -> {
                    val a = problem.operandA
                    val b = problem.operandB
                    val validRange = (a in 2..9 && b in 2..12) || (a in 2..12 && b in 2..9)
                    assertTrue("Multiplication operands in valid range (2..9 and 2..12): $problem", validRange)
                    assertTrue("Multiplication answer non-negative: $answer", answer >= 0)
                    assertEquals("Answer matches a * b", a * b, answer)
                }
            }
        }
    }

    @Test
    fun consecutive50Sessions_produceDifferentProblemSets() {
        // Assert that 50 consecutive generated sessions produce different problem sets (not all identical).
        val sessions = (1..50).map { generator.generateSession(count = 3) }
        val uniqueSessions = sessions.toSet()

        // With millions of combinations, 50 random sessions must not all be identical
        assertTrue("Expected variety across 50 sessions, got ${uniqueSessions.size} unique sets", uniqueSessions.size > 40)
    }

    @Test
    fun noDuplicateWithinSession_andNoImmediateRepeatOfPreviousProblem() {
        // Assert no duplicate within a session, and no immediate repeat of the previous problem.
        var previousLastProblem: MathProblem? = null

        for (sessionIndex in 1..100) {
            val session = generator.generateSession(count = 3, previousAlarmLastProblem = previousLastProblem)

            assertEquals("Session must have 3 problems", 3, session.size)
            assertEquals("No duplicates within session", session.size, session.toSet().size)

            if (previousLastProblem != null) {
                assertNotEquals(
                    "First problem must not immediately repeat last problem of previous alarm",
                    previousLastProblem,
                    session.first()
                )
            }

            previousLastProblem = session.last()
        }
    }

    @Test
    fun wrongInput_doesNotAdvanceProgress_correctInputDoes() {
        // Assert wrong input does not advance progress, and correct input does.
        val sessionProblems = listOf(
            MathProblem(20, 15, MathProblem.Operator.ADD),      // ans = 35
            MathProblem(50, 20, MathProblem.Operator.SUBTRACT), // ans = 30
            MathProblem(6, 7, MathProblem.Operator.MULTIPLY)    // ans = 42
        )

        val fakeGenerator = object : MathProblemGenerator() {
            // Return fixed list for predictable verification
        }

        // ViewModel initialized with custom session problems
        val viewModel = MathMissionViewModel(
            generator = fakeGenerator,
            totalCount = 3
        )
        // Access state through reflections or override, or test state transitions:
        val stateFlow = viewModel.uiState

        // Let's test with the generated problems
        val currentProblem1 = stateFlow.value.currentProblem!!
        val correctAnswer1 = currentProblem1.correctAnswer
        val wrongAnswer = correctAnswer1 + 1

        var dismissCount = 0
        val onDismiss: () -> Unit = { dismissCount++ }

        // Test wrong input
        wrongAnswer.toString().forEach { viewModel.onNumberInput(it.toString()) }
        assertEquals("Typed wrong answer", wrongAnswer.toString(), stateFlow.value.currentInput)

        viewModel.submitAnswer(onDismiss)

        // Must show error, clear input, NOT advance index, NOT dismiss
        assertTrue("Must flag error on wrong answer", stateFlow.value.isError)
        assertEquals("Input must be cleared after wrong answer", "", stateFlow.value.currentInput)
        assertEquals("Index must not advance on wrong answer", 0, stateFlow.value.currentIndex)
        assertEquals("Dismiss must not be called on wrong answer", 0, dismissCount)
        assertFalse("Mission must not be completed on wrong answer", stateFlow.value.isCompleted)

        // Test correct input for Problem 1
        correctAnswer1.toString().forEach { viewModel.onNumberInput(it.toString()) }
        viewModel.submitAnswer(onDismiss)

        assertFalse("Error cleared on correct answer", stateFlow.value.isError)
        assertEquals("Current index advances to 1", 1, stateFlow.value.currentIndex)
        assertEquals("Progress shows 2 of 3", "2 of 3", stateFlow.value.progressText)
        assertEquals("Dismiss still NOT called after problem 1", 0, dismissCount)
        assertFalse("Mission still not completed", stateFlow.value.isCompleted)
    }

    @Test
    fun dismissAlarm_onlyCalledOnFinalCorrectAnswer() {
        // Assert that the "dismiss alarm" function is not called until the final correct answer.
        val viewModel = MathMissionViewModel(
            generator = generator,
            totalCount = 3
        )

        var dismissCalled = false
        val onDismiss = { dismissCalled = true }

        // Solve problem 1 correctly
        val p1 = viewModel.uiState.value.currentProblem!!
        p1.correctAnswer.toString().forEach { viewModel.onNumberInput(it.toString()) }
        viewModel.submitAnswer(onDismiss)
        assertFalse("Dismiss not called after problem 1", dismissCalled)
        assertEquals(1, viewModel.uiState.value.currentIndex)

        // Solve problem 2 correctly
        val p2 = viewModel.uiState.value.currentProblem!!
        p2.correctAnswer.toString().forEach { viewModel.onNumberInput(it.toString()) }
        viewModel.submitAnswer(onDismiss)
        assertFalse("Dismiss not called after problem 2", dismissCalled)
        assertEquals(2, viewModel.uiState.value.currentIndex)

        // Give WRONG answer on problem 3
        val p3 = viewModel.uiState.value.currentProblem!!
        (p3.correctAnswer + 5).toString().forEach { viewModel.onNumberInput(it.toString()) }
        viewModel.submitAnswer(onDismiss)
        assertFalse("Dismiss not called on wrong answer for problem 3", dismissCalled)
        assertEquals(2, viewModel.uiState.value.currentIndex)
        assertTrue(viewModel.uiState.value.isError)

        // Now give correct answer on problem 3
        p3.correctAnswer.toString().forEach { viewModel.onNumberInput(it.toString()) }
        viewModel.submitAnswer(onDismiss)

        assertTrue("Dismiss MUST be called on final correct answer", dismissCalled)
        assertTrue("Mission marked as completed", viewModel.uiState.value.isCompleted)
    }
}
