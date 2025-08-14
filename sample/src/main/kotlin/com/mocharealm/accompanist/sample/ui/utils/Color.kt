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
    // 1. 创建一个Float数组来存储HSL值
    val hsl = FloatArray(3)

    // 2. 将Compose Color (ARGB) 转换为 HSL
    // toArgb() 将Compose Color转为Android的Int颜色值
    ColorUtils.colorToHSL(this.toArgb(), hsl)

    // 3. 使用用户提供的新值，如果用户未提供（为null），则保留原始值
    // ?: 是 Elvis 操作符，如果左侧为null，则使用右侧的值
    val newHue = hue ?: hsl[0]
    val newSaturation = saturation ?: hsl[1]
    val newLightness = lightness ?: hsl[2]
    val newAlpha = alpha ?: this.alpha

    // 4. 将最终的HSL值转换回 ARGB Int 颜色值
    val finalColorInt = ColorUtils.HSLToColor(floatArrayOf(newHue, newSaturation, newLightness))

    // 5. 从Int颜色值创建Compose Color，并应用最终的透明度
    return Color(finalColorInt).copy(alpha = newAlpha)
}