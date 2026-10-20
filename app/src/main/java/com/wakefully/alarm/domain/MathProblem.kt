package com.wakefully.alarm.domain

/**
 * A single arithmetic problem generated for the Math Mission.
 *
 * The [correctAnswer] is computed directly from [operandA] and [operandB] using [operator] —
 * it is never stored separately or guessed from user input, which prevents mismatches.
 */
data class MathProblem(
    val operandA: Int,
    val operandB: Int,
    val operator: Operator,
) {
    enum class Operator { ADD, SUBTRACT, MULTIPLY }

    /** The ground-truth answer derived from operands; never hardcoded. */
    val correctAnswer: Int
        get() = when (operator) {
            Operator.ADD -> operandA + operandB
            Operator.SUBTRACT -> operandA - operandB
            Operator.MULTIPLY -> operandA * operandB
        }

    /** User-facing expression string, e.g. "45 + 32 = ?" */
    val expression: String
        get() = when (operator) {
            Operator.ADD -> "$operandA + $operandB = ?"
            Operator.SUBTRACT -> "$operandA − $operandB = ?"
            Operator.MULTIPLY -> "$operandA × $operandB = ?"
        }
}
