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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.theme.SuccessGreen
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.WakefullySpacing

@Composable
fun SuccessScreen(
    missionSummary: String = "Optical Signature Verified",
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    text = "TARGET CONFIRMED // ALARM SILENCED",
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
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
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

                Text(
                    text = "Wakefulness Verified",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                Text(
                    text = "Mission protocol successfully completed. Audio stream silenced and discipline record updated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

                // Verification details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
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
