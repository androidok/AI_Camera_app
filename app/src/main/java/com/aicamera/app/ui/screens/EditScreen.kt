package com.aicamera.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.BackHandler
import coil.compose.rememberAsyncImagePainter
import com.aicamera.app.ui.components.ToolButton
import com.aicamera.app.ui.components.TopBarWithActions
import com.aicamera.app.ui.theme.*
import com.aicamera.app.ui.theme.getColorScheme
import com.aicamera.app.backend.storage.StorageBackend
import com.aicamera.app.backend.rotate.RotateBackend
import kotlinx.coroutines.launch
import java.io.File

/**
 * 编辑界面
 *
 * 后端开发者注意：
 * - imageUri: 要编辑的图片路径
 * - onNavigateToCrop: 跳转到裁剪界面
 * - onNavigateToColor: 跳转到调色界面
 * - onImageRotated: 图片旋转后的回调，参数为新图片路径
 */
@Composable
fun EditScreen(
    themeType: ThemeType,
    imageUri: String,
    onNavigateBack: () -> Unit,
    onNavigateToCrop: () -> Unit,
    onNavigateToColor: () -> Unit,
    onImageRotated: (String) -> Unit = {} // 默认空实现，向后兼容
) {
    val colorScheme = getColorScheme(themeType)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTool by remember { mutableStateOf("") }
    var isRotating by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit) {
            val threshold = with(density) { 100.dp.toPx() }
            var totalOffset = 0f
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (kotlin.math.abs(totalOffset) > threshold) {
                        onNavigateBack()
                    }
                    totalOffset = 0f
                }
            ) { change, dragAmount ->
                totalOffset += dragAmount
            }
        }
        .background(colorScheme.background)) {
        val sensorRatioPortrait = 0.75f // 3:4 竖屏比例，与相机取景框一致
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 计算取景框高度（与相机预览保持一致）
        val viewfinderHeight = if (screenWidth.value / screenHeight.value > sensorRatioPortrait) {
            // 屏幕更宽，图像以高度为准缩放
            screenHeight
        } else {
            // 屏幕更高，图像以宽度为准缩放
            screenWidth / sensorRatioPortrait
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopBarWithActions(
                title = "编辑",
                onClose = onNavigateBack,
                onConfirm = {
                    StorageBackend.saveToGallery(
                        context = context,
                        imageUri = imageUri,
                        onSuccess = { onNavigateBack() },
                        onError = { onNavigateBack() }
                    )
                },
                themeType = themeType
            )

            // 图片预览区域 - 高度与相机取景框一致，并垂直居中于剩余空间
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewfinderHeight)
                        .background(colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                Image(
                    painter = rememberAsyncImagePainter(File(imageUri)),
                    contentDescription = "编辑照片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // 保持比例居中，与取景框一致
                )
                }
            }

            // 底部工具栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ToolButton(
                        icon = Icons.Default.Crop,
                        label = "裁剪",
                        onClick = onNavigateToCrop,
                        isSelected = selectedTool == "crop",
                        themeType = themeType
                    )

                    ToolButton(
                        icon = Icons.Default.Edit,
                        label = "编辑",
                        onClick = onNavigateToColor,
                        isSelected = selectedTool == "edit",
                        themeType = themeType
                    )

                    ToolButton(
                        icon = Icons.Default.RotateRight,
                        label = "旋转",
                        onClick = {
                            selectedTool = "rotate"
                            isRotating = true
                            scope.launch {
                                try {
                                    val rotatedUri = RotateBackend.rotateImage(imageUri, 90f)
                                    onImageRotated(rotatedUri)
                                } catch (e: Exception) {
                                    // 可以显示错误提示，这里暂时忽略
                                    e.printStackTrace()
                                } finally {
                                    isRotating = false
                                }
                            }
                        },
                        isSelected = selectedTool == "rotate",
                        themeType = themeType
                    )
                }
            }
        }
    }
}
