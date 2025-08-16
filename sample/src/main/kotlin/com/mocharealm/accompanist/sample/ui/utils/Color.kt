package com.mocharealm.accompanist.sample.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun Color.copyHsl(
    hue: Float? = null,
    saturation: Float? = null,
    lightness: Float? = null,
    alpha: Float? = null
): Color {
    // 1. Create a Float array to store HSL values
    val hsl = FloatArray(3)

    // 2. Convert Compose Color (ARGB) to HSL
    // toArgb() converts Compose Color to Android's Int color value
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    // 3. Use user-provided new values, if user didn't provide (null), keep original values
    // ?: is Elvis operator, if left side is null, use right side value
    val newHue = hue ?: hsl[0]
    val newSaturation = saturation ?: hsl[1]
    val newLightness = lightness ?: hsl[2]
    val newAlpha = alpha ?: this.alpha

    // 4. Convert final HSL values back to ARGB Int color value
    val finalColorInt = ColorUtils.HSLToColor(floatArrayOf(newHue, newSaturation, newLightness))

    // 5. Create Compose Color from Int color value and apply final alpha
    return Color(finalColorInt).copy(alpha = newAlpha)
}