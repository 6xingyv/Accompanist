package com.mocharealm.accompanist.lyrics.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mocharealm.accompanist.lyrics.ui.R

@OptIn(ExperimentalTextApi::class)
val SFPro = FontFamily(
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.ExtraLight.weight),
        )
    ),
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
        )
    ),
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
        )
    ),
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
        )
    ),
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
        )
    ),
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.ExtraBold.weight),
        )
    ),
    Font(
        resId = R.font.sf_pro,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Black.weight),
        )
    ),
)
