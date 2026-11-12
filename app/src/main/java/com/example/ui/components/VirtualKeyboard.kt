package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Finger
import com.example.domain.KeyboardLayouts
import com.example.domain.Language
import com.example.domain.VirtualKeyDef
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTypeProColors

@Composable
fun VirtualKeyboard(
  language: Language,
  expectedGrapheme: String?,
  interactive: Boolean,
  onKeyInput: ((String) -> Unit)? = null,
  onBackspace: (() -> Unit)? = null,
  highlightedFinger: Finger? = null,
  compact: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val ext = LocalTypeProColors.current
  var shiftActive by remember { mutableStateOf(false) }

  val rows = if (language == Language.HI) KeyboardLayouts.HINDI_ROWS else KeyboardLayouts.EN_ROWS
  val expectedDef = remember(language, expectedGrapheme) {
    if (language == Language.HI) {
      KeyboardLayouts.findHindiKey(expectedGrapheme)
    } else {
      KeyboardLayouts.findEnglishKey(expectedGrapheme)
    }
  }
  val needsShift = remember(expectedDef, expectedGrapheme) {
    expectedDef != null && expectedGrapheme != null && expectedDef.shift == expectedGrapheme
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    rows.forEach { row ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        row.forEach { keyDef ->
          val isExpected = expectedDef?.code == keyDef.code ||
            (needsShift && (keyDef.code == "ShiftLeft" || keyDef.code == "ShiftRight" || keyDef.code == "Shift"))
          val isFingerHighlighted = highlightedFinger != null && keyDef.finger == highlightedFinger
          val isShiftLit = (keyDef.code.startsWith("Shift")) && shiftActive

          val displayChar = when {
            keyDef.label != null -> keyDef.label
            shiftActive && keyDef.shift != null -> keyDef.shift
            else -> keyDef.base
          }

          val keyBg = when {
            isExpected -> MaterialTheme.colorScheme.primary
            isShiftLit -> ext.brandSoft
            isFingerHighlighted -> keyDef.finger.color.copy(alpha = 0.24f)
            else -> ext.panel
          }
          val keyFg = when {
            isExpected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
          }
          val border = when {
            isExpected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
            isFingerHighlighted -> BorderStroke(1.dp, keyDef.finger.color)
            else -> BorderStroke(1.dp, ext.line)
          }

          Surface(
            modifier = Modifier
              .weight(keyDef.width)
              .height(if (compact) 38.dp else 44.dp)
              .clip(RoundedCornerShape(7.dp))
              .then(
                if (interactive) {
                  Modifier.clickable {
                    when (keyDef.code) {
                      "ShiftLeft", "ShiftRight", "Shift" -> shiftActive = !shiftActive
                      "Backspace" -> onBackspace?.invoke()
                      "Enter" -> onKeyInput?.invoke("\n")
                      "Tab", "CapsLock" -> Unit
                      "Space" -> onKeyInput?.invoke(" ")
                      else -> {
                        val out = if (shiftActive && keyDef.shift != null) {
                          shiftActive = false
                          keyDef.shift
                        } else {
                          keyDef.base
                        }
                        onKeyInput?.invoke(out)
                      }
                    }
                  }
                } else {
                  Modifier
                },
              ),
            shape = RoundedCornerShape(7.dp),
            color = keyBg,
            border = border,
          ) {
            Box(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier
                  .align(Alignment.Center)
                  .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = displayChar,
                  fontFamily = if (language == Language.HI && keyDef.label == null) {
                    DevanagariFontFamily
                  } else {
                    JetBrainsMonoFamily
                  },
                  fontSize = if (compact) 11.sp else 13.sp,
                  fontWeight = if (isExpected) FontWeight.Bold else FontWeight.Medium,
                  color = keyFg,
                  textAlign = TextAlign.Center,
                  maxLines = 1,
                )
                if (!compact && keyDef.shift != null && keyDef.label == null && language == Language.HI) {
                  Text(
                    text = keyDef.shift,
                    fontFamily = DevanagariFontFamily,
                    fontSize = 9.sp,
                    color = keyFg.copy(alpha = 0.65f),
                    maxLines = 1,
                  )
                }
              }
              Box(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .height(3.dp)
                  .background(keyDef.finger.color),
              )
            }
          }
        }
      }
    }
  }
}
