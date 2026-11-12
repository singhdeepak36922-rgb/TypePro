package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ContentCatalog
import com.example.domain.StatsCalculator
import com.example.domain.TypingEngine
import com.example.ui.TypeProAppState
import com.example.ui.components.ActivityBarChart
import com.example.ui.components.BadgePill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoalProgressRing
import com.example.ui.components.LinearProgressMeter
import com.example.ui.components.SectionCard
import com.example.ui.components.StatCard
import com.example.ui.components.Tone
import com.example.ui.components.WpmTrendChart
import com.example.ui.theme.LocalTypeProColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
  state: TypeProAppState,
  onStartTest: () -> Unit,
  onOpenLesson: (String) -> Unit,
  onBrowseLessons: () -> Unit,
  onOpenHindi: () -> Unit,
  onOpenGames: () -> Unit,
  onOpenReports: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val summary = state.summary
  val unlockedCount = state.achievements.count { it.unlocked }
  val completedSlugs = state.lessonProgress.filterValues { it.completed }.keys
  val nextLesson = ContentCatalog.LESSONS.firstOrNull { it.slug !in completedSlugs }
    ?: ContentCatalog.LESSONS.first()

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Hero Banner with Daily Goal Ring
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, ext.line),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BadgePill(
              text = "Welcome back, ${state.settings.displayName}",
              tone = Tone.BRAND,
            )
            Text(
              text = "TypePro Studio",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = if (summary.totalResults == 0) {
                "Take a baseline test or start lesson 1 to build your speed & accuracy profile."
              } else {
                "Personal best ${summary.bestWpm.toInt()} WPM · ${TypingEngine.round(summary.avgAccuracy, 1)}% avg accuracy · ${summary.streak}d streak"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.width(12.dp))
          GoalProgressRing(
            percent = summary.goalPercent,
            todayMinutes = summary.todayMinutes,
            goalMinutes = state.settings.dailyGoalMinutes,
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onStartTest,
            modifier = Modifier.testTag("dashboard_start_test_button"),
          ) {
            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Start Typing Test")
          }
          OutlinedButton(
            onClick = { onOpenLesson(nextLesson.slug) },
            modifier = Modifier.testTag("dashboard_continue_lesson_button"),
          ) {
            Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Continue Lesson")
          }
          OutlinedButton(
            onClick = onOpenHindi,
            modifier = Modifier.testTag("dashboard_hindi_button"),
          ) {
            Text("हिन्दी Practice")
          }
          OutlinedButton(
            onClick = onOpenGames,
            modifier = Modifier.testTag("dashboard_games_button"),
          ) {
            Text("Games")
          }
        }
      }
    }

    // Key Stats Grid (2 columns)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatCard(
        label = "Avg Net WPM",
        value = "${summary.avgWpm.toInt()}",
        icon = Icons.Filled.Speed,
        hint = "Best ${summary.bestWpm.toInt()} WPM",
        tone = Tone.BRAND,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Avg Accuracy",
        value = "${TypingEngine.round(summary.avgAccuracy, 1)}%",
        icon = Icons.Filled.TrackChanges,
        hint = "Best ${TypingEngine.round(summary.bestAccuracy, 1)}%",
        tone = Tone.SUCCESS,
        modifier = Modifier.weight(1f),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatCard(
        label = "Practice Time",
        value = StatsCalculator.formatDuration(summary.totalPracticeSeconds),
        icon = Icons.Filled.CalendarMonth,
        hint = "${summary.totalTests} timed tests",
        tone = Tone.CYAN,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Streak",
        value = "${summary.streak}d",
        icon = Icons.Filled.LocalFireDepartment,
        hint = "$unlockedCount/${state.achievements.size} badges",
        tone = Tone.WARN,
        modifier = Modifier.weight(1f),
      )
    }

    // Continue Learning Card
    SectionCard(
      title = "Continue learning",
      description = "Structured touch-typing curriculum (${completedSlugs.size}/${ContentCatalog.LESSONS.size} completed)",
      action = {
        TextButton(onClick = onBrowseLessons) {
          Text("All lessons")
        }
      },
    ) {
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ext.line),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            BadgePill(text = nextLesson.level.label, tone = Tone.BRAND)
            Text(
              text = "${nextLesson.estMinutes} min · ${nextLesson.focus}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = nextLesson.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = nextLesson.courseTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          LinearProgressMeter(
            percent = if (ContentCatalog.LESSONS.isEmpty()) 0f
            else (completedSlugs.size.toFloat() / ContentCatalog.LESSONS.size) * 100f,
            tone = Tone.SUCCESS,
          )
          Button(
            onClick = { onOpenLesson(nextLesson.slug) },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Start ${nextLesson.title}")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    // Weekly Activity Chart
    SectionCard(
      title = "Weekly activity",
      description = "Minutes practiced over the last 7 days (${summary.weekMinutes.toInt()}m this week)",
      action = {
        TextButton(onClick = onOpenReports) {
          Text("Reports")
        }
      },
    ) {
      ActivityBarChart(data = summary.weekly)
    }

    // WPM Trend Chart
    SectionCard(
      title = "Speed & accuracy trend",
      description = "30-day net WPM and accuracy progression",
    ) {
      WpmTrendChart(data = summary.monthly)
    }

    // Recent Sessions
    SectionCard(
      title = "Recent sessions",
      description = "Synced across your devices",
    ) {
      if (summary.recent.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Filled.EmojiEvents,
          title = "Your history is empty",
          description = "Saved tests, lessons, practices, Hindi sessions, and games appear here.",
          actionLabel = "Take your first test",
          onAction = onStartTest,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          summary.recent.take(6).forEach { result ->
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
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = result.refTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${result.mode.label} · ${StatsCalculator.formatRelative(result.createdAtMillis)} · ${result.language.code.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "${result.netWpm.toInt()} WPM",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${TypingEngine.round(result.accuracy, 0).toInt()}% acc",
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
