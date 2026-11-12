package com.example.ui.sound

import android.media.AudioManager
import android.media.ToneGenerator

enum class SoundCue {
  KEY,
  ERROR,
  FINISH,
}

object SoundFeedback {
  private var generator: ToneGenerator? = null

  private fun getOrCreateGenerator(): ToneGenerator? {
    if (generator != null) return generator
    return try {
      ToneGenerator(AudioManager.STREAM_MUSIC, 25).also { generator = it }
    } catch (_: Throwable) {
      null
    }
  }

  fun play(cue: SoundCue, enabled: Boolean) {
    if (!enabled) return
    try {
      val gen = getOrCreateGenerator() ?: return
      when (cue) {
        SoundCue.KEY -> gen.startTone(ToneGenerator.TONE_PROP_BEEP, 18)
        SoundCue.ERROR -> gen.startTone(ToneGenerator.TONE_PROP_NACK, 45)
        SoundCue.FINISH -> gen.startTone(ToneGenerator.TONE_PROP_ACK, 120)
      }
    } catch (_: Throwable) {
      // Ignore audio failures on devices without audio sink
    }
  }
}
