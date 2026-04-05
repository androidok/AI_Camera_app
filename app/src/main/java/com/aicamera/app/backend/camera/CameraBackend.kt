@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)

package com.aicamera.app.backend.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aicamera.app.backend.hdr.HdrService
import com.aicamera.app.backend.models.CameraParams
import com.aicamera.app.backend.models.FlashMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max

object CameraBackend {
    private const val TAG = "CameraBackend"
    private var hdrService: HdrService? = null
    
    private val settingsListeners = mutableListOf<() -> Unit>()
    
    fun addSettingsListener(listener: () -> Unit) {
        settingsListeners.add(listener)
    }
    
    fun removeSettingsListener(listener: () -> Unit) {
        settingsListeners.remove(listener)
    }
    
    private fun notifySettingsChanged() {
        settingsListeners.forEach { it.invoke() }
    }
    
    object ManualSettings {
        var iso: Int? = null
            set(value) {
                field = value
                incrementVersion()
                notifyChanged()
            }
        var exposureTimeNs: Long? = null
            set(value) {
                field = value
                incrementVersion()
                notifyChanged()
            }
        var evIndex: Int? = null
            set(value) {
                field = value
                incrementVersion()
                notifyChanged()
            }
        var hdrEnabled: Boolean = false
            set(value) {
                field = value
                incrementVersion()
                notifyChanged()
            }
        var previewAspectRatioPortrait: Float = 0.75f
            set(value) {
                field = value
                incrementVersion()
                notifyChanged()
            }
        private var version: Int = 0

        fun getVersion(): Int = version

        private fun incrementVersion() {
            version++
        }
        
        private fun notifyChanged() {
            CameraBackend.notifySettingsChanged()
        }
        
        fun reset() {
            iso = null
            exposureTimeNs = null
            evIndex = null
            hdrEnabled = false
            previewAspectRatioPortrait = 0.75f
        }
    }

    fun capturePhoto(
        context: Context,
        imageCapture: ImageCapture?,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (imageCapture == null) {
            onError("相机未初始化")
            return
        }
        
        // Note: HDR is now handled by CameraX Extensions in CameraScreen.
        // When hdrEnabled is true, CameraX uses ExtensionMode.HDR which handles
        // both preview and capture automatically. We should NOT call custom HDR
        // capture here as it would cause camera resource conflict and crash.
        // The custom HdrService is kept for future advanced HDR features.

        val timestamp = System.currentTimeMillis()
        val fileName = "IMG_${timestamp}.jpg"

        val parentDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            ?: context.cacheDir
        val photoFile = File(parentDir, fileName)

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "capturePhoto ok: ${photoFile.absolutePath}")

                    val aspectRatio = ManualSettings.previewAspectRatioPortrait
                    var finalFile = if (aspectRatio > 0f && aspectRatio != 0.75f) {
                        cropToPreviewAspect(photoFile, aspectRatio)
                    } else if (aspectRatio < 0f) {
                        cropToPreviewAspect(photoFile, aspectRatio)
                    } else {
                        photoFile
                    }

                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        finalFile = mirrorImageHorizontally(finalFile)
                    }

                    notifyMediaScanner(context, finalFile)

                    onSuccess(finalFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "capturePhoto failed", exception)
                    onError("拍照失败：${exception.message ?: "unknown"}")
                }
            }
        )
    }
    
    fun captureHdrPhoto(
        context: Context,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "Starting HDR capture")
        
        if (hdrService == null) {
            hdrService = HdrService.getInstance(context)
        }
        
        val service = hdrService!!
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cameraId = if (lensFacing == CameraSelector.LENS_FACING_BACK) "0" else "1"
                
                val result = service.captureHdr(cameraId)
                
                Log.d(TAG, "HDR capture completed: ${result.filePath}, ${result.processingTimeMs}ms, ${result.frameCount} frames")
                
                val aspectRatio = ManualSettings.previewAspectRatioPortrait
                var finalFile = File(result.filePath)
                
                if (aspectRatio > 0f) {
                    finalFile = cropImageToAspectRatio(finalFile, aspectRatio)
                }
                
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    finalFile = mirrorImageHorizontally(finalFile)
                }
                
                notifyMediaScanner(context, finalFile)
                
                onSuccess(finalFile.absolutePath)
                
            } catch (e: Exception) {
                Log.e(TAG, "HDR capture failed", e)
                withContext(Dispatchers.Main) {
                    onError("HDR 拍照失败：${e.message ?: "unknown"}")
                }
            }
        }
    }
    
    fun isHdrCapturing(): Boolean {
        return hdrService?.isCapturing() ?: false
    }
    
    fun getHdrProgress(): Pair<Int, Int> {
        return hdrService?.getProgress() ?: Pair(0, 0)
    }
    
    fun initHdrService(context: Context) {
        if (hdrService == null) {
            hdrService = HdrService.getInstance(context)
        }
    }
    
    fun releaseHdrService() {
        hdrService?.release()
        hdrService = null
    }

    private fun cropToPreviewAspect(photoFile: File, aspectRatio: Float): File {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return photoFile
            val width = bitmap.width
            val height = bitmap.height

            val cameraRatio = 0.75f

            val targetRatio = if (aspectRatio < 0f) {
                9f / 19.5f
            } else {
                aspectRatio
            }

            val targetWidth: Int
            var targetHeight = height

            if (kotlin.math.abs(targetRatio - cameraRatio) < 0.001f) {
                targetWidth = width
                targetHeight = height
            } else if (targetRatio > cameraRatio) {
                val squareSize = minOf(width, height)
                targetWidth = squareSize
                targetHeight = squareSize
            } else {
                val cropRatio = targetRatio / cameraRatio
                targetWidth = (width * cropRatio).toInt()
                targetHeight = height
            }

            val x = (width - targetWidth) / 2
            val y = (height - targetHeight) / 2

            val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, targetWidth, targetHeight)

            FileOutputStream(photoFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            if (croppedBitmap != bitmap) {
                bitmap.recycle()
                croppedBitmap.recycle()
            }

            Log.d(TAG, "Image cropped to ${targetWidth}x${targetHeight}, originalRatio=$aspectRatio, targetRatio=$targetRatio")
            return photoFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop image to preview aspect", e)
            return photoFile
        }
    }

    private fun mirrorImageHorizontally(photoFile: File): File {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return photoFile
            val width = bitmap.width
            val height = bitmap.height

            val matrix = android.graphics.Matrix().apply {
                preScale(-1f, 1f, width / 2f, height / 2f)
            }

            val mirroredBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)

            FileOutputStream(photoFile).use { out ->
                mirroredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            if (mirroredBitmap != bitmap) {
                bitmap.recycle()
                mirroredBitmap.recycle()
            } else {
                bitmap.recycle()
            }

            Log.d(TAG, "Image mirrored horizontally for front camera")
            return photoFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to mirror image", e)
            return photoFile
        }
    }

    private fun cropImageToAspectRatio(photoFile: File, aspectRatio: Float): File {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return photoFile
            val width = bitmap.width
            val height = bitmap.height

            val targetWidth: Int
            val targetHeight: Int

            val isPortrait = height > width

            if (isPortrait) {
                val currentRatio = width.toFloat() / height
                if (currentRatio > aspectRatio) {
                    targetHeight = height
                    targetWidth = (height * aspectRatio).toInt()
                } else {
                    targetWidth = width
                    targetHeight = (width / aspectRatio).toInt()
                }
            } else {
                val landscapeRatio = 1f / aspectRatio
                val currentRatio = height.toFloat() / width
                if (currentRatio > landscapeRatio) {
                    targetWidth = width
                    targetHeight = (width * landscapeRatio).toInt()
                } else {
                    targetHeight = height
                    targetWidth = (height / landscapeRatio).toInt()
                }
            }

            val x = (width - targetWidth) / 2
            val y = (height - targetHeight) / 2

            val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, targetWidth, targetHeight)

            FileOutputStream(photoFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            if (croppedBitmap != bitmap) {
                bitmap.recycle()
                croppedBitmap.recycle()
            } else {
                bitmap.recycle()
            }

            Log.d(TAG, "Image cropped to ${targetWidth}x${targetHeight}, ratio=${aspectRatio}")
            return photoFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop image", e)
            return photoFile
        }
    }
    
    private fun notifyMediaScanner(context: Context, photoFile: File) {
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(photoFile)
            mediaScanIntent.setData(contentUri)
            context.sendBroadcast(mediaScanIntent)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                try {
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                } catch (e: Exception) {
                }
            }
            
            Log.d(TAG, "Media scanner notified: ${photoFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify media scanner", e)
        }
    }

    fun setFlashMode(imageCapture: ImageCapture?, mode: FlashMode) {
        if (imageCapture == null) return
        imageCapture.flashMode = when (mode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    fun switchCamera(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        preview: Preview,
        imageCapture: ImageCapture,
        currentFacing: Int
    ): Int {
        val newFacing = if (currentFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        val selector = CameraSelector.Builder()
            .requireLensFacing(newFacing)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        return newFacing
    }

    fun getCameraParams(camera: Camera?): CameraParams {
        if (camera == null) return CameraParams("Auto", "Auto", "Auto")

        val info = camera.cameraInfo

        val isoStr = if (ManualSettings.iso != null) {
            ManualSettings.iso.toString()
        } else {
            "Auto"
        }

        val shutterStr = if (ManualSettings.exposureTimeNs != null) {
            val exposureNs = ManualSettings.exposureTimeNs!!
            if (exposureNs == 0L) {
                "Auto"
            } else if (exposureNs >= 1_000_000_000L) {
                val seconds = exposureNs.toDouble() / 1_000_000_000.0
                if (seconds >= 1.0) {
                    "${seconds.toInt()}s"
                } else {
                    val denom = (1.0 / seconds).toInt()
                    if (denom == 0) "Auto" else "1/${denom}s"
                }
            } else {
                val denom = 1_000_000_000L / exposureNs
                if (denom == 0L) "Auto" else "1/${denom}s"
            }
        } else {
            val exposureIndex = info.exposureState.exposureCompensationIndex
            exposureIndexToShutterSpeed(exposureIndex)
        }

        val apertureStr = if (ManualSettings.evIndex != null) {
            "EV ${ManualSettings.evIndex}"
        } else {
            "Auto"
        }

        return CameraParams(iso = isoStr, shutter = shutterStr, aperture = apertureStr)
    }

    private fun exposureIndexToShutterSpeed(exposureIndex: Int?): String {
        if (exposureIndex == null) return "Auto"
        val base = 1.0 / 125.0
        val factor = Math.pow(2.0, exposureIndex.toDouble())
        val seconds = base * factor
        val denom = max(1, (1.0 / seconds).toInt())
        return "1/${denom}s"
    }

    private fun trimOneDecimal(v: Float): String {
        val scaled = (v * 10f).toInt() / 10f
        return if (abs(scaled - scaled.toInt()) < 1e-6) scaled.toInt().toString() else scaled.toString()
    }
    
    fun getCurrentSettingsSnapshot(): SettingsSnapshot {
        return SettingsSnapshot(
            iso = ManualSettings.iso,
            exposureTimeNs = ManualSettings.exposureTimeNs,
            evIndex = ManualSettings.evIndex,
            hdrEnabled = ManualSettings.hdrEnabled,
            previewAspectRatioPortrait = ManualSettings.previewAspectRatioPortrait
        )
    }
    
    data class SettingsSnapshot(
        val iso: Int?,
        val exposureTimeNs: Long?,
        val evIndex: Int?,
        val hdrEnabled: Boolean,
        val previewAspectRatioPortrait: Float
    )
}
