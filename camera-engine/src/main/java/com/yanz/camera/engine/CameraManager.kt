package com.yanz.camera.engine

import androidx.camera.core.ImageCapture.OutputFileOptions

/**
 * @author yanz
 * @date 2023/6/5 16:18
 */
class CameraManager : ICamera {

    companion object {
        private lateinit var camera: ICamera
        fun init(camera: ICamera): CameraManager {
            Companion.camera = camera
            return CameraManager()
        }
    }

    override fun initCamera(defaultConfig: MyCameraConfig) {
        camera.initCamera(defaultConfig)
    }

    override fun takePhoto(options: OutputFileOptions) {
        camera.takePhoto(options)
    }

    override fun takePhoto() {
        camera.takePhoto()
    }

    override fun selectLens(lens: Int) {
        camera.selectLens(lens)
    }

    override fun switchLens() {
        camera.switchLens()
    }

    override fun captureVideo() {
        camera.captureVideo()
    }

    override fun startFocus(x: Float, y: Float) {
        camera.startFocus(x, y)
    }

    override fun setZoomRatio(ratio: Float) {
        camera.setZoomRatio(ratio)
    }

    override fun hasBackLens(): Boolean {
        return camera.hasBackLens()
    }

    override fun hasFrontLens(): Boolean {
        return camera.hasFrontLens()
    }
}