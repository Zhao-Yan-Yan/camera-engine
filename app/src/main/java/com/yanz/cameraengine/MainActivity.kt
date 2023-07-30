package com.yanz.cameraengine

import android.Manifest
import android.content.ContentValues
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import com.yanz.camera.engine.CameraManager
import com.yanz.camera.engine.CameraXImpl
import com.yanz.camera.engine.MyCameraConfig
import com.yanz.cameraengine.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraManager by lazy {
        CameraManager.init(CameraXImpl(this, this, binding.cameraxPreview))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 991)
        }

        cameraManager.initCamera(MyCameraConfig(MyCameraConfig.LENS_BACK))
        binding.take.setOnClickListener {
            take()
        }

        binding.switchLens.setOnClickListener {
            switchLens()
        }
    }

    private fun take() {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, UUID.randomUUID().toString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraEngine")
            }
        }
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
        cameraManager.takePhoto(outputFileOptions)
    }

    private fun switchLens() {
        cameraManager.switchLens()
    }
}