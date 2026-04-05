package com.aicamera.app.backend.color

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aicamera.app.backend.models.ColorAdjustmentParams
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.io.FileOutputStream

/**
 * ColorAdjustmentUtils 单元测试
 * 验证调色参数应用效果
 */
@RunWith(AndroidJUnit4::class)
class ColorAdjustmentUtilsTest {

    private lateinit var testBitmap: Bitmap
    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Before
    fun setup() {
        // 创建测试用的纯色图像
        testBitmap = createTestBitmap(224, 224, Color.rgb(128, 128, 128))
    }

    /**
     * 测试：曝光调整
     * 验证曝光参数是否正确应用
     */
    @Test
    fun testExposureAdjustment() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = 0.5f,  // 增加曝光
            contrast = 1f,
            saturation = 1f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)

        // 验证图像尺寸不变
        assertEquals(testBitmap.width, result.width)
        assertEquals(testBitmap.height, result.height)

        // 获取中心像素验证亮度增加
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        val brightness = Color.red(centerPixel) // 灰度图像 R=G=B

        // 曝光增加后，亮度应该增加 (128 * 1.5 = 192)
        assertTrue("曝光调整后亮度应该增加", brightness > 150)

        saveTestImage(result, "test_exposure_plus.jpg")
    }

    /**
     * 测试：曝光减少
     */
    @Test
    fun testExposureDecrease() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = -0.5f,  // 减少曝光
            contrast = 1f,
            saturation = 1f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        val brightness = Color.red(centerPixel)

        // 曝光减少后，亮度应该降低 (128 * 0.5 = 64)
        assertTrue("曝光减少后亮度应该降低", brightness < 100)

        saveTestImage(result, "test_exposure_minus.jpg")
    }

    /**
     * 测试：饱和度调整
     * 创建彩色图像测试饱和度
     */
    @Test
    fun testSaturationAdjustment() = runBlocking {
        // 创建红色测试图像
        val redBitmap = createTestBitmap(224, 224, Color.RED)

        val params = ColorAdjustmentParams(
            exposure = 0f,
            contrast = 1f,
            saturation = 2f,  // 增加饱和度
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(redBitmap, params)

        // 验证图像仍然是红色（高饱和度）
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        assertTrue("红色分量应该保持较高", Color.red(centerPixel) > 200)

        saveTestImage(result, "test_saturation_high.jpg")
    }

    /**
     * 测试：饱和度降低
     */
    @Test
    fun testSaturationDecrease() = runBlocking {
        // 创建红色测试图像
        val redBitmap = createTestBitmap(224, 224, Color.RED)

        val params = ColorAdjustmentParams(
            exposure = 0f,
            contrast = 1f,
            saturation = 0f,  // 降低饱和度到0（灰度）
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(redBitmap, params)

        // 验证图像变为灰度（R=G=B）
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        val r = Color.red(centerPixel)
        val g = Color.green(centerPixel)
        val b = Color.blue(centerPixel)

        // 允许一定的误差
        assertTrue("饱和度为0时应该接近灰度", kotlin.math.abs(r - g) < 30 && kotlin.math.abs(g - b) < 30)

        saveTestImage(result, "test_saturation_zero.jpg")
    }

    /**
     * 测试：对比度调整
     */
    @Test
    fun testContrastAdjustment() = runBlocking {
        // 创建渐变测试图像
        val gradientBitmap = createGradientBitmap(224, 224)

        val params = ColorAdjustmentParams(
            exposure = 0f,
            contrast = 2f,  // 增加对比度
            saturation = 1f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(gradientBitmap, params)

        // 验证图像尺寸
        assertEquals(gradientBitmap.width, result.width)
        assertEquals(gradientBitmap.height, result.height)

        saveTestImage(result, "test_contrast_high.jpg")
    }

    /**
     * 测试：高光/阴影调整
     */
    @Test
    fun testHighlightShadowAdjustment() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = 0f,
            contrast = 1f,
            saturation = 1f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.8f,  // 提高高光
            shadows = 0.2f      // 降低阴影
        )

        val result = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)

        assertEquals(testBitmap.width, result.width)
        assertEquals(testBitmap.height, result.height)

        saveTestImage(result, "test_highlight_shadow.jpg")
    }

    /**
     * 测试：综合调色参数
     */
    @Test
    fun testCombinedAdjustments() = runBlocking {
        // 创建彩色测试图像
        val colorBitmap = createTestBitmap(224, 224, Color.rgb(100, 150, 200))

        val params = ColorAdjustmentParams(
            exposure = 0.2f,
            contrast = 1.2f,
            saturation = 1.3f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.6f,
            shadows = 0.4f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(colorBitmap, params)

        assertEquals(colorBitmap.width, result.width)
        assertEquals(colorBitmap.height, result.height)

        saveTestImage(result, "test_combined.jpg")
    }

    /**
     * 测试：默认参数（无调整）
     */
    @Test
    fun testDefaultParams() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = 0f,
            contrast = 1f,
            saturation = 1f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val result = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)

        // 默认参数应该保持图像基本不变
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        val brightness = Color.red(centerPixel)

        // 允许一定的处理误差
        assertTrue("默认参数应该保持亮度基本不变", brightness in 100..180)

        saveTestImage(result, "test_default.jpg")
    }

    /**
     * 测试：大尺寸图像处理性能
     */
    @Test
    fun testLargeImagePerformance() = runBlocking {
        val largeBitmap = createTestBitmap(1920, 1080, Color.rgb(128, 128, 128))

        val params = ColorAdjustmentParams(
            exposure = 0.3f,
            contrast = 1.1f,
            saturation = 1.2f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val startTime = System.currentTimeMillis()
        val result = ColorAdjustmentUtils.applyAdjustments(largeBitmap, params)
        val endTime = System.currentTimeMillis()

        val duration = endTime - startTime
        android.util.Log.i("ColorAdjustmentUtilsTest", "Large image processing time: ${duration}ms")

        assertEquals(largeBitmap.width, result.width)
        assertEquals(largeBitmap.height, result.height)

        // 性能测试：1920x1080 图像处理应该在 5 秒内完成
        assertTrue("大尺寸图像处理应该在合理时间内完成", duration < 5000)

        saveTestImage(result, "test_large_image.jpg")
    }

    /**
     * 测试：边界值处理
     */
    @Test
    fun testBoundaryValues() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = -1f,     // 最小值
            contrast = 0f,      // 最小值
            saturation = 0f,    // 最小值
            sharpness = -1f,    // 最小值
            temperature = -1f,  // 最小值
            highlights = 0f,    // 最小值
            shadows = 0f        // 最小值
        )

        val result = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)

        assertNotNull(result)
        assertEquals(testBitmap.width, result.width)
        assertEquals(testBitmap.height, result.height)

        saveTestImage(result, "test_boundary_min.jpg")
    }

    /**
     * 测试：最大边界值
     */
    @Test
    fun testMaxBoundaryValues() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = 1f,      // 最大值
            contrast = 2f,      // 最大值
            saturation = 2f,    // 最大值
            sharpness = 1f,     // 最大值
            temperature = 1f,   // 最大值
            highlights = 1f,    // 最大值
            shadows = 1f        // 最大值
        )

        val result = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)

        assertNotNull(result)
        assertEquals(testBitmap.width, result.width)
        assertEquals(testBitmap.height, result.height)

        saveTestImage(result, "test_boundary_max.jpg")
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建纯色测试图像
     */
    private fun createTestBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    /**
     * 创建渐变测试图像
     */
    private fun createGradientBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val gray = (255 * x / width).coerceIn(0, 255)
                bitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return bitmap
    }

    /**
     * 保存测试图像到外部存储（用于视觉验证）
     */
    private fun saveTestImage(bitmap: Bitmap, filename: String) {
        try {
            val testDir = File(context.getExternalFilesDir(null), "test_results")
            testDir.mkdirs()
            val file = File(testDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            android.util.Log.i("ColorAdjustmentUtilsTest", "Saved test image: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("ColorAdjustmentUtilsTest", "Failed to save test image", e)
        }
    }
}
