package com.wakefully.alarm.domain

import kotlin.random.Random

/**
 * Generator for arithmetic problems adhering to the Easy Mode criteria:
 * - Addition: two numbers, 10-99 (answer strictly under 200, max 99+99 = 198)
 * - Subtraction: first number larger than second, 10-99, no negative answers
 * - Multiplication: one number 2-9, the other 2-12
 * - Easy mode uses mostly addition/subtraction (~45% each) with a few multiplications (~10%)
 * - No division, no decimals, no negative numbers.
 * - Non-repeating within session, and no immediate repeat across consecutive alarms.
 * - Uses kotlin.random.Random.Default or an injected Random source (never fixed seeds).
 */
open class MathProblemGenerator(
    private val random: Random = Random.Default
) {

    /**
     * Generates a single valid [MathProblem] that is not present in [disallowed].
     */
    fun generateProblem(disallowed: Set<MathProblem> = emptySet()): MathProblem {
        var attempts = 0
        while (attempts < 1000) {
            attempts++
            val opRoll = random.nextInt(100)
            val problem = when {
                // 45% Addition
                opRoll < 45 -> {
                    val a = random.nextInt(10, 100)
                    val b = random.nextInt(10, 100)
                    // Max is 99 + 99 = 198 < 200
                    MathProblem(a, b, MathProblem.Operator.ADD)
                }
                // 45% Subtraction (first number strictly larger than second, both in 10..99)
                opRoll < 90 -> {
                    val a = random.nextInt(11, 100) // 11..99
                    val b = random.nextInt(10, a)    // 10..(a - 1)
                    MathProblem(a, b, MathProblem.Operator.SUBTRACT)
                }
                // 10% Multiplication (one number 2..9, the other 2..12)
                else -> {
                    val swap = random.nextBoolean()
                    val a = if (swap) random.nextInt(2, 10) else random.nextInt(2, 13)
                    val b = if (swap) random.nextInt(2, 13) else random.nextInt(2, 10)
                    MathProblem(a, b, MathProblem.Operator.MULTIPLY)
                }
            }

            if (problem !in disallowed) {
                return problem
            }
        }

        // Extremely rare fallback to ensure termination: guaranteed unique addition
        var a = 10
        var b = 10
        while (MathProblem(a, b, MathProblem.Operator.ADD) in disallowed && a < 99) {
            b++
            if (b >= 100) {
                b = 10
                a++
            }
        }
        return MathProblem(a, b, MathProblem.Operator.ADD)
    }

    /**
     * Generates a full session of [count] problems.
     * Guarantees:
     * 1. No two problems in the session are identical.
     * 2. The first problem is not equal to [previousAlarmLastProblem] (if provided).
     */
    fun generateSession(
        count: Int = 3,
        previousAlarmLastProblem: MathProblem? = null
    ): List<MathProblem> {
        require(count > 0) { "Session problem count must be greater than 0" }
        val sessionProblems = mutableListOf<MathProblem>()
        val disallowed = mutableSetOf<MathProblem>()
        previousAlarmLastProblem?.let { disallowed.add(it) }

        for (i in 0 until count) {
            val problem = generateProblem(disallowed)
            sessionProblems.add(problem)
            disallowed.add(problem)
        }

        return sessionProblems
    }
}
