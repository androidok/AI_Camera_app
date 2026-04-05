package com.aicamera.app.ui.panels

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicamera.app.backend.camera.CameraBackend
import com.aicamera.app.backend.camera.CameraSession
import com.aicamera.app.backend.camera.CameraAdvancedControls
import com.aicamera.app.ui.components.LabeledSlider
import com.aicamera.app.ui.theme.ThemeType
import com.aicamera.app.ui.theme.getColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.log2

/**
 * ============================================
 * 参数设置面板 - iOS风格底部呼出
 * ============================================
 */
@Composable
fun ParamSettingsPanel(
    onDismiss: () -> Unit,
    themeType: ThemeType = ThemeType.PROFESSIONAL
) {
    val colorScheme = getColorScheme(themeType)
    var evIndex by remember { mutableStateOf(CameraBackend.ManualSettings.evIndex ?: 0) }
    var iso by remember { mutableStateOf(CameraBackend.ManualSettings.iso ?: 100) }
    var exposureTimeNs by remember { mutableStateOf(CameraBackend.ManualSettings.exposureTimeNs) }
    var hdrEnabled by remember { mutableStateOf(CameraBackend.ManualSettings.hdrEnabled) }
    var currentRatio by remember { mutableStateOf(CameraBackend.ManualSettings.previewAspectRatioPortrait) }
    val camera = CameraSession.camera()

    val minExposureSeconds = 1.0 / 4000.0
    val maxExposureSeconds = 8.0
    val log2Min = log2(minExposureSeconds)
    val log2Max = log2(maxExposureSeconds)
    val log2Range = log2Max - log2Min

    fun exposureToSlider(ns: Long?): Float {
        if (ns == null || ns <= 0) return 0.5f
        val seconds = ns / 1_000_000_000.0
        val logValue = log2(seconds)
        return ((logValue - log2Min) / log2Range).toFloat().coerceIn(0f, 1f)
    }

    fun sliderToExposure(value: Float): Long? {
        if (value >= 0.95f) return null
        val logValue = log2Min + value * log2Range
        val seconds = Math.pow(2.0, logValue)
        return (seconds * 1_000_000_000).toLong()
    }

    fun formatShutter(ns: Long?): String {
        if (ns == null || ns == 0L) return "Auto"
        val seconds = ns / 1_000_000_000.0
        return when {
            seconds >= 1.0 -> String.format("%.1fs", seconds)
            seconds >= 0.1 -> String.format("%.2fs", seconds)
            else -> {
                val denom = (1.0 / seconds).toInt()
                "1/${denom}s"
            }
        }
    }

    fun applySettings() {
        camera?.let { cam ->
            try {
                CameraAdvancedControls.setExposureCompensationIndex(cam, evIndex)
                CameraAdvancedControls.applyManualExposure(cam, iso, exposureTimeNs)
            } catch (e: Exception) {
                Log.e("ParamSettingsPanel", "Failed to apply settings", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            CameraBackend.ManualSettings.evIndex?.let { if (it != evIndex) evIndex = it }
            CameraBackend.ManualSettings.iso?.let { if (it != iso) iso = it }
            if (CameraBackend.ManualSettings.exposureTimeNs != exposureTimeNs) {
                exposureTimeNs = CameraBackend.ManualSettings.exposureTimeNs
            }
            if (CameraBackend.ManualSettings.hdrEnabled != hdrEnabled) {
                hdrEnabled = CameraBackend.ManualSettings.hdrEnabled
            }
            if (CameraBackend.ManualSettings.previewAspectRatioPortrait != currentRatio) {
                currentRatio = CameraBackend.ManualSettings.previewAspectRatioPortrait
            }
            delay(100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
        ) {
            // 底部透明背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.surface,
                                colorScheme.surface.copy(alpha = 0.95f),
                                colorScheme.surface.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount > 50) onDismiss()
                        }
                    }
                    .clickable { /* 阻止穿透 */ }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // 顶部拖动指示条
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                colorScheme.primary.copy(alpha = 0f),
                                                colorScheme.primary,
                                                colorScheme.primary.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                        }

                        // 标题和恢复默认按钮在同一行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "参数设置",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )

                            TextButton(
                                onClick = {
                                    evIndex = 0
                                    iso = 100
                                    exposureTimeNs = null
                                    CameraBackend.ManualSettings.evIndex = null
                                    CameraBackend.ManualSettings.iso = null
                                    CameraBackend.ManualSettings.exposureTimeNs = null
                                    camera?.let { cam ->
                                        CameraAdvancedControls.restoreAutoMode(cam)
                                        CameraAdvancedControls.setExposureCompensationIndex(cam, 0)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("恢复默认参数", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        // 滑动内容
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            LabeledSlider(
                                label = "EV【曝光补偿】",
                                value = evIndex.toFloat(),
                                onValueChange = {
                                    evIndex = it.roundToInt()
                                    CameraBackend.ManualSettings.evIndex = evIndex
                                    applySettings()
                                },
                                valueRange = -12f..12f,
                                valueFormatter = { "%+.1f".format(it) },
                                themeType = themeType
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LabeledSlider(
                                label = "ISO【感光度】",
                                value = iso.toFloat(),
                                onValueChange = {
                                    iso = it.roundToInt()
                                    CameraBackend.ManualSettings.iso = iso
                                    applySettings()
                                },
                                valueRange = 100f..3200f,
                                valueFormatter = { it.toInt().toString() },
                                themeType = themeType
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val shutterValue = exposureToSlider(exposureTimeNs)
                            LabeledSlider(
                                label = "Shutter【快门速度】",
                                value = shutterValue,
                                onValueChange = {
                                    exposureTimeNs = sliderToExposure(it)
                                    CameraBackend.ManualSettings.exposureTimeNs = exposureTimeNs
                                    applySettings()
                                },
                                valueRange = 0f..1f,
                                valueFormatter = { formatShutter(exposureTimeNs) },
                                themeType = themeType
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 照片比例
                            Text(
                                text = "照片比例",
                                fontSize = 13.sp,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // 四个按钮单独一行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val aspectList = listOf(-1f to "全屏", 0.5625f to "16:9", 0.75f to "4:3", 1.0f to "1:1")

                                for ((ratio, label) in aspectList) {
                                    val isSelected = kotlin.math.abs(currentRatio - ratio) < 0.01f
                                    val gradientColors = listOf(
                                        colorScheme.primary,
                                        colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .then(
                                                if (isSelected && themeType == ThemeType.FRESH) {
                                                    Modifier.background(
                                                        brush = Brush.horizontalGradient(gradientColors)
                                                    )
                                                } else if (isSelected) {
                                                    Modifier.background(colorScheme.primary)
                                                } else {
                                                    Modifier
                                                        .background(Color.Transparent)
                                                        .border(
                                                            width = 1.dp,
                                                            color = colorScheme.outline.copy(alpha = 0.5f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                }
                                            )
                                            .clickable {
                                                currentRatio = ratio
                                                CameraBackend.ManualSettings.previewAspectRatioPortrait = ratio
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                    }
                }
            }
        }
    }
}