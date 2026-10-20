package com.wakefully.alarm.ui.success

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.theme.BorderHairline
import com.wakefully.alarm.theme.CarbonInk
import com.wakefully.alarm.theme.ChalkWhite
import com.wakefully.alarm.theme.MutedText
import com.wakefully.alarm.theme.PaperWhite
import com.wakefully.alarm.theme.RecessiveGray
import com.wakefully.alarm.theme.SuccessGreen
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.WakefullySpacing
import com.wakefully.alarm.theme.WakefullyTheme

@Composable
fun SuccessScreen(
    missionSummary: String = "Optical Signature Verified (Ensuite C-09)",
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChalkWhite)
            .padding(
                top = topInset + WakefullySpacing.Space24,
                bottom = bottomInset + WakefullySpacing.Space24,
                start = WakefullySpacing.ScreenHorizontalPadding,
                end = WakefullySpacing.ScreenHorizontalPadding
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Tag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                    .background(RecessiveGray)
                    .padding(horizontal = WakefullySpacing.Space12, vertical = WakefullySpacing.Space8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Text(
                    text = "TARGET CONFIRMED // ALARM MUTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }

            // Central Hero Icon & Confirmation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(RecessiveGray)
                        .border(1.dp, BorderHairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = PaperWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

                Text(
                    text = "Wakefulness Earned",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CarbonInk
                )

                Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                Text(
                    text = "Cognitive or physical mission successfully completed. Audio stream silenced.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

                // Verification details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = PaperWhite),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(WakefullySpacing.CardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VERIFICATION AUDIT LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space4))
                        Text(
                            text = missionSummary,
                            style = MaterialTheme.typography.titleMedium,
                            color = CarbonInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Return to Dashboard Button
            Button(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WakefullySpacing.Space56),
                shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                colors = ButtonDefaults.buttonColors(containerColor = CarbonInk, contentColor = PaperWhite)
            ) {
                Text(
                    text = "RETURN TO SCHEDULE",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Preview(name = "Small Phone (360x640)", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun SuccessScreenPreview() {
    WakefullyTheme {
        SuccessScreen(onReturnHome = {})
    }
}
