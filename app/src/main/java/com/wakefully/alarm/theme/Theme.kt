package com.wakefully.alarm.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CarbonInk,
    onPrimary = PaperWhite,
    primaryContainer = RecessiveGray,
    onPrimaryContainer = CarbonInk,
    secondary = Terracotta,
    onSecondary = PaperWhite,
    secondaryContainer = TerracottaBright,
    onSecondaryContainer = TerracottaMuted,
    background = ChalkWhite,
    onBackground = DarkText,
    surface = PaperWhite,
    onSurface = DarkText,
    surfaceVariant = RecessiveGray,
    onSurfaceVariant = MutedText,
    outline = SlateGraphite,
    outlineVariant = BorderHairline,
    error = ErrorRed,
    onError = PaperWhite,
    errorContainer = ErrorContainerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PaperWhite,
    onPrimary = CarbonInk,
    primaryContainer = CarbonInk,
    onPrimaryContainer = PaperWhite,
    secondary = TerracottaBright,
    onSecondary = PaperWhite,
    secondaryContainer = Terracotta,
    onSecondaryContainer = PaperWhite,
    background = CarbonInk,
    onBackground = PaperWhite,
    surface = Color(0xFF1B1B1F),
    onSurface = PaperWhite,
    surfaceVariant = Color(0xFF2E3132),
    onSurfaceVariant = SlateGraphite,
    outline = SlateGraphite,
    outlineVariant = Color(0x33FFFFFF),
    error = ErrorRed,
    onError = PaperWhite,
    errorContainer = ErrorContainerRed
)

@Composable
fun WakefullyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep editorial brand colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WakefullyTypography,
        content = content
    )
}
