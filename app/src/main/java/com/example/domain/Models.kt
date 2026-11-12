package com.example.domain

enum class Language(val code: String, val label: String) {
  EN("en", "English"),
  HI("hi", "हिन्दी");

  companion object {
    fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: EN
  }
}

enum class ThemeMode(val value: String, val label: String) {
  DARK("dark", "Dark"),
  LIGHT("light", "Light"),
  SYSTEM("system", "System");

  companion object {
    fun fromValue(value: String?): ThemeMode = entries.firstOrNull { it.value == value } ?: DARK
  }
}

enum class TypingMode(val value: String, val label: String) {
  TEST("test", "Typing test"),
  PRACTICE("practice", "Free practice"),
  LESSON("lesson", "Lesson"),
  GAME("game", "Game"),
  HINDI("hindi", "Hindi practice"),
  DRILL("drill", "Keyboard drill");

  companion object {
    fun fromValue(value: String?): TypingMode = entries.firstOrNull { it.value == value } ?: TEST
  }
}

enum class CursorStyle(val value: String, val label: String) {
  LINE("line", "Line"),
  BLOCK("block", "Block"),
  UNDERLINE("underline", "Underline");

  companion object {
    fun fromValue(value: String?): CursorStyle = entries.firstOrNull { it.value == value } ?: LINE
  }
}

enum class KeyboardLayoutPref(val value: String, val label: String) {
  QWERTY("qwerty", "English QWERTY"),
  INSCRIPT("inscript", "Hindi InScript");

  companion object {
    fun fromValue(value: String?): KeyboardLayoutPref =
      entries.firstOrNull { it.value == value } ?: QWERTY
  }
}

enum class Difficulty(val value: String, val label: String) {
  BEGINNER("beginner", "Beginner"),
  INTERMEDIATE("intermediate", "Intermediate"),
  ADVANCED("advanced", "Advanced"),
  COMPETITIVE("competitive", "Competitive");

  companion object {
    fun fromValue(value: String?): Difficulty = entries.firstOrNull { it.value == value } ?: BEGINNER
  }
}

data class CategoryDef(
  val value: String,
  val label: String,
  val description: String,
)

val DURATIONS = listOf(60, 120, 180, 300, 600, 900)

val CATEGORIES = listOf(
  CategoryDef("beginner", "Beginner", "Short, simple sentences"),
  CategoryDef("intermediate", "Intermediate", "Everyday vocabulary"),
  CategoryDef("advanced", "Advanced", "Complex wording"),
  CategoryDef("general-english", "General English", "Modern paragraphs"),
  CategoryDef("general-hindi", "General Hindi", "Devanagari passages"),
  CategoryDef("competitive", "Competitive Exam", "SSC / Railway / Banking"),
)

data class UserSettings(
  val displayName: String = "Typist",
  val theme: ThemeMode = ThemeMode.DARK,
  val language: Language = Language.EN,
  val soundEnabled: Boolean = true,
  val cursorStyle: CursorStyle = CursorStyle.LINE,
  val keyboardLayout: KeyboardLayoutPref = KeyboardLayoutPref.QWERTY,
  val defaultDuration: Int = 60,
  val defaultCategory: String = "beginner",
  val dailyGoalMinutes: Int = 15,
  val showKeyboard: Boolean = true,
  val strictMode: Boolean = false,
)

data class KeyErrorPair(
  val expected: String,
  val actual: String,
)

data class KeyError(
  val index: Int,
  val expected: String,
  val actual: String,
)

data class StoredResult(
  val id: String,
  val mode: TypingMode,
  val refId: String,
  val refTitle: String,
  val language: Language,
  val durationSeconds: Int,
  val elapsedSeconds: Int,
  val grossWpm: Double,
  val netWpm: Double,
  val standardWpm: Double,
  val accuracy: Double,
  val errors: Int,
  val totalTyped: Int,
  val correctChars: Int,
  val keystrokeErrors: Int,
  val backspaceCount: Int,
  val score: Int,
  val createdAtMillis: Long,
)

data class ResultInput(
  val mode: TypingMode,
  val refId: String,
  val refTitle: String,
  val language: Language,
  val durationSeconds: Int,
  val elapsedSeconds: Int,
  val grossWpm: Double,
  val netWpm: Double,
  val standardWpm: Double,
  val accuracy: Double,
  val errors: Int,
  val totalTyped: Int,
  val correctChars: Int,
  val keystrokeErrors: Int,
  val backspaceCount: Int,
  val score: Int = 0,
  val keyErrors: List<KeyErrorPair> = emptyList(),
  val correctKeys: Map<String, Int> = emptyMap(),
)

data class KeyStat(
  val key: String,
  val correct: Int,
  val incorrect: Int,
)

data class LessonProgress(
  val lessonSlug: String,
  val completed: Boolean,
  val bestWpm: Double,
  val bestAccuracy: Double,
  val attempts: Int,
  val updatedAtMillis: Long,
)

data class CourseDef(
  val slug: String,
  val title: String,
  val level: Difficulty,
  val language: Language,
  val description: String,
  val sortOrder: Int,
)

data class LessonDef(
  val courseSlug: String,
  val courseTitle: String,
  val slug: String,
  val title: String,
  val level: Difficulty,
  val language: Language,
  val instructions: String,
  val content: String,
  val focus: String,
  val estMinutes: Int,
  val sortOrder: Int,
)

data class PassageDef(
  val slug: String,
  val title: String,
  val language: Language,
  val category: String,
  val difficulty: Difficulty,
  val content: String,
) {
  val wordCount: Int
    get() = content.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
}
