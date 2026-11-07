package com.essential.services.utils

import android.content.Context
import android.media.MediaPlayer
import com.essential.services.R

object BackgroundMusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, resId: Int = R.raw.a_gentle_bloom) {
        if (mediaPlayer?.isPlaying == true) return
        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId).apply {
                isLooping = false
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
