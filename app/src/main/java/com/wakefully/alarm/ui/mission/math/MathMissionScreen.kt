package com.wakefully.alarm.ui.mission.math

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.domain.MathProblem
import com.wakefully.alarm.theme.BorderHairline
import com.wakefully.alarm.theme.CarbonInk
import com.wakefully.alarm.theme.ChalkWhite
import com.wakefully.alarm.theme.ErrorContainerRed
import com.wakefully.alarm.theme.ErrorRed
import com.wakefully.alarm.theme.MutedText
import com.wakefully.alarm.theme.PaperWhite
import com.wakefully.alarm.theme.RecessiveGray
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.TerracottaBright
import com.wakefully.alarm.theme.WakefullySpacing
import com.wakefully.alarm.theme.WakefullyTheme
import kotlin.math.roundToInt

@Composable
fun MathMissionScreen(
    viewModel: MathMissionViewModel,
    onDismissAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Block back button: alarm cannot be bypassed by navigating back
    BackHandler(enabled = true) {}

    val uiState by viewModel.uiState.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Shake animation for incorrect answer feedback
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.isError) {
        if (uiState.isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -8f at 250
                    8f at 300
                    0f at 350
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChalkWhite)
            .padding(
                top = topInset + WakefullySpacing.Space12,
                bottom = bottomInset + WakefullySpacing.Space16,
                start = WakefullySpacing.ScreenHorizontalPadding,
                end = WakefullySpacing.ScreenHorizontalPadding
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Telemetry Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                        .background(RecessiveGray)
                        .padding(horizontal = WakefullySpacing.Space12, vertical = WakefullySpacing.Space8),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Terracotta)
                        )
                        Text(
                            text = "MISSION // MENTAL MATH",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space4)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AUDIO ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonInk
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                // Progress Step Indicators (1 of 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOLVE TO DISMISS // STEP ${uiState.progressText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)) {
                        for (i in 0 until uiState.totalProblems) {
                            val isCompleted = i < uiState.currentIndex
                            val isCurrent = i == uiState.currentIndex
                            Box(
                                modifier = Modifier
                                    .size(WakefullySpacing.Space12)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompleted -> Terracotta
                                            isCurrent -> CarbonInk
                                            else -> RecessiveGray
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Math Expression & Input Card
            val problem = uiState.currentProblem
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .border(
                        1.dp,
                        if (uiState.isError) ErrorRed else BorderHairline,
                        RoundedCornerShape(WakefullySpacing.RadiusCard)
                    ),
                shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isError) ErrorContainerRed.copy(alpha = 0.5f) else PaperWhite
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WakefullySpacing.CardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = problem?.expression ?: "Generating...",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 36.sp
                        ),
                        color = CarbonInk,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space16))

                    // Input display box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(WakefullySpacing.Space56)
                            .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                            .background(RecessiveGray)
                            .border(
                                1.dp,
                                if (uiState.isError) ErrorRed else CarbonInk,
                                RoundedCornerShape(WakefullySpacing.RadiusButton)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.currentInput.isEmpty()) "ANSWER" else uiState.currentInput,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = if (uiState.currentInput.isEmpty()) MutedText else if (uiState.isError) ErrorRed else CarbonInk
                        )
                    }

                    if (uiState.isError) {
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                        Text(
                            text = "Incorrect solution. Recalculate.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed
                        )
                    }
                }
            }

            // Numeric Dialpad with >= 48x48dp accessible tap targets
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                val padRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "BACK")
                )

                padRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp) // Accessible touch height >= 48dp
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                                    .background(
                                        when (key) {
                                            "CLEAR", "BACK" -> RecessiveGray
                                            else -> PaperWhite
                                        }
                                    )
                                    .border(1.dp, BorderHairline, RoundedCornerShape(WakefullySpacing.RadiusButton))
                                    .clickable {
                                        when (key) {
                                            "CLEAR" -> viewModel.onClear()
                                            "BACK" -> viewModel.onBackspace()
                                            else -> viewModel.onNumberInput(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "BACK") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = CarbonInk,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = if (key == "CLEAR") {
                                            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        } else {
                                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                                        },
                                        color = CarbonInk
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space4))

                // Verify Button
                Button(
                    onClick = { viewModel.submitAnswer(onDismissAlarm) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WakefullySpacing.Space56),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarbonInk,
                        contentColor = PaperWhite
                    ),
                    enabled = uiState.currentInput.isNotEmpty()
                ) {
                    Text(
                        text = "VERIFY SOLUTION",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ==================== COMPOSE PREVIEWS ====================

@Preview(name = "Small Phone (360x640)", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun MathMissionScreenSmallPreview() {
    WakefullyTheme {
        MathMissionScreen(
            viewModel = MathMissionViewModel(),
            onDismissAlarm = {}
        )
    }
}

@Preview(name = "Standard Phone (411x891)", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun MathMissionScreenStandardPreview() {
    WakefullyTheme {
        MathMissionScreen(
            viewModel = MathMissionViewModel(),
            onDismissAlarm = {}
        )
    }
}
