package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.StatsCalculator
import com.example.domain.TypingEngine
import com.example.domain.TypingMode
import com.example.ui.TypeProAppState
import com.example.ui.components.ActivityBarChart
import com.example.ui.components.BadgePill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionCard
import com.example.ui.components.StatCard
import com.example.ui.components.Tone
import com.example.ui.components.WpmTrendChart
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTypeProColors

private enum class ReportRange(val label: String, val days: Int?) {
  WEEK("7 days", 7),
  MONTH("30 days", 30),
  ALL("All time", null),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
  state: TypeProAppState,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val ext = LocalTypeProColors.current
  var range by remember { mutableStateOf(ReportRange.MONTH) }
  var modeFilter by remember { mutableStateOf<TypingMode?>(null) }

  val filteredResults = remember(state.results, range, modeFilter) {
    val cutoff = range.days?.let { System.currentTimeMillis() - it * 86_400_000L }
    state.results.filter { res ->
      val inRange = cutoff == null || res.createdAtMillis >= cutoff
      val inMode = modeFilter == null || res.mode == modeFilter
      inRange && inMode
    }
  }

  val filteredSummary = remember(filteredResults, state.keyStats, state.settings.dailyGoalMinutes) {
    StatsCalculator.summarize(
      results = filteredResults,
      keyStats = state.keyStats,
      dailyGoalMinutes = state.settings.dailyGoalMinutes,
    )
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Performance reports",
      description = "Filter typing history by timeframe and mode, analyze weak keys, or export CSV.",
      action = {
        Button(
          enabled = filteredResults.isNotEmpty(),
          onClick = {
            val csv = StatsCalculator.exportResultsCsv(filteredResults)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/csv"
              putExtra(Intent.EXTRA_SUBJECT, "TypePro Typing Report")
              putExtra(Intent.EXTRA_TEXT, csv)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export TypePro CSV"))
          },
          modifier = Modifier.testTag("export_csv_button"),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("CSV")
        }
      },
    ) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ReportRange.entries.forEach { r ->
          FilterChip(
            selected = range == r,
            onClick = { range = r },
            label = { Text(r.label) },
          )
        }
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = modeFilter == null,
          onClick = { modeFilter = null },
          label = { Text("All modes") },
        )
        TypingMode.entries.forEach { m ->
          FilterChip(
            selected = modeFilter == m,
            onClick = { modeFilter = m },
            label = { Text(m.label) },
          )
        }
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatCard(
        label = "Avg Net WPM",
        value = "${filteredSummary.avgWpm.toInt()}",
        icon = Icons.Filled.Speed,
        hint = "Best ${filteredSummary.bestWpm.toInt()} WPM",
        tone = Tone.BRAND,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Avg Accuracy",
        value = "${TypingEngine.round(filteredSummary.avgAccuracy, 1)}%",
        icon = Icons.Filled.TrackChanges,
        hint = "Best ${TypingEngine.round(filteredSummary.bestAccuracy, 1)}%",
        tone = Tone.SUCCESS,
        modifier = Modifier.weight(1f),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatCard(
        label = "Sessions",
        value = "${filteredResults.size}",
        icon = Icons.Filled.CalendarMonth,
        hint = "${filteredSummary.totalTests} timed tests",
        tone = Tone.CYAN,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Time Logged",
        value = StatsCalculator.formatDuration(filteredSummary.totalPracticeSeconds),
        icon = Icons.Filled.CalendarMonth,
        hint = "${filteredSummary.hindiSessions} Hindi sessions",
        tone = Tone.WARN,
        modifier = Modifier.weight(1f),
      )
    }

    SectionCard(
      title = "WPM & accuracy trend",
      description = if (range == ReportRange.WEEK) "Last 7 days" else "Last 30 days",
    ) {
      WpmTrendChart(data = if (range == ReportRange.WEEK) filteredSummary.weekly else filteredSummary.monthly)
    }

    SectionCard(
      title = "Daily practice volume",
      description = "Minutes typed per day",
    ) {
      ActivityBarChart(data = if (range == ReportRange.WEEK) filteredSummary.weekly else filteredSummary.monthly)
    }

    SectionCard(
      title = "Weak keys analysis",
      description = "Keys with the lowest accuracy (minimum 3 keystrokes recorded).",
    ) {
      if (state.summary.weakKeys.isEmpty()) {
        EmptyStateCard(
          title = "No weak-key profile yet",
          description = "Finish a few tests or drills so TypePro can identify keys that need focused practice.",
        )
      } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          state.summary.weakKeys.forEach { wk ->
            val isDevanagari = Regex("[\\u0900-\\u097F]").containsMatchIn(wk.key)
            Surface(
              color = ext.panel,
              shape = MaterialTheme.shapes.small,
              border = BorderStroke(
                1.dp,
                if (wk.accuracy < 80.0) ext.danger.copy(alpha = 0.5f) else ext.line,
              ),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = wk.key,
                  fontFamily = if (isDevanagari) DevanagariFontFamily else JetBrainsMonoFamily,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                )
                Column {
                  Text(
                    text = "${wk.accuracy.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (wk.accuracy < 80.0) ext.danger else ext.warn,
                  )
                  Text(
                    text = "${wk.attempts} taps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    SectionCard(
      title = "Session log (${filteredResults.size})",
      description = "Sorted newest first",
    ) {
      if (filteredResults.isEmpty()) {
        EmptyStateCard(
          title = "No sessions match this filter",
          description = "Try widening your timeframe or selecting 'All modes'.",
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredResults.take(30).forEach { r ->
            Surface(
              color = ext.panel,
              shape = MaterialTheme.shapes.small,
              border = BorderStroke(1.dp, ext.line),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = r.refTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BadgePill(text = r.mode.label, tone = Tone.NEUTRAL)
                    Text(
                      text = "${StatsCalculator.formatDuration(r.elapsedSeconds)} · ${StatsCalculator.formatRelative(r.createdAtMillis)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "${r.netWpm.toInt()} WPM",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${TypingEngine.round(r.accuracy, 0).toInt()}% · ${r.errors} err",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.success,
                  )
                }
              }
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}
