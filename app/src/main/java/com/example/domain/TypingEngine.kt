package com.example.domain

import java.text.BreakIterator
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class GraphemeComparison(
  val correctChars: Int,
  val incorrectChars: Int,
  val keyErrors: List<KeyError>,
  val isComplete: Boolean,
  val completion: Double,
  val targetLength: Int,
  val typedLength: Int,
)

data class TypingMetrics(
  val elapsedMs: Long,
  val elapsedMinutes: Double,
  val totalTyped: Int,
  val targetLength: Int,
  val correctChars: Int,
  val incorrectChars: Int,
  val accuracy: Double,
  val grossWpm: Double,
  val netWpm: Double,
  val standardWpm: Double,
  val keystrokeErrors: Int,
  val backspaceCount: Int,
  val completion: Double,
  val isComplete: Boolean,
  val keyErrors: List<KeyError>,
)

object TypingEngine {

  /**
   * Splits a string into user-perceived Unicode grapheme clusters using BreakIterator
   * so Devanagari matras and conjuncts stay attached as single units.
   */
  fun toGraphemes(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val iterator = BreakIterator.getCharacterInstance(Locale.forLanguageTag("hi-IN"))
    iterator.setText(text)
    val result = mutableListOf<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
      result.add(text.substring(start, end))
      start = end
      end = iterator.next()
    }
    return result
  }

  private fun safeMinutes(elapsedMs: Long): Double {
    val ms = max(elapsedMs, 1000L)
    return ms / 60_000.0
  }

  fun round(value: Double, digits: Int = 1): Double {
    if (!value.isFinite()) return 0.0
    val factor = 10.0.pow(digits)
    return (value * factor).roundToInt() / factor
  }

  fun calculateWpm(correctChars: Int, elapsedMs: Long): Double {
    if (correctChars <= 0) return 0.0
    return round(correctChars / 5.0 / safeMinutes(elapsedMs))
  }

  fun calculateAccuracy(correctChars: Int, totalTyped: Int): Double {
    if (totalTyped <= 0) return 0.0
    return round((correctChars.toDouble() / totalTyped.toDouble()) * 100.0)
  }

  fun compareGraphemes(target: String, typed: String): GraphemeComparison {
    val targetG = toGraphemes(target)
    val typedG = toGraphemes(typed)
    var correctChars = 0
    val keyErrors = mutableListOf<KeyError>()

    for (i in typedG.indices) {
      val expected = targetG.getOrElse(i) { "" }
      if (expected == typedG[i]) {
        correctChars += 1
      } else {
        keyErrors.add(KeyError(index = i, expected = expected, actual = typedG[i]))
      }
    }

    val completion = if (targetG.isEmpty()) {
      0.0
    } else {
      min(1.0, typedG.size.toDouble() / targetG.size.toDouble())
    }

    return GraphemeComparison(
      correctChars = correctChars,
      incorrectChars = typedG.size - correctChars,
      keyErrors = keyErrors,
      isComplete = targetG.isNotEmpty() && typedG.size >= targetG.size,
      completion = completion,
      targetLength = targetG.size,
      typedLength = typedG.size,
    )
  }

  fun computeMetrics(
    target: String,
    typed: String,
    elapsedMs: Long,
    keystrokeErrors: Int = 0,
    backspaceCount: Int = 0,
  ): TypingMetrics {
    val safeElapsed = max(0L, elapsedMs)
    val minutes = safeMinutes(safeElapsed)
    val comparison = compareGraphemes(target, typed)
    val totalTyped = comparison.typedLength
    val correctChars = comparison.correctChars
    val incorrectChars = comparison.incorrectChars

    val grossWpm = if (totalTyped <= 0) 0.0 else round(totalTyped / 5.0 / minutes)
    val standardWpm = if (correctChars <= 0) 0.0 else round(correctChars / 5.0 / minutes)
    val netWpm = if (totalTyped <= 0) 0.0 else round(max(0.0, grossWpm - incorrectChars / minutes))

    return TypingMetrics(
      elapsedMs = safeElapsed,
      elapsedMinutes = minutes,
      totalTyped = totalTyped,
      targetLength = comparison.targetLength,
      correctChars = correctChars,
      incorrectChars = incorrectChars,
      accuracy = calculateAccuracy(correctChars, totalTyped),
      grossWpm = grossWpm,
      netWpm = netWpm,
      standardWpm = standardWpm,
      keystrokeErrors = keystrokeErrors,
      backspaceCount = backspaceCount,
      completion = round(comparison.completion * 100.0),
      isComplete = comparison.isComplete,
      keyErrors = comparison.keyErrors,
    )
  }

  fun nextExpectedGrapheme(target: String, typed: String): String? {
    val targetG = toGraphemes(target)
    val typedG = toGraphemes(typed)
    return targetG.getOrNull(typedG.size)
  }

  fun normalizePassage(text: String): String {
    return text.replace(Regex("\\s+"), " ").trim()
  }

  /**
   * Builds per-key correct counts from final aligned graphemes for weak-key diagnostics.
   */
  fun collectCorrectKeyCounts(target: String, typed: String): Map<String, Int> {
    val targetG = toGraphemes(target)
    val typedG = toGraphemes(typed)
    val counts = mutableMapOf<String, Int>()
    for (i in typedG.indices) {
      val expected = targetG.getOrNull(i) ?: continue
      if (expected == typedG[i] && expected != " ") {
        counts[expected] = (counts[expected] ?: 0) + 1
      }
    }
    return counts
  }
}
