package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.CATEGORIES
import com.example.domain.ContentCatalog
import com.example.domain.DURATIONS
import com.example.domain.Language
import com.example.domain.PassageDef
import com.example.domain.ResultInput
import com.example.domain.TypingMode
import com.example.domain.UserSettings
import com.example.ui.TypeProAppState
import com.example.ui.components.BadgePill
import com.example.ui.components.SectionCard
import com.example.ui.components.Tone
import com.example.ui.components.TypingSurface
import com.example.ui.theme.LocalTypeProColors

private fun matchesCategory(passage: PassageDef, category: String): Boolean {
  return when (category) {
    "general-english" -> passage.language == Language.EN &&
      passage.category in listOf("general-english", "intermediate", "advanced")
    "general-hindi" -> passage.language == Language.HI
    else -> passage.category == category || passage.difficulty.value == category
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestScreen(
  state: TypeProAppState,
  onUpdateSettings: ((UserSettings) -> UserSettings) -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  var duration by remember(state.settings.defaultDuration) {
    mutableIntStateOf(state.settings.defaultDuration.takeIf { it in DURATIONS } ?: 60)
  }
  var category by remember(state.settings.defaultCategory) {
    mutableStateOf(
      if (CATEGORIES.any { it.value == state.settings.defaultCategory }) state.settings.defaultCategory
      else "beginner",
    )
  }
  var passageIndex by remember { mutableIntStateOf(0) }

  val filtered = remember(category) {
    val matches = ContentCatalog.PASSAGES.filter { matchesCategory(it, category) }
    matches.ifEmpty { ContentCatalog.PASSAGES }
  }
  val passage = filtered[passageIndex % filtered.size]

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Timed typing test",
      description = "Select duration and category. Clock begins on your first keystroke.",
      action = {
        BadgePill(text = "${duration / 60} min", tone = Tone.BRAND)
      },
    ) {
      Text(
        text = "DURATION",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        DURATIONS.forEach { sec ->
          FilterChip(
            selected = duration == sec,
            onClick = {
              duration = sec
              onUpdateSettings { it.copy(defaultDuration = sec) }
            },
            label = { Text("${sec / 60} min") },
            modifier = Modifier.testTag("duration_chip_$sec"),
          )
        }
      }

      Text(
        text = "CATEGORY",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CATEGORIES.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { cat ->
              val selected = category == cat.value
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(MaterialTheme.shapes.medium)
                  .clickable {
                    category = cat.value
                    passageIndex = 0
                    onUpdateSettings { it.copy(defaultCategory = cat.value) }
                  }
                  .testTag("category_card_${cat.value}"),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) ext.brandSoft else ext.panel,
                border = BorderStroke(
                  1.dp,
                  if (selected) MaterialTheme.colorScheme.primary else ext.line,
                ),
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = cat.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = cat.description,
                    style = MaterialTheme.typography.bodySmall,
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
      title = passage.title,
      description = "${passage.language.label} · ${passage.difficulty.label} · ${passage.wordCount} words",
      action = {
        OutlinedButton(
          onClick = { passageIndex = (passageIndex + 1) % filtered.size },
          modifier = Modifier.testTag("new_passage_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("New")
        }
      },
    ) {
      TypingSurface(
        target = passage.content,
        language = passage.language,
        durationSeconds = duration,
        mode = TypingMode.TEST,
        refId = passage.slug,
        refTitle = passage.title,
        settings = state.settings,
        onSaveResult = onSaveResult,
        allowVirtualInput = passage.language == Language.HI || state.settings.showKeyboard,
        restartLabel = "Restart test",
        extraResultActions = {
          OutlinedButton(onClick = { passageIndex = (passageIndex + 1) % filtered.size }) {
            Text("Next passage")
          }
        },
      )
    }
    Spacer(Modifier.height(12.dp))
  }
}
