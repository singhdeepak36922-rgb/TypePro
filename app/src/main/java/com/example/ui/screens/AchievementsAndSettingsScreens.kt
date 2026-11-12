package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AchievementTier
import com.example.domain.CATEGORIES
import com.example.domain.DURATIONS
import com.example.domain.KeyboardLayoutPref
import com.example.domain.Language
import com.example.domain.ThemeMode
import com.example.domain.UserSettings
import com.example.ui.TypeProAppState
import com.example.ui.components.BadgePill
import com.example.ui.components.LinearProgressMeter
import com.example.ui.components.SectionCard
import com.example.ui.components.Tone
import com.example.ui.theme.LocalTypeProColors
import kotlin.math.roundToInt

private fun achievementIcon(iconName: String): ImageVector {
  return when (iconName) {
    "flag" -> Icons.Filled.Flag
    "gauge" -> Icons.Filled.Speed
    "zap" -> Icons.Filled.Bolt
    "rocket" -> Icons.Filled.RocketLaunch
    "trophy" -> Icons.Filled.EmojiEvents
    "target" -> Icons.Filled.TrackChanges
    "gem" -> Icons.Filled.Diamond
    "flame" -> Icons.Filled.LocalFireDepartment
    "calendar" -> Icons.Filled.CalendarMonth
    "book" -> Icons.Filled.School
    "layers" -> Icons.Filled.Layers
    "languages" -> Icons.Filled.Translate
    else -> Icons.Filled.EmojiEvents
  }
}

private fun tierTone(tier: AchievementTier): Tone {
  return when (tier) {
    AchievementTier.BRONZE -> Tone.WARN
    AchievementTier.SILVER -> Tone.NEUTRAL
    AchievementTier.GOLD -> Tone.BRAND
    AchievementTier.PLATINUM -> Tone.CYAN
  }
}

@Composable
fun AchievementsScreen(
  state: TypeProAppState,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val unlockedCount = state.achievements.count { it.unlocked }
  val total = state.achievements.size.coerceAtLeast(1)

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Milestones & badges",
      description = "Unlocked automatically as you improve WPM, accuracy, streaks, lessons, and Hindi skills.",
      action = {
        BadgePill(text = "$unlockedCount/$total unlocked", tone = Tone.SUCCESS)
      },
    ) {
      LinearProgressMeter(
        percent = (unlockedCount.toFloat() / total) * 100f,
        tone = Tone.SUCCESS,
        height = 10.dp,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      state.achievements.forEach { badge ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = if (badge.unlocked) MaterialTheme.colorScheme.surface else ext.panel,
          border = BorderStroke(
            1.dp,
            if (badge.unlocked) ext.success.copy(alpha = 0.5f) else ext.line,
          ),
          modifier = Modifier.fillMaxWidth().testTag("badge_card_${badge.key}"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              color = if (badge.unlocked) ext.successSoft else ext.panel,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.size(46.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = achievementIcon(badge.iconName),
                  contentDescription = null,
                  tint = if (badge.unlocked) ext.success else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = badge.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                )
                BadgePill(text = badge.tier.label, tone = tierTone(badge.tier))
              }
              Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(modifier = Modifier.weight(1f)) {
                  LinearProgressMeter(
                    percent = badge.progressPercent.toFloat(),
                    tone = if (badge.unlocked) Tone.SUCCESS else Tone.BRAND,
                    height = 6.dp,
                  )
                }
                Icon(
                  imageVector = if (badge.unlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                  contentDescription = if (badge.unlocked) "Unlocked" else "Locked",
                  tint = if (badge.unlocked) ext.success else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndProfileScreen(
  state: TypeProAppState,
  userEmail: String?,
  onUpdateSettings: ((UserSettings) -> UserSettings) -> Unit,
  onClearHistory: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val settings = state.settings
  var displayNameDraft by remember(settings.displayName) { mutableStateOf(settings.displayName) }
  var goalMinutesSlider by remember(settings.dailyGoalMinutes) {
    mutableFloatStateOf(settings.dailyGoalMinutes.toFloat())
  }
  var showClearDialog by remember { mutableStateOf(false) }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text("Clear typing history?") },
      text = {
        Text("This permanently deletes all saved test results, lesson progress, weak-key stats, and game high scores from your account.")
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
          onClick = {
            showClearDialog = false
            onClearHistory()
          },
          modifier = Modifier.testTag("confirm_clear_history_button"),
        ) {
          Text("Delete all")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Profile Card
    SectionCard(
      title = "Profile & Google account",
      description = userEmail ?: "Signed in with Google",
    ) {
      OutlinedTextField(
        value = displayNameDraft,
        onValueChange = { displayNameDraft = it },
        label = { Text("Display name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("display_name_input"),
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          enabled = displayNameDraft.trim().isNotEmpty() && displayNameDraft.trim() != settings.displayName,
          onClick = {
            val clean = displayNameDraft.trim().take(80)
            onUpdateSettings { it.copy(displayName = clean) }
          },
          modifier = Modifier.testTag("save_display_name_button"),
        ) {
          Text("Save name")
        }
        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier.testTag("sign_out_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Sign out")
        }
      }
    }

    // Appearance & Defaults
    SectionCard(
      title = "Appearance & layout",
      description = "Customize theme, preferred language, and default virtual keyboard.",
    ) {
      Text("THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
          FilterChip(
            selected = settings.theme == mode,
            onClick = { onUpdateSettings { it.copy(theme = mode) } },
            label = { Text(mode.label) },
            modifier = Modifier.testTag("theme_chip_${mode.value}"),
          )
        }
      }

      Text("PREFERRED LANGUAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Language.entries.forEach { lang ->
          FilterChip(
            selected = settings.language == lang,
            onClick = { onUpdateSettings { it.copy(language = lang) } },
            label = { Text(lang.label) },
          )
        }
      }

      Text("VIRTUAL KEYBOARD LAYOUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KeyboardLayoutPref.entries.forEach { pref ->
          FilterChip(
            selected = settings.keyboardLayout == pref,
            onClick = { onUpdateSettings { it.copy(keyboardLayout = pref) } },
            label = { Text(pref.label) },
          )
        }
      }
    }

    // Typing Behavior
    SectionCard(
      title = "Typing behavior & daily goal",
      description = "Changes sync immediately across your signed-in sessions.",
    ) {
      SettingSwitchRow(
        title = "Sound feedback",
        subtitle = "Play subtle tone cues for keystrokes, errors, and completion.",
        checked = settings.soundEnabled,
        onCheckedChange = { checked -> onUpdateSettings { it.copy(soundEnabled = checked) } },
        testTag = "switch_sound_enabled",
      )
      SettingSwitchRow(
        title = "Show virtual keyboard",
        subtitle = "Display the finger-colored keyboard guide during typing sessions.",
        checked = settings.showKeyboard,
        onCheckedChange = { checked -> onUpdateSettings { it.copy(showKeyboard = checked) } },
        testTag = "switch_show_keyboard",
      )
      SettingSwitchRow(
        title = "Strict mode",
        subtitle = "Disable Backspace during drills & tests to enforce forward accuracy.",
        checked = settings.strictMode,
        onCheckedChange = { checked -> onUpdateSettings { it.copy(strictMode = checked) } },
        testTag = "switch_strict_mode",
      )

      Spacer(Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("DAILY PRACTICE GOAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${goalMinutesSlider.roundToInt()} min/day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
      Slider(
        value = goalMinutesSlider,
        onValueChange = { goalMinutesSlider = it },
        valueRange = 5f..120f,
        steps = 22,
        onValueChangeFinished = {
          onUpdateSettings { it.copy(dailyGoalMinutes = goalMinutesSlider.roundToInt().coerceIn(5, 120)) }
        },
        modifier = Modifier.testTag("daily_goal_slider"),
      )

      Text("DEFAULT TEST DURATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DURATIONS.forEach { sec ->
          FilterChip(
            selected = settings.defaultDuration == sec,
            onClick = { onUpdateSettings { it.copy(defaultDuration = sec) } },
            label = { Text("${sec / 60}m") },
          )
        }
      }

      Text("DEFAULT TEST CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CATEGORIES.forEach { cat ->
          FilterChip(
            selected = settings.defaultCategory == cat.value,
            onClick = { onUpdateSettings { it.copy(defaultCategory = cat.value) } },
            label = { Text(cat.label) },
          )
        }
      }
    }

    // Danger Zone
    SectionCard(
      title = "Data management",
      description = "Reset your typing logs, lesson completions, and weak-key analytics.",
    ) {
      Surface(
        color = ext.dangerSoft,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Clear history", style = MaterialTheme.typography.titleSmall, color = ext.danger)
            Text(
              text = "${state.results.size} saved sessions in cloud",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
            onClick = { showClearDialog = true },
            modifier = Modifier.testTag("clear_history_button"),
          ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Clear")
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun SettingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
