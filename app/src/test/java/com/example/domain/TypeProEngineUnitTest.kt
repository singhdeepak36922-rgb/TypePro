package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeProEngineUnitTest {

  @Test
  fun emptyInput_returnsZeroMetrics() {
    val m = TypingEngine.computeMetrics(target = "hello", typed = "", elapsedMs = 0L)
    assertEquals(0.0, m.netWpm, 0.01)
    assertEquals(0.0, m.accuracy, 0.01)
    assertFalse(m.isComplete)
  }

  @Test
  fun perfectTyping_computesWpmAndAccuracy() {
    val m = TypingEngine.computeMetrics(
      target = "hello world",
      typed = "hello world",
      elapsedMs = 60_000L,
    )
    assertEquals(100.0, m.accuracy, 0.01)
    assertEquals(2.2, m.netWpm, 0.05)
    assertTrue(m.isComplete)
  }

  @Test
  fun hindiGraphemes_keepMatrasAttached() {
    val graphemes = TypingEngine.toGraphemes("हिंदी")
    assertEquals(2, graphemes.size)
    assertEquals("हिं", graphemes[0])
    assertEquals("दी", graphemes[1])
  }

  @Test
  fun transliterator_convertsRomanToDevanagari() {
    assertEquals("नमस्ते", Transliterator.transliterate("namaste"))
    assertEquals("भारत", Transliterator.transliterate("bhaarat"))
  }
}
