package com.example.sportsballtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.example.sportsballtracker.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var isBackCamera = true
    private var isDualMode = false
    private var isRecording = false
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, "需要相機權限", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        }

        binding.btnSwitchCamera.setOnClickListener {
            isBackCamera = !isBackCamera
            startCamera()
        }

        binding.btnDualCamera.setOnClickListener {
            isDualMode = !isDualMode
            binding.btnDualCamera.text = if (isDualMode) "單鏡頭" else "雙鏡頭"
            binding.previewViewFront.visibility = if (isDualMode) android.view.View.VISIBLE else android.view.View.GONE
            startCamera()
        }

        binding.btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else startRecording()
        }

        binding.btnEditor.setOnClickListener {
            startActivity(Intent(this, VideoEditorActivity::class.java))
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, ObjectAnalyzer())
            }

        try {
            if (isDualMode && cameraProvider.availableConcurrentCameraInfos.isNotEmpty()) {
                val concurrentInfos = cameraProvider.availableConcurrentCameraInfos.firstOrNull()
                if (concurrentInfos != null && concurrentInfos.size >= 2) {
                    val backSelector = concurrentInfos.first { it.lensFacing == CameraSelector.LENS_FACING_BACK }.cameraSelector
                    val frontSelector = concurrentInfos.first { it.lensFacing == CameraSelector.LENS_FACING_FRONT }.cameraSelector

                    val backPreview = Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }
                    val frontPreview = Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.previewViewFront.surfaceProvider)
                    }

                    val useCaseGroup = UseCaseGroup.Builder()
                        .addUseCase(backPreview)
                        .addUseCase(videoCapture!!)
                        .addUseCase(imageAnalysis)
                        .build()

                    val backConfig = ConcurrentCamera.SingleCameraConfig(backSelector, useCaseGroup, this)
                    val frontConfig = ConcurrentCamera.SingleCameraConfig(frontSelector, UseCaseGroup.Builder().addUseCase(frontPreview).build(), this)

                    cameraProvider.bindToLifecycle(listOf(backConfig, frontConfig))
                    binding.statusText.text = "雙鏡頭模式啟用"
                } else {
                    fallbackToSingleCamera(cameraProvider, preview, imageAnalysis)
                }
            } else {
                fallbackToSingleCamera(cameraProvider, preview, imageAnalysis)
            }
        } catch (e: Exception) {
            Log.e("SportsBallTracker", "Camera bind failed", e)
            Toast.makeText(this, "雙鏡頭不支援，已切換單鏡頭", Toast.LENGTH_SHORT).show()
            isDualMode = false
            binding.btnDualCamera.text = "雙鏡頭"
            binding.previewViewFront.visibility = android.view.View.GONE
            fallbackToSingleCamera(cameraProvider, preview, imageAnalysis)
        }
    }

    private fun fallbackToSingleCamera(provider: ProcessCameraProvider, preview: Preview, analysis: ImageAnalysis) {
        val selector = if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        provider.bindToLifecycle(this, selector, preview, analysis, videoCapture)
        binding.statusText.text = if (isBackCamera) "後鏡頭" else "前鏡頭"
    }

    private inner class ObjectAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    binding.trackingOverlay.updateDetections(
                        detectedObjects,
                        imageProxy.width,
                        imageProxy.height
                    )
                    val status = if (detectedObjects.isEmpty()) "等待偵測球體…"
                    else "已追蹤 ${detectedObjects.size} 個物件"
                    runOnUiThread { binding.statusText.text = status }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun startRecording() {
        val videoCapture = videoCapture ?: return
        val outputOptions = FileOutputOptions.Builder(
            java.io.File(getExternalFilesDir(null), "ball_track_${System.currentTimeMillis()}.mp4")
        ).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        binding.btnRecord.text = "停止錄影"
                        Toast.makeText(this, "開始錄影", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        binding.btnRecord.text = "開始錄影"
                        if (!event.hasError()) {
                            Toast.makeText(this, "錄影已儲存", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        objectDetector.close()
    }
}
