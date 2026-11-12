package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.ThemeMode

private val DarkColorScheme = darkColorScheme(
  primary = DarkBrand,
  onPrimary = Color.White,
  primaryContainer = DarkBrandStrong,
  onPrimaryContainer = Color.White,
  secondary = DarkCyan,
  onSecondary = DarkBg,
  tertiary = DarkSuccess,
  background = DarkBg,
  onBackground = DarkInk,
  surface = DarkSurface,
  onSurface = DarkInk,
  surfaceVariant = DarkPanel,
  onSurfaceVariant = DarkMuted,
  outline = DarkLine,
  error = DarkDanger,
  onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
  primary = LightBrand,
  onPrimary = Color.White,
  primaryContainer = LightBrandStrong,
  onPrimaryContainer = Color.White,
  secondary = LightCyan,
  onSecondary = Color.White,
  tertiary = LightSuccess,
  background = LightBg,
  onBackground = LightInk,
  surface = LightSurface,
  onSurface = LightInk,
  surfaceVariant = LightPanel,
  onSurfaceVariant = LightMuted,
  outline = LightLine,
  error = LightDanger,
  onError = Color.White,
)

val TypeProShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun TypeProTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalTypeProColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = TypeProTypography,
      shapes = TypeProShapes,
      content = content,
    )
  }
}
