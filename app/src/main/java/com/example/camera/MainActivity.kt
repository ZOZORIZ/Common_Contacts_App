package com.example.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import androidx.camera.core.CameraControl
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object GlobalCameraManager {

    var isRecording = false
    var isFlashlightOn = false

    private var currentRecording: Recording? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var cameraControl: CameraControl? = null
    private lateinit var cameraSelector: CameraSelector
    private var contextRef: WeakReference<Context>? = null
    private var viewFinder: PreviewView? = null
    private var timerTextView: TextView? = null
    private var recordingStartTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsedMillis = SystemClock.elapsedRealtime() - recordingStartTime
                val minutes = (elapsedMillis / 1000) / 60
                val seconds = (elapsedMillis / 1000) % 60
                val millis = (elapsedMillis % 1000) / 10
                timerTextView?.text = String.format("%02d:%02d:%02d", minutes, seconds, millis)
                handler.postDelayed(this, 100)
            }
        }
    }

    fun initialize(context: Context, viewFinder: PreviewView, timerTextView: TextView) {
        this.contextRef = WeakReference(context)
        this.viewFinder = viewFinder
        this.timerTextView = timerTextView
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    }

    fun startCamera() {
        val context = contextRef?.get() ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder?.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview, imageCapture, videoCapture)
                cameraControl = camera.cameraControl
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera() // Reinitialize camera with the new selector
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val context = contextRef?.get() ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(context, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo saved to gallery: ${output.savedUri}"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraManager", msg)
                }
            }
        )
    }

    @SuppressLint("CheckResult")
    fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        val context = contextRef?.get() ?: return

        if (isRecording) return

        val hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val recording = videoCapture.output.prepareRecording(context, mediaStoreOutputOptions)

        if (hasAudioPermission) {
            recording.withAudioEnabled() // Only enable audio if the permission is granted
        }

        currentRecording = recording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    recordingStartTime = SystemClock.elapsedRealtime()
                    handler.post(updateTimerRunnable)
                    Log.d("CameraManager", "Recording started")
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        Toast.makeText(context, "Video saved: ${recordEvent.outputResults.outputUri}", Toast.LENGTH_SHORT).show()
                        Log.d("CameraManager", "Video capture succeeded: ${recordEvent.outputResults.outputUri}")
                    } else {
                        Toast.makeText(context, "Video capture failed: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                        Log.e("CameraManager", "Video capture failed: ${recordEvent.error}")
                    }
                    isRecording = false
                    handler.removeCallbacks(updateTimerRunnable)
                }
            }
        }
    }

    fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }

    fun turnOnFlashlight() {
        cameraControl?.enableTorch(true)
        isFlashlightOn = true
    }

    fun turnOffFlashlight() {
        cameraControl?.enableTorch(false)
        isFlashlightOn = false
    }
}


class MainActivity : ComponentActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var flashlightButton: Button
    private lateinit var switchCamButton: Button
    private lateinit var captureButton: Button
    private lateinit var galleryButton: Button
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val permissionGranted = permissions.all { it.value }
        if (permissionGranted) {
            GlobalCameraManager.startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerTextView = findViewById(R.id.timerTextView)
        flashlightButton = findViewById(R.id.flashlightButton)
        switchCamButton = findViewById(R.id.switchcambutton)
        captureButton = findViewById(R.id.captureButton)
        galleryButton = findViewById(R.id.openGalleryButton)
        viewFinder = findViewById(R.id.viewFinder)

        cameraExecutor = Executors.newSingleThreadExecutor()

        GlobalCameraManager.initialize(this, viewFinder, timerTextView)

        flashlightButton.setOnClickListener {
            if (GlobalCameraManager.isFlashlightOn) {
                GlobalCameraManager.turnOffFlashlight()
            } else {
                GlobalCameraManager.turnOnFlashlight()
            }
        }

        switchCamButton.setOnClickListener {
            GlobalCameraManager.switchCamera()
        }

        captureButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    GlobalCameraManager.startRecording()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    GlobalCameraManager.stopRecording()
                    true
                }
                else -> false
            }
        }

        captureButton.setOnLongClickListener {
            GlobalCameraManager.takePhoto()
            true
        }

        galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        if (requiredPermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            GlobalCameraManager.startCamera()
        } else {
            activityResultLauncher.launch(requiredPermissions)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            val selectedImageUri = data?.data
            selectedImageUri?.let {
                // You can now use the selectedImageUri to display the image or do whatever you need
                // For example, set it to an ImageView
                val imageView: ImageView = findViewById(R.id.placeholderImageView) // replace with your ImageView's ID
                imageView.setImageURI(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
