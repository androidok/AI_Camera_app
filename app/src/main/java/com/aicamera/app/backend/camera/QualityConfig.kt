package com.aicamera.app.backend.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

object QualityConfig {
    private const val TAG = "QualityConfig"
    
    enum class QualityLevel(val displayName: String, val minMegapixels: Int) {
        MAX("最高画质", 0),
        HIGH("高画质(12MP+)", 12),
        MEDIUM("中等画质(8MP+)", 8),
        STANDARD("标准画质(4MP+)", 4)
    }
    
    data class CameraCapabilities(
        val cameraId: String,
        val maxResolution: Size,
        val supportedResolutions: List<Size>,
        val is4KCapable: Boolean,
        val isHighResCapable: Boolean
    )
    
    var currentQualityLevel: QualityLevel = QualityLevel.MAX
        private set
    
    fun setQualityLevel(level: QualityLevel) {
        currentQualityLevel = level
        Log.d(TAG, "Quality level set to: ${level.displayName}")
    }
    
    fun getCameraCapabilities(context: Context, lensFacing: Int): CameraCapabilities? {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = findCameraId(manager, lensFacing) ?: return null
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return null
            
            val outputSizes = configMap.getOutputSizes(android.graphics.ImageFormat.JPEG)
                ?: return null
            
            val sortedSizes = outputSizes.sortedByDescending { it.width * it.height }
            val maxSize = sortedSizes.firstOrNull() ?: return null
            
            val is4K = maxSize.width >= 3840 && maxSize.height >= 2160
            val isHighRes = (maxSize.width * maxSize.height) >= 12_000_000
            
            CameraCapabilities(
                cameraId = cameraId,
                maxResolution = maxSize,
                supportedResolutions = sortedSizes.toList(),
                is4KCapable = is4K,
                isHighResCapable = isHighRes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera capabilities", e)
            null
        }
    }
    
    private fun findCameraId(manager: CameraManager, lensFacing: Int): String? {
        val facing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraCharacteristics.LENS_FACING_BACK
        } else {
            CameraCharacteristics.LENS_FACING_FRONT
        }
        
        for (id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(id)
            val lensFacingCharacteristic = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (lensFacingCharacteristic == facing) {
                return id
            }
        }
        return null
    }
    
    fun selectBestResolution(
        availableResolutions: List<Size>,
        qualityLevel: QualityLevel = currentQualityLevel
    ): Size? {
        if (availableResolutions.isEmpty()) return null
        
        val minPixels = qualityLevel.minMegapixels * 1_000_000
        
        val filteredResolutions = if (minPixels > 0) {
            availableResolutions.filter { (it.width * it.height) >= minPixels }
        } else {
            availableResolutions
        }
        
        return filteredResolutions.maxByOrNull { it.width * it.height }
            ?: availableResolutions.maxByOrNull { it.width * it.height }
    }
    
    fun createImageCaptureBuilder(
        context: Context,
        lensFacing: Int,
        displayRotation: Int
    ): ImageCapture.Builder {
        val capabilities = getCameraCapabilities(context, lensFacing)
        
        Log.d(TAG, "Creating ImageCapture for ${if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置" else "前置"}摄像头")
        Log.d(TAG, "相机最高分辨率: ${capabilities?.maxResolution?.width}x${capabilities?.maxResolution?.height}")
        
        return ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            setJpegQuality(100)
            setTargetRotation(displayRotation)
            
            Log.d(TAG, "Using CAPTURE_MODE_MAXIMIZE_QUALITY with JPEG quality 100")
        }
    }
    
    fun getResolutionDescription(size: Size?): String {
        if (size == null) return "未知"
        val megapixels = (size.width * size.height) / 1_000_000.0
        val mpString = String.format("%.1fMP", megapixels)
        
        val qualityLabel = when {
            size.width >= 4000 && size.height >= 3000 -> "超高清"
            size.width >= 3840 && size.height >= 2160 -> "4K"
            size.width >= 3000 && size.height >= 2000 -> "高清+"
            size.width >= 1920 && size.height >= 1080 -> "全高清"
            else -> "标准"
        }
        
        return "${size.width}×${size.height} ($mpString, $qualityLabel)"
    }
}
