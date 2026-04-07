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
            
            // 详细日志：输出所有支持的分辨率
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            Log.d(TAG, "📷 相机硬件能力分析 [${if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置" else "前置"}摄像头]")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            Log.d(TAG, "相机ID: $cameraId")
            Log.d(TAG, "最高分辨率: ${maxSize.width}×${maxSize.height} = ${(maxSize.width * maxSize.height / 1000000.0)}MP")
            Log.d(TAG, "支持4K: $is4K, 支持高分辨率: $isHighRes")
            Log.d(TAG, "支持的分辨率列表 (按像素排序):")
            sortedSizes.take(10).forEachIndexed { index, size ->
                val mp = size.width * size.height / 1000000.0
                val ratio = String.format("%.2f", size.width.toDouble() / size.height.toDouble())
                Log.d(TAG, "  [$index] ${size.width}×${size.height} = ${String.format("%.1f", mp)}MP, 比例=$ratio")
            }
            if (sortedSizes.size > 10) {
                Log.d(TAG, "  ... 还有 ${sortedSizes.size - 10} 个分辨率")
            }
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            
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

        Log.d(TAG, "════════════════════════════════════════════════════════════")
        Log.d(TAG, "📸 拍照配置分析")
        Log.d(TAG, "════════════════════════════════════════════════════════════")
        Log.d(TAG, "摄像头: ${if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置" else "前置"}")
        Log.d(TAG, "相机最高分辨率: ${capabilities?.maxResolution?.width}x${capabilities?.maxResolution?.height}")
        Log.d(TAG, "显示旋转角度: $displayRotation")

        return ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            setJpegQuality(100)
            setTargetRotation(displayRotation)

            capabilities?.maxResolution?.let { maxSize ->
                setTargetResolution(maxSize)
                Log.d(TAG, "✅ 设置目标分辨率: ${maxSize.width}x${maxSize.height}")
                Log.d(TAG, "✅ 拍照模式: CAPTURE_MODE_MAXIMIZE_QUALITY")
                Log.d(TAG, "✅ JPEG质量: 100")
            }

            Log.d(TAG, "════════════════════════════════════════════════════════════")
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
