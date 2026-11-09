package com.filyar.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import com.filyar.app.core.ThemeMode
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.filyar.app.R

// ---------- Typography (Vazirmatn) ----------
val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold),
)

private fun TextStyle.vz() = copy(fontFamily = Vazir)

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.vz(),
    displayMedium = base.displayMedium.vz(),
    displaySmall = base.displaySmall.vz(),
    headlineLarge = base.headlineLarge.vz().copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.vz().copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.vz().copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.vz().copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.vz().copy(fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.vz().copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.vz(),
    bodyMedium = base.bodyMedium.vz(),
    bodySmall = base.bodySmall.vz(),
    labelLarge = base.labelLarge.vz().copy(fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.vz(),
    labelSmall = base.labelSmall.vz(),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ---------- Colors: deep ink + mint, amber accent ----------
private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F1E6),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF3F5F5A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3EAE4),
    onSecondaryContainer = Color(0xFF0E1F1C),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE2B8),
    onTertiaryContainer = Color(0xFF2E1500),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4F8F7),
    onBackground = Color(0xFF0F1A18),
    surface = Color(0xFFF4F8F7),
    onSurface = Color(0xFF0F1A18),
    surfaceVariant = Color(0xFFDCE5E2),
    onSurfaceVariant = Color(0xFF4F5B58),
    outline = Color(0xFF7F8B88),
    outlineVariant = Color(0xFFC7D1CE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF4F2),
    surfaceContainer = Color(0xFFE8EFED),
    surfaceContainerHigh = Color(0xFFE2EAE7),
    surfaceContainerHighest = Color(0xFFDCE4E1),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF0B4F48),
    onPrimaryContainer = Color(0xFFB4F1E6),
    secondary = Color(0xFFB7CEC8),
    onSecondary = Color(0xFF22342F),
    secondaryContainer = Color(0xFF2B3D39),
    onSecondaryContainer = Color(0xFFD3EAE4),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF5F3E00),
    onTertiaryContainer = Color(0xFFFFE2B8),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C1312),
    onBackground = Color(0xFFDDE5E2),
    surface = Color(0xFF0C1312),
    onSurface = Color(0xFFDDE5E2),
    surfaceVariant = Color(0xFF3A4744),
    onSurfaceVariant = Color(0xFFB0BEBA),
    outline = Color(0xFF7A8884),
    outlineVariant = Color(0xFF34413E),
    surfaceContainerLowest = Color(0xFF070D0C),
    surfaceContainerLow = Color(0xFF131C1A),
    surfaceContainer = Color(0xFF17211F),
    surfaceContainerHigh = Color(0xFF1F2A28),
    surfaceContainerHighest = Color(0xFF283432),
    scrim = Color(0xFF000000),
)

// ---------- Extra tokens: file-type tints + hero gradient ----------
@Immutable
data class FilyarTokens(
    val folder: Color,
    val image: Color,
    val video: Color,
    val audio: Color,
    val archive: Color,
    val apk: Color,
    val document: Color,
    val text: Color,
    val code: Color,
    val other: Color,
    val success: Color,
    val warning: Color,
    val heroBrush: Brush,
    val onHero: Color,
    val onHeroButton: Color,
)

private val LightTokens = FilyarTokens(
    folder = Color(0xFF0F766E),
    image = Color(0xFFDB2777),
    video = Color(0xFF7C3AED),
    audio = Color(0xFFEA580C),
    archive = Color(0xFFB45309),
    apk = Color(0xFF16A34A),
    document = Color(0xFF2563EB),
    text = Color(0xFF475569),
    code = Color(0xFF0891B2),
    other = Color(0xFF64748B),
    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706),
    heroBrush = Brush.linearGradient(listOf(Color(0xFF0B3B36), Color(0xFF0F766E), Color(0xFF14B8A6))),
    onHero = Color(0xFFFFFFFF),
    onHeroButton = Color(0xFF0B3B36),
)

private val DarkTokens = FilyarTokens(
    folder = Color(0xFF5EEAD4),
    image = Color(0xFFF472B6),
    video = Color(0xFFA78BFA),
    audio = Color(0xFFFB923C),
    archive = Color(0xFFFBBF24),
    apk = Color(0xFF4ADE80),
    document = Color(0xFF60A5FA),
    text = Color(0xFFCBD5E1),
    code = Color(0xFF22D3EE),
    other = Color(0xFF94A3B8),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    heroBrush = Brush.linearGradient(listOf(Color(0xFF07211E), Color(0xFF0B4F48), Color(0xFF0F766E))),
    onHero = Color(0xFFFFFFFF),
    onHeroButton = Color(0xFF0B3B36),
)

val LocalTokens = staticCompositionLocalOf { LightTokens }

object Filyar {
    val tokens: FilyarTokens
        @Composable get() = LocalTokens.current
    val colors: ColorScheme
        @Composable get() = MaterialTheme.colorScheme
}

/** True when the app should draw in dark colours for the chosen theme. */
@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun FilyarTheme(mode: ThemeMode = ThemeMode.System, dynamic: Boolean = false, content: @Composable () -> Unit) {
    val dark = isDarkTheme(mode)
    val context = LocalContext.current
    val useDynamic = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    val base = if (dark) DarkTokens else LightTokens
    // With wallpaper colours, folders and the header card follow the wallpaper palette too.
    val tokens = if (!useDynamic) base else base.copy(
        folder = scheme.primary,
        heroBrush = Brush.linearGradient(listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer)),
        onHero = scheme.onPrimaryContainer,
        onHeroButton = scheme.primaryContainer,
    )
    CompositionLocalProvider(LocalTokens provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
