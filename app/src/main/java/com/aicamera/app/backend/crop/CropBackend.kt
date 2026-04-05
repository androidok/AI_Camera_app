package com.aicamera.app.backend.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import com.aicamera.app.backend.models.CropMode
import com.aicamera.app.backend.models.CropRect
import com.aicamera.app.backend.models.SmartCropResult
import com.aicamera.app.backend.models.SubjectType
import com.aicamera.app.backend.storage.ExifUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object CropBackend {
    suspend fun analyzeSmartCrop(
        imageUri: String,
        cropMode: CropMode = CropMode.AUTO
    ): SmartCropResult = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(imageUri)
            ?: return@withContext defaultResult("无法读取图片", cropMode)

        val image = InputImage.fromBitmap(bitmap, 0)
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = ObjectDetection.getClient(options)

        try {
            val objects = detector.process(image).await()
            if (objects.isEmpty()) {
                return@withContext defaultResult("未检测到主体，已给出默认裁剪框", cropMode)
            }

            val main = objects.maxBy { it.boundingBox.width() * it.boundingBox.height() }
            val rect = main.boundingBox

            val padded = padRect(rect, bitmap.width, bitmap.height, paddingRatio = 0.10f)
            val cropRect = CropRect(
                left = padded.left.toFloat() / bitmap.width,
                top = padded.top.toFloat() / bitmap.height,
                width = padded.width().toFloat() / bitmap.width,
                height = padded.height().toFloat() / bitmap.height
            )

            val subjects = inferSubjects(main)
            val suggestion = when {
                subjects.contains(SubjectType.FACE) -> "✨ AI 建议：检测到人像主体，已优化构图"
                subjects.contains(SubjectType.OBJECT) -> "✨ AI 建议：检测到主体，已优化裁剪"
                else -> "✨ AI 建议：已优化构图"
            }

            SmartCropResult(
                success = true,
                cropRect = clampCropRect(cropRect),
                confidence = 0.90f,
                suggestion = suggestion,
                detectedSubjects = subjects,
                aspectRatio = aspectRatioFor(cropMode)
            )
        } catch (e: Throwable) {
            Log.e("CropBackend", "analyzeSmartCrop failed", e)
            defaultResult("AI 分析失败，请手动调整", cropMode)
        }
    }

    suspend fun cropImage(
        imageUri: String,
        cropRect: CropRect,
        outputQuality: Int = 95
    ): String = withContext(Dispatchers.Default) {
        val original = BitmapFactory.decodeFile(imageUri)
            ?: throw IllegalArgumentException("无法读取图片: $imageUri")

        val safe = clampCropRect(cropRect)

        val left = (safe.left * original.width).toInt().coerceIn(0, original.width - 1)
        val top = (safe.top * original.height).toInt().coerceIn(0, original.height - 1)
        val width = (safe.width * original.width).toInt().coerceIn(1, original.width - left)
        val height = (safe.height * original.height).toInt().coerceIn(1, original.height - top)

        val cropped = Bitmap.createBitmap(original, left, top, width, height)

        val parent = File(imageUri).parentFile ?: File(imageUri).absoluteFile.parentFile ?: File(".")
        val out = File(parent, "cropped_${System.currentTimeMillis()}.jpg")

        FileOutputStream(out).use { fos ->
            cropped.compress(Bitmap.CompressFormat.JPEG, outputQuality.coerceIn(50, 100), fos)
        }
        ExifUtils.copyExif(imageUri, out.absolutePath)
        out.absolutePath
    }

    private fun defaultResult(message: String, cropMode: CropMode): SmartCropResult {
        return SmartCropResult(
            success = false,
            cropRect = CropRect(0.1f, 0.2f, 0.8f, 0.6f),
            confidence = 0f,
            suggestion = message,
            detectedSubjects = emptyList(),
            aspectRatio = aspectRatioFor(cropMode)
        )
    }

    private fun aspectRatioFor(mode: CropMode): String = when (mode) {
        CropMode.SQUARE -> "1:1"
        CropMode.PORTRAIT -> "3:4"
        CropMode.LANDSCAPE -> "4:3"
        CropMode.AUTO -> "4:3"
    }

    private fun inferSubjects(obj: DetectedObject): List<SubjectType> {
        val labels = obj.labels.map { it.text.lowercase() }
        val subjects = mutableSetOf<SubjectType>()
        labels.forEach { t ->
            when {
                "person" in t || "face" in t -> subjects.add(SubjectType.FACE)
                "text" in t -> subjects.add(SubjectType.TEXT)
                else -> subjects.add(SubjectType.OBJECT)
            }
        }
        if (subjects.isEmpty()) subjects.add(SubjectType.UNKNOWN)
        return subjects.toList()
    }

    private fun padRect(rect: Rect, w: Int, h: Int, paddingRatio: Float): Rect {
        val padX = (rect.width() * paddingRatio).toInt()
        val padY = (rect.height() * paddingRatio).toInt()
        val left = (rect.left - padX).coerceIn(0, w - 1)
        val top = (rect.top - padY).coerceIn(0, h - 1)
        val right = (rect.right + padX).coerceIn(left + 1, w)
        val bottom = (rect.bottom + padY).coerceIn(top + 1, h)
        return Rect(left, top, right, bottom)
    }

    private fun clampCropRect(r: CropRect): CropRect {
        val left = r.left.coerceIn(0f, 1f)
        val top = r.top.coerceIn(0f, 1f)
        val width = r.width.coerceIn(0f, 1f - left)
        val height = r.height.coerceIn(0f, 1f - top)
        val minW = max(width, 0.01f)
        val minH = max(height, 0.01f)
        return CropRect(left, top, minW, minH)
    }
}

