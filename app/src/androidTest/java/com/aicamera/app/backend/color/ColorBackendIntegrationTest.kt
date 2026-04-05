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
 * ColorBackend 集成测试
 * 验证完整的调色流程：模型推理 -> 参数应用 -> 结果保存
 */
@RunWith(AndroidJUnit4::class)
class ColorBackendIntegrationTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private lateinit var testImagePath: String

    @Before
    fun setup() = runBlocking {
        // 初始化 ONNX 模型
        val success = ColorBackend.initialize(context)
        assertTrue("ONNX 模型初始化失败", success)

        // 创建测试图像
        testImagePath = createTestImageFile()
    }

    /**
     * 测试：完整的 AI 调色流程
     * 1. 使用 ONNX 模型分析图像
     * 2. 应用调色参数
     * 3. 保存结果
     */
    @Test
    fun testCompleteAITuningWorkflow() = runBlocking {
        // 步骤 1: AI 分析图像
        val aiResult = ColorBackend.analyzeColorEnhancement(testImagePath)

        assertTrue("AI 分析应该成功", aiResult.success)
        assertNotNull("应该返回调色参数", aiResult.params)

        android.util.Log.i("ColorBackendIntegrationTest",
            "AI Analysis: ${aiResult.detectedInfo}, Confidence: ${aiResult.confidence}")
        android.util.Log.i("ColorBackendIntegrationTest",
            "Params: exposure=${aiResult.params.exposure}, " +
                    "contrast=${aiResult.params.contrast}, " +
                    "saturation=${aiResult.params.saturation}, " +
                    "highlights=${aiResult.params.highlights}, " +
                    "shadows=${aiResult.params.shadows}")

        // 步骤 2: 应用调色参数
        val outputPath = ColorBackend.applyColorAdjustments(testImagePath, aiResult.params)

        assertNotNull("应该返回输出路径", outputPath)
        assertTrue("输出文件应该存在", File(outputPath).exists())

        android.util.Log.i("ColorBackendIntegrationTest", "Output saved to: $outputPath")

        // 步骤 3: 验证输出图像
        val outputBitmap = android.graphics.BitmapFactory.decodeFile(outputPath)
        assertNotNull("应该能读取输出图像", outputBitmap)
        assertTrue("输出图像应该有内容", outputBitmap.width > 0 && outputBitmap.height > 0)
    }

    /**
     * 测试：手动设置参数调色
     */
    @Test
    fun testManualColorAdjustment() = runBlocking {
        // 创建自定义调色参数
        val customParams = ColorAdjustmentParams(
            exposure = 0.3f,
            contrast = 1.2f,
            saturation = 1.5f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.7f,
            shadows = 0.3f
        )

        // 应用调色
        val outputPath = ColorBackend.applyColorAdjustments(testImagePath, customParams)

        assertNotNull("应该返回输出路径", outputPath)
        assertTrue("输出文件应该存在", File(outputPath).exists())

        // 验证输出
        val outputBitmap = android.graphics.BitmapFactory.decodeFile(outputPath)
        assertNotNull("应该能读取输出图像", outputBitmap)

        android.util.Log.i("ColorBackendIntegrationTest",
            "Manual adjustment output saved to: $outputPath")
    }

    /**
     * 测试：生成预览图像
     */
    @Test
    fun testGeneratePreview() = runBlocking {
        val params = ColorAdjustmentParams(
            exposure = 0.2f,
            contrast = 1.1f,
            saturation = 1.2f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.6f,
            shadows = 0.4f
        )

        // 生成预览
        val previewBitmap = ColorBackend.generatePreview(testImagePath, params)

        assertNotNull("应该生成预览图像", previewBitmap)
        assertTrue("预览图像宽度应该在合理范围", previewBitmap!!.width <= 800)

        // 保存预览图像
        saveTestImage(previewBitmap, "test_preview.jpg")

        android.util.Log.i("ColorBackendIntegrationTest",
            "Preview generated: ${previewBitmap.width}x${previewBitmap.height}")
    }

    /**
     * 测试：对比 AI 调色和手动调色
     */
    @Test
    fun testAIvsManualAdjustment() = runBlocking {
        // AI 调色
        val aiResult = ColorBackend.analyzeColorEnhancement(testImagePath)
        assertTrue("AI 分析应该成功", aiResult.success)

        val aiOutputPath = ColorBackend.applyColorAdjustments(testImagePath, aiResult.params)

        // 手动调色（中性参数）
        val manualParams = ColorAdjustmentParams(
            exposure = 0f,
            contrast = 1f,
            saturation = 1f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.5f,
            shadows = 0.5f
        )

        val manualOutputPath = ColorBackend.applyColorAdjustments(testImagePath, manualParams)

        // 验证两个输出都存在
        assertTrue("AI 调色输出应该存在", File(aiOutputPath).exists())
        assertTrue("手动调色输出应该存在", File(manualOutputPath).exists())

        android.util.Log.i("ColorBackendIntegrationTest",
            "AI output: $aiOutputPath")
        android.util.Log.i("ColorBackendIntegrationTest",
            "Manual output: $manualOutputPath")
    }

    /**
     * 测试：多种场景图像的 AI 调色
     */
    @Test
    fun testMultipleSceneTypes() = runBlocking {
        val sceneTypes = listOf(
            Color.RED to "red_scene",
            Color.GREEN to "green_scene",
            Color.BLUE to "blue_scene",
            Color.rgb(128, 128, 128) to "gray_scene",
            Color.rgb(255, 200, 100) to "warm_scene",
            Color.rgb(100, 150, 255) to "cool_scene"
        )

        sceneTypes.forEach { (color, sceneName) ->
            // 创建场景图像
            val sceneBitmap = createTestBitmap(512, 512, color)
            val scenePath = saveBitmapToFile(sceneBitmap, "scene_$sceneName.jpg")

            // AI 分析
            val aiResult = ColorBackend.analyzeColorEnhancement(scenePath)

            if (aiResult.success) {
                // 应用调色
                val outputPath = ColorBackend.applyColorAdjustments(scenePath, aiResult.params)

                android.util.Log.i("ColorBackendIntegrationTest",
                    "Scene [$sceneName]: exposure=${aiResult.params.exposure.toFloat()}, " +
                            "contrast=${aiResult.params.contrast.toFloat()}, " +
                            "saturation=${aiResult.params.saturation.toFloat()}")

                assertTrue("$sceneName 调色输出应该存在", File(outputPath).exists())
            }
        }
    }

    /**
     * 测试：调色参数边界值
     */
    @Test
    fun testExtremeParameters() = runBlocking {
        val extremeParams = listOf(
            ColorAdjustmentParams(-1f, 0f, 0f, 0f, 0f, 0f, 0f) to "min_all",
            ColorAdjustmentParams(1f, 2f, 2f, 1f, 1f, 1f, 1f) to "max_all",
            ColorAdjustmentParams(0f, 1f, 1f, 0f, 0f, 0.5f, 0.5f) to "neutral"
        )

        extremeParams.forEach { (params, name) ->
            val outputPath = ColorBackend.applyColorAdjustments(testImagePath, params)
            assertTrue("$name 调色输出应该存在", File(outputPath).exists())

            android.util.Log.i("ColorBackendIntegrationTest",
                "Extreme params [$name] applied successfully")
        }
    }

    /**
     * 测试：性能测试 - 连续处理多张图像
     */
    @Test
    fun testBatchProcessingPerformance() = runBlocking {
        val numImages = 5
        val processingTimes = mutableListOf<Long>()

        repeat(numImages) { index ->
            // 创建不同的测试图像
            val color = when (index % 3) {
                0 -> Color.RED
                1 -> Color.GREEN
                else -> Color.BLUE
            }
            val bitmap = createTestBitmap(1024, 768, color)
            val imagePath = saveBitmapToFile(bitmap, "batch_test_$index.jpg")

            val startTime = System.currentTimeMillis()

            // AI 分析 + 调色
            val aiResult = ColorBackend.analyzeColorEnhancement(imagePath)
            if (aiResult.success) {
                ColorBackend.applyColorAdjustments(imagePath, aiResult.params)
            }

            val endTime = System.currentTimeMillis()
            processingTimes.add(endTime - startTime)
        }

        val avgTime = processingTimes.average()
        val totalTime = processingTimes.sum()

        android.util.Log.i("ColorBackendIntegrationTest",
            "Batch processing: $numImages images, " +
                    "avg time: ${avgTime}ms, total time: ${totalTime}ms")

        // 单张图像处理应该在 3 秒内完成
        assertTrue("单张图像处理应该在合理时间内完成", avgTime < 3000)
    }

    /**
     * 测试：预览生成性能
     */
    @Test
    fun testPreviewPerformance() = runBlocking {
        val largeBitmap = createTestBitmap(1920, 1080, Color.rgb(128, 128, 128))
        val largeImagePath = saveBitmapToFile(largeBitmap, "large_test.jpg")

        val params = ColorAdjustmentParams(
            exposure = 0.2f,
            contrast = 1.1f,
            saturation = 1.2f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.6f,
            shadows = 0.4f
        )

        val startTime = System.currentTimeMillis()
        val preview = ColorBackend.generatePreview(largeImagePath, params)
        val endTime = System.currentTimeMillis()

        assertNotNull("应该生成预览", preview)

        val duration = endTime - startTime
        android.util.Log.i("ColorBackendIntegrationTest",
            "Preview generation time: ${duration}ms for 1920x1080 image")

        // 预览生成应该在 2 秒内完成
        assertTrue("预览生成应该在合理时间内完成", duration < 2000)
    }

    /**
     * 测试：从 Bitmap 直接生成预览
     */
    @Test
    fun testGeneratePreviewFromBitmap() = runBlocking {
        val bitmap = createTestBitmap(1024, 1024, Color.rgb(100, 150, 200))

        val params = ColorAdjustmentParams(
            exposure = 0.15f,
            contrast = 1.15f,
            saturation = 1.25f,
            sharpness = 0f,
            temperature = 0f,
            highlights = 0.55f,
            shadows = 0.45f
        )

        val preview = ColorBackend.generatePreviewFromBitmap(bitmap, params)

        assertNotNull("应该从 Bitmap 生成预览", preview)
        assertTrue("预览应该有内容", preview!!.width > 0 && preview.height > 0)

        saveTestImage(preview, "test_preview_from_bitmap.jpg")
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试图像文件
     */
    private fun createTestImageFile(): String {
        val bitmap = createTestBitmap(1024, 768, Color.rgb(128, 128, 128))
        return saveBitmapToFile(bitmap, "test_input.jpg")
    }

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
     * 保存 Bitmap 到文件
     */
    private fun saveBitmapToFile(bitmap: Bitmap, filename: String): String {
        val testDir = File(context.getExternalFilesDir(null), "test_images")
        testDir.mkdirs()
        val file = File(testDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file.absolutePath
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
            android.util.Log.i("ColorBackendIntegrationTest", "Saved test image: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("ColorBackendIntegrationTest", "Failed to save test image", e)
        }
    }
}
