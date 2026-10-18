package com.example.sportsballtracker

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.sportsballtracker.databinding.ActivityVideoEditorBinding
import com.google.android.material.button.MaterialButton
import java.io.File

class VideoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoEditorBinding
    private var player: ExoPlayer? = null
    private var videoFiles: List<File> = emptyList()
    private var currentFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayer()
        loadRecordedVideos()

        binding.btnTrim.setOnClickListener {
            Toast.makeText(this, "裁切功能開發中（Media3 Transformer）", Toast.LENGTH_SHORT).show()
        }
        binding.btnMerge.setOnClickListener {
            Toast.makeText(this, "合併功能開發中", Toast.LENGTH_SHORT).show()
        }
        binding.btnExport.setOnClickListener {
            exportCurrentVideoToGallery()
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        binding.statusText.text = currentFile?.name ?: "播放中"
                    }
                }
            })
        }
    }

    private fun loadRecordedVideos() {
        val dir = getExternalFilesDir(null) ?: return
        videoFiles = dir.listFiles { file ->
            file.isFile && (file.extension.equals("mp4", true) || file.extension.equals("jpg", true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        binding.fileListContainer.removeAllViews()

        if (videoFiles.isEmpty()) {
            binding.statusText.text = "尚未有錄影或照片"
            return
        }

        binding.statusText.text = "找到 ${videoFiles.size} 個檔案"

        videoFiles.forEach { file ->
            val btn = MaterialButton(this).apply {
                text = if (file.extension.equals("mp4", true)) "影片\n${file.name.take(12)}" else "照片\n${file.name.take(12)}"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                setOnClickListener {
                    currentFile = file
                    if (file.extension.equals("mp4", true)) {
                        playVideo(file)
                    } else {
                        Toast.makeText(this@VideoEditorActivity, "照片：${file.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            binding.fileListContainer.addView(btn)
        }

        // Auto play first video
        videoFiles.firstOrNull { it.extension.equals("mp4", true) }?.let {
            currentFile = it
            playVideo(it)
        }
    }

    private fun playVideo(file: File) {
        val uri = Uri.fromFile(file)
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
        binding.statusText.text = "播放：${file.name}"
    }

    private fun exportCurrentVideoToGallery() {
        val file = currentFile ?: run {
            Toast.makeText(this, "請先選擇影片", Toast.LENGTH_SHORT).show()
            return
        }
        if (!file.extension.equals("mp4", true)) {
            Toast.makeText(this, "目前僅支援匯出影片", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SportsBallTracker")
            }
            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                Toast.makeText(this, "已匯出到相簿/Movies/SportsBallTracker", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "匯出失敗", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "匯出錯誤：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
