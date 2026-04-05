package com.aicamera.app.backend.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

object StorageBackend {
    fun saveToGallery(
        context: Context,
        imageUri: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val inputFile = File(imageUri)
        if (!inputFile.exists()) {
            onError("文件不存在")
            return
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            onError("无法创建相册条目")
            return
        }

        try {
            resolver.openOutputStream(uri).use { out ->
                if (out == null) throw IllegalStateException("无法打开输出流")
                FileInputStream(inputFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            onSuccess()
        } catch (e: Throwable) {
            Log.e("StorageBackend", "saveToGallery failed", e)
            try {
                resolver.delete(uri, null, null)
            } catch (_: Throwable) {
            }
            onError("保存失败: ${e.message ?: "unknown"}")
        }
    }

    fun clearCache(context: Context): Long {
        var total = 0L
        total += deleteDirChildren(context.cacheDir)
        val ext = context.externalCacheDir
        if (ext != null) total += deleteDirChildren(ext)
        return total
    }

    fun getCacheSize(context: Context): Long {
        var total = 0L
        total += dirSize(context.cacheDir)
        val ext = context.externalCacheDir
        if (ext != null) total += dirSize(ext)
        return total
    }

    private fun deleteDirChildren(dir: File): Long {
        var total = 0L
        try {
            if (!dir.exists() || !dir.isDirectory) return 0L
            dir.listFiles()?.forEach { f ->
                total += if (f.isDirectory) deleteDirRecursive(f) else f.length().also { f.delete() }
            }
        } catch (e: Throwable) {
            Log.e("StorageBackend", "deleteDirChildren failed", e)
        }
        return total
    }

    private fun deleteDirRecursive(dir: File): Long {
        var total = 0L
        dir.listFiles()?.forEach { f ->
            total += if (f.isDirectory) deleteDirRecursive(f) else f.length().also { f.delete() }
        }
        dir.delete()
        return total
    }

    private fun dirSize(dir: File): Long {
        var total = 0L
        try {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            dir.listFiles()?.forEach { f -> total += dirSize(f) }
        } catch (_: Throwable) {
        }
        return total
    }
}

