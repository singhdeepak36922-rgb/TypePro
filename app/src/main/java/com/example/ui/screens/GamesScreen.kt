package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.domain.ContentCatalog
import com.example.domain.Language
import com.example.domain.ResultInput
import com.example.domain.TypingEngine
import com.example.domain.TypingMode
import com.example.ui.TypeProAppState
import com.example.ui.components.BadgePill
import com.example.ui.components.LinearProgressMeter
import com.example.ui.components.SectionCard
import com.example.ui.components.SessionSaveStatus
import com.example.ui.components.Tone
import com.example.ui.components.TypingSurface
import com.example.ui.sound.SoundCue
import com.example.ui.sound.SoundFeedback
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTypeProColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private data class TypingGameDef(
  val id: String,
  val title: String,
  val description: String,
  val durationSeconds: Int,
  val icon: ImageVector,
  val badge: String,
)

private val GAME_CATALOG = listOf(
  TypingGameDef(
    id = "speed-challenge",
    title = "Speed Challenge",
    description = "30-second high-frequency word blitz. Type each word and press Space.",
    durationSeconds = 30,
    icon = Icons.Filled.Speed,
    badge = "30s Blitz",
  ),
  TypingGameDef(
    id = "word-challenge",
    title = "Word Challenge",
    description = "45-second endurance run with longer multi-syllable words.",
    durationSeconds = 45,
    icon = Icons.Filled.Bolt,
    badge = "45s Fluency",
  ),
  TypingGameDef(
    id = "accuracy-challenge",
    title = "Accuracy Challenge",
    description = "45-second precision passage drill. Clean keystrokes earn top scores.",
    durationSeconds = 45,
    icon = Icons.Filled.TrackChanges,
    badge = "45s Precision",
  ),
  TypingGameDef(
    id = "time-attack",
    title = "Time Attack",
    description = "15-second explosive word sprint. Quick reflexes score bonus points.",
    durationSeconds = 15,
    icon = Icons.Filled.Timer,
    badge = "15s Sprint",
  ),
  TypingGameDef(
    id = "typing-race",
    title = "Typing Race vs Bot",
    description = "90-second head-to-head passage race against a steady 32 WPM pace bot.",
    durationSeconds = 90,
    icon = Icons.Filled.Flag,
    badge = "90s Race",
  ),
)

@Composable
fun GamesScreen(
  state: TypeProAppState,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  var activeGameId by remember { mutableStateOf<String?>(null) }
  val activeGame = GAME_CATALOG.firstOrNull { it.id == activeGameId }

  if (activeGame != null) {
    BackHandler { activeGameId = null }
    ActiveGameView(
      game = activeGame,
      state = state,
      onBack = { activeGameId = null },
      onSaveResult = onSaveResult,
      modifier = modifier,
    )
    return
  }

  val ext = LocalTypeProColors.current

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Typing games",
      description = "Five skill challenges that train speed, precision, and endurance while syncing your personal best scores.",
      action = {
        BadgePill(text = "${state.summary.gamesPlayed} played", tone = Tone.BRAND)
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GAME_CATALOG.forEach { game ->
          val bestScore = state.summary.bestGameScores[game.id] ?: 0
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { activeGameId = game.id }
              .testTag("game_card_${game.id}"),
            shape = MaterialTheme.shapes.medium,
            color = ext.panel,
            border = BorderStroke(1.dp, ext.line),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                color = ext.brandSoft,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(44.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = game.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  BadgePill(text = game.badge, tone = Tone.CYAN)
                }
                Text(
                  text = game.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = if (bestScore > 0) "Best score: $bestScore" else "Not played yet",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (bestScore > 0) ext.success else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Play ${game.title}",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun ActiveGameView(
  game: TypingGameDef,
  state: TypeProAppState,
  onBack: () -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("game_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to games")
        }
        Column {
          Text(
            text = "TYPING GAME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = game.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      BadgePill(text = game.badge, tone = Tone.CYAN)
    }

    when (game.id) {
      "accuracy-challenge" -> {
        val passage = remember { ContentCatalog.PASSAGES.first { it.language == Language.EN && it.difficulty.value == "intermediate" } }
        SectionCard(
          title = "Precision Sprint",
          description = "Aim for 98%+ accuracy before the 45-second timer ends.",
        ) {
          TypingSurface(
            target = passage.content,
            language = Language.EN,
            durationSeconds = game.durationSeconds,
            mode = TypingMode.GAME,
            refId = game.id,
            refTitle = game.title,
            settings = state.settings,
            onSaveResult = onSaveResult,
            allowVirtualInput = state.settings.showKeyboard,
            restartLabel = "Play again",
            extraResultActions = {
              OutlinedButton(onClick = onBack) { Text("All games") }
            },
          )
        }
      }
      "typing-race" -> {
        TypingRaceArena(
          game = game,
          state = state,
          onBack = onBack,
          onSaveResult = onSaveResult,
        )
      }
      else -> {
        WordSprintArena(
          game = game,
          state = state,
          onBack = onBack,
          onSaveResult = onSaveResult,
        )
      }
    }

    Spacer(Modifier.height(12.dp))
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordSprintArena(
  game: TypingGameDef,
  state: TypeProAppState,
  onBack: () -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
) {
  val ext = LocalTypeProColors.current
  val scope = rememberCoroutineScope()
  val pool = remember(game.id) { ContentCatalog.WORDS_BY_GAME[game.id] ?: ContentCatalog.WORD_POOL }

  var roundSeed by remember(game.id) { mutableIntStateOf(0) }
  val wordsQueue = remember(game.id, roundSeed) {
    buildList {
      repeat(5) { addAll(pool.shuffled()) }
    }
  }

  var wordIndex by remember(game.id, roundSeed) { mutableIntStateOf(0) }
  var currentInput by remember(game.id, roundSeed) { mutableStateOf("") }
  var startedAtMs by remember(game.id, roundSeed) { mutableStateOf<Long?>(null) }
  var elapsedMs by remember(game.id, roundSeed) { mutableLongStateOf(0L) }
  var correctWords by remember(game.id, roundSeed) { mutableIntStateOf(0) }
  var totalCharsTyped by remember(game.id, roundSeed) { mutableIntStateOf(0) }
  var correctChars by remember(game.id, roundSeed) { mutableIntStateOf(0) }
  var errors by remember(game.id, roundSeed) { mutableIntStateOf(0) }
  var finished by remember(game.id, roundSeed) { mutableStateOf(false) }
  var saveStatus by remember(game.id, roundSeed) { mutableStateOf(SessionSaveStatus.IDLE) }

  val targetWord = wordsQueue.getOrElse(wordIndex) { "typing" }

  fun finishGame() {
    if (finished) return
    finished = true
    val finalElapsed = (startedAtMs?.let { System.currentTimeMillis() - it } ?: 1000L)
      .coerceIn(1000L, game.durationSeconds * 1000L)
    elapsedMs = finalElapsed
    SoundFeedback.play(SoundCue.FINISH, state.settings.soundEnabled)

    val mins = finalElapsed / 60_000.0
    val grossWpm = TypingEngine.round((totalCharsTyped / 5.0) / mins)
    val netWpm = TypingEngine.round(((totalCharsTyped - errors).coerceAtLeast(0) / 5.0) / mins)
    val accuracy = if (totalCharsTyped == 0) 100.0 else TypingEngine.round((correctChars.toDouble() / totalCharsTyped) * 100.0)
    val finalScore = (correctWords * 12) + netWpm.toInt()

    saveStatus = SessionSaveStatus.SAVING
    scope.launch {
      val res = onSaveResult(
        ResultInput(
          mode = TypingMode.GAME,
          refId = game.id,
          refTitle = game.title,
          language = Language.EN,
          durationSeconds = game.durationSeconds,
          elapsedSeconds = (finalElapsed / 1000L).toInt().coerceAtLeast(1),
          grossWpm = grossWpm,
          netWpm = netWpm,
          standardWpm = netWpm,
          accuracy = accuracy,
          errors = errors,
          totalTyped = totalCharsTyped,
          correctChars = correctChars,
          keystrokeErrors = errors,
          backspaceCount = 0,
          score = finalScore,
        ),
      )
      saveStatus = if (res.isSuccess) SessionSaveStatus.SAVED else SessionSaveStatus.ERROR
    }
  }

  LaunchedEffect(startedAtMs, finished, roundSeed) {
    val start = startedAtMs ?: return@LaunchedEffect
    while (!finished) {
      val diff = System.currentTimeMillis() - start
      elapsedMs = diff
      if (diff >= game.durationSeconds * 1000L) {
        finishGame()
        break
      }
      delay(100L)
    }
  }

  fun handleWordInput(raw: String) {
    if (finished) return
    if (startedAtMs == null && raw.isNotEmpty()) {
      startedAtMs = System.currentTimeMillis()
    }
    if (raw.endsWith(" ") || raw.endsWith("\n")) {
      val trimmed = raw.trim()
      if (trimmed.isNotEmpty()) {
        val len = maxOf(trimmed.length, targetWord.length)
        var wordCorrect = 0
        for (i in 0 until min(trimmed.length, targetWord.length)) {
          if (trimmed[i] == targetWord[i]) wordCorrect++
        }
        totalCharsTyped += trimmed.length
        correctChars += wordCorrect
        errors += (len - wordCorrect)
        if (trimmed == targetWord) {
          correctWords += 1
          SoundFeedback.play(SoundCue.KEY, state.settings.soundEnabled)
        } else {
          SoundFeedback.play(SoundCue.ERROR, state.settings.soundEnabled)
        }
        wordIndex += 1
      }
      currentInput = ""
    } else {
      if (raw.length > currentInput.length) {
        val lastIdx = raw.lastIndex
        val matches = targetWord.getOrNull(lastIdx) == raw.getOrNull(lastIdx)
        SoundFeedback.play(if (matches) SoundCue.KEY else SoundCue.ERROR, state.settings.soundEnabled)
        if (raw == targetWord && game.id == "time-attack") {
          totalCharsTyped += targetWord.length
          correctChars += targetWord.length
          correctWords += 1
          wordIndex += 1
          currentInput = ""
          return
        }
      }
      currentInput = raw
    }
  }

  val remainingSec = ((game.durationSeconds * 1000L - elapsedMs).coerceAtLeast(0L) / 1000L).toInt()
  val liveMins = (elapsedMs.coerceAtLeast(1000L)) / 60_000.0
  val liveWpm = if (startedAtMs == null) 0 else ((correctChars / 5.0) / liveMins).toInt()
  val liveScore = (correctWords * 12) + liveWpm

  SectionCard(
    title = game.title,
    description = game.description,
    action = {
      BadgePill(
        text = if (finished) "Finished" else "${remainingSec}s left",
        tone = if (remainingSec <= 5 && !finished) Tone.DANGER else Tone.BRAND,
      )
    },
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      BadgePill(text = "Score: $liveScore", tone = Tone.BRAND)
      BadgePill(text = "Words: $correctWords", tone = Tone.SUCCESS)
      BadgePill(text = "$liveWpm WPM", tone = Tone.CYAN)
    }

    if (finished) {
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ext.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Final Score: $liveScore", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            BadgePill(
              text = when (saveStatus) {
                SessionSaveStatus.SAVED -> "Saved"
                SessionSaveStatus.SAVING -> "Saving…"
                SessionSaveStatus.ERROR -> "Save error"
                SessionSaveStatus.IDLE -> "Done"
              },
              tone = if (saveStatus == SessionSaveStatus.ERROR) Tone.DANGER else Tone.SUCCESS,
            )
          }
          Text(
            text = "Completed $correctWords words at $liveWpm WPM with $errors character errors.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { roundSeed += 1 },
              modifier = Modifier.testTag("game_play_again_button"),
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Play again")
            }
            OutlinedButton(onClick = onBack) { Text("All games") }
          }
        }
      }
    } else {
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ext.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          val successColor = ext.success
          val dangerColor = ext.danger
          val onSurface = MaterialTheme.colorScheme.onSurface
          val annotatedWord = buildAnnotatedString {
            targetWord.forEachIndexed { idx, ch ->
              val typedCh = currentInput.getOrNull(idx)
              when {
                typedCh == null -> withStyle(SpanStyle(color = onSurface)) { append(ch) }
                typedCh == ch -> withStyle(SpanStyle(color = successColor)) { append(ch) }
                else -> withStyle(SpanStyle(color = dangerColor, textDecoration = TextDecoration.Underline)) { append(ch) }
              }
            }
          }
          Text(
            text = annotatedWord,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wordsQueue.drop(wordIndex + 1).take(4).forEach { upcoming ->
              BadgePill(text = upcoming, tone = Tone.NEUTRAL)
            }
          }
        }
      }

      OutlinedTextField(
        value = currentInput,
        onValueChange = ::handleWordInput,
        label = { Text(if (game.id == "time-attack") "Type word (auto-advances on match)" else "Type word + Space") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          autoCorrectEnabled = false,
          keyboardType = KeyboardType.Text,
          imeAction = ImeAction.Next,
        ),
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
        modifier = Modifier.fillMaxWidth().testTag("game_word_input"),
      )

      OutlinedButton(onClick = { roundSeed += 1 }) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Restart round")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypingRaceArena(
  game: TypingGameDef,
  state: TypeProAppState,
  onBack: () -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
) {
  val ext = LocalTypeProColors.current
  val scope = rememberCoroutineScope()
  val passages = remember { ContentCatalog.PASSAGES.filter { it.language == Language.EN } }
  var raceSeed by remember { mutableIntStateOf(0) }
  val passage = passages[raceSeed % passages.size]
  val targetGraphemes = remember(passage.slug) { TypingEngine.toGraphemes(passage.content) }

  var typed by remember(raceSeed) { mutableStateOf("") }
  var startedAtMs by remember(raceSeed) { mutableStateOf<Long?>(null) }
  var elapsedMs by remember(raceSeed) { mutableLongStateOf(0L) }
  var keystrokeErrors by remember(raceSeed) { mutableIntStateOf(0) }
  var finished by remember(raceSeed) { mutableStateOf(false) }
  var winner by remember(raceSeed) { mutableStateOf<String?>(null) }
  var saveStatus by remember(raceSeed) { mutableStateOf(SessionSaveStatus.IDLE) }

  val botCharsPerSecond = 32.0 * 5.0 / 60.0 // 32 WPM steady bot
  val botProgress = min(100f, ((elapsedMs / 1000.0 * botCharsPerSecond) / targetGraphemes.size.coerceAtLeast(1) * 100.0).toFloat())
  val userGraphemes = remember(typed) { TypingEngine.toGraphemes(typed) }
  val userProgress = min(100f, (userGraphemes.size.toFloat() / targetGraphemes.size.coerceAtLeast(1)) * 100f)

  fun finishRace(finalTyped: String, won: Boolean) {
    if (finished) return
    finished = true
    winner = if (won) "You finished ahead of TypeBot!" else "TypeBot crossed the finish line first."
    val finalElapsed = (startedAtMs?.let { System.currentTimeMillis() - it } ?: 1000L)
      .coerceIn(1000L, game.durationSeconds * 1000L)
    elapsedMs = finalElapsed
    SoundFeedback.play(SoundCue.FINISH, state.settings.soundEnabled)

    val metrics = TypingEngine.computeMetrics(passage.content, finalTyped, finalElapsed, keystrokeErrors)
    val finalScore = metrics.netWpm.roundToInt() + if (won) 50 else 0
    saveStatus = SessionSaveStatus.SAVING
    scope.launch {
      val res = onSaveResult(
        ResultInput(
          mode = TypingMode.GAME,
          refId = game.id,
          refTitle = game.title,
          language = Language.EN,
          durationSeconds = game.durationSeconds,
          elapsedSeconds = (finalElapsed / 1000L).toInt().coerceAtLeast(1),
          grossWpm = metrics.grossWpm,
          netWpm = metrics.netWpm,
          standardWpm = metrics.standardWpm,
          accuracy = metrics.accuracy,
          errors = metrics.incorrectChars,
          totalTyped = metrics.totalTyped,
          correctChars = metrics.correctChars,
          keystrokeErrors = metrics.keystrokeErrors,
          backspaceCount = metrics.backspaceCount,
          score = finalScore,
        ),
      )
      saveStatus = if (res.isSuccess) SessionSaveStatus.SAVED else SessionSaveStatus.ERROR
    }
  }

  LaunchedEffect(startedAtMs, finished, raceSeed) {
    val start = startedAtMs ?: return@LaunchedEffect
    while (!finished) {
      val diff = System.currentTimeMillis() - start
      elapsedMs = diff
      val botChars = (diff / 1000.0) * botCharsPerSecond
      if (botChars >= targetGraphemes.size || diff >= game.durationSeconds * 1000L) {
        finishRace(typed, won = userGraphemes.size >= targetGraphemes.size)
        break
      }
      delay(100L)
    }
  }

  val liveMetrics = TypingEngine.computeMetrics(passage.content, typed, elapsedMs, keystrokeErrors)

  SectionCard(
    title = "Race Track · ${passage.title}",
    description = "Beat TypeBot (32 WPM) to the end of the passage.",
    action = {
      BadgePill(text = "${liveMetrics.netWpm.toInt()} WPM", tone = Tone.BRAND)
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("You (${liveMetrics.netWpm.toInt()} WPM)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("${userProgress.toInt()}%", style = MaterialTheme.typography.labelMedium)
      }
      LinearProgressMeter(percent = userProgress, tone = Tone.BRAND, height = 10.dp)

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("TypeBot (32 WPM)", style = MaterialTheme.typography.labelMedium, color = ext.warn)
        Text("${botProgress.toInt()}%", style = MaterialTheme.typography.labelMedium)
      }
      LinearProgressMeter(percent = botProgress, tone = Tone.WARN, height = 10.dp)
    }

    if (finished) {
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ext.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ext.warn)
            Text(winner ?: "Race complete", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "Net speed: ${liveMetrics.netWpm.toInt()} WPM · Accuracy: ${TypingEngine.round(liveMetrics.accuracy, 1)}% · Status: $saveStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { raceSeed += 1 }) { Text("Race again") }
            OutlinedButton(onClick = onBack) { Text("All games") }
          }
        }
      }
    } else {
      Surface(
        color = ext.panel,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ext.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        val successColor = ext.success
        val dangerColor = ext.danger
        val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
        val annotated = buildAnnotatedString {
          targetGraphemes.forEachIndexed { idx, g ->
            val t = userGraphemes.getOrNull(idx)
            when {
              t == null -> withStyle(SpanStyle(color = mutedColor)) { append(g) }
              t == g -> withStyle(SpanStyle(color = successColor)) { append(g) }
              else -> withStyle(SpanStyle(color = dangerColor, textDecoration = TextDecoration.LineThrough)) { append(g) }
            }
          }
        }
        Text(
          text = annotated,
          fontFamily = JetBrainsMonoFamily,
          fontSize = 15.sp,
          lineHeight = 24.sp,
          modifier = Modifier.padding(14.dp),
        )
      }

      OutlinedTextField(
        value = typed,
        onValueChange = { next ->
          if (startedAtMs == null && next.isNotEmpty()) startedAtMs = System.currentTimeMillis()
          val nextG = TypingEngine.toGraphemes(next)
          if (nextG.size > userGraphemes.size) {
            val expected = targetGraphemes.getOrNull(nextG.lastIndex)
            val actual = nextG.lastOrNull()
            if (expected != null && actual != expected) {
              keystrokeErrors += 1
              SoundFeedback.play(SoundCue.ERROR, state.settings.soundEnabled)
            } else {
              SoundFeedback.play(SoundCue.KEY, state.settings.soundEnabled)
            }
          }
          typed = next
          if (nextG.size >= targetGraphemes.size) {
            finishRace(next, won = true)
          }
        },
        label = { Text("Type passage to race TypeBot…") },
        maxLines = 3,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
        modifier = Modifier.fillMaxWidth().testTag("race_input_field"),
      )
    }
  }
}
