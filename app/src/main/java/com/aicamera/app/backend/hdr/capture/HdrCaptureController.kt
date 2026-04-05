package com.aicamera.app.backend.hdr.capture

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.CaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Pair
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.aicamera.app.backend.hdr.model.ImageFrame
import java.util.ArrayList
import java.util.Collections

class HdrCaptureController(private val context: Context) {
    companion object {
        private const val TAG = "HdrCaptureController"
        private const val FRAME_COUNT = 8
    }
    
    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private var characteristics: CameraCharacteristics? = null
    private var currentCameraId: String = ""
    
    private val capturedFrames = Collections.synchronizedList(mutableListOf<ImageFrame>())
    private val captureResults = ArrayList<TotalCaptureResult>()
    private val captureRequests = ArrayList<CaptureRequest>()
    
    private var burstCallback: HdrCaptureCallback? = null
    private var isCapturing = false
    private var expectedFrameCount = FRAME_COUNT
    private var capturedFrameCount = 0
    
    private val exposurePairs: List<Pair<Int, Long>> = createExposurePairs()
    
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireNextImage() ?: return@OnImageAvailableListener
        
        try {
            val sensitivity = captureResults.getOrNull(capturedFrameCount - 1)
                ?.get(CaptureResult.SENSOR_SENSITIVITY) ?: 100
            val exposureTime = captureResults.getOrNull(capturedFrameCount - 1)
                ?.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 33_000_000L
            
            val frame = ImageFrame.fromImage(image, sensitivity, exposureTime)
            if (frame != null) {
                capturedFrames.add(frame)
                Log.d(TAG, "Frame captured: ${capturedFrames.size}, ISO: $sensitivity, Exposure: ${exposureTime}ns")
            }
            
            image.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing captured image", e)
            image.close()
        }
    }
    
    interface HdrCaptureCallback {
        fun onCaptureStarted()
        fun onFrameCaptured(frameIndex: Int, totalFrames: Int)
        fun onCaptureCompleted(frames: List<ImageFrame>)
        fun onCaptureFailed(reason: String)
    }
    
    fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("HdrCameraBackground").also { it.start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }
    
    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }
    
    @RequiresPermission(Manifest.permission.CAMERA)
    fun openCamera(cameraId: String, callback: HdrCaptureCallback) {
        this.burstCallback = callback
        this.currentCameraId = cameraId
        
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            startBackgroundThread()
            
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    Log.d(TAG, "Camera opened: $cameraId")
                    callback.onCaptureStarted()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    callback.onCaptureFailed("Camera disconnected")
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    callback.onCaptureFailed("Camera error: $error")
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
            callback.onCaptureFailed("Failed to open camera: ${e.message}")
        }
    }
    
    fun captureHdrBurst(surface: Surface? = null) {
        if (cameraDevice == null) {
            burstCallback?.onCaptureFailed("Camera not opened")
            return
        }
        
        if (isCapturing) {
            burstCallback?.onCaptureFailed("Already capturing")
            return
        }
        
        isCapturing = true
        capturedFrameCount = 0
        capturedFrames.clear()
        captureResults.clear()
        captureRequests.clear()
        
        try {
            val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR)
            
            val largestSize = sizes?.maxByOrNull { it.width * it.height }
                ?: sizes?.firstOrNull()
            
            if (largestSize == null) {
                burstCallback?.onCaptureFailed("No RAW sensor size available")
                isCapturing = false
                return
            }
            
            imageReader = ImageReader.newInstance(
                largestSize.width,
                largestSize.height,
                ImageFormat.RAW_SENSOR,
                expectedFrameCount + 2
            )
            imageReader?.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            
            val surfaces = ArrayList<Surface>()
            imageReader?.surface?.let { surfaces.add(it) }
            surface?.let { surfaces.add(it) }
            
            cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    setupCaptureRequest()
                }
                
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    burstCallback?.onCaptureFailed("Failed to configure capture session")
                    isCapturing = false
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup HDR capture", e)
            burstCallback?.onCaptureFailed("Failed to setup capture: ${e.message}")
            isCapturing = false
        }
    }
    
    private fun setupCaptureRequest() {
        try {
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                ?: run {
                    burstCallback?.onCaptureFailed("Failed to create capture request")
                    isCapturing = false
                    return
                }
            
            imageReader?.surface?.let { captureBuilder.addTarget(it) }
            
            for (i in 0 until expectedFrameCount) {
                val exposurePair = exposurePairs[i % exposurePairs.size]
                captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, exposurePair.first)
                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposurePair.second)
                captureRequests.add(captureBuilder.build())
            }
            
            captureSession?.captureBurst(captureRequests, hdrCaptureCallback, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup capture request", e)
            burstCallback?.onCaptureFailed(e.message ?: "Unknown error")
            isCapturing = false
        }
    }
    
    private fun createExposurePairs(): List<Pair<Int, Long>> {
        val pairs = mutableListOf<Pair<Int, Long>>()
        
        val baseExposure = 33_000_000L
        val baseISO = 100
        
        pairs.add(Pair(baseISO, baseExposure))
        pairs.add(Pair(baseISO, (baseExposure * 1.5).toLong()))
        pairs.add(Pair(baseISO, (baseExposure * 0.75).toLong()))
        pairs.add(Pair(baseISO * 2, (baseExposure * 0.5).toLong()))
        pairs.add(Pair(baseISO, baseExposure))
        pairs.add(Pair(baseISO, (baseExposure * 1.5).toLong()))
        pairs.add(Pair(baseISO, (baseExposure * 0.75).toLong()))
        pairs.add(Pair(baseISO * 2, (baseExposure * 0.5).toLong()))
        
        return pairs
    }
    
    private val hdrCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
            Log.d(TAG, "Capture started: frame $frameNumber")
        }
        
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            captureResults.add(result)
            capturedFrameCount++
            
            Log.d(TAG, "Capture completed: $capturedFrameCount / $expectedFrameCount")
            
            burstCallback?.onFrameCaptured(capturedFrameCount, expectedFrameCount)
            
            if (capturedFrameCount >= expectedFrameCount) {
                finishCapture()
            }
        }
        
        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            Log.e(TAG, "Capture failed: ${failure.reason}")
            burstCallback?.onCaptureFailed("Capture failed: ${failure.reason}")
        }
        
        override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
            Log.d(TAG, "Capture sequence completed: $frameNumber frames")
        }
    }
    
    private fun finishCapture() {
        isCapturing = false
        
        if (capturedFrames.isEmpty()) {
            burstCallback?.onCaptureFailed("No frames captured")
            return
        }
        
        burstCallback?.onCaptureCompleted(ArrayList(capturedFrames))
    }
    
    fun isCapturing(): Boolean = isCapturing
    
    fun getCapturedFrameCount(): Int = capturedFrameCount
    
    fun getExpectedFrameCount(): Int = expectedFrameCount
    
    fun close() {
        stopBackgroundThread()
        imageReader?.close()
        captureSession?.close()
        cameraDevice?.close()
        
        capturedFrames.forEach { it.close() }
        capturedFrames.clear()
        captureResults.clear()
        captureRequests.clear()
    }
}
