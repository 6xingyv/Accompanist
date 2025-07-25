package com.mocharealm.accompanist.domain.model

import androidx.media3.common.MediaItem

data class MusicItem(
    val label: String,
    val testTarget: String,
    val mediaItem: MediaItem,
    val lyrics: String,
    val translation: String? = null,
)