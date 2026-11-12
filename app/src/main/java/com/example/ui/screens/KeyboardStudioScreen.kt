package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Finger
import com.example.domain.KeyErrorPair
import com.example.domain.KeyboardLayoutPref
import com.example.domain.KeyboardLayouts
import com.example.domain.Language
import com.example.domain.ResultInput
import com.example.domain.TypingEngine
import com.example.domain.TypingMode
import com.example.ui.TypeProAppState
import com.example.ui.components.BadgePill
import com.example.ui.components.SectionCard
import com.example.ui.components.Tone
import com.example.ui.components.VirtualKeyboard
import com.example.ui.sound.SoundCue
import com.example.ui.sound.SoundFeedback
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTypeProColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardStudioScreen(
  state: TypeProAppState,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val scope = rememberCoroutineScope()

  var layoutLang by remember(state.settings.keyboardLayout) {
    mutableStateOf(
      if (state.settings.keyboardLayout == KeyboardLayoutPref.INSCRIPT) Language.HI else Language.EN,
    )
  }
  var selectedFinger by remember { mutableStateOf<Finger?>(null) }

  val drillPrompt = remember(layoutLang) {
    if (layoutLang == Language.HI) {
      listOf("प", "र", "क", "त", "च", "ो", "े", "्", "ि", "ु", "ब", "ह", "ग", "द", "ज", "म", "न", "ल", "स")
    } else {
      listOf("a", "s", "d", "f", "j", "k", "l", ";", "q", "w", "e", "r", "u", "i", "o", "p", "g", "h")
    }
  }

  var stepIndex by remember(layoutLang) { mutableIntStateOf(0) }
  var hits by remember(layoutLang) { mutableIntStateOf(0) }
  var misses by remember(layoutLang) { mutableIntStateOf(0) }
  var startedAtMs by remember(layoutLang) { mutableStateOf<Long?>(null) }
  var lastTapFeedback by remember(layoutLang) { mutableStateOf("Tap the highlighted key below to drill finger placement.") }
  var savingMsg by remember(layoutLang) { mutableStateOf<String?>(null) }
  val collectedErrors = remember(layoutLang) { mutableListOf<KeyErrorPair>() }
  val collectedCorrect = remember(layoutLang) { mutableMapOf<String, Int>() }

  val expectedKey = drillPrompt[stepIndex % drillPrompt.size]
  val totalAttempts = hits + misses
  val accuracy = if (totalAttempts == 0) 100.0 else TypingEngine.round((hits.toDouble() / totalAttempts) * 100.0)

  fun resetDrill() {
    stepIndex = 0
    hits = 0
    misses = 0
    startedAtMs = null
    savingMsg = null
    collectedErrors.clear()
    collectedCorrect.clear()
    lastTapFeedback = "Tap the highlighted key below to drill finger placement."
  }

  fun onKeyPressed(ch: String) {
    if (startedAtMs == null) startedAtMs = System.currentTimeMillis()
    if (ch == expectedKey) {
      hits += 1
      collectedCorrect[expectedKey] = (collectedCorrect[expectedKey] ?: 0) + 1
      stepIndex += 1
      lastTapFeedback = "Good! Hit '$ch'"
      SoundFeedback.play(SoundCue.KEY, state.settings.soundEnabled)
    } else {
      misses += 1
      collectedErrors.add(KeyErrorPair(expected = expectedKey, actual = ch))
      lastTapFeedback = "Tapped '$ch' — expected '$expectedKey'"
      SoundFeedback.play(SoundCue.ERROR, state.settings.soundEnabled)
    }
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Virtual keyboard studio",
      description = "Explore English QWERTY & Hindi InScript finger zones with an interactive key drill.",
      action = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = layoutLang == Language.EN,
            onClick = { layoutLang = Language.EN },
            label = { Text("QWERTY") },
            modifier = Modifier.testTag("studio_lang_en"),
          )
          FilterChip(
            selected = layoutLang == Language.HI,
            onClick = { layoutLang = Language.HI },
            label = { Text("InScript") },
            modifier = Modifier.testTag("studio_lang_hi"),
          )
        }
      },
    ) {
      // Drill target banner
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ext.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "TARGET KEY",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = expectedKey,
              fontFamily = if (layoutLang == Language.HI) DevanagariFontFamily else JetBrainsMonoFamily,
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = lastTapFeedback,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BadgePill(text = "$hits hits", tone = Tone.SUCCESS)
            BadgePill(text = "${accuracy.toInt()}% acc", tone = Tone.BRAND)
          }
        }
      }

      VirtualKeyboard(
        language = layoutLang,
        expectedGrapheme = expectedKey,
        interactive = true,
        onKeyInput = ::onKeyPressed,
        onBackspace = {},
        highlightedFinger = selectedFinger,
        compact = false,
      )

      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          enabled = totalAttempts >= 8,
          onClick = {
            val elapsedSec = ((System.currentTimeMillis() - (startedAtMs ?: System.currentTimeMillis())) / 1000L)
              .toInt().coerceAtLeast(1)
            val mins = elapsedSec / 60.0
            val netWpm = TypingEngine.round(((hits / 5.0) / mins))
            savingMsg = "Saving…"
            scope.launch {
              val res = onSaveResult(
                ResultInput(
                  mode = TypingMode.DRILL,
                  refId = "keyboard-drill-${layoutLang.code}",
                  refTitle = "${layoutLang.label} Keyboard Drill",
                  language = layoutLang,
                  durationSeconds = 0,
                  elapsedSeconds = elapsedSec,
                  grossWpm = TypingEngine.round((totalAttempts / 5.0) / mins),
                  netWpm = netWpm,
                  standardWpm = netWpm,
                  accuracy = accuracy,
                  errors = misses,
                  totalTyped = totalAttempts,
                  correctChars = hits,
                  keystrokeErrors = misses,
                  backspaceCount = 0,
                  score = hits * 5,
                  keyErrors = collectedErrors.toList(),
                  correctKeys = collectedCorrect.toMap(),
                ),
              )
              savingMsg = if (res.isSuccess) "Drill saved ($hits hits, ${accuracy.toInt()}% accuracy)."
              else res.exceptionOrNull()?.localizedMessage ?: "Failed to save drill."
            }
          },
          modifier = Modifier.testTag("save_drill_button"),
        ) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Save drill (${totalAttempts.coerceAtMost(8)}/8+)")
        }
        OutlinedButton(onClick = ::resetDrill) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Reset")
        }
      }

      if (savingMsg != null) {
        Text(
          text = savingMsg!!,
          style = MaterialTheme.typography.bodySmall,
          color = ext.success,
        )
      }
    }

    SectionCard(
      title = "Finger zone filter",
      description = "Tap any finger below to highlight its keys on the keyboard.",
    ) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = selectedFinger == null,
          onClick = { selectedFinger = null },
          label = { Text("All fingers") },
        )
        Finger.entries.forEach { finger ->
          FilterChip(
            selected = selectedFinger == finger,
            onClick = { selectedFinger = if (selectedFinger == finger) null else finger },
            leadingIcon = {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(finger.color, CircleShape),
              )
            },
            label = { Text(finger.label) },
          )
        }
      }
    }

    SectionCard(
      title = "Home row anchor guide",
      description = if (layoutLang == Language.EN) {
        "Rest left fingers on A S D F and right fingers on J K L ;. Thumbs hover over Space."
      } else {
        "Left hand rests on ो े ् ि and right hand on र क त च. Thumbs rest on Space."
      },
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val homeKeys = if (layoutLang == Language.EN) {
          KeyboardLayouts.HOME_ROW
        } else {
          listOf("ो", "े", "्", "ि", "र", "क", "त", "च")
        }
        homeKeys.forEach { key ->
          Surface(
            modifier = Modifier.weight(1f),
            color = ext.panel,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = key,
                fontFamily = if (layoutLang == Language.HI) DevanagariFontFamily else JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    Spacer(Modifier.height(12.dp))
  }
}
