package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DailyPoint(
  val date: String,
  val label: String,
  val minutes: Double,
  val tests: Int,
  val wpm: Double,
  val accuracy: Double,
)

data class WeakKeySummary(
  val key: String,
  val accuracy: Double,
  val attempts: Int,
)

data class ProgressSummary(
  val totalResults: Int,
  val totalTests: Int,
  val totalPracticeSeconds: Int,
  val totalMinutes: Double,
  val avgWpm: Double,
  val avgNetWpm: Double,
  val bestWpm: Double,
  val avgAccuracy: Double,
  val bestAccuracy: Double,
  val todayMinutes: Double,
  val weekMinutes: Double,
  val goalPercent: Double,
  val streak: Int,
  val daily: List<DailyPoint>,
  val weekly: List<DailyPoint>,
  val monthly: List<DailyPoint>,
  val recent: List<StoredResult>,
  val lessonsCompleted: Int,
  val gamesPlayed: Int,
  val bestGameScores: Map<String, Int>,
  val weakKeys: List<WeakKeySummary>,
  val hindiSessions: Int,
)

enum class AchievementTier(val label: String) {
  BRONZE("Bronze"),
  SILVER("Silver"),
  GOLD("Gold"),
  PLATINUM("Platinum"),
}

data class AchievementState(
  val key: String,
  val title: String,
  val description: String,
  val iconName: String,
  val tier: AchievementTier,
  val unlocked: Boolean,
  val progressPercent: Int,
)

object StatsCalculator {

  private fun dayKey(date: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(date)
  }

  private fun buildDays(count: Int): List<DailyPoint> {
    val cal = Calendar.getInstance()
    val labelFmt = SimpleDateFormat("MMM d", Locale.US)
    val list = mutableListOf<DailyPoint>()
    for (i in (count - 1) downTo 0) {
      val dayCal = cal.clone() as Calendar
      dayCal.add(Calendar.DAY_OF_YEAR, -i)
      val d = dayCal.time
      list.add(
        DailyPoint(
          date = dayKey(d),
          label = labelFmt.format(d),
          minutes = 0.0,
          tests = 0,
          wpm = 0.0,
          accuracy = 0.0,
        ),
      )
    }
    return list
  }

  private fun average(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    return TypingEngine.round(values.sum() / values.size)
  }

  fun summarize(
    results: List<StoredResult>,
    keyStats: List<KeyStat> = emptyList(),
    dailyGoalMinutes: Int = 15,
  ): ProgressSummary {
    val sorted = results.sortedBy { it.createdAtMillis }
    val days = buildDays(30).associateBy { it.date }.toMutableMap()
    val dayOrder = days.keys.toList()
    val wpmBuckets = mutableMapOf<String, MutableList<Double>>()
    val accBuckets = mutableMapOf<String, MutableList<Double>>()

    var totalSeconds = 0
    var totalTests = 0
    var gamesPlayed = 0
    var hindiSessions = 0
    val bestGameScores = mutableMapOf<String, Int>()
    val completedLessons = mutableSetOf<String>()

    for (res in sorted) {
      totalSeconds += res.elapsedSeconds
      if (res.mode == TypingMode.TEST) totalTests += 1
      if (res.mode == TypingMode.LESSON && res.refId.isNotBlank()) {
        completedLessons.add(res.refId)
      }
      if (res.language == Language.HI) hindiSessions += 1
      if (res.mode == TypingMode.GAME && res.refId.isNotBlank()) {
        gamesPlayed += 1
        bestGameScores[res.refId] = max(bestGameScores[res.refId] ?: 0, res.score)
      }

      val key = dayKey(Date(res.createdAtMillis))
      val point = days[key]
      if (point != null) {
        days[key] = point.copy(
          minutes = TypingEngine.round(point.minutes + res.elapsedSeconds / 60.0),
          tests = point.tests + 1,
        )
      }
      wpmBuckets.getOrPut(key) { mutableListOf() }.add(res.netWpm)
      accBuckets.getOrPut(key) { mutableListOf() }.add(res.accuracy)
    }

    val daily = dayOrder.map { key ->
      val p = days.getValue(key)
      p.copy(
        wpm = average(wpmBuckets[key].orEmpty()),
        accuracy = average(accBuckets[key].orEmpty()),
      )
    }

    val todayKey = dayKey(Date())
    val todayMinutes = daily.firstOrNull { it.date == todayKey }?.minutes ?: 0.0
    val weekMinutes = TypingEngine.round(daily.takeLast(7).sumOf { it.minutes })
    val goal = if (dailyGoalMinutes > 0) dailyGoalMinutes.toDouble() else 15.0

    var streak = 0
    for (i in daily.indices.reversed()) {
      val point = daily[i]
      if (point.minutes > 0.0) {
        streak += 1
      } else if (i == daily.lastIndex) {
        continue
      } else {
        break
      }
    }

    val wpmValues = sorted.map { it.netWpm }.filter { it > 0.0 }
    val accValues = sorted.map { it.accuracy }.filter { it > 0.0 }

    val weakKeys = keyStats
      .filter { it.correct + it.incorrect >= 3 }
      .map { stat ->
        val total = stat.correct + stat.incorrect
        WeakKeySummary(
          key = stat.key,
          attempts = total,
          accuracy = TypingEngine.round((stat.correct.toDouble() / total.toDouble()) * 100.0),
        )
      }
      .sortedBy { it.accuracy }
      .take(12)

    return ProgressSummary(
      totalResults = sorted.size,
      totalTests = totalTests,
      totalPracticeSeconds = totalSeconds,
      totalMinutes = TypingEngine.round(totalSeconds / 60.0),
      avgWpm = average(wpmValues),
      avgNetWpm = average(sorted.map { it.netWpm }),
      bestWpm = sorted.maxOfOrNull { it.netWpm } ?: 0.0,
      avgAccuracy = average(accValues),
      bestAccuracy = sorted.maxOfOrNull { it.accuracy } ?: 0.0,
      todayMinutes = todayMinutes,
      weekMinutes = weekMinutes,
      goalPercent = min(100.0, TypingEngine.round((todayMinutes / goal) * 100.0)),
      streak = streak,
      daily = daily,
      weekly = daily.takeLast(7),
      monthly = daily,
      recent = sorted.reversed().take(20),
      lessonsCompleted = completedLessons.size,
      gamesPlayed = gamesPlayed,
      bestGameScores = bestGameScores,
      weakKeys = weakKeys,
      hindiSessions = hindiSessions,
    )
  }

  fun evaluateAchievements(s: ProgressSummary): List<AchievementState> {
    fun pct(raw: Double): Int = min(100, max(0, raw.roundToInt()))
    return listOf(
      AchievementState(
        key = "first-test",
        title = "First Typing Test",
        description = "Complete your very first typing test.",
        iconName = "flag",
        tier = AchievementTier.BRONZE,
        unlocked = s.totalTests >= 1,
        progressPercent = pct(s.totalTests * 100.0),
      ),
      AchievementState(
        key = "wpm-20",
        title = "20 WPM Club",
        description = "Reach 20 net WPM in a saved session.",
        iconName = "gauge",
        tier = AchievementTier.BRONZE,
        unlocked = s.bestWpm >= 20.0,
        progressPercent = pct((s.bestWpm / 20.0) * 100.0),
      ),
      AchievementState(
        key = "wpm-30",
        title = "30 WPM Club",
        description = "Reach 30 net WPM.",
        iconName = "zap",
        tier = AchievementTier.SILVER,
        unlocked = s.bestWpm >= 30.0,
        progressPercent = pct((s.bestWpm / 30.0) * 100.0),
      ),
      AchievementState(
        key = "wpm-40",
        title = "40 WPM Club",
        description = "Reach 40 net WPM.",
        iconName = "rocket",
        tier = AchievementTier.SILVER,
        unlocked = s.bestWpm >= 40.0,
        progressPercent = pct((s.bestWpm / 40.0) * 100.0),
      ),
      AchievementState(
        key = "wpm-50",
        title = "50 WPM Club",
        description = "Reach 50 net WPM.",
        iconName = "trophy",
        tier = AchievementTier.GOLD,
        unlocked = s.bestWpm >= 50.0,
        progressPercent = pct((s.bestWpm / 50.0) * 100.0),
      ),
      AchievementState(
        key = "accuracy-95",
        title = "Precision 95%",
        description = "Finish a session with at least 95% accuracy.",
        iconName = "target",
        tier = AchievementTier.SILVER,
        unlocked = s.bestAccuracy >= 95.0,
        progressPercent = pct((s.bestAccuracy / 95.0) * 100.0),
      ),
      AchievementState(
        key = "accuracy-100",
        title = "Flawless",
        description = "Finish a session with 100% accuracy.",
        iconName = "gem",
        tier = AchievementTier.PLATINUM,
        unlocked = s.bestAccuracy >= 100.0,
        progressPercent = pct(s.bestAccuracy),
      ),
      AchievementState(
        key = "streak-7",
        title = "7-Day Streak",
        description = "Practice on seven consecutive days.",
        iconName = "flame",
        tier = AchievementTier.GOLD,
        unlocked = s.streak >= 7,
        progressPercent = pct((s.streak / 7.0) * 100.0),
      ),
      AchievementState(
        key = "streak-30",
        title = "30-Day Streak",
        description = "Practice on thirty consecutive days.",
        iconName = "calendar",
        tier = AchievementTier.PLATINUM,
        unlocked = s.streak >= 30,
        progressPercent = pct((s.streak / 30.0) * 100.0),
      ),
      AchievementState(
        key = "lesson-first",
        title = "First Lesson",
        description = "Complete your first structured lesson.",
        iconName = "book",
        tier = AchievementTier.BRONZE,
        unlocked = s.lessonsCompleted >= 1,
        progressPercent = pct(s.lessonsCompleted * 100.0),
      ),
      AchievementState(
        key = "tests-25",
        title = "Committed Learner",
        description = "Complete 25 typing tests.",
        iconName = "layers",
        tier = AchievementTier.SILVER,
        unlocked = s.totalTests >= 25,
        progressPercent = pct((s.totalTests / 25.0) * 100.0),
      ),
      AchievementState(
        key = "hindi-first",
        title = "Devanagari Debut",
        description = "Save a Hindi typing session.",
        iconName = "languages",
        tier = AchievementTier.BRONZE,
        unlocked = s.hindiSessions >= 1,
        progressPercent = pct(s.hindiSessions * 100.0),
      ),
    )
  }

  fun formatDuration(seconds: Int): String {
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    val rest = seconds % 60
    if (minutes < 60) return if (rest > 0) "${minutes}m ${rest}s" else "${minutes}m"
    val hours = minutes / 60
    return "${hours}h ${minutes % 60}m"
  }

  fun formatRelative(millis: Long): String {
    val diff = max(0L, System.currentTimeMillis() - millis)
    val minutes = diff / 60_000L
    if (minutes < 1) return "just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 7) return "${days}d ago"
    return SimpleDateFormat("MMM d", Locale.US).format(Date(millis))
  }

  fun exportResultsCsv(results: List<StoredResult>): String {
    val sb = StringBuilder()
    sb.appendLine("id,mode,title,language,duration_sec,elapsed_sec,gross_wpm,net_wpm,standard_wpm,accuracy,errors,total_typed,correct_chars,score,created_at")
    val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    for (r in results) {
      val safeTitle = "\"${r.refTitle.replace("\"", "\"\"")}\""
      sb.appendLine(
        "${r.id},${r.mode.value},$safeTitle,${r.language.code},${r.durationSeconds},${r.elapsedSeconds}," +
          "${r.grossWpm},${r.netWpm},${r.standardWpm},${r.accuracy},${r.errors},${r.totalTyped},${r.correctChars},${r.score},${isoFmt.format(Date(r.createdAtMillis))}",
      )
    }
    return sb.toString()
  }
}
