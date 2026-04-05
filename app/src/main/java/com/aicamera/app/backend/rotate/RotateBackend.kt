package com.aicamera.app.backend.rotate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.aicamera.app.backend.storage.ExifUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object RotateBackend {
    /**
     * 旋转图片并保存为新文件
     * @param imageUri 原始图片路径
     * @param degrees 旋转角度（默认90度）
     * @return 旋转后图片的路径
     */
    suspend fun rotateImage(
        imageUri: String,
        degrees: Float = 90f
    ): String = withContext(Dispatchers.Default) {
        val original = BitmapFactory.decodeFile(imageUri)
            ?: throw IllegalArgumentException("无法读取图片: $imageUri")

        // 创建旋转矩阵
        val matrix = Matrix()
        matrix.postRotate(degrees)

        // 旋转图片
        val rotated = Bitmap.createBitmap(
            original, 0, 0, original.width, original.height, matrix, true
        )

        // 保存到新文件
        val parent = File(imageUri).parentFile ?: File(imageUri).absoluteFile.parentFile ?: File(".")
        val out = File(parent, "rotated_${System.currentTimeMillis()}.jpg")

        FileOutputStream(out).use { fos ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }

        // 复制EXIF信息
        ExifUtils.copyExif(imageUri, out.absolutePath)

        // 回收Bitmap资源
        original.recycle()
        rotated.recycle()

        out.absolutePath
    }

    /**
     * 生成旋转预览（用于UI实时预览）
     * @param bitmap 原始位图
     * @param degrees 旋转角度
     * @return 旋转后的位图
     */
    suspend fun generatePreview(
        bitmap: Bitmap,
        degrees: Float = 90f
    ): Bitmap = withContext(Dispatchers.Default) {
        val matrix = Matrix()
        matrix.postRotate(degrees)

        Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    /**
     * 从文件生成旋转预览
     * @param imageUri 图片路径
     * @param degrees 旋转角度
     * @return 旋转后的位图
     */
    suspend fun generatePreviewFromUri(
        imageUri: String,
        degrees: Float = 90f
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val bitmap = BitmapFactory.decodeFile(imageUri)
                ?: return@withContext null
            generatePreview(bitmap, degrees)
        } catch (e: Exception) {
            Log.e("RotateBackend", "generatePreviewFromUri failed", e)
            null
        }
    }
}