package com.example.domain

/**
 * Roman -> Devanagari phonetic transliteration engine.
 * Converts roman sequences into Unicode Devanagari clusters cleanly,
 * matching the TypePro phonetic practice behavior.
 */
object Transliterator {

  private val CONSONANTS = mapOf(
    "ksh" to "क्ष",
    "gya" to "ज्ञ",
    "shr" to "श्र",
    "chh" to "छ",
    "kh" to "ख",
    "gh" to "घ",
    "ch" to "च",
    "jh" to "झ",
    "th" to "थ",
    "dh" to "ध",
    "ph" to "फ",
    "bh" to "भ",
    "sh" to "श",
    "ss" to "ष",
    "tr" to "त्र",
    "ng" to "ङ",
    "ny" to "ञ",
    "k" to "क",
    "g" to "ग",
    "j" to "ज",
    "T" to "ट",
    "Th" to "ठ",
    "D" to "ड",
    "Dh" to "ढ",
    "N" to "ण",
    "t" to "त",
    "d" to "द",
    "n" to "न",
    "p" to "प",
    "f" to "फ",
    "b" to "ब",
    "m" to "म",
    "y" to "य",
    "r" to "र",
    "l" to "ल",
    "v" to "व",
    "w" to "व",
    "s" to "स",
    "h" to "ह",
    "L" to "ळ",
    "q" to "क़",
    "x" to "क्ष",
    "z" to "ज़",
  )

  private val VOWELS = mapOf(
    "a" to "अ",
    "aa" to "आ",
    "A" to "आ",
    "i" to "इ",
    "ee" to "ई",
    "I" to "ई",
    "ii" to "ई",
    "u" to "उ",
    "oo" to "ऊ",
    "U" to "ऊ",
    "uu" to "ऊ",
    "e" to "ए",
    "ai" to "ऐ",
    "o" to "ओ",
    "au" to "औ",
    "ri" to "ऋ",
  )

  private val MATRAS = mapOf(
    "a" to "",
    "aa" to "ा",
    "A" to "ा",
    "i" to "ि",
    "ee" to "ी",
    "I" to "ी",
    "ii" to "ी",
    "u" to "ु",
    "oo" to "ू",
    "U" to "ू",
    "uu" to "ू",
    "e" to "े",
    "ai" to "ै",
    "o" to "ो",
    "au" to "ौ",
    "ri" to "ृ",
  )

  private val SIGNS = mapOf(
    "M" to "ं",
    "H" to "ः",
    "~" to "ँ",
    "OM" to "ॐ",
  )

  private val DIGITS = listOf("०", "१", "२", "३", "४", "५", "६", "७", "८", "९")

  private val TOKEN_ORDER = listOf(
    "OM",
    "ksh", "gya", "shr", "chh",
    "kh", "gh", "ch", "jh", "th", "dh", "ph", "bh", "sh", "ss", "tr", "ng", "ny",
    "aa", "ee", "ii", "oo", "uu", "ai", "au", "ri",
    "Th", "Dh",
    "T", "D", "N", "L",
    "k", "g", "j", "t", "d", "n", "p", "f", "b", "m", "y", "r", "l", "v", "w", "s", "h", "q", "x", "z",
    "a", "A", "i", "I", "u", "U", "e", "o",
    "M", "H",
  )

  private fun tokenize(roman: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < roman.length) {
      var matched = ""
      for (candidate in TOKEN_ORDER) {
        if (roman.startsWith(candidate, i)) {
          matched = candidate
          break
        }
      }
      if (matched.isNotEmpty()) {
        tokens.add(matched)
        i += matched.length
      } else {
        tokens.add(roman[i].toString())
        i += 1
      }
    }
    return tokens
  }

  fun transliterate(roman: String): String {
    if (roman.isEmpty()) return ""
    val tokens = tokenize(roman)
    val output = StringBuilder()
    var pendingConsonant = false

    for (token in tokens) {
      if (token == "OM") {
        output.append(SIGNS["OM"])
        pendingConsonant = false
        continue
      }
      val consonant = CONSONANTS[token]
      if (consonant != null) {
        if (pendingConsonant) output.append('\u094d')
        output.append(consonant)
        pendingConsonant = true
        continue
      }
      val vowel = VOWELS[token]
      if (vowel != null) {
        if (pendingConsonant) {
          output.append(MATRAS[token] ?: "")
          pendingConsonant = false
        } else {
          output.append(vowel)
        }
        continue
      }
      if (token == "M" || token == "H" || token == "~") {
        output.append(SIGNS[token] ?: "")
        pendingConsonant = false
        continue
      }
      if (token.length == 1 && token[0] in '0'..'9') {
        output.append(DIGITS[token[0] - '0'])
        pendingConsonant = false
        continue
      }
      output.append(token)
      pendingConsonant = false
    }
    return output.toString()
  }
}
