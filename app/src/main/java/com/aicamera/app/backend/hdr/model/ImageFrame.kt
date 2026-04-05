package com.aicamera.app.backend.hdr.model

import android.media.Image
import java.nio.ByteBuffer

data class ImageFrame(
    val buffer: ByteBuffer?,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val iso: Int = 100,
    val exposureTime: Long = 33_000_000L,
    val frameNumber: Long = 0
) {
    companion object {
        fun fromImage(image: Image, iso: Int = 100, exposureTime: Long = 33_000_000L): ImageFrame? {
            if (image.format != android.graphics.ImageFormat.RAW_SENSOR) {
                return null
            }
            
            val plane = image.planes[0]
            val buffer = plane.buffer
            
            val width = image.width
            val height = image.height
            
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            
            val outputBuffer = ByteBuffer.allocateDirect(width * height * 2)
            
            if (rowStride == width * pixelStride) {
                outputBuffer.put(buffer)
            } else {
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    val rowBuffer = ByteBuffer.allocate(width * 2)
                    for (col in 0 until width) {
                        rowBuffer.putShort(buffer.short)
                    }
                    rowBuffer.rewind()
                    outputBuffer.put(rowBuffer)
                }
            }
            
            outputBuffer.rewind()
            
            return ImageFrame(
                buffer = outputBuffer,
                width = width,
                height = height,
                timestamp = image.timestamp,
                iso = iso,
                exposureTime = exposureTime
            )
        }
    }
    
    fun close() {
        buffer?.clear()
    }
}
