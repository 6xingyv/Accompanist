package com.mocharealm.accompanist.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mocharealm.accompanist.MainActivity

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    // 在 Service 创建时被调用
    override fun onCreate() {
        super.onCreate()
        initializePlayerAndSession()
    }

    private fun initializePlayerAndSession() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        val sessionActivityIntent = Intent(this, MainActivity::class.java)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // ✨ 3. 在构建 MediaSession 时，设置 sessionActivity
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}