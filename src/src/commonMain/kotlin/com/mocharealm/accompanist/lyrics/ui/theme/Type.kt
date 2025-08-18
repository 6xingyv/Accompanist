package com.mocharealm.accompanist.lyrics.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mocharealm.accompanist.lyrics.ui.Res
import com.mocharealm.accompanist.lyrics.ui.sf_pro
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalTextApi::class)
@Composable
fun SFPro(): FontFamily = FontFamily(
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.ExtraLight.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.ExtraBold.weight),
        )
    ),
)
