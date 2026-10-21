package com.wakefully.alarm.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemeManager {
    private var prefs: SharedPreferences? = null
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences("verve_theme_prefs", Context.MODE_PRIVATE)
        prefs = sp
        val saved = sp.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = try {
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString("theme_mode", mode.name)?.apply()
    }

    fun cycleThemeMode() {
        val next = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }
}

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }
val LocalOnCycleThemeMode = compositionLocalOf { {} }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1B1F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = Color(0xFF1C1B1F),
    secondary = Terracotta,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = TerracottaBright,
    onSecondaryContainer = TerracottaMuted,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6B6B70),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFE5E5EA),
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = ErrorContainerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F7),
    onPrimary = Color(0xFF121214),
    primaryContainer = Color(0xFF2A2A2E),
    onPrimaryContainer = Color(0xFFF5F5F7),
    secondary = TerracottaBright,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Terracotta,
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF121214),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1E1E22),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2A2A2E),
    onSurfaceVariant = Color(0xFFA1A1A8),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF33333A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A)
)

@Composable
fun WakefullyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

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

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalOnCycleThemeMode provides { ThemeManager.cycleThemeMode() }
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WakefullyTypography,
            content = content
        )
    }
}

/** Verve Brand Theme with dynamic ThemeMode switching */
@Composable
fun VerveTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WakefullyTheme(themeMode = themeMode, dynamicColor = dynamicColor, content = content)
}
