package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.DailyPoint
import com.example.ui.theme.LocalTypeProColors
import kotlin.math.max

enum class Tone {
  NEUTRAL,
  BRAND,
  CYAN,
  SUCCESS,
  WARN,
  DANGER,
}

@Composable
fun toneColor(tone: Tone): Color {
  val ext = LocalTypeProColors.current
  return when (tone) {
    Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    Tone.BRAND -> MaterialTheme.colorScheme.primary
    Tone.CYAN -> ext.cyan
    Tone.SUCCESS -> ext.success
    Tone.WARN -> ext.warn
    Tone.DANGER -> ext.danger
  }
}

@Composable
fun toneContainerColor(tone: Tone): Color {
  val ext = LocalTypeProColors.current
  return when (tone) {
    Tone.NEUTRAL -> ext.panel
    Tone.BRAND -> ext.brandSoft
    Tone.CYAN -> ext.cyan.copy(alpha = 0.14f)
    Tone.SUCCESS -> ext.successSoft
    Tone.WARN -> ext.warnSoft
    Tone.DANGER -> ext.dangerSoft
  }
}

@Composable
fun BadgePill(
  text: String,
  tone: Tone = Tone.NEUTRAL,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = toneContainerColor(tone),
    shape = RoundedCornerShape(999.dp),
    border = BorderStroke(1.dp, toneColor(tone).copy(alpha = 0.25f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = toneColor(tone),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun SectionCard(
  title: String,
  description: String? = null,
  action: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val ext = LocalTypeProColors.current
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, ext.line),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (!description.isNullOrBlank()) {
            Text(
              text = description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (action != null) {
          Spacer(Modifier.size(8.dp))
          action()
        }
      }
      content()
    }
  }
}

@Composable
fun StatCard(
  label: String,
  value: String,
  icon: ImageVector,
  hint: String,
  tone: Tone = Tone.BRAND,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val accent = toneColor(tone)
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, ext.line),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = label.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Surface(
          color = toneContainerColor(tone),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.size(28.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accent,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = accent,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = hint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun LinearProgressMeter(
  percent: Float,
  tone: Tone = Tone.BRAND,
  height: Dp = 8.dp,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val clamped = percent.coerceIn(0f, 100f) / 100f
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .background(ext.panel, RoundedCornerShape(999.dp)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(clamped)
        .height(height)
        .background(toneColor(tone), RoundedCornerShape(999.dp)),
    )
  }
}

@Composable
fun GoalProgressRing(
  percent: Double,
  todayMinutes: Double,
  goalMinutes: Int,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val primary = MaterialTheme.colorScheme.primary
  val cyan = ext.cyan
  val track = ext.line
  val sweep = ((percent.coerceIn(0.0, 100.0)) / 100.0 * 360.0).toFloat()

  Box(
    modifier = modifier.size(104.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeWidth = 10.dp.toPx()
      drawArc(
        color = track,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )
      drawArc(
        color = if (percent >= 100.0) cyan else primary,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "${percent.toInt()}%",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "${todayMinutes.toInt()}/${goalMinutes}m",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun ActivityBarChart(
  data: List<DailyPoint>,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val barColor = MaterialTheme.colorScheme.primary
  val gridColor = ext.line

  if (data.all { it.minutes <= 0.0 }) {
    EmptyStateCard(
      title = "No activity recorded yet",
      description = "Complete a test, lesson, or practice session to populate your daily minutes chart.",
      modifier = modifier,
    )
    return
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val maxMinutes = max(5.0, data.maxOfOrNull { it.minutes } ?: 5.0)
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(150.dp),
    ) {
      val barCount = data.size.coerceAtLeast(1)
      val slotWidth = size.width / barCount
      val barWidth = (slotWidth * 0.54f).coerceAtMost(28.dp.toPx())

      for (i in 0..2) {
        val y = size.height * (i / 2f)
        drawLine(
          color = gridColor.copy(alpha = 0.5f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.dp.toPx(),
        )
      }

      data.forEachIndexed { index, point ->
        val ratio = (point.minutes / maxMinutes).toFloat().coerceIn(0f, 1f)
        val barH = max(4.dp.toPx(), size.height * ratio)
        val x = index * slotWidth + (slotWidth - barWidth) / 2f
        val y = size.height - barH
        drawRoundRect(
          color = if (point.minutes > 0) barColor else gridColor.copy(alpha = 0.4f),
          topLeft = Offset(x, y),
          size = Size(barWidth, barH),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      val step = if (data.size > 10) 5 else 1
      data.forEachIndexed { index, point ->
        if (index % step == 0 || index == data.lastIndex) {
          Text(
            text = point.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun WpmTrendChart(
  data: List<DailyPoint>,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val active = data.filter { it.tests > 0 }
  if (active.isEmpty()) {
    EmptyStateCard(
      title = "No WPM trend data yet",
      description = "Complete typing sessions to track your WPM and accuracy progression.",
      modifier = modifier,
    )
    return
  }

  val wpmColor = MaterialTheme.colorScheme.primary
  val accColor = ext.success
  val gridColor = ext.line
  val maxWpm = max(40.0, active.maxOf { it.wpm } * 1.15)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      BadgePill(text = "Net WPM", tone = Tone.BRAND)
      BadgePill(text = "Accuracy %", tone = Tone.SUCCESS)
    }
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(160.dp),
    ) {
      for (i in 0..3) {
        val y = size.height * (i / 3f)
        drawLine(
          color = gridColor.copy(alpha = 0.45f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.dp.toPx(),
        )
      }

      val wpmPath = Path()
      val accPath = Path()
      val count = active.size
      active.forEachIndexed { idx, pt ->
        val x = if (count == 1) size.width / 2f else idx.toFloat() / (count - 1) * size.width
        val wpmY = size.height - ((pt.wpm / maxWpm).toFloat().coerceIn(0f, 1f) * size.height)
        val accY = size.height - ((pt.accuracy / 100.0).toFloat().coerceIn(0f, 1f) * size.height)

        if (idx == 0) {
          wpmPath.moveTo(x, wpmY)
          accPath.moveTo(x, accY)
        } else {
          wpmPath.lineTo(x, wpmY)
          accPath.lineTo(x, accY)
        }
        drawCircle(color = wpmColor, radius = 4.dp.toPx(), center = Offset(x, wpmY))
        drawCircle(color = accColor, radius = 3.5.dp.toPx(), center = Offset(x, accY))
      }

      if (count > 1) {
        drawPath(wpmPath, color = wpmColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(accPath, color = accColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  title: String,
  description: String,
  icon: ImageVector? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = ext.panel,
    border = BorderStroke(1.dp, ext.line),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(4.dp))
        Button(onClick = onAction) {
          Text(actionLabel)
        }
      }
    }
  }
}
