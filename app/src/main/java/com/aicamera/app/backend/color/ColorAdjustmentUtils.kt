package com.aicamera.app.backend.color

import android.graphics.Bitmap
import android.util.Log
import com.aicamera.app.backend.models.ColorAdjustmentParams
import java.lang.Math.pow
import kotlin.math.min


/**
 * 调色参数应用工具类
 * 实现与 Python 脚本一致的调色逻辑
 * 所有方法都包含异常处理，确保出错时不会影响其他部分
 */
object ColorAdjustmentUtils {

    private const val TAG = "ColorAdjustmentUtils"

    /**
     * 应用调色参数到图像
     * 优化顺序：exposure -> contrast -> saturation -> highlight/shadow
     * 先调整对比度再调整饱和度，确保色彩更鲜艳
     * 每个步骤都独立捕获异常，确保即使某一步失败也能返回可用的结果
     */
    fun applyAdjustments(bitmap: Bitmap, params: ColorAdjustmentParams): Bitmap {
        // 打印输入参数，用于调试
        Log.d(TAG, "========================================")
        Log.d(TAG, "INPUT PARAMS (原始输入):")
        Log.d(TAG, "  exposure=${params.exposure}")
        Log.d(TAG, "  contrast=${params.contrast}")
        Log.d(TAG, "  saturation=${params.saturation}")
        Log.d(TAG, "  highlights=${params.highlights}")
        Log.d(TAG, "  shadows=${params.shadows}")
        Log.d(TAG, "========================================")
        // 验证输入参数
        if (bitmap.isRecycled) {
            Log.e(TAG, "Cannot apply adjustments: bitmap is recycled")
            return bitmap
        }

        var result = safeCopyBitmap(bitmap) ?: return bitmap

        try {
            // 1. 应用曝光 (exposure)
            // 曝光范围 [-1.0, 1.0]，允许更大幅度的调整
            val clampedExposure = params.exposure.coerceIn(-1.0f, 1.0f)
            // 总是应用曝光（即使为0，也确保通过）
            Log.d(TAG, "Applying exposure: %.4f (alpha=%.4f)".format(clampedExposure, 1.0f + clampedExposure))
            result = safeApplyExposure(result, clampedExposure) ?: result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply exposure", e)
        }

        try {
            // 2. 应用对比度 (contrast) - 先调整对比度
            // 对比度范围 [0.5, 2.0]，1.0 表示不调整
            val clampedContrast = params.contrast.coerceIn(0.5f, 2.0f)
            Log.d(TAG, "Applying contrast: %.4f".format(clampedContrast))
            result = safeApplyContrast(result, clampedContrast) ?: result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply contrast", e)
        }

        try {
            // 3. 应用饱和度 (saturation) - 后调整饱和度，确保色彩鲜艳
            // 饱和度范围 [0.5, 2.0]，1.0 表示不调整
            // 关键：总是应用饱和度调整，确保色彩正确
            val clampedSaturation = params.saturation.coerceIn(0.5f, 2.0f)
            Log.d(TAG, "Applying saturation: %.4f".format(clampedSaturation))
            result = safeApplySaturation(result, clampedSaturation) ?: result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply saturation", e)
        }

        try {
            // 4. 应用高光/阴影 (highlight/shadow)
            // 高光/阴影范围 [0.0, 1.0]，0.5 表示中性
            val clampedHighlights = params.highlights.coerceIn(0.0f, 1.0f)
            val clampedShadows = params.shadows.coerceIn(0.0f, 1.0f)
            Log.d(TAG, "Applying highlight/shadow: h=%.4f, s=%.4f".format(clampedHighlights, clampedShadows))
            result = safeApplyHighlightShadow(result, clampedHighlights, clampedShadows) ?: result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply highlight/shadow", e)
        }

        // 移除双边滤波，避免模糊效果影响色彩鲜艳度
        // try {
        //     result = safeApplyBilateralFilter(result) ?: result
        // } catch (e: Exception) {
        //     Log.e(TAG, "Failed to apply bilateral filter", e)
        // }

        return result
    }

    /**
     * 安全复制位图
     */
    private fun safeCopyBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy bitmap", e)
            null
        }
    }

    /**
     * 应用曝光调整
     * Python: adjusted_image = np.clip(adjusted_image * alpha, 0, 255)
     * alpha = 1.0 + exposure
     */
    private fun safeApplyExposure(bitmap: Bitmap, exposure: Float): Bitmap? {
        return try {
            val alpha = 1.0f + exposure
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                val r = min(255f, ((pixel shr 16) and 0xFF) * alpha).toInt()
                val g = min(255f, ((pixel shr 8) and 0xFF) * alpha).toInt()
                val b = min(255f, (pixel and 0xFF) * alpha).toInt()
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }

            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyExposure", e)
            null
        }
    }

    /**
     * 应用饱和度调整
     * Python: HSV 色彩空间调整 S 通道
     * 重要：确保色彩鲜艳度，避免灰色
     */
    private fun safeApplySaturation(bitmap: Bitmap, saturation: Float): Bitmap? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)

                // 计算灰度值（用于饱和度调整）
                val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                
                // 应用饱和度：在灰色和原色之间插值
                // saturation > 1 增加饱和度，saturation < 1 降低饱和度
                val newR = (gray + (r - gray) * saturation).toInt().coerceIn(0, 255)
                val newG = (gray + (g - gray) * saturation).toInt().coerceIn(0, 255)
                val newB = (gray + (b - gray) * saturation).toInt().coerceIn(0, 255)

                pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }

            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in applySaturation", e)
            null
        }
    }

    /**
     * 应用对比度调整
     * Python: adjusted_image = np.clip((adjusted_image - mean_val) * contrast + mean_val, 0, 255)
     * 简化版：使用亮度平均值，保持色彩比例
     */
    private fun safeApplyContrast(bitmap: Bitmap, contrast: Float): Bitmap? {
        return try {
            val width = bitmap.width
            val height = bitmap.height

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // 计算平均亮度（使用标准亮度公式）
            var totalLuminance = 0f
            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)
                totalLuminance += (0.299f * r + 0.587f * g + 0.114f * b)
            }
            val meanLuminance = totalLuminance / pixels.size

            // 应用对比度
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)

                // 计算当前像素的亮度
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                
                // 调整亮度差值
                val diff = luminance - meanLuminance
                val newLuminance = meanLuminance + diff * contrast
                
                // 保持色彩比例调整 RGB
                val ratio = if (luminance > 0) newLuminance / luminance else 1f
                val newR = (r * ratio).toInt().coerceIn(0, 255)
                val newG = (g * ratio).toInt().coerceIn(0, 255)
                val newB = (b * ratio).toInt().coerceIn(0, 255)

                pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }

            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyContrast", e)
            null
        }
    }

    /**
     * 应用高光/阴影调整
     * 简化版：直接在 RGB 空间调整亮度，避免 LAB 转换导致的色彩失真
     */
    private fun safeApplyHighlightShadow(bitmap: Bitmap, highlight: Float, shadow: Float): Bitmap? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // 计算调整因子
            val highlightAdjustment = 1.0f + highlight * 0.3f
            val shadowAdjustment = 0.7f + shadow * 0.3f

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)

                // 计算亮度（简化版 L 通道）
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                val lNormalized = luminance / 255f

                // 计算高光/阴影权重
                val highlightWeight = ((lNormalized - 0.5f) * 2f).coerceIn(0f, 1f)
                val shadowWeight = ((0.5f - lNormalized) * 2f).coerceIn(0f, 1f)

                // 计算调整系数
                val adjustment = highlightWeight * highlightAdjustment + 
                                shadowWeight * shadowAdjustment +
                                (1 - highlightWeight - shadowWeight)

                // 应用调整（保持色彩比例）
                val newR = (r * adjustment).toInt().coerceIn(0, 255)
                val newG = (g * adjustment).toInt().coerceIn(0, 255)
                val newB = (b * adjustment).toInt().coerceIn(0, 255)

                pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }

            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyHighlightShadow", e)
            null
        }
    }

    /**
     * 应用双边滤波 (简化版)
     * Python: cv2.bilateralFilter(adjusted_image, 5, 50, 50)
     */
    private fun safeApplyBilateralFilter(bitmap: Bitmap): Bitmap? {
        return try {
            // 简化实现：使用简单的均值滤波
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val newPixels = pixels.copyOf()

            // 简单的 3x3 均值滤波
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var r = 0
                    var g = 0
                    var b = 0

                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val pixel = pixels[(y + dy) * width + (x + dx)]
                            r += (pixel shr 16) and 0xFF
                            g += (pixel shr 8) and 0xFF
                            b += pixel and 0xFF
                        }
                    }

                    val a = (pixels[y * width + x] shr 24) and 0xFF
                    newPixels[y * width + x] = (a shl 24) or ((r / 9) shl 16) or ((g / 9) shl 8) or (b / 9)
                }
            }

            result.setPixels(newPixels, 0, width, 0, 0, width, height)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyBilateralFilter", e)
            null
        }
    }

    // ==================== 色彩空间转换工具函数 ====================

    /**
     * RGB 转 HSV
     */
    private fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val maxVal = maxOf(rf, gf, bf)
        val minVal = minOf(rf, gf, bf)
        val delta = maxVal - minVal

        var h = 0f
        var s = 0f
        val v = maxVal

        if (delta != 0f) {
            s = delta / maxVal

            h = when (maxVal) {
                rf -> ((gf - bf) / delta) % 6
                gf -> ((bf - rf) / delta) + 2
                else -> ((rf - gf) / delta) + 4
            }
            h *= 60f
            if (h < 0) h += 360f
        }

        return floatArrayOf(h, s, v)
    }

    /**
     * HSV 转 RGB
     */
    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = v - c

        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return intArrayOf(
            ((r + m) * 255).toInt(),
            ((g + m) * 255).toInt(),
            ((b + m) * 255).toInt()
        )
    }

    /**
     * RGB 转 LAB (简化版)
     */
    private fun rgbToLab(r: Int, g: Int, b: Int): FloatArray {
        // 先转 XYZ
        var rf = r / 255.0
        var gf = g / 255.0
        var bfVal = b / 255.0

        rf = if (rf > 0.04045) pow((rf + 0.055) / 1.055, 2.4) else rf / 12.92
        gf = if (gf > 0.04045) pow((gf + 0.055) / 1.055, 2.4) else gf / 12.92
        bfVal = if (bfVal > 0.04045) pow((bfVal + 0.055) / 1.055, 2.4) else bfVal / 12.92

        val x = (rf * 0.4124 + gf * 0.3576 + bfVal * 0.1805) * 100.0
        val y = (rf * 0.2126 + gf * 0.7152 + bfVal * 0.0722) * 100.0
        val z = (rf * 0.0193 + gf * 0.1192 + bfVal * 0.9505) * 100.0

        // XYZ 转 LAB
        val xRef = 95.047
        val yRef = 100.000
        val zRef = 108.883

        val xRatio = x / xRef
        val yRatio = y / yRef
        val zRatio = z / zRef

        fun f(t: Double): Double {
            return if (t > 0.008856) pow(t, 1.0 / 3.0) else (7.787 * t + 16.0 / 116.0)
        }

        val l = (116.0 * f(yRatio) - 16.0).toFloat()
        val a = (500.0 * (f(xRatio) - f(yRatio))).toFloat()
        val bResult = (200.0 * (f(yRatio) - f(zRatio))).toFloat()

        return floatArrayOf(l, a, bResult)
    }

    /**
     * LAB 转 RGB (简化版)
     */
    private fun labToRgb(l: Float, a: Float, b: Float): IntArray {
        // LAB 转 XYZ
        val yRef = 100.000

        val fy = (l + 16f) / 116f
        val fx = a / 500f + fy
        val fz = fy - b / 200f

        val xRef = 95.047
        val zRef = 108.883

        fun fInv(t: Double): Double {
            val delta = 6.0 / 29.0
            return if (t > delta) t * t * t else 3.0 * delta * delta * (t - 4.0 / 29.0)
        }

        val x = xRef * fInv(fx.toDouble())
        val y = yRef * fInv(fy.toDouble())
        val z = zRef * fInv(fz.toDouble())

        // XYZ 转 RGB
        val xNorm = x / 100.0
        val yNorm = y / 100.0
        val zNorm = z / 100.0

        var rf = xNorm * 3.2406 + yNorm * -1.5372 + zNorm * -0.4986
        var gf = xNorm * -0.9689 + yNorm * 1.8758 + zNorm * 0.0415
        var bf = xNorm * 0.0557 + yNorm * -0.2040 + zNorm * 1.0570

        rf = if (rf > 0.0031308) 1.055 * pow(rf, 1.0 / 2.4) - 0.055 else 12.92 * rf
        gf = if (gf > 0.0031308) 1.055 * pow(gf, 1.0 / 2.4) - 0.055 else 12.92 * gf
        bf = if (bf > 0.0031308) 1.055 * pow(bf, 1.0 / 2.4) - 0.055 else 12.92 * bf

        return intArrayOf(
            (rf * 255).toInt().coerceIn(0, 255),
            (gf * 255).toInt().coerceIn(0, 255),
            (bf * 255).toInt().coerceIn(0, 255)
        )
    }
}
