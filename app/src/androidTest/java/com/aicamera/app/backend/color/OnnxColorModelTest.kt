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
 * OnnxColorModel 模型推理测试
 * 验证 ONNX 模型加载和推理功能
 */
@RunWith(AndroidJUnit4::class)
class OnnxColorModelTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Before
    fun setup() = runBlocking {
        // 初始化 ONNX 模型
        val success = OnnxColorModel.initialize(context)
        assertTrue("ONNX 模型初始化失败", success)
    }

    /**
     * 测试：模型初始化
     */
    @Test
    fun testModelInitialization() = runBlocking {
        // 重新初始化应该返回 true（已经初始化过）
        val success = OnnxColorModel.initialize(context)
        assertTrue("模型应该已经初始化", success)
    }

    /**
     * 测试：纯色图像推理
     * 验证模型对纯色图像的输出
     */
    @Test
    fun testInferenceOnSolidColor() = runBlocking {
        // 创建灰色测试图像
        val grayBitmap = createTestBitmap(224, 224, Color.rgb(128, 128, 128))

        val params = OnnxColorModel.analyzeImage(grayBitmap)

        // 验证返回的参数在合理范围内
        assertTrue("exposure 应该在合理范围内", params.exposure in -2f..2f)
        assertTrue("contrast 应该在合理范围内", params.contrast in 0f..2f)
        assertTrue("saturation 应该在合理范围内", params.saturation in 0f..2f)
        assertTrue("highlights 应该在合理范围内", params.highlights in 0f..1f)
        assertTrue("shadows 应该在合理范围内", params.shadows in 0f..1f)

        android.util.Log.i("OnnxColorModelTest", "Gray image params: $params")
    }

    /**
     * 测试：红色图像推理
     */
    @Test
    fun testInferenceOnRedImage() = runBlocking {
        val redBitmap = createTestBitmap(224, 224, Color.RED)

        val params = OnnxColorModel.analyzeImage(redBitmap)

        assertNotNull(params)
        android.util.Log.i("OnnxColorModelTest", "Red image params: $params")

        // 保存测试图像
        saveTestImage(redBitmap, "test_red_input.jpg")
    }

    /**
     * 测试：蓝色图像推理
     */
    @Test
    fun testInferenceOnBlueImage() = runBlocking {
        val blueBitmap = createTestBitmap(224, 224, Color.BLUE)

        val params = OnnxColorModel.analyzeImage(blueBitmap)

        assertNotNull(params)
        android.util.Log.i("OnnxColorModelTest", "Blue image params: $params")

        saveTestImage(blueBitmap, "test_blue_input.jpg")
    }

    /**
     * 测试：绿色图像推理
     */
    @Test
    fun testInferenceOnGreenImage() = runBlocking {
        val greenBitmap = createTestBitmap(224, 224, Color.GREEN)

        val params = OnnxColorModel.analyzeImage(greenBitmap)

        assertNotNull(params)
        android.util.Log.i("OnnxColorModelTest", "Green image params: $params")

        saveTestImage(greenBitmap, "test_green_input.jpg")
    }

    /**
     * 测试：白色图像推理
     */
    @Test
    fun testInferenceOnWhiteImage() = runBlocking {
        val whiteBitmap = createTestBitmap(224, 224, Color.WHITE)

        val params = OnnxColorModel.analyzeImage(whiteBitmap)

        assertNotNull(params)
        android.util.Log.i("OnnxColorModelTest", "White image params: $params")
    }

    /**
     * 测试：黑色图像推理
     */
    @Test
    fun testInferenceOnBlackImage() = runBlocking {
        val blackBitmap = createTestBitmap(224, 224, Color.BLACK)

        val params = OnnxColorModel.analyzeImage(blackBitmap)

        assertNotNull(params)
        android.util.Log.i("OnnxColorModelTest", "Black image params: $params")
    }

    /**
     * 测试：彩色图像推理
     */
    @Test
    fun testInferenceOnColorfulImage() = runBlocking {
        val colorfulBitmap = createColorfulBitmap(224, 224)

        val params = OnnxColorModel.analyzeImage(colorfulBitmap)

        assertNotNull(params)
        android.util.Log.i("OnnxColorModelTest", "Colorful image params: $params")

        saveTestImage(colorfulBitmap, "test_colorful_input.jpg")
    }

    /**
     * 测试：推理性能
     */
    @Test
    fun testInferencePerformance() = runBlocking {
        val testBitmap = createTestBitmap(224, 224, Color.rgb(128, 128, 128))

        // 预热
        repeat(3) {
            OnnxColorModel.analyzeImage(testBitmap)
        }

        // 正式测试
        val iterations = 10
        val startTime = System.currentTimeMillis()

        repeat(iterations) {
            OnnxColorModel.analyzeImage(testBitmap)
        }

        val endTime = System.currentTimeMillis()
        val avgTime = (endTime - startTime) / iterations.toFloat()

        android.util.Log.i("OnnxColorModelTest", "Average inference time: ${avgTime}ms over $iterations iterations")

        // 单次推理应该在 500ms 内完成
        assertTrue("推理速度应该在合理范围内", avgTime < 500)
    }

    /**
     * 测试：不同尺寸输入
     * 模型应该能处理不同尺寸的图像（内部会缩放到 224x224）
     */
    @Test
    fun testDifferentInputSizes() = runBlocking {
        val sizes = listOf(
            100 to 100,
            512 to 512,
            1024 to 768,
            1920 to 1080
        )

        sizes.forEach { (width, height) ->
            val bitmap = createTestBitmap(width, height, Color.rgb(128, 128, 128))
            val params = OnnxColorModel.analyzeImage(bitmap)

            assertNotNull("尺寸 ${width}x${height} 应该能正常处理", params)
            android.util.Log.i("OnnxColorModelTest", "Size ${width}x${height} params: $params")
        }
    }

    /**
     * 测试：连续推理稳定性
     */
    @Test
    fun testContinuousInference() = runBlocking {
        val testBitmap = createTestBitmap(224, 224, Color.rgb(128, 128, 128))

        // 连续推理 20 次
        val results = mutableListOf<ColorAdjustmentParams>()
        repeat(20) {
            val params = OnnxColorModel.analyzeImage(testBitmap)
            results.add(params)
        }

        // 验证所有结果一致（同一输入应该产生相同输出）
        val first = results.first()
        results.forEach { params ->
            assertEquals("exposure 应该一致", first.exposure, params.exposure, 0.001f)
            assertEquals("contrast 应该一致", first.contrast, params.contrast, 0.001f)
            assertEquals("saturation 应该一致", first.saturation, params.saturation, 0.001f)
            assertEquals("highlights 应该一致", first.highlights, params.highlights, 0.001f)
            assertEquals("shadows 应该一致", first.shadows, params.shadows, 0.001f)
        }

        android.util.Log.i("OnnxColorModelTest", "Continuous inference test passed, all ${results.size} results consistent")
    }

    /**
     * 测试：应用模型预测的参数并验证效果
     */
    @Test
    fun testApplyPredictedParams() = runBlocking {
        // 创建测试图像
        val testBitmap = createTestBitmap(512, 512, Color.rgb(128, 128, 128))

        // 获取模型预测参数
        val params = OnnxColorModel.analyzeImage(testBitmap)
        android.util.Log.i("OnnxColorModelTest", "Predicted params: $params")

        // 应用调色参数
        val adjustedBitmap = ColorAdjustmentUtils.applyAdjustments(testBitmap, params)

        // 验证输出图像
        assertEquals(testBitmap.width, adjustedBitmap.width)
        assertEquals(testBitmap.height, adjustedBitmap.height)

        // 保存输入和输出图像
        saveTestImage(testBitmap, "test_model_input.jpg")
        saveTestImage(adjustedBitmap, "test_model_output.jpg")

        android.util.Log.i("OnnxColorModelTest", "Input and output images saved for visual verification")
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
     * 创建彩色测试图像（多色块）
     */
    private fun createColorfulBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA)

        val blockWidth = width / 3
        val blockHeight = height / 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                val colorIndex = (y / blockHeight) * 3 + (x / blockWidth)
                val color = colors.getOrElse(colorIndex.coerceIn(0, colors.size - 1)) { Color.GRAY }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    /**
     * 保存测试图像到外部存储
     */
    private fun saveTestImage(bitmap: Bitmap, filename: String) {
        try {
            val testDir = File(context.getExternalFilesDir(null), "test_results")
            testDir.mkdirs()
            val file = File(testDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            android.util.Log.i("OnnxColorModelTest", "Saved test image: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("OnnxColorModelTest", "Failed to save test image", e)
        }
    }
}
