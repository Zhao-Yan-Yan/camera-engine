package com.yanz.camera.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

/**
 * @author yanz
 * @date 2023/6/19 20:43
 */
class CameraXControllerImpl(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
) : ICamera {
    val TAG = "CameraXControllerImpl"
    private lateinit var cameraController: LifecycleCameraController

    private lateinit var defaultConfig: MyCameraConfig

    private val cameraExecutor = ContextCompat.getMainExecutor(context)

    @SuppressLint("RestrictedApi")
    override fun initCamera(defaultConfig: MyCameraConfig) {
        this.defaultConfig = defaultConfig
        cameraController = LifecycleCameraController(context)
        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)

        cameraController.imageCaptureFlashMode
        cameraController.imageCaptureTargetSize = CameraController.OutputSize(AspectRatio.RATIO_4_3)
        previewView.controller = cameraController
        cameraController.bindToLifecycle(lifecycleOwner)

    }

    override fun takePhoto(options: ImageCapture.OutputFileOptions) {
        cameraController.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.e(TAG, "onImageSaved: ${outputFileResults.savedUri}")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "onError: ${exception.message}")
            }
        })
    }

    override fun takePhoto() {
        cameraController.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    @SuppressLint("RestrictedApi")
    override fun selectLens(lens: Int) {
        if (lens == MyCameraConfig.LENS_FRONT && hasFrontLens()) {
            cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        } else if (lens == MyCameraConfig.LENS_BACK && hasBackLens()) {
            cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    override fun switchLens() {
        if (cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            selectLens(MyCameraConfig.LENS_BACK)
        } else {
            selectLens(MyCameraConfig.LENS_FRONT)
        }
    }

    override fun captureVideo() {

    }

    override fun startFocus(x: Float, y: Float) {
        val cameraControl = cameraController.cameraControl ?: return
        val meteringPointFactory = previewView.meteringPointFactory
        val focusPoint = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(focusPoint).build()
        cameraControl.startFocusAndMetering(action)
    }

    override fun setZoomRatio(ratio: Float) {
        cameraController.setZoomRatio(ratio)
    }

    override fun hasBackLens(): Boolean {
        return cameraController.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    override fun hasFrontLens(): Boolean {
        return cameraController.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }
}