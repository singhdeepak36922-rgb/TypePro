package com.example.domain

import androidx.compose.ui.graphics.Color

enum class Finger(val label: String, val color: Color, val hand: String) {
  LEFT_PINKY("Left pinky", Color(0xFFF472B6), "Left hand"),
  LEFT_RING("Left ring", Color(0xFFA78BFA), "Left hand"),
  LEFT_MIDDLE("Left middle", Color(0xFF60A5FA), "Left hand"),
  LEFT_INDEX("Left index", Color(0xFF22D3EE), "Left hand"),
  RIGHT_INDEX("Right index", Color(0xFF34D399), "Right hand"),
  RIGHT_MIDDLE("Right middle", Color(0xFFFBBF24), "Right hand"),
  RIGHT_RING("Right ring", Color(0xFFFB923C), "Right hand"),
  RIGHT_PINKY("Right pinky", Color(0xFFF87171), "Right hand"),
  THUMB("Thumb", Color(0xFF94A3B8), "Either"),
}

data class VirtualKeyDef(
  val code: String,
  val base: String,
  val shift: String? = null,
  val finger: Finger,
  val width: Float = 1f,
  val label: String? = null,
)

object KeyboardLayouts {
  val HOME_ROW = listOf("a", "s", "d", "f", "j", "k", "l", ";")

  val EN_ROWS: List<List<VirtualKeyDef>> = listOf(
    listOf(
      VirtualKeyDef("Backquote", "`", "~", Finger.LEFT_PINKY),
      VirtualKeyDef("Digit1", "1", "!", Finger.LEFT_PINKY),
      VirtualKeyDef("Digit2", "2", "@", Finger.LEFT_RING),
      VirtualKeyDef("Digit3", "3", "#", Finger.LEFT_MIDDLE),
      VirtualKeyDef("Digit4", "4", "$", Finger.LEFT_INDEX),
      VirtualKeyDef("Digit5", "5", "%", Finger.LEFT_INDEX),
      VirtualKeyDef("Digit6", "6", "^", Finger.RIGHT_INDEX),
      VirtualKeyDef("Digit7", "7", "&", Finger.RIGHT_INDEX),
      VirtualKeyDef("Digit8", "8", "*", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("Digit9", "9", "(", Finger.RIGHT_RING),
      VirtualKeyDef("Digit0", "0", ")", Finger.RIGHT_PINKY),
      VirtualKeyDef("Minus", "-", "_", Finger.RIGHT_PINKY),
      VirtualKeyDef("Equal", "=", "+", Finger.RIGHT_PINKY),
      VirtualKeyDef("Backspace", "⌫", null, Finger.RIGHT_PINKY, width = 1.8f, label = "⌫"),
    ),
    listOf(
      VirtualKeyDef("Tab", "Tab", null, Finger.LEFT_PINKY, width = 1.4f, label = "Tab"),
      VirtualKeyDef("KeyQ", "q", "Q", Finger.LEFT_PINKY),
      VirtualKeyDef("KeyW", "w", "W", Finger.LEFT_RING),
      VirtualKeyDef("KeyE", "e", "E", Finger.LEFT_MIDDLE),
      VirtualKeyDef("KeyR", "r", "R", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyT", "t", "T", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyY", "y", "Y", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyU", "u", "U", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyI", "i", "I", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("KeyO", "o", "O", Finger.RIGHT_RING),
      VirtualKeyDef("KeyP", "p", "P", Finger.RIGHT_PINKY),
      VirtualKeyDef("BracketLeft", "[", "{", Finger.RIGHT_PINKY),
      VirtualKeyDef("BracketRight", "]", "}", Finger.RIGHT_PINKY),
      VirtualKeyDef("Backslash", "\\", "|", Finger.RIGHT_PINKY, width = 1.4f),
    ),
    listOf(
      VirtualKeyDef("CapsLock", "Caps", null, Finger.LEFT_PINKY, width = 1.6f, label = "Caps"),
      VirtualKeyDef("KeyA", "a", "A", Finger.LEFT_PINKY),
      VirtualKeyDef("KeyS", "s", "S", Finger.LEFT_RING),
      VirtualKeyDef("KeyD", "d", "D", Finger.LEFT_MIDDLE),
      VirtualKeyDef("KeyF", "f", "F", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyG", "g", "G", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyH", "h", "H", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyJ", "j", "J", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyK", "k", "K", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("KeyL", "l", "L", Finger.RIGHT_RING),
      VirtualKeyDef("Semicolon", ";", ":", Finger.RIGHT_PINKY),
      VirtualKeyDef("Quote", "'", "\"", Finger.RIGHT_PINKY),
      VirtualKeyDef("Enter", "Enter", null, Finger.RIGHT_PINKY, width = 2.0f, label = "Enter"),
    ),
    listOf(
      VirtualKeyDef("ShiftLeft", "Shift", null, Finger.LEFT_PINKY, width = 2.1f, label = "Shift"),
      VirtualKeyDef("KeyZ", "z", "Z", Finger.LEFT_PINKY),
      VirtualKeyDef("KeyX", "x", "X", Finger.LEFT_RING),
      VirtualKeyDef("KeyC", "c", "C", Finger.LEFT_MIDDLE),
      VirtualKeyDef("KeyV", "v", "V", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyB", "b", "B", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyN", "n", "N", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyM", "m", "M", Finger.RIGHT_INDEX),
      VirtualKeyDef("Comma", ",", "<", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("Period", ".", ">", Finger.RIGHT_RING),
      VirtualKeyDef("Slash", "/", "?", Finger.RIGHT_PINKY),
      VirtualKeyDef("ShiftRight", "Shift", null, Finger.RIGHT_PINKY, width = 2.2f, label = "Shift"),
    ),
    listOf(
      VirtualKeyDef("Space", " ", null, Finger.THUMB, width = 6.5f, label = "space"),
    ),
  )

  val HINDI_ROWS: List<List<VirtualKeyDef>> = listOf(
    listOf(
      VirtualKeyDef("Digit1", "१", "!", Finger.LEFT_PINKY),
      VirtualKeyDef("Digit2", "२", "@", Finger.LEFT_RING),
      VirtualKeyDef("Digit3", "३", "#", Finger.LEFT_MIDDLE),
      VirtualKeyDef("Digit4", "४", "$", Finger.LEFT_INDEX),
      VirtualKeyDef("Digit5", "५", "%", Finger.LEFT_INDEX),
      VirtualKeyDef("Digit6", "६", "^", Finger.RIGHT_INDEX),
      VirtualKeyDef("Digit7", "७", "&", Finger.RIGHT_INDEX),
      VirtualKeyDef("Digit8", "८", "*", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("Digit9", "९", "(", Finger.RIGHT_RING),
      VirtualKeyDef("Digit0", "०", ")", Finger.RIGHT_PINKY),
    ),
    listOf(
      VirtualKeyDef("KeyQ", "ौ", "औ", Finger.LEFT_PINKY),
      VirtualKeyDef("KeyW", "ै", "ऐ", Finger.LEFT_RING),
      VirtualKeyDef("KeyE", "ा", "आ", Finger.LEFT_MIDDLE),
      VirtualKeyDef("KeyR", "ी", "ई", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyT", "ू", "ऊ", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyY", "ब", "भ", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyU", "ह", "ङ", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyI", "ग", "घ", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("KeyO", "द", "ध", Finger.RIGHT_RING),
      VirtualKeyDef("KeyP", "ज", "झ", Finger.RIGHT_PINKY),
      VirtualKeyDef("BracketLeft", "ड", "ढ", Finger.RIGHT_PINKY),
      VirtualKeyDef("BracketRight", "़", "ञ", Finger.RIGHT_PINKY),
    ),
    listOf(
      VirtualKeyDef("KeyA", "ो", "ओ", Finger.LEFT_PINKY),
      VirtualKeyDef("KeyS", "े", "ए", Finger.LEFT_RING),
      VirtualKeyDef("KeyD", "्", "अ", Finger.LEFT_MIDDLE),
      VirtualKeyDef("KeyF", "ि", "इ", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyG", "ु", "उ", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyH", "प", "फ", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyJ", "र", "ऱ", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyK", "क", "ख", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("KeyL", "त", "थ", Finger.RIGHT_RING),
      VirtualKeyDef("Semicolon", "च", "छ", Finger.RIGHT_PINKY),
      VirtualKeyDef("Quote", "ट", "ठ", Finger.RIGHT_PINKY),
    ),
    listOf(
      VirtualKeyDef("KeyZ", "ॆ", "ॐ", Finger.LEFT_PINKY),
      VirtualKeyDef("KeyX", "ं", "ँ", Finger.LEFT_RING),
      VirtualKeyDef("KeyC", "म", "ण", Finger.LEFT_MIDDLE),
      VirtualKeyDef("KeyV", "न", "ऩ", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyB", "व", "ळ", Finger.LEFT_INDEX),
      VirtualKeyDef("KeyN", "ल", "श", Finger.RIGHT_INDEX),
      VirtualKeyDef("KeyM", "स", "ष", Finger.RIGHT_INDEX),
      VirtualKeyDef("Comma", ",", "ष", Finger.RIGHT_MIDDLE),
      VirtualKeyDef("Period", ".", "।", Finger.RIGHT_RING),
      VirtualKeyDef("Slash", "य", "्य", Finger.RIGHT_PINKY),
    ),
    listOf(
      VirtualKeyDef("Shift", "Shift", null, Finger.LEFT_PINKY, width = 2f, label = "Shift"),
      VirtualKeyDef("Space", " ", null, Finger.THUMB, width = 6f, label = "space"),
      VirtualKeyDef("Backspace", "⌫", null, Finger.RIGHT_PINKY, width = 2f, label = "⌫"),
    ),
  )

  fun findEnglishKey(char: String?): VirtualKeyDef? {
    if (char == null) return null
    for (row in EN_ROWS) {
      for (key in row) {
        if (key.base == char || key.shift == char) return key
      }
    }
    return null
  }

  fun findHindiKey(grapheme: String?): VirtualKeyDef? {
    if (grapheme == null) return null
    for (row in HINDI_ROWS) {
      for (key in row) {
        if (key.base == grapheme || key.shift == grapheme) return key
      }
    }
    // Check base character if grapheme is a Devanagari cluster (consonant + matra)
    val firstChar = grapheme.firstOrNull()?.toString() ?: return null
    for (row in HINDI_ROWS) {
      for (key in row) {
        if (key.base == firstChar || key.shift == firstChar) return key
      }
    }
    return null
  }
}
