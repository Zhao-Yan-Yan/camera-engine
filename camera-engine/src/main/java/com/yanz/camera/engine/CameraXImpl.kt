package com.yanz.camera.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.ResolutionSelector
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * @author yanz
 * @date 2023/6/6 16:27
 */
class CameraXImpl(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
) : ICamera {

    companion object {
        const val TAG = "CameraXImpl"
    }

    @SuppressLint("RestrictedApi")
    private val imageCapture = ImageCapture.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setPreferredAspectRatio(RATIO_16_9)
                .build()
        )
        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    @SuppressLint("RestrictedApi")
    private val preview = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setPreferredAspectRatio(RATIO_16_9)
                .build()
        )
        .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }


    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var camera: Camera? = null

    private var cameraProvider: ProcessCameraProvider? = null

    override fun initCamera(defaultConfig: MyCameraConfig) {
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraSelector = safeSelectorLens(defaultConfig.lens)
        initCameraProvider()
        handleOrientation()
        handleGesture()
    }

    override fun takePhoto(options: ImageCapture.OutputFileOptions) {
        imageCapture.takePicture(options, Dispatchers.Default.asExecutor(), object : ImageCapture.OnImageSavedCallback {
            override fun onError(error: ImageCaptureException) {
                Log.e("TAG", "onError: ${error.message}")
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.e("TAG", "onImageSaved: ${outputFileResults.savedUri}")
            }
        })
    }

    override fun takePhoto() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    override fun selectLens(lens: Int) {
        cameraSelector = safeSelectorLens(lens)
        unBindUseCase()
        startCamera()
    }

    @SuppressLint("RestrictedApi")
    override fun switchLens() {
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            selectLens(MyCameraConfig.LENS_FRONT)
        } else {
            selectLens(MyCameraConfig.LENS_BACK)
        }

    }

    override fun captureVideo() {

    }

    override fun startFocus(x: Float, y: Float) {
        val camera = camera ?: return
        val meteringPointFactory = previewView.meteringPointFactory
        val focusPoint = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(focusPoint).build()
        camera.cameraControl.startFocusAndMetering(action)
    }

    override fun setZoomRatio(ratio: Float) {
        val camera = camera ?: return
        camera.cameraControl.setZoomRatio(ratio)
    }

    override fun hasBackLens(): Boolean {
        return try {
            cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
        } catch (exception: CameraInfoUnavailableException) {
            false
        }
    }

    override fun hasFrontLens(): Boolean {
        return try {
            cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
        } catch (exception: CameraInfoUnavailableException) {
            false
        }
    }


    @SuppressLint("RestrictedApi")
    private fun initCameraProvider() {
        Futures.transform(ProcessCameraProvider.getInstance(context), {
            cameraProvider = it
            startCamera()
            return@transform null
        }, CameraXExecutors.mainThreadExecutor())
    }

    private fun startCamera() {
        val cameraProvider = cameraProvider ?: return
        this.camera?.cameraInfo?.let { removeCameraStateObservers(it) }
        this.camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, createUseCase())
        this.camera?.cameraInfo?.let { observeCameraState(it) }
    }

    private fun createUseCase(): UseCaseGroup {
        val builder = UseCaseGroup.Builder().addUseCase(preview)
        builder.addUseCase(imageCapture)
        return builder.build()
    }

    private fun unBindUseCase() {
        val cameraProvider = cameraProvider ?: return
        cameraProvider.unbindAll()
    }

    private fun handleOrientation() {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture.targetRotation = rotation
            }
        }
        orientationEventListener.enable()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleGesture() {
        val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                startFocus(e.x, e.y)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                switchLens()
                return true
            }
        })
        val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val camera = camera ?: return true
                val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                setZoomRatio(detector.scaleFactor * currentZoomRatio)
                return true
            }
        })
        previewView.setOnTouchListener { _, event ->
            var didConsume = scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress) {
                didConsume = gestureDetector.onTouchEvent(event)
            }
            didConsume
        }
    }

    private fun safeSelectorLens(lens: Int): CameraSelector {
        return if (lens == MyCameraConfig.LENS_FRONT && hasFrontLens()) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(lifecycleOwner)
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(lifecycleOwner) { cameraState ->
            when (cameraState.type) {
                CameraState.Type.PENDING_OPEN -> {
                    // Ask the user to close other camera apps
                    Log.d(TAG, "CameraState: Pending Open")
                }

                CameraState.Type.OPENING -> {
                    // Show the Camera UI
                    Log.d(TAG, "CameraState: Opening")
                }

                CameraState.Type.OPEN -> {
                    // Setup Camera resources and begin processing
                    Log.d(TAG, "CameraState: Open")
                }

                CameraState.Type.CLOSING -> {
                    // Close camera UI
                    Log.d(TAG, "CameraState: Closing")
                }

                CameraState.Type.CLOSED -> {
                    // Free camera resources
                    Log.d(TAG, "CameraState: Closed")
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Log.e(TAG, "CameraState: Stream config error")
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the camera
                        Log.e(TAG, "CameraState: Camera in use")
                    }

                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another camera app that's using the camera
                        Log.e(TAG, "CameraState: Max cameras in use")
                    }

                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Log.e(TAG, "CameraState: Other recoverable error")
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Log.e(TAG, "CameraState: Camera disabled")
                    }

                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Log.e(TAG, "CameraState: Fatal error")
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Log.e(TAG, "CameraState: Do not disturb mode enabled")
                    }
                }
            }
        }
    }
}