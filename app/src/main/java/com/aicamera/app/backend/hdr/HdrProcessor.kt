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
    
    private fun mergeFrames(frames: List<ImageFrame>, parameters: ProcessingParameters): ByteBuffer {
        if (frames.size == 1) {
            return frames[0].buffer!!
        }
        
        val width = frames[0].width
        val height = frames[0].height
        
        val outputBuffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())
        
        val firstFrame = frames[0]
        val baseBuffer = firstFrame.buffer!!
        baseBuffer.rewind()
        
        val tempBuffer = ByteBuffer.allocateDirect(width * height * 2)
            .order(ByteOrder.nativeOrder())
        
        while (baseBuffer.hasRemaining()) {
            tempBuffer.putShort(baseBuffer.short)
        }
        tempBuffer.rewind()
        
        val frameCount = frames.size.toFloat()
        for (i in 1 until frames.size) {
            val frameBuffer = frames[i].buffer!!
            frameBuffer.rewind()
            
            tempBuffer.rewind()
            while (frameBuffer.hasRemaining() && tempBuffer.hasRemaining()) {
                val currentPos = tempBuffer.position()
                val existing = tempBuffer.short.toInt() and 0xFFFF
                val incoming = frameBuffer.short.toInt() and 0xFFFF
                
                val blended = ((existing * i + incoming) / (i + 1)).toInt()
                
                tempBuffer.position(currentPos)
                tempBuffer.putShort(blended.toShort())
            }
        }
        
        tempBuffer.rewind()
        outputBuffer.rewind()
        
        val blackLevel = parameters.blackLevel[0]
        val whiteLevel = parameters.whiteLevel.toFloat()
        
        while (tempBuffer.hasRemaining() && outputBuffer.hasRemaining()) {
            val rawValue = tempBuffer.short.toInt() and 0xFFFF
            val normalized = ((rawValue - blackLevel) / (whiteLevel - blackLevel)).coerceIn(0f, 1f)
            val outputValue = (normalized * 65535).toInt().toShort()
            outputBuffer.putShort(outputValue)
            outputBuffer.putShort(outputValue)
        }
        
        outputBuffer.rewind()
        return outputBuffer
    }
    
    private fun saveBitmap(bitmap: Bitmap, file: java.io.File) {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }
    
    fun isProcessing(): Boolean = isProcessing
}
