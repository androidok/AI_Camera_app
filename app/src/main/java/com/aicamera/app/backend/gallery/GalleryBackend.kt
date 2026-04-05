package com.aicamera.app.backend.gallery

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 相册后端工具类
 */
object GalleryBackend {
    
    /**
     * 获取相册中最后一张照片的 URI
     */
    suspend fun getLastPhotoUri(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )
            
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val limit = 1
            
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "$sortOrder LIMIT $limit"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(MediaStore.Images.Media._ID)
                    val id = it.getLong(idIndex)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    Log.d("GalleryBackend", "找到最后一张照片：$uri")
                    return@withContext uri
                }
            }
            
            Log.d("GalleryBackend", "相册中没有照片")
            null
        } catch (e: Exception) {
            Log.e("GalleryBackend", "获取最后一张照片失败", e)
            null
        }
    }
    
    /**
     * 获取最后一张照片的缩略图
     */
    suspend fun getLastPhotoThumbnail(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = getLastPhotoUri(context)
            if (uri == null) {
                return@withContext null
            }
            
            // 加载缩略图（减小内存占用）
            val options = BitmapFactory.Options()
            options.inSampleSize = 4 // 加载 1/4 大小的图片
            options.inJustDecodeBounds = false
            
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            Log.d("GalleryBackend", "缩略图加载成功：${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Log.e("GalleryBackend", "加载缩略图失败", e)
            null
        }
    }
    
    /**
     * 打开系统相册并显示最新照片
     */
    fun openGallery(context: Context, photoUri: Uri? = null) {
        // 如果有指定的照片 URI，直接打开这张照片
        if (photoUri != null) {
            openPhotoDirectly(context, photoUri)
            return
        }
        
        // 没有指定照片时，先尝试获取最新照片
        runBlocking {
            val lastPhotoUri = getLastPhotoUri(context)
            if (lastPhotoUri != null) {
                openPhotoDirectly(context, lastPhotoUri)
            } else {
                // 如果相册中没有照片，打开相册应用
                openGalleryApp(context)
            }
        }
    }
    
    /**
     * 直接打开指定照片（使用内容 URI）
     */
    private fun openPhotoDirectly(context: Context, photoUri: Uri) {
        try {
            // 尝试使用系统图片查看器打开
            val intent = Intent(Intent.ACTION_VIEW).apply {
                // 使用 content:// URI 而不是 file:// URI
                val contentUri = if (photoUri.scheme == "file") {
                    // 如果是 file:// URI，尝试转换为 content:// URI
                    try {
                        val file = java.io.File(photoUri.path!!)
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        photoUri
                    }
                } else {
                    photoUri
                }
                
                setDataAndType(contentUri, "image/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // 强制使用图片查看器
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            // 检查是否有可以处理这个 Intent 的应用
            val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                context.startActivity(intent)
                Log.d("GalleryBackend", "已打开照片：$photoUri")
            } else {
                // 没有应用可以处理，打开相册应用
                openGalleryApp(context)
            }
        } catch (e: Exception) {
            Log.e("GalleryBackend", "打开照片失败", e)
            // 如果失败，尝试打开相册应用
            openGalleryApp(context)
        }
    }
    
    /**
     * 打开相册应用（不指定具体照片）
     */
    private fun openGalleryApp(context: Context) {
        try {
            // 方式 1: 使用 Google 相册的包名（如果已安装）
            val googlePhotosIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
            if (googlePhotosIntent != null) {
                googlePhotosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(googlePhotosIntent)
                Log.d("GalleryBackend", "已打开 Google 相册")
                return
            }
            
            // 方式 2: 使用系统图库
            val galleryIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_GALLERY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(galleryIntent)
            Log.d("GalleryBackend", "已打开系统图库")
        } catch (e: Exception) {
            Log.e("GalleryBackend", "打开相册失败", e)
            // 方式 3: 如果都失败，打开文件管理器让用户自己找
            try {
                val fileManagerIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fileManagerIntent)
                Log.d("GalleryBackend", "已打开文件选择器")
            } catch (e2: Exception) {
                Log.e("GalleryBackend", "所有方式都失败", e2)
            }
        }
    }
}

/**
 * Composable 函数：获取最后一张照片的 URI
 */
@Composable
fun rememberLastPhotoUri(): Uri? {
    val context = LocalContext.current
    val lastPhotoUri = remember { mutableStateOf<Uri?>(null) }
    
    LaunchedEffect(Unit) {
        lastPhotoUri.value = GalleryBackend.getLastPhotoUri(context)
    }
    
    return lastPhotoUri.value
}

/**
 * Composable 函数：获取最后一张照片的缩略图
 */
@Composable
fun rememberLastPhotoThumbnail(): Bitmap? {
    val context = LocalContext.current
    val thumbnail = remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(Unit) {
        thumbnail.value = GalleryBackend.getLastPhotoThumbnail(context)
    }
    
    return thumbnail.value
}
