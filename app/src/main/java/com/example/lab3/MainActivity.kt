package com.example.musicplayer
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle

import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.lab3.R

class MainActivity : AppCompatActivity() {
    private val PICK_AUDIO_REQUEST = 1
    private var selectedAudioUri: Uri? = null
    private var isPlaying = false
    private lateinit var playPauseButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var timeElapsed: TextView
    private lateinit var albumArt: ImageView
    private val handler = Handler(Looper.getMainLooper())
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge() // Включаємо режим "edge-to-edge"
// Ініціалізація елементів UI
        val selectFileButton: Button = findViewById(R.id.selectFileButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seekBar)
        timeElapsed = findViewById(R.id.timeElapsed)
        albumArt = findViewById(R.id.albumArt)
// Обробник кнопки вибору файлу
        selectFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }
// Обробник кнопки play/pause
        playPauseButton.setOnClickListener {
            if (isPlaying) {
                startService(Intent(
                    this,
                    MusicService::class.java
                ).setAction("PAUSE"))
                playPauseButton.text = "Play"
                isPlaying = false
            } else {
                if (selectedAudioUri != null && !isPlaying) {
                    val serviceIntent = Intent(
                        this, MusicService::class.java
                    ).apply {
                        action = "OPEN_FILE"
                        data = selectedAudioUri
                    }
                    startService(serviceIntent)
                } else {
                    startService(Intent(
                        this,
                        MusicService::class.java

                    ).setAction("RESUME"))
                }
                playPauseButton.text = "Pause"
                isPlaying = true
                updateSeekBar() // Оновлюємо слайдер
            }
        }
// Обробник змін слайдера
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val intent = Intent(
                        this@MainActivity,
                        MusicService::class.java
                    ).apply {
                        action = "SEEK_TO"
                        putExtra("position", progress)
                    }
                    startService(intent)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
// Реєстрація приймача прогресу відтворення
        registerReceiver(progressReceiver, IntentFilter("MUSIC_PROGRESS"), RECEIVER_NOT_EXPORTED)
    }
    // Обробка вибору аудіофайлу
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedAudioUri = data?.data
            contentResolver.takePersistableUriPermission(
                selectedAudioUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            updateAlbumArt(selectedAudioUri!!) // Оновлюємо обкладинку
        }
    }
    // Оновлення обкладинки альбому
    private fun updateAlbumArt(uri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val art = retriever.embeddedPicture

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                albumArt.setImageBitmap(bitmap)
            } else {
                albumArt.setImageResource(R.drawable.default_album_art)
            }
        } catch (e: Exception) {
            albumArt.setImageResource(R.drawable.default_album_art)
        } finally {
            retriever.release()
        }
    }

    // Оновлення слайдера (прогресу відтворення)
    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val intent = Intent(
                    this@MainActivity,
                    MusicService::class.java
                ).apply {
                    action = "GET_PROGRESS"
                }
                startService(intent)
                handler.postDelayed(this, 1000) // Оновлення щосекунди
            }
        }, 1000)
    }
    // Приймач для отримання прогресу
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra("progress", 0) ?: 0
            val duration = intent?.getIntExtra("duration", 1) ?: 1
            seekBar.max = duration // Оновлення макс. значення слайдера
            seekBar.progress = progress // Оновлення поточного значення
// Форматування часу
            val minutes = progress / 60
            val seconds = progress % 60
            timeElapsed.text = String.format("%02d:%02d", minutes, seconds)
        }
    }
}
