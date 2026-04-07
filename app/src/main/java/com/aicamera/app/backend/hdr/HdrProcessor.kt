package com.aicamera.app.backend.hdr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.util.Log
import com.aicamera.app.backend.hdr.gl.PostPipeline
import com.aicamera.app.backend.hdr.model.ImageFrame
import com.aicamera.app.backend.hdr.model.ProcessingParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object HdrProcessor {
    private const val TAG = "HdrProcessor"
    
    private var isProcessing = false
    
    data class HdrResult(
        val bitmap: Bitmap,
        val filePath: String,
        val processingTimeMs: Long
    )
    
    suspend fun processHdrBurst(
        context: Context,
        frames: List<ImageFrame>,
        characteristics: CameraCharacteristics,
        captureResult: CaptureResult?,
        outputDir: java.io.File
    ): Result<HdrResult> = withContext(Dispatchers.Default) {
        if (isProcessing) {
            return@withContext Result.failure(IllegalStateException("Already processing"))
        }
        
        isProcessing = true
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "Starting HDR processing with ${frames.size} frames")
            
            if (frames.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No frames to process"))
            }
            
            val firstFrame = frames[0]
            val width = firstFrame.width
            val height = firstFrame.height
            
            Log.d(TAG, "Image dimensions: ${width}x${height}")
            
            val parameters = ProcessingParameters().apply {
                FillFromCharacteristics(characteristics, Point(width, height))
                FillFromCaptureResult(captureResult)
            }
            
            val mergedBuffer = mergeFrames(frames, parameters)
            
            val pipeline = PostPipeline(context)
            val bitmap = pipeline.process(mergedBuffer, parameters, width, height)
            
            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "HDR processing completed in ${processingTime}ms")
            
            val outputFile = java.io.File(outputDir, "HDR_${System.currentTimeMillis()}.jpg")
            saveBitmap(bitmap, outputFile)
            
            Result.success(HdrResult(bitmap, outputFile.absolutePath, processingTime))
            
        } catch (e: Exception) {
            Log.e(TAG, "HDR processing failed", e)
            Result.failure(e)
        } finally {
            isProcessing = false
        }
    }
    
    /**
     * 改进的多帧合成算法 - 使用加权平均增强细节保留
     * 对中心帧（通常是正常曝光帧）赋予更高权重以提升清晰度
     */
    private fun mergeFrames(frames: List<ImageFrame>, parameters: ProcessingParameters): ByteBuffer {
        if (frames.size == 1) {
            return frames[0].buffer!!
        }

        val width = frames[0].width
        val height = frames[0].height

        val outputBuffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())

        // 找到最佳参考帧（通常是曝光时间最接近期望值的帧）
        // 给予中心帧更高的权重以保持细节清晰度
        val centerIndex = frames.size / 2
        val frameWeights = calculateFrameWeights(frames.size, centerIndex)

        // 累加加权的像素值
        val accumulatedPixels = FloatArray(width * height)
        val totalWeight = FloatArray(width * height) { 0f }

        for (frameIndex in frames.indices) {
            val frame = frames[frameIndex]
            val buffer = frame.buffer!!
            buffer.rewind()

            val weight = frameWeights[frameIndex]
            var pixelIndex = 0

            while (buffer.hasRemaining() && pixelIndex < width * height) {
                val rawValue = (buffer.short.toInt() and 0xFFFF).toFloat()

                // 使用基于像素值的自适应权重
                // 避免过曝和欠曝像素的影响
                val pixelWeight = weight * calculatePixelConfidence(rawValue, parameters)

                accumulatedPixels[pixelIndex] += rawValue * pixelWeight
                totalWeight[pixelIndex] += pixelWeight

                pixelIndex++
            }
        }

        // 归一化并写入输出缓冲区
        outputBuffer.rewind()
        val blackLevel = parameters.blackLevel[0]
        val whiteLevel = parameters.whiteLevel.toFloat()

        for (i in 0 until width * height) {
            val weightedValue = if (totalWeight[i] > 0) {
                accumulatedPixels[i] / totalWeight[i]
            } else {
                accumulatedPixels[i]
            }

            val rawValue = weightedValue.toInt().coerceIn(0, 65535)
            val normalized = ((rawValue - blackLevel) / (whiteLevel - blackLevel)).coerceIn(0f, 1f)
            val outputValue = (normalized * 65535).toInt().toShort()
            outputBuffer.putShort(outputValue)
            outputBuffer.putShort(outputValue)
        }

        outputBuffer.rewind()
        return outputBuffer
    }

    /**
     * 计算每帧的权重 - 中心帧权重更高以提升清晰度
     */
    private fun calculateFrameWeights(frameCount: Int, centerIndex: Int): FloatArray {
        val weights = FloatArray(frameCount)
        val centerWeight = 0.25f  // 中心帧基础权重
        val otherWeight = (1f - centerWeight) / (frameCount - 1)

        for (i in 0 until frameCount) {
            weights[i] = if (i == centerIndex) centerWeight else otherWeight
        }

        return weights
    }

    /**
     * 计算像素置信度 - 避免过曝和欠曝区域
     */
    private fun calculatePixelConfidence(rawValue: Float, parameters: ProcessingParameters): Float {
        val whiteLevel = parameters.whiteLevel.toFloat()
        val blackLevel = parameters.blackLevel[0]

        // 过曝检测（接近白电平的值）
        val overexposedThreshold = whiteLevel * 0.95f
        // 欠曝检测（接近黑电平的值）
        val underexposedThreshold = blackLevel + (whiteLevel - blackLevel) * 0.05f

        return when {
            rawValue > overexposedThreshold -> 0.1f  // 过曝像素权重低
            rawValue < underexposedThreshold -> 0.3f  // 欠曝像素权重较低
            else -> 1.0f  // 正常曝光像素权重正常
        }
    }
    
    private fun saveBitmap(bitmap: Bitmap, file: java.io.File) {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }
    
    fun isProcessing(): Boolean = isProcessing
}
