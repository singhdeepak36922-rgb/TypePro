package com.example.data

import com.example.domain.CursorStyle
import com.example.domain.KeyboardLayoutPref
import com.example.domain.KeyStat
import com.example.domain.Language
import com.example.domain.LessonProgress
import com.example.domain.StoredResult
import com.example.domain.ThemeMode
import com.example.domain.TypingMode
import com.example.domain.UserSettings
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileSettingsDoc(
  val userId: String = "",
  val displayName: String = "Typist",
  val theme: String = "dark",
  val language: String = "en",
  val soundEnabled: Boolean = true,
  val cursorStyle: String = "line",
  val keyboardLayout: String = "qwerty",
  val defaultDuration: Long = 60L,
  val defaultCategory: String = "beginner",
  val dailyGoalMinutes: Long = 15L,
  val showKeyboard: Boolean = true,
  val strictMode: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): UserSettings = UserSettings(
    displayName = displayName.ifBlank { "Typist" },
    theme = ThemeMode.fromValue(theme),
    language = Language.fromCode(language),
    soundEnabled = soundEnabled,
    cursorStyle = CursorStyle.fromValue(cursorStyle),
    keyboardLayout = KeyboardLayoutPref.fromValue(keyboardLayout),
    defaultDuration = defaultDuration.toInt(),
    defaultCategory = defaultCategory.ifBlank { "beginner" },
    dailyGoalMinutes = dailyGoalMinutes.toInt().coerceIn(5, 120),
    showKeyboard = showKeyboard,
    strictMode = strictMode,
  )
}

@IgnoreExtraProperties
data class TypingResultDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val mode: String = "test",
  val refId: String = "",
  val refTitle: String = "",
  val language: String = "en",
  val durationSeconds: Long = 0L,
  val elapsedSeconds: Long = 0L,
  val grossWpm: Double = 0.0,
  val netWpm: Double = 0.0,
  val standardWpm: Double = 0.0,
  val accuracy: Double = 0.0,
  val errors: Long = 0L,
  val totalTyped: Long = 0L,
  val correctChars: Long = 0L,
  val keystrokeErrors: Long = 0L,
  val backspaceCount: Long = 0L,
  val score: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  fun toDomain(): StoredResult = StoredResult(
    id = id,
    mode = TypingMode.fromValue(mode),
    refId = refId,
    refTitle = refTitle.ifBlank { "Session" },
    language = Language.fromCode(language),
    durationSeconds = durationSeconds.toInt(),
    elapsedSeconds = elapsedSeconds.toInt(),
    grossWpm = grossWpm,
    netWpm = netWpm,
    standardWpm = standardWpm,
    accuracy = accuracy,
    errors = errors.toInt(),
    totalTyped = totalTyped.toInt(),
    correctChars = correctChars.toInt(),
    keystrokeErrors = keystrokeErrors.toInt(),
    backspaceCount = backspaceCount.toInt(),
    score = score.toInt(),
    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
  )
}

@IgnoreExtraProperties
data class KeyStatDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val keyChar: String = "",
  val correct: Long = 0L,
  val incorrect: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): KeyStat = KeyStat(
    key = keyChar,
    correct = correct.toInt(),
    incorrect = incorrect.toInt(),
  )
}

@IgnoreExtraProperties
data class LessonProgressDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val lessonSlug: String = "",
  val completed: Boolean = false,
  val bestWpm: Double = 0.0,
  val bestAccuracy: Double = 0.0,
  val attempts: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): LessonProgress = LessonProgress(
    lessonSlug = lessonSlug.ifBlank { id },
    completed = completed,
    bestWpm = bestWpm,
    bestAccuracy = bestAccuracy,
    attempts = attempts.toInt(),
    updatedAtMillis = updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
  )
}
