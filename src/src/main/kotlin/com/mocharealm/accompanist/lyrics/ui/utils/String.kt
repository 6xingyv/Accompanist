package com.mocharealm.accompanist.lyrics.ui.utils

import android.os.Build

fun Char.isCjk(): Boolean {
    val cjkBlock = mutableListOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E)
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F)
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H)
    }
    return Character.UnicodeBlock.of(this) in cjkBlock
}

fun String.isPureCjk(): Boolean {
    val cleanedStr = this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    if (cleanedStr.isEmpty()) {
        return false
    }
    return cleanedStr.all { it.isCjk() }
}

fun String.isPunctuation(): Boolean {
    return isNotEmpty() && all { char ->
        char.isWhitespace() ||
                char in ".,!?;:\"'()[]{}…—–-、。，！？；：\"\"''（）【】《》～·" ||
                Character.getType(char) in setOf(
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt()
        )
    }
}