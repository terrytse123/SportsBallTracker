package com.example.sportsballtracker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.sportsballtracker.databinding.ActivityVideoEditorBinding
import java.io.File

class VideoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoEditorBinding
    private var player: ExoPlayer? = null
    private var videoFiles: List<File> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadRecordedVideos()
        setupPlayer()

        binding.btnTrim.setOnClickListener {
            Toast.makeText(this, "裁切功能將在下一版完成（使用 Media3 Transformer）", Toast.LENGTH_LONG).show()
        }
        binding.btnMerge.setOnClickListener {
            Toast.makeText(this, "合併多段影片功能將在下一版完成", Toast.LENGTH_LONG).show()
        }
        binding.btnExport.setOnClickListener {
            Toast.makeText(this, "導出到相簿功能將在下一版完成", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadRecordedVideos() {
        val dir = getExternalFilesDir(null)
        videoFiles = dir?.listFiles { file -> file.extension == "mp4" }?.toList() ?: emptyList()
        if (videoFiles.isNotEmpty()) {
            binding.statusText.text = "找到 ${videoFiles.size} 個錄影檔案"
            playVideo(videoFiles.first())
        } else {
            binding.statusText.text = "尚未有錄影檔案"
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
    }

    private fun playVideo(file: File) {
        val mediaItem = MediaItem.fromUri(file.toURI().toString())
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
