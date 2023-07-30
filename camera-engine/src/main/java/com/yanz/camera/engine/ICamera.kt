package com.yanz.camera.engine

import androidx.camera.core.ImageCapture

/**
 * @author yanz
 * @date 2023/6/6 16:05
 */
interface ICamera {

    fun initCamera(defaultConfig: MyCameraConfig)

    fun takePhoto(options: ImageCapture.OutputFileOptions)

    fun takePhoto()

    fun selectLens(lens: Int)

    fun switchLens()

    fun captureVideo()

    fun startFocus(x: Float, y: Float)

    fun setZoomRatio(ratio: Float)

    fun hasBackLens(): Boolean

    fun hasFrontLens(): Boolean
}

data class MyCameraConfig(
    // 镜头 前置 / 后置
    val lens: Int = LENS_BACK,
    val zoomRatio: Float = 1.0f,
) {
    companion object {
        const val LENS_FRONT = 0
        const val LENS_BACK = 1

        fun defaultConfig(): MyCameraConfig {
            return MyCameraConfig()
        }
    }
}

