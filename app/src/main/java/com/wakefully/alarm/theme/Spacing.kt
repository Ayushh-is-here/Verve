package com.wakefully.alarm.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4/8 dp architectural spacing tokens.
 * ALL hardcoded random dp spacing in the app is strictly replaced by these tokens.
 */
object WakefullySpacing {
    val Space2: Dp = 2.dp
    val Space4: Dp = 4.dp
    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space20: Dp = 20.dp
    val Space24: Dp = 24.dp
    val Space32: Dp = 32.dp
    val Space40: Dp = 40.dp
    val Space48: Dp = 48.dp
    val Space56: Dp = 56.dp
    val Space64: Dp = 64.dp

    // Screen padding standards
    val ScreenHorizontalPadding: Dp = 20.dp
    val ScreenVerticalPadding: Dp = 20.dp
    val CardPadding: Dp = 18.dp
    val ListGap: Dp = 12.dp

    // Minimum accessible tap target according to WCAG & Android Material
    val MinTouchTarget: Dp = 48.dp

    // Concentric Apple squircle corner radii
    val RadiusCard: Dp = 20.dp
    val RadiusButton: Dp = 16.dp
    val RadiusChip: Dp = 12.dp
    val RadiusPill: Dp = 999.dp
    val RadiusSmall: Dp = 8.dp
}
