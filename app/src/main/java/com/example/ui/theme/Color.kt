package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark theme tokens (matching TypePro globals.css)
val DarkBg = Color(0xFF050B1A)
val DarkBgSoft = Color(0xFF08122A)
val DarkSurface = Color(0xFF0B1730)
val DarkPanel = Color(0xFF0F1D3A)
val DarkLine = Color(0xFF1D2C4D)
val DarkInk = Color(0xFFE8EEFC)
val DarkMuted = Color(0xFF8E9DBD)
val DarkBrand = Color(0xFF3B82F6)
val DarkBrandStrong = Color(0xFF2563EB)
val DarkCyan = Color(0xFF22D3EE)
val DarkSuccess = Color(0xFF34D399)
val DarkDanger = Color(0xFFFB7185)
val DarkWarn = Color(0xFFFBBF24)

// Light theme tokens (matching TypePro globals.css)
val LightBg = Color(0xFFF3F6FD)
val LightSurface = Color(0xFFFFFFFF)
val LightPanel = Color(0xFFEEF3FC)
val LightLine = Color(0xFFD9E3F4)
val LightInk = Color(0xFF0C1730)
val LightMuted = Color(0xFF5B6A89)
val LightBrand = Color(0xFF2563EB)
val LightBrandStrong = Color(0xFF1D4ED8)
val LightCyan = Color(0xFF0891B2)
val LightSuccess = Color(0xFF059669)
val LightDanger = Color(0xFFDC2626)
val LightWarn = Color(0xFFB45309)

@Immutable
data class TypeProExtendedColors(
  val panel: Color,
  val line: Color,
  val brandSoft: Color,
  val cyan: Color,
  val success: Color,
  val successSoft: Color,
  val warn: Color,
  val warnSoft: Color,
  val danger: Color,
  val dangerSoft: Color,
)

val DarkExtendedColors = TypeProExtendedColors(
  panel = DarkPanel,
  line = DarkLine,
  brandSoft = DarkBrand.copy(alpha = 0.16f),
  cyan = DarkCyan,
  success = DarkSuccess,
  successSoft = DarkSuccess.copy(alpha = 0.14f),
  warn = DarkWarn,
  warnSoft = DarkWarn.copy(alpha = 0.14f),
  danger = DarkDanger,
  dangerSoft = DarkDanger.copy(alpha = 0.14f),
)

val LightExtendedColors = TypeProExtendedColors(
  panel = LightPanel,
  line = LightLine,
  brandSoft = LightBrand.copy(alpha = 0.11f),
  cyan = LightCyan,
  success = LightSuccess,
  successSoft = LightSuccess.copy(alpha = 0.12f),
  warn = LightWarn,
  warnSoft = LightWarn.copy(alpha = 0.12f),
  danger = LightDanger,
  dangerSoft = LightDanger.copy(alpha = 0.12f),
)

val LocalTypeProColors = staticCompositionLocalOf { DarkExtendedColors }
