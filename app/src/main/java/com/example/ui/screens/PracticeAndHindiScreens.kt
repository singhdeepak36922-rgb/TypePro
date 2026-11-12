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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ContentCatalog
import com.example.domain.Difficulty
import com.example.domain.KeyErrorPair
import com.example.domain.Language
import com.example.domain.ResultInput
import com.example.domain.Transliterator
import com.example.domain.TypingEngine
import com.example.domain.TypingMode
import com.example.ui.TypeProAppState
import com.example.ui.components.BadgePill
import com.example.ui.components.SectionCard
import com.example.ui.components.Tone
import com.example.ui.components.TypingSurface
import com.example.ui.components.VirtualKeyboard
import com.example.ui.sound.SoundCue
import com.example.ui.sound.SoundFeedback
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTypeProColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FreePracticeScreen(
  state: TypeProAppState,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  var isCustomMode by remember { mutableStateOf(false) }
  var language by remember { mutableStateOf(Language.EN) }
  var difficultyFilter by remember { mutableStateOf<Difficulty?>(null) }
  var index by remember { mutableIntStateOf(0) }
  var customDraft by remember { mutableStateOf("") }
  var activeCustom by remember { mutableStateOf<String?>(null) }

  val filtered = remember(language, difficultyFilter) {
    val list = ContentCatalog.PASSAGES.filter {
      it.language == language && (difficultyFilter == null || it.difficulty == difficultyFilter)
    }
    list.ifEmpty { ContentCatalog.PASSAGES.filter { it.language == language } }
  }
  val passage = filtered[index % filtered.size]
  val target = if (isCustomMode && activeCustom != null) activeCustom!! else passage.content
  val activeLang = if (isCustomMode && activeCustom != null) {
    if (Regex("[\\u0900-\\u097F]").containsMatchIn(activeCustom!!)) Language.HI else Language.EN
  } else {
    language
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Free practice",
      description = "Untimed training with random passages or your own custom English/Hindi text.",
    ) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = !isCustomMode,
          onClick = { isCustomMode = false },
          label = { Text("Random passage") },
        )
        FilterChip(
          selected = isCustomMode,
          onClick = { isCustomMode = true },
          label = { Text("My own text") },
        )
        if (!isCustomMode) {
          FilterChip(
            selected = language == Language.EN,
            onClick = {
              language = Language.EN
              index = 0
            },
            label = { Text("English") },
          )
          FilterChip(
            selected = language == Language.HI,
            onClick = {
              language = Language.HI
              index = 0
            },
            label = { Text("हिन्दी") },
          )
        }
      }

      if (!isCustomMode) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = difficultyFilter == null,
            onClick = { difficultyFilter = null; index = 0 },
            label = { Text("All levels") },
          )
          Difficulty.entries.forEach { diff ->
            FilterChip(
              selected = difficultyFilter == diff,
              onClick = { difficultyFilter = diff; index = 0 },
              label = { Text(diff.label) },
            )
          }
        }
      } else {
        OutlinedTextField(
          value = customDraft,
          onValueChange = { customDraft = it },
          label = { Text("Paste or type English / Hindi text (min 10 chars)") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("custom_practice_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { activeCustom = TypingEngine.normalizePassage(customDraft) },
            enabled = customDraft.trim().length >= 10,
            modifier = Modifier.testTag("use_custom_text_button"),
          ) {
            Text("Use this text")
          }
          OutlinedButton(onClick = { customDraft = ""; activeCustom = null }) {
            Text("Reset")
          }
        }
      }
    }

    SectionCard(
      title = if (isCustomMode && activeCustom != null) "Custom practice text" else passage.title,
      description = if (isCustomMode && activeCustom != null) {
        "${activeLang.label} · Untimed"
      } else {
        "${passage.language.label} · ${passage.difficulty.label} · Untimed"
      },
      action = if (!isCustomMode) {
        {
          OutlinedButton(onClick = { index = (index + 1) % filtered.size }) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Shuffle")
          }
        }
      } else null,
    ) {
      TypingSurface(
        target = target,
        language = activeLang,
        durationSeconds = 0,
        mode = TypingMode.PRACTICE,
        refId = if (isCustomMode) "custom" else passage.slug,
        refTitle = if (isCustomMode) "Custom practice" else passage.title,
        settings = state.settings,
        onSaveResult = onSaveResult,
        allowVirtualInput = true,
        restartLabel = "Restart practice",
      )
    }
    Spacer(Modifier.height(12.dp))
  }
}

@Composable
fun HindiTypingScreen(
  state: TypeProAppState,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val hindiPassages = remember { ContentCatalog.PASSAGES.filter { it.language == Language.HI } }
  var passageIndex by remember { mutableIntStateOf(0) }
  val passage = hindiPassages[passageIndex % hindiPassages.size]

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("InScript") },
        icon = { Icon(Icons.Filled.Keyboard, contentDescription = null) },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Phonetic") },
        icon = { Icon(Icons.Filled.Translate, contentDescription = null) },
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Reference") },
      )
    }

    when (selectedTab) {
      0 -> {
        SectionCard(
          title = passage.title,
          description = "${passage.difficulty.label} · ${passage.wordCount} words · InScript Devanagari",
          action = {
            OutlinedButton(onClick = { passageIndex = (passageIndex + 1) % hindiPassages.size }) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Next")
            }
          },
        ) {
          TypingSurface(
            target = passage.content,
            language = Language.HI,
            durationSeconds = 0,
            mode = TypingMode.HINDI,
            refId = "inscript-${passage.slug}",
            refTitle = "InScript · ${passage.title}",
            settings = state.settings.copy(showKeyboard = true),
            onSaveResult = onSaveResult,
            allowVirtualInput = true,
            restartLabel = "Restart drill",
            instructions = "Tap the on-screen InScript keys or type with your system Hindi keyboard. Conjuncts use the halant key (्). Devanagari matras are scored as single grapheme clusters.",
          )
        }
      }
      1 -> {
        PhoneticHindiPracticeCard(
          passage = passage,
          state = state,
          onNext = { passageIndex = (passageIndex + 1) % hindiPassages.size },
          onSaveResult = onSaveResult,
        )
      }
      2 -> {
        SectionCard(
          title = "Hindi InScript layout reference",
          description = "Standard Unicode Devanagari key map with finger colour guides.",
          action = { BadgePill(text = "Unicode", tone = Tone.BRAND) },
        ) {
          VirtualKeyboard(
            language = Language.HI,
            expectedGrapheme = null,
            interactive = false,
            compact = false,
          )
          Text(
            text = "Keep left hand on ा ै ् ि ु and right hand on प र क त च. Use halant (्) between consonants to form conjuncts like क्ष, त्र, ज्ञ.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun PhoneticHindiPracticeCard(
  passage: com.example.domain.PassageDef,
  state: TypeProAppState,
  onNext: () -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
) {
  val ext = LocalTypeProColors.current
  val scope = rememberCoroutineScope()

  var romanBuffer by remember(passage.slug) { mutableStateOf("") }
  var committedHindi by remember(passage.slug) { mutableStateOf("") }
  var startedAtMs by remember(passage.slug) { mutableStateOf<Long?>(null) }
  var elapsedMs by remember(passage.slug) { mutableLongStateOf(0L) }
  var finished by remember(passage.slug) { mutableStateOf(false) }
  var saveMsg by remember(passage.slug) { mutableStateOf("Commit words with Space; Finish to save.") }

  val targetGraphemes = remember(passage.content) { TypingEngine.toGraphemes(passage.content) }
  val committedGraphemes = remember(committedHindi) { TypingEngine.toGraphemes(committedHindi) }
  val preview = remember(romanBuffer) { Transliterator.transliterate(romanBuffer) }

  LaunchedEffect(startedAtMs, finished) {
    val start = startedAtMs ?: return@LaunchedEffect
    while (!finished) {
      elapsedMs = System.currentTimeMillis() - start
      delay(100L)
    }
  }

  fun finishPhonetic(finalCommitted: String) {
    if (finished) return
    finished = true
    val finalElapsed = (startedAtMs?.let { System.currentTimeMillis() - it } ?: 1000L).coerceAtLeast(1000L)
    elapsedMs = finalElapsed
    val metrics = TypingEngine.computeMetrics(passage.content, finalCommitted, finalElapsed)
    SoundFeedback.play(SoundCue.FINISH, state.settings.soundEnabled)
    saveMsg = "Saving…"
    scope.launch {
      val res = onSaveResult(
        ResultInput(
          mode = TypingMode.HINDI,
          refId = "phonetic-${passage.slug}",
          refTitle = "Phonetic · ${passage.title}",
          language = Language.HI,
          durationSeconds = 0,
          elapsedSeconds = (finalElapsed / 1000L).toInt().coerceAtLeast(1),
          grossWpm = metrics.grossWpm,
          netWpm = metrics.netWpm,
          standardWpm = metrics.standardWpm,
          accuracy = metrics.accuracy,
          errors = metrics.incorrectChars,
          totalTyped = metrics.totalTyped,
          correctChars = metrics.correctChars,
          keystrokeErrors = 0,
          backspaceCount = 0,
          score = (metrics.netWpm * (metrics.accuracy / 100.0)).toInt(),
          keyErrors = metrics.keyErrors.map { KeyErrorPair(it.expected, it.actual) },
          correctKeys = TypingEngine.collectCorrectKeyCounts(passage.content, finalCommitted),
        ),
      )
      saveMsg = if (res.isSuccess) "Result saved to cloud (${metrics.netWpm.toInt()} WPM · ${metrics.accuracy.toInt()}% acc)."
      else res.exceptionOrNull()?.localizedMessage ?: "Save failed."
    }
  }

  fun onRomanChanged(raw: String) {
    if (finished) return
    if (startedAtMs == null && raw.isNotEmpty()) {
      startedAtMs = System.currentTimeMillis()
    }
    if (raw.length > romanBuffer.length) {
      val added = raw.substring(romanBuffer.length)
      var buf = romanBuffer
      var out = committedHindi
      for (ch in added) {
        when {
          ch == ' ' -> {
            out += Transliterator.transliterate(buf) + " "
            buf = ""
          }
          ch == '.' -> {
            out += Transliterator.transliterate(buf) + "।"
            buf = ""
          }
          ch.isLetterOrDigit() -> buf += ch
          else -> {
            out += Transliterator.transliterate(buf) + ch
            buf = ""
          }
        }
      }
      romanBuffer = buf
      committedHindi = out
      SoundFeedback.play(SoundCue.KEY, state.settings.soundEnabled)
      if (TypingEngine.toGraphemes(out).size >= targetGraphemes.size && targetGraphemes.isNotEmpty()) {
        finishPhonetic(out)
      }
    } else {
      if (romanBuffer.isNotEmpty()) {
        romanBuffer = raw
      } else if (committedGraphemes.isNotEmpty()) {
        committedHindi = committedGraphemes.dropLast(1).joinToString("")
      }
    }
  }

  val liveMetrics = TypingEngine.computeMetrics(passage.content, committedHindi, elapsedMs)

  SectionCard(
    title = "Phonetic · ${passage.title}",
    description = "Type Roman letters (e.g. 'namaste bharat') — Space commits Devanagari.",
    action = {
      OutlinedButton(onClick = onNext) {
        Text("Next")
      }
    },
  ) {
    val successColor = ext.success
    val dangerColor = ext.danger
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val annotated = remember(targetGraphemes, committedGraphemes) {
      buildAnnotatedString {
        targetGraphemes.forEachIndexed { idx, g ->
          val t = committedGraphemes.getOrNull(idx)
          when {
            t == null && idx == committedGraphemes.size ->
              withStyle(SpanStyle(color = onSurface, background = primaryColor.copy(alpha = 0.25f), textDecoration = TextDecoration.Underline)) { append(g) }
            t == null -> withStyle(SpanStyle(color = mutedColor)) { append(g) }
            t == g -> withStyle(SpanStyle(color = successColor)) { append(g) }
            else -> withStyle(SpanStyle(color = dangerColor, textDecoration = TextDecoration.LineThrough)) { append(g) }
          }
        }
      }
    }

    Surface(
      color = ext.panel,
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, ext.line),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = annotated,
        fontFamily = DevanagariFontFamily,
        fontSize = 19.sp,
        lineHeight = 30.sp,
        modifier = Modifier.padding(14.dp),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Surface(color = ext.panel, shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, ext.line), modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text("COMMITTED & PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = (committedHindi + preview).ifBlank { "—" },
            fontFamily = DevanagariFontFamily,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      Surface(color = ext.panel, shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, ext.line), modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text("LIVE WPM / ACC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "${liveMetrics.netWpm.toInt()} WPM · ${liveMetrics.accuracy.toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    OutlinedTextField(
      value = romanBuffer,
      onValueChange = ::onRomanChanged,
      enabled = !finished,
      label = { Text("Type Roman Hindi (e.g. hindi men typing)") },
      textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
      modifier = Modifier.fillMaxWidth().testTag("phonetic_roman_input"),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = { finishPhonetic(committedHindi + preview) },
        enabled = !finished && (committedHindi.isNotEmpty() || preview.isNotEmpty()),
      ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Finish & Save")
      }
      OutlinedButton(
        onClick = {
          romanBuffer = ""
          committedHindi = ""
          startedAtMs = null
          elapsedMs = 0L
          finished = false
          saveMsg = "Commit words with Space; Finish to save."
        },
      ) {
        Text("Restart")
      }
    }

    Text(text = saveMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
