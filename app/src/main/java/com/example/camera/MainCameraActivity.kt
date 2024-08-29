package com.example.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainCameraActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var captureButton: ImageButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var flashButton: ImageButton
    private lateinit var gridButton: ImageButton
    private lateinit var zoomSeekBar: SeekBar

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize UI components
        surfaceView = findViewById(R.id.surfaceView)
        captureButton = findViewById(R.id.captureButton)
        switchCameraButton = findViewById(R.id.switchCameraButton)
        flashButton = findViewById(R.id.flashButton)
        gridButton = findViewById(R.id.gridButton)
        zoomSeekBar = findViewById(R.id.zoomSeekBar)

        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        // Set up SurfaceView holder
        surfaceView.holder.addCallback(this)

        // Set up capture button
        captureButton.setOnClickListener {
            capturePhoto()
        }

        // Set up other buttons...
    }

    private fun capturePhoto() {
        val photoFile: File? = getOutputMediaFile()
        photoFile?.also {
            imageUri = FileProvider.getUriForFile(
                this,
                "com.example.camera.fileprovider",
                it
            )
            // Assuming you have already implemented camera preview and capture logic
            // Here we simulate the capture action and return the URI
            returnCapturedImageUri()
        }
    }

    private fun returnCapturedImageUri() {
        val resultIntent = Intent().apply {
            putExtra("imageUri", imageUri.toString())
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish() // Close this activity and return to the previous one
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Initialize camera here
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Release camera resources
    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyCameraApp"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory")
                return null
            }
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
    }
}
