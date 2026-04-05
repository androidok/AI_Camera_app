package com.aicamera.app.backend.camera

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import java.util.concurrent.atomic.AtomicReference

/**
 * Simple shared session so different screens (e.g. Settings) can control current camera.
 * This app is single-process and single-camera at a time.
 */
object CameraSession {
    private val cameraRef = AtomicReference<Camera?>(null)
    private val captureRef = AtomicReference<ImageCapture?>(null)

    fun set(camera: Camera?, imageCapture: ImageCapture?) {
        cameraRef.set(camera)
        captureRef.set(imageCapture)
    }

    fun camera(): Camera? = cameraRef.get()
    fun imageCapture(): ImageCapture? = captureRef.get()

    fun clear() {
        cameraRef.set(null)
        captureRef.set(null)
    }
}

