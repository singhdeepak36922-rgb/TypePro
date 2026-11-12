package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.KeyErrorPair
import com.example.domain.Language
import com.example.domain.ResultInput
import com.example.domain.TypingEngine
import com.example.domain.TypingMetrics
import com.example.domain.TypingMode
import com.example.domain.UserSettings
import com.example.ui.sound.SoundCue
import com.example.ui.sound.SoundFeedback
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTypeProColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SessionSaveStatus {
  IDLE,
  SAVING,
  SAVED,
  ERROR,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypingSurface(
  target: String,
  language: Language,
  durationSeconds: Int,
  mode: TypingMode,
  refId: String,
  refTitle: String,
  settings: UserSettings,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  allowVirtualInput: Boolean = true,
  restartLabel: String = "Restart",
  instructions: String? = null,
  autoSaveOnFinish: Boolean = true,
  onCustomFinish: ((TypingMetrics, ResultInput) -> Unit)? = null,
  extraResultActions: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val scope = rememberCoroutineScope()

  var typed by remember(target, durationSeconds, refId) { mutableStateOf("") }
  var startedAtMs by remember(target, durationSeconds, refId) { mutableStateOf<Long?>(null) }
  var elapsedMs by remember(target, durationSeconds, refId) { mutableLongStateOf(0L) }
  var keystrokeErrors by remember(target, durationSeconds, refId) { mutableIntStateOf(0) }
  var backspaceCount by remember(target, durationSeconds, refId) { mutableIntStateOf(0) }
  var finishedMetrics by remember(target, durationSeconds, refId) { mutableStateOf<TypingMetrics?>(null) }
  var saveStatus by remember(target, durationSeconds, refId) { mutableStateOf(SessionSaveStatus.IDLE) }
  var saveError by remember(target, durationSeconds, refId) { mutableStateOf<String?>(null) }

  val targetGraphemes = remember(target) { TypingEngine.toGraphemes(target) }
  val typedGraphemes = remember(typed) { TypingEngine.toGraphemes(typed) }

  fun finishSession(finalTyped: String) {
    if (finishedMetrics != null) return
    val now = System.currentTimeMillis()
    val start = startedAtMs ?: now
    val finalElapsed = (now - start).coerceAtLeast(1000L)
    val cappedElapsed = if (durationSeconds > 0) {
      minOf(finalElapsed, durationSeconds * 1000L)
    } else {
      finalElapsed
    }
    elapsedMs = cappedElapsed

    val metrics = TypingEngine.computeMetrics(
      target = target,
      typed = finalTyped,
      elapsedMs = cappedElapsed,
      keystrokeErrors = keystrokeErrors,
      backspaceCount = backspaceCount,
    )
    finishedMetrics = metrics
    SoundFeedback.play(SoundCue.FINISH, settings.soundEnabled)

    val payload = ResultInput(
      mode = mode,
      refId = refId,
      refTitle = refTitle,
      language = language,
      durationSeconds = durationSeconds,
      elapsedSeconds = (cappedElapsed / 1000L).toInt().coerceAtLeast(1),
      grossWpm = metrics.grossWpm,
      netWpm = metrics.netWpm,
      standardWpm = metrics.standardWpm,
      accuracy = metrics.accuracy,
      errors = metrics.incorrectChars,
      totalTyped = metrics.totalTyped,
      correctChars = metrics.correctChars,
      keystrokeErrors = metrics.keystrokeErrors,
      backspaceCount = metrics.backspaceCount,
      score = (metrics.netWpm * (metrics.accuracy / 100.0)).toInt(),
      keyErrors = metrics.keyErrors.map { KeyErrorPair(it.expected, it.actual) },
      correctKeys = TypingEngine.collectCorrectKeyCounts(target, finalTyped),
    )

    if (onCustomFinish != null) {
      onCustomFinish(metrics, payload)
    }

    if (autoSaveOnFinish) {
      saveStatus = SessionSaveStatus.SAVING
      scope.launch {
        val res = onSaveResult(payload)
        if (res.isSuccess) {
          saveStatus = SessionSaveStatus.SAVED
        } else {
          saveStatus = SessionSaveStatus.ERROR
          saveError = res.exceptionOrNull()?.localizedMessage ?: "Failed to save"
        }
      }
    }
  }

  // Live timer tick
  LaunchedEffect(startedAtMs, finishedMetrics) {
    val start = startedAtMs ?: return@LaunchedEffect
    while (finishedMetrics == null) {
      val now = System.currentTimeMillis()
      val diff = now - start
      elapsedMs = diff
      if (durationSeconds > 0 && diff >= durationSeconds * 1000L) {
        finishSession(typed)
        break
      }
      delay(100L)
    }
  }

  fun applyNewInput(nextText: String) {
    if (finishedMetrics != null) return
    val prevGraphemes = TypingEngine.toGraphemes(typed)
    val nextGraphemes = TypingEngine.toGraphemes(nextText)

    if (settings.strictMode && nextGraphemes.size < prevGraphemes.size) {
      return
    }

    if (startedAtMs == null && nextText.isNotEmpty()) {
      startedAtMs = System.currentTimeMillis()
    }

    if (nextGraphemes.size < prevGraphemes.size) {
      backspaceCount += (prevGraphemes.size - nextGraphemes.size)
      SoundFeedback.play(SoundCue.KEY, settings.soundEnabled)
    } else if (nextGraphemes.size > prevGraphemes.size) {
      var hadError = false
      for (i in prevGraphemes.size until nextGraphemes.size) {
        val expected = targetGraphemes.getOrNull(i)
        if (expected != null && nextGraphemes[i] != expected) {
          keystrokeErrors += 1
          hadError = true
        }
      }
      SoundFeedback.play(if (hadError) SoundCue.ERROR else SoundCue.KEY, settings.soundEnabled)
    }

    typed = nextText
    if (nextGraphemes.size >= targetGraphemes.size && targetGraphemes.isNotEmpty()) {
      finishSession(nextText)
    }
  }

  fun restart() {
    typed = ""
    startedAtMs = null
    elapsedMs = 0L
    keystrokeErrors = 0
    backspaceCount = 0
    finishedMetrics = null
    saveStatus = SessionSaveStatus.IDLE
    saveError = null
  }

  val liveMetrics = finishedMetrics ?: TypingEngine.computeMetrics(
    target = target,
    typed = typed,
    elapsedMs = elapsedMs,
    keystrokeErrors = keystrokeErrors,
    backspaceCount = backspaceCount,
  )

  val activeResult = finishedMetrics
  if (activeResult != null) {
    SessionResultCard(
      metrics = activeResult,
      saveStatus = saveStatus,
      saveError = saveError,
      language = language,
      targetGraphemes = targetGraphemes,
      typedGraphemes = typedGraphemes,
      durationSeconds = durationSeconds,
      restartLabel = restartLabel,
      onRestart = ::restart,
      extraResultActions = extraResultActions,
      modifier = modifier,
    )
    return
  }

  val remainingSec = if (durationSeconds > 0) {
    ((durationSeconds * 1000L - elapsedMs).coerceAtLeast(0L) / 1000L).toInt()
  } else {
    (elapsedMs / 1000L).toInt()
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (!instructions.isNullOrBlank()) {
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, ext.line),
      ) {
        Text(
          text = instructions,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    // Live Telemetry Bar
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      BadgePill(text = "${liveMetrics.netWpm.toInt()} WPM", tone = Tone.BRAND)
      BadgePill(text = "${liveMetrics.accuracy.toInt()}% Acc", tone = Tone.SUCCESS)
      BadgePill(text = "${liveMetrics.incorrectChars} Err", tone = if (liveMetrics.incorrectChars > 0) Tone.DANGER else Tone.NEUTRAL)
      BadgePill(
        text = if (durationSeconds > 0) "${remainingSec}s left" else "${remainingSec}s",
        tone = Tone.CYAN,
      )
      if (settings.strictMode) {
        BadgePill(text = "Strict mode", tone = Tone.WARN)
      }
    }

    LinearProgressMeter(percent = liveMetrics.completion.toFloat(), tone = Tone.BRAND, height = 6.dp)

    // Annotated Passage Display
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = ext.success
    val dangerColor = ext.danger
    val dangerSoft = ext.dangerSoft
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    val annotatedPassage = remember(targetGraphemes, typedGraphemes, primaryColor, successColor, dangerColor) {
      buildAnnotatedString {
        targetGraphemes.forEachIndexed { idx, g ->
          val typedG = typedGraphemes.getOrNull(idx)
          when {
            typedG == null && idx == typedGraphemes.size -> {
              withStyle(
                SpanStyle(
                  color = onSurface,
                  background = primaryColor.copy(alpha = 0.25f),
                  textDecoration = TextDecoration.Underline,
                  fontWeight = FontWeight.Bold,
                ),
              ) {
                append(g)
              }
            }
            typedG == null -> {
              withStyle(SpanStyle(color = mutedColor)) { append(g) }
            }
            typedG == g -> {
              withStyle(SpanStyle(color = successColor)) { append(g) }
            }
            else -> {
              withStyle(
                SpanStyle(
                  color = dangerColor,
                  background = dangerSoft,
                  textDecoration = TextDecoration.LineThrough,
                ),
              ) {
                append(g)
              }
            }
          }
        }
      }
    }

    Surface(
      shape = MaterialTheme.shapes.medium,
      color = ext.panel,
      border = BorderStroke(1.dp, ext.line),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 120.dp, max = 210.dp),
    ) {
      Box(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(14.dp),
      ) {
        Text(
          text = annotatedPassage,
          fontFamily = if (language == Language.HI) DevanagariFontFamily else JetBrainsMonoFamily,
          fontSize = if (language == Language.HI) 19.sp else 16.sp,
          lineHeight = if (language == Language.HI) 30.sp else 26.sp,
        )
      }
    }

    // Input Box
    OutlinedTextField(
      value = typed,
      onValueChange = ::applyNewInput,
      label = {
        Text(
          if (startedAtMs == null) "Tap here & start typing (timer starts on first key)…"
          else "Keep typing…",
        )
      },
      singleLine = false,
      maxLines = 3,
      keyboardOptions = KeyboardOptions(
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
      ),
      textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = if (language == Language.HI) DevanagariFontFamily else JetBrainsMonoFamily,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("typing_input_field"),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${typedGraphemes.size} / ${targetGraphemes.size} chars",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (typed.isNotEmpty() && durationSeconds == 0) {
          OutlinedButton(
            onClick = { finishSession(typed) },
            modifier = Modifier.testTag("finish_session_button"),
          ) {
            Text("Finish")
          }
        }
        OutlinedButton(
          onClick = ::restart,
          modifier = Modifier.testTag("restart_session_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(restartLabel)
        }
      }
    }

    if (settings.showKeyboard) {
      val nextChar = TypingEngine.nextExpectedGrapheme(target, typed)
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.line),
      ) {
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = if (language == Language.HI) "HINDI INSCRIPT KEYBOARD" else "VIRTUAL KEYBOARD",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Next: ${nextChar ?: "—"}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          VirtualKeyboard(
            language = language,
            expectedGrapheme = nextChar,
            interactive = allowVirtualInput,
            onKeyInput = { ch -> applyNewInput(typed + ch) },
            onBackspace = {
              if (typedGraphemes.isNotEmpty()) {
                applyNewInput(typedGraphemes.dropLast(1).joinToString(""))
              }
            },
            compact = true,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionResultCard(
  metrics: TypingMetrics,
  saveStatus: SessionSaveStatus,
  saveError: String?,
  language: Language,
  targetGraphemes: List<String>,
  typedGraphemes: List<String>,
  durationSeconds: Int,
  restartLabel: String,
  onRestart: () -> Unit,
  extraResultActions: (@Composable () -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  val headline = when {
    metrics.accuracy >= 98.0 -> "Excellent precision"
    metrics.accuracy >= 92.0 -> "Solid performance"
    else -> "Good effort — focus on accuracy"
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, ext.line),
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
              text = "SESSION COMPLETE",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = headline,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          when (saveStatus) {
            SessionSaveStatus.SAVED -> BadgePill(text = "Saved", tone = Tone.SUCCESS)
            SessionSaveStatus.SAVING -> BadgePill(text = "Saving…", tone = Tone.BRAND)
            SessionSaveStatus.ERROR -> BadgePill(text = saveError ?: "Save error", tone = Tone.DANGER)
            SessionSaveStatus.IDLE -> BadgePill(text = "Done", tone = Tone.NEUTRAL)
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ResultMetricTile("Net WPM", "${metrics.netWpm.toInt()}", Tone.BRAND, Modifier.weight(1f))
          ResultMetricTile("Accuracy", "${TypingEngine.round(metrics.accuracy, 1)}%", Tone.SUCCESS, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ResultMetricTile("Gross WPM", "${metrics.grossWpm.toInt()}", Tone.NEUTRAL, Modifier.weight(1f))
          ResultMetricTile("Standard", "${metrics.standardWpm.toInt()}", Tone.NEUTRAL, Modifier.weight(1f))
          ResultMetricTile("Errors", "${metrics.incorrectChars}", Tone.DANGER, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ResultMetricTile("Keystroke Err", "${metrics.keystrokeErrors}", Tone.WARN, Modifier.weight(1f))
          ResultMetricTile("Backspaces", "${metrics.backspaceCount}", Tone.NEUTRAL, Modifier.weight(1f))
          ResultMetricTile("Elapsed", "${TypingEngine.round(metrics.elapsedMs / 1000.0, 1)}s", Tone.CYAN, Modifier.weight(1f))
        }

        // Mistake analysis
        Surface(
          color = ext.panel,
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, ext.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "MISTAKE ANALYSIS",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (metrics.keyErrors.isEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ext.success, modifier = Modifier.size(16.dp))
                Text("No incorrect characters!", style = MaterialTheme.typography.bodySmall, color = ext.success)
              }
            } else {
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.keyErrors.take(10).forEach { err ->
                  Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ext.line),
                  ) {
                    Text(
                      text = "${err.actual.ifBlank { "␣" }} → ${err.expected.ifBlank { "␣" }}",
                      fontFamily = if (language == Language.HI) DevanagariFontFamily else JetBrainsMonoFamily,
                      style = MaterialTheme.typography.bodySmall,
                      color = ext.danger,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = onRestart, modifier = Modifier.testTag("result_restart_button")) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(restartLabel)
          }
          if (extraResultActions != null) {
            extraResultActions()
          }
        }

        Text(
          text = if (durationSeconds > 0) {
            "Timed session (${durationSeconds}s). Correct: ${metrics.correctChars}/${metrics.totalTyped} chars."
          } else {
            "Untimed session. Correct: ${metrics.correctChars}/${metrics.totalTyped} chars."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Final Text Review
    val successColor = ext.success
    val dangerColor = ext.danger
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val reviewText = remember(targetGraphemes, typedGraphemes) {
      buildAnnotatedString {
        targetGraphemes.forEachIndexed { idx, g ->
          val typedG = typedGraphemes.getOrNull(idx)
          when {
            typedG == null -> withStyle(SpanStyle(color = mutedColor)) { append(g) }
            typedG == g -> withStyle(SpanStyle(color = successColor)) { append(g) }
            else -> withStyle(SpanStyle(color = dangerColor, textDecoration = TextDecoration.LineThrough)) { append(g) }
          }
        }
      }
    }
    SectionCard(title = "Final text review") {
      Text(
        text = reviewText,
        fontFamily = if (language == Language.HI) DevanagariFontFamily else JetBrainsMonoFamily,
        fontSize = 14.sp,
        lineHeight = 22.sp,
      )
    }
  }
}

@Composable
private fun ResultMetricTile(
  label: String,
  value: String,
  tone: Tone,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  Surface(
    modifier = modifier,
    color = ext.panel,
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(1.dp, ext.line),
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = toneColor(tone),
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
