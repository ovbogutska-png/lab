package com.example.musicplayer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log

class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: Uri? = null
    private var lastPosition: Int = 0

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            "OPEN_FILE" -> {
                currentUri = intent?.data
                currentUri?.let { playAudio(it) }
            }

            "PAUSE" -> pauseAudio()
            "RESUME" -> resumeAudio()
            "SEEK_TO" -> seekTo(intent.getIntExtra("position", 0))
            "GET_PROGRESS" -> broadcastProgress()
        }

        return START_STICKY
    }

    private fun playAudio(uri: Uri) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
                    prepareAsync()
                    setOnPreparedListener {
                        it.seekTo(lastPosition)
                        it.start()
                    }
                }
                seekTo(lastPosition)
                start()
            }
        } else {
            mediaPlayer?.start()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                lastPosition = it.currentPosition
                it.pause()
                Log.d("MusicService", "Paused at: $lastPosition")
            }
        }
    }

    private fun resumeAudio() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.seekTo(lastPosition)
                it.start()
            }
        }
    }

    private fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position * 1000)
        lastPosition = position * 1000
    }

    private fun broadcastProgress() {
        mediaPlayer?.let {
            val intent = Intent("MUSIC_PROGRESS")
            intent.putExtra("progress", it.currentPosition / 1000)
            intent.putExtra("duration", it.duration / 1000)
            sendBroadcast(intent)
        }
    }
}