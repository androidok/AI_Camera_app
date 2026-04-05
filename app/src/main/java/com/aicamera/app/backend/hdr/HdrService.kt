package com.aicamera.app.backend.hdr

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import com.aicamera.app.backend.hdr.capture.HdrCaptureController
import com.aicamera.app.backend.hdr.model.ImageFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HdrService(private val context: Context) {
    companion object {
        private const val TAG = "HdrService"
        
        @Volatile
        private var instance: HdrService? = null
        
        fun getInstance(context: Context): HdrService {
            return instance ?: synchronized(this) {
                instance ?: HdrService(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var captureController: HdrCaptureController? = null
    private var isInitialized = false
    
    data class HdrCaptureResult(
        val bitmap: Bitmap,
        val filePath: String,
        val processingTimeMs: Long,
        val frameCount: Int
    )
    
    fun initialize() {
        if (isInitialized) return
        
        captureController = HdrCaptureController(context)
        isInitialized = true
        Log.d(TAG, "HDR Service initialized")
    }
    
    suspend fun captureHdr(
        cameraId: String = "0",
        outputDir: File = context.getExternalFilesDir("HDR") ?: context.filesDir
    ): HdrCaptureResult = withContext(Dispatchers.Main) {
        if (!isInitialized) {
            initialize()
        }
        
        val controller = captureController ?: throw IllegalStateException("HDR controller not initialized")
        
        val frames = captureBurst(controller, cameraId)
        
        val startTime = System.currentTimeMillis()
        
        val characteristics = getCameraCharacteristics(cameraId)
        
        val result = HdrProcessor.processHdrBurst(
            context,
            frames,
            characteristics,
            null,
            outputDir
        ).getOrThrow()
        
        val processingTime = System.currentTimeMillis() - startTime
        
        Log.d(TAG, "HDR capture completed: ${frames.size} frames, ${processingTime}ms processing")
        
        HdrCaptureResult(
            bitmap = result.bitmap,
            filePath = result.filePath,
            processingTimeMs = processingTime,
            frameCount = frames.size
        )
    }
    
    private suspend fun captureBurst(
        controller: HdrCaptureController,
        cameraId: String
    ): List<ImageFrame> = suspendCancellableCoroutine { continuation ->
        controller.openCamera(cameraId, object : HdrCaptureController.HdrCaptureCallback {
            override fun onCaptureStarted() {
                Log.d(TAG, "HDR capture started")
                controller.captureHdrBurst()
            }
            
            override fun onFrameCaptured(frameIndex: Int, totalFrames: Int) {
                Log.d(TAG, "Frame captured: $frameIndex / $totalFrames")
            }
            
            override fun onCaptureCompleted(frames: List<ImageFrame>) {
                Log.d(TAG, "Burst capture completed: ${frames.size} frames")
                continuation.resume(frames)
            }
            
            override fun onCaptureFailed(reason: String) {
                Log.e(TAG, "HDR capture failed: $reason")
                continuation.resumeWithException(Exception(reason))
            }
        })
        
        continuation.invokeOnCancellation {
            controller.close()
        }
    }
    
    private fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        return manager.getCameraCharacteristics(cameraId)
    }
    
    fun release() {
        captureController?.close()
        captureController = null
        isInitialized = false
        Log.d(TAG, "HDR Service released")
    }
    
    fun isCapturing(): Boolean {
        return captureController?.isCapturing() ?: false
    }
    
    fun getProgress(): Pair<Int, Int> {
        val controller = captureController ?: return Pair(0, 0)
        return Pair(controller.getCapturedFrameCount(), controller.getExpectedFrameCount())
    }
}
