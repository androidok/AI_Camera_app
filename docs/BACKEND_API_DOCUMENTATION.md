# 智能相机 APP - 前后端接口文档

## 文档版本信息

| 版本 | 日期 | 作者    | 变更说明 |
|------|------|-------|----------|
| 1.0.0 | 2026-03-06 | 前端111 | 初始版本，完整接口定义 |

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [前端完成情况](#3-前端完成情况)
4. [后端功能需求](#4-后端功能需求)
5. [相机功能接口](#5-相机功能接口)
6. [AI 识别接口](#6-ai-识别接口)
7. [智能裁剪接口](#7-智能裁剪接口)
8. [调色处理接口](#8-调色处理接口)
9. [数据存储接口](#9-数据存储接口)
10. [实现指南](#10-实现指南)
11. [性能优化](#11-性能优化)
12. [测试验收标准](#12-测试验收标准)

---

## 1. 项目概述

### 1.1 项目背景

智能相机 APP 是一款基于 Android 平台的智能摄影辅助应用，聚焦"拍前构图辅助"核心需求，通过 AI 技术为用户提供实时构图建议、场景识别、智能裁剪和智能调色等功能。

### 1.2 核心功能

* **智能构图引导**：实时分析相机预览帧，提供构图建议和参考线
* **场景识别**：自动识别拍摄场景（人像/风景/美食/夜景等）
* **智能裁剪**：拍摄后自动识别主体，提供最佳裁剪建议
* **AI 智能调色**：基于美学模型自动优化照片参数
* **参数控制**：支持 ISO、快门、曝光补偿等参数调节

### 1.3 设计原则

* **本地化**：所有核心功能在设备端完成，无需依赖远程服务器
* **轻量化**：使用 ONNX 模型，确保低延迟和高性能
* **高可用**：采用成熟的 Android 生态工具链（CameraX、ML Kit、OpenCV）

---

## 2. 技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                   UI 层                              │
│         (Jetpack Compose)                           │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │Camera    │Edit      │Crop      │Color     │   │
│  │Screen    │Screen    │Screen    │Screen    │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│              业务逻辑层                              │
│         (Kotlin + ViewModel)                       │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │Camera    │AI        │Image     │Data      │   │
│  │Manager   │Analyzer  │Processor │Manager   │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│            AI 与图像处理层                          │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │ONNX      │ML Kit    │OpenCV    │GPUImage  │   │
│  │Models    │Face Det  │Geometry  │Filters   │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
└─────────────────────────────────────────────────────┘
```

### 2.2 技术栈

#### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Jetpack Compose | 1.5.4 | 声明式 UI 框架 |
| Kotlin | 1.9+ | 开发语言 |
| CameraX | 1.1.0-beta01 | 相机 API |
| Material3 | 2024.09.00 | UI 组件库 |
| Coil | 2.4.0 | 图片加载库 |

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| ONNX Runtime | Latest | AI 模型推理 |
| ML Kit | 16.1.6+ | 人脸检测、场景识别 |
| OpenCV | 4.x | 图像处理、几何分析 |
| GPUImage | 2.1.0 | GPU 加速滤镜 |
| TensorFlow Lite | 2.14.0 | 备选 AI 框架 |

### 2.3 前后端职责划分

| 模块 | 前端职责 | 后端职责 |
|------|----------|----------|
| **UI 界面** | ✅ 所有界面和组件 | - |
| **导航逻辑** | ✅ 屏幕跳转和路由 | - |
| **相机预览** | ✅ 预览显示 | ✅ 相机初始化和控制 |
| **拍照功能** | ✅ 按钮交互 | ✅ 图片保存和处理 |
| **AI 识别** | ✅ 结果显示 | ✅ 模型推理和分析 |
| **构图引导** | ✅ 参考线绘制 | ✅ 构图分析算法 |
| **智能裁剪** | ✅ 裁剪框交互 | ✅ 主体识别和坐标计算 |
| **调色处理** | ✅ 参数调节 UI | ✅ 滤镜应用和优化 |
| **数据存储** | ✅ 缓存管理 | ✅ 文件保存和 Exif 处理 |

---

## 3. 前端完成情况

### 3.1 已实现界面

| 界面 | 文件路径 | 状态 | 说明 |
|------|----------|------|------|
| 启动界面 | `SplashScreen.kt` | ✅ 完成 | 功能介绍和启动引导 |
| 相机界面 | `CameraScreen.kt` | ✅ 完成 | 预览、构图引导、参数显示 |
| 编辑界面 | `EditScreen.kt` | ✅ 完成 | 图片预览和工具选择 |
| 裁剪界面 | `CropScreen.kt` | ✅ 完成 | 裁剪框交互和 AI 建议 |
| 调色界面 | `ColorAdjustScreen.kt` | ✅ 完成 | 参数调节和 AI 增强 |
| 设置界面 | `SettingsScreen.kt` | ✅ 完成 | 相机参数设置 |

### 3.2 已实现组件

| 组件 | 功能 | 使用场景 |
|------|------|----------|
| `CircleIconButton` | 圆形图标按钮 | 工具栏按钮 |
| `PrimaryButton` | 主按钮 | 确认、应用等操作 |
| `FeatureCard` | 功能卡片 | 功能列表展示 |
| `TopBarWithActions` | 顶部操作栏 | 编辑、裁剪等界面 |
| `ToolButton` | 工具按钮 | 底部工具栏 |
| `LabeledSlider` | 带标签滑块 | 参数调节 |
| `AIEnhanceCard` | AI 增强卡片 | AI 功能入口 |
| `StatusLabel` | 状态标签 | 场景识别显示 |
| `CaptureButton` | 拍摄按钮 | 相机界面 |
| `CameraParams` | 相机参数显示 | ISO、快门、光圈 |

### 3.3 导航流程

```
SplashScreen (启动)
    │
    ▼
CameraScreen (相机主界面)
    │
    ├─→ SettingsScreen (设置)
    │
    └─→ EditScreen (编辑)
          │
          ├─→ CropScreen (裁剪)
          │
          └─→ ColorAdjustScreen (调色)
```

### 3.4 待对接点

| 界面 | 待对接功能 | 优先级 |
|------|-----------|--------|
| CameraScreen | 拍照保存、场景识别、构图分析、参数获取 | P0 |
| CropScreen | AI 裁剪识别、裁剪执行 | P0 |
| ColorAdjustScreen | AI 调色、滤镜应用 | P0 |
| SettingsScreen | 参数同步到相机 | P1 |

---

## 4. 后端功能需求

### 4.1 功能优先级

#### 🔴 P0 核心功能（必须实现）

1. **相机基础功能**
   * 拍照并保存图片
   * 获取相机参数（ISO、快门、光圈）
   * 闪光灯控制
   * 前后摄像头切换

2. **AI 识别功能**
   * 场景识别（人像/风景/美食/夜景等）
   * AI 构图分析（实时预览帧分析）
   * 人脸检测与关键点定位
   * 构图评分和建议生成

3. **图片处理功能**
   * AI 智能裁剪识别
   * 图片裁剪执行
   * AI 智能调色
   * 滤镜应用

#### 🟡 P1 重要功能（建议实现）

1. **高级相机功能**
   * HDR 模式控制
   * 定时器功能
   * 对焦控制
   * 变焦控制

2. **数据管理功能**
   * 图片保存到相册
   * 缓存管理
   * Exif 信息处理

#### 🟢 P2 增强功能（可选实现）

1. **性能优化**
   * 智能帧率控制
   * 模型缓存
   * GPU 加速

2. **用户体验**
   * 语音播报
   * 手势控制
   * 快捷操作

### 4.2 接口设计原则

1. **本地优先**：所有功能优先在设备端完成，减少网络依赖
2. **异步处理**：耗时操作必须在后台线程执行，避免阻塞 UI
3. **错误处理**：完善的错误处理和降级策略
4. **性能优化**：合理控制分析频率，平衡准确性和性能
5. **状态管理**：使用状态驱动 UI，确保数据一致性

---

## 5. 相机功能接口

### 5.1 拍照功能

#### 功能描述

用户点击拍摄按钮后，捕获当前相机预览帧并保存为图片文件，返回图片 URI 供后续编辑使用。

#### 接口定义

```kotlin
/**
 * 拍照并保存图片
 *
 * @param context 应用上下文
 * @param imageCapture CameraX ImageCapture 实例
 * @param onSuccess 成功回调，返回图片 URI
 * @param onError 失败回调，返回错误信息
 */
fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
)
```

#### 实现示例

```kotlin
fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    if (imageCapture == null) {
        onError("相机未初始化")
        return
    }

    val photoFile = File(
        context.externalCacheDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(photoFile)
        .build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("Camera", "拍照成功: ${photoFile.absolutePath}")
                onSuccess(photoFile.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "拍照失败", exception)
                onError("拍照失败: ${exception.message}")
            }
        }
    )
}
```

#### 调用位置

在 `CameraScreen.kt` 的 `BottomControls` 组件的 `onCapture` 回调中：

```kotlin
onCapture = {
    capturePhoto(context, imageCapture, 
        onSuccess = { uri ->
            onNavigateToEdit(uri)
        },
        onError = { error ->
            // 显示错误提示
        }
    )
}
```

#### 注意事项

* 确保在调用前已获取相机权限
* 保存路径使用 `externalCacheDir`，确保应用有写入权限
* 文件名使用时间戳避免冲突
* 质量设置为 `CAPTURE_MODE_MAXIMIZE_QUALITY`

---

### 5.2 相机参数获取

#### 功能描述

实时获取相机参数（ISO、快门速度、光圈值），并在界面底部显示。

#### 接口定义

```kotlin
/**
 * 相机参数数据类
 */
data class CameraParams(
    val iso: String,        // ISO 值，如 "100", "400", "800"
    val shutter: String,     // 快门速度，如 "1/125s", "1/500s"
    val aperture: String      // 光圈值，如 "f/1.8", "f/2.4"
)

/**
 * 获取相机参数
 *
 * @param camera CameraX Camera 实例
 * @return 相机参数
 */
fun getCameraParams(camera: Camera?): CameraParams
```

#### 实现示例

```kotlin
fun getCameraParams(camera: Camera?): CameraParams {
    if (camera == null) {
        return CameraParams("Auto", "Auto", "Auto")
    }

    val cameraInfo = camera.cameraInfo

    val iso = try {
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        val isoValue = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_SENSITIVITY
        )
        isoValue?.toString() ?: "Auto"
    } catch (e: Exception) {
        "Auto"
    }

    val exposureTime = cameraInfo.exposureState?.exposureCompensationIndex
    val shutter = exposureTimeToShutterSpeed(exposureTime)

    val aperture = try {
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        val apertureValue = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_APERTURE
        )
        apertureValue?.let { "f/$it" } ?: "f/1.8"
    } catch (e: Exception) {
        "f/1.8"
    }

    return CameraParams(iso, shutter, aperture)
}

private fun exposureTimeToShutterSpeed(exposureIndex: Int?): String {
    if (exposureIndex == null) return "Auto"

    val baseShutter = 1.0 / 125.0
    val factor = Math.pow(2.0, exposureIndex.toDouble())
    val shutterTime = baseShutter * factor

    return "1/${(1.0 / shutterTime).toInt()}s"
}
```

#### 调用位置

在 `CameraScreen.kt` 中定期更新参数：

```kotlin
LaunchedEffect(camera) {
    camera?.let {
        while (true) {
            val params = getCameraParams(it)
            iso = params.iso
            shutter = params.shutter
            aperture = params.aperture
            delay(100)
        }
    }
}
```

#### 注意事项

* 部分手机不支持获取 ISO 和光圈，需要降级处理
* 曝光补偿索引需要转换为实际的快门速度
* 更新频率不宜过高，避免性能问题

---

### 5.3 闪光灯控制

#### 功能描述

控制相机闪光灯的开关状态，支持自动、开启、关闭三种模式。

#### 接口定义

```kotlin
/**
 * 闪光灯模式
 */
enum class FlashMode {
    AUTO,   // 自动
    ON,     // 开启
    OFF     // 关闭
}

/**
 * 设置闪光灯模式
 *
 * @param imageCapture CameraX ImageCapture 实例
 * @param mode 闪光灯模式
 */
fun setFlashMode(
    imageCapture: ImageCapture?,
    mode: FlashMode
)
```

#### 实现示例

```kotlin
fun setFlashMode(
    imageCapture: ImageCapture?,
    mode: FlashMode
) {
    if (imageCapture == null) return

    val flashMode = when (mode) {
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
    }

    imageCapture.flashMode = flashMode
    Log.d("Camera", "闪光灯模式: $mode")
}
```

#### 调用位置

在 `CameraScreen.kt` 中响应闪光灯切换：

```kotlin
LaunchedEffect(flashEnabled) {
    val mode = if (flashEnabled) FlashMode.ON else FlashMode.OFF
    setFlashMode(imageCapture, mode)
}
```

#### 注意事项

* 切换闪光灯模式需要重新绑定相机
* 某些设备可能不支持自动模式
* 闪光灯状态需要与 UI 同步

---

### 5.4 前后摄像头切换

#### 功能描述

在前后摄像头之间切换，重新绑定相机预览。

#### 接口定义

```kotlin
/**
 * 切换摄像头
 *
 * @param cameraProvider CameraX ProcessCameraProvider 实例
 * @param lifecycleOwner 生命周期所有者
 * @param preview Preview 实例
 * @param imageCapture ImageCapture 实例
 * @param currentFacing 当前摄像头朝向
 * @return 新的摄像头朝向
 */
fun switchCamera(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    preview: Preview,
    imageCapture: ImageCapture,
    currentFacing: Int
): Int
```

#### 实现示例

```kotlin
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

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(newFacing)
        .build()

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        Log.d("Camera", "切换到${if (newFacing == CameraSelector.LENS_FACING_FRONT) "前" else "后"}摄像头")
    } catch (e: Exception) {
        Log.e("Camera", "切换失败", e)
        throw e
    }

    return newFacing
}
```

#### 调用位置

在 `CameraScreen.kt` 中响应切换按钮：

```kotlin
var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

CircleIconButton(
    icon = Icons.Default.FlipCameraAndroid,
    onClick = {
        lensFacing = switchCamera(
            cameraProvider,
            lifecycleOwner,
            preview,
            imageCapture!!,
            lensFacing
        )
    },
    contentDescription = "切换摄像头"
)
```

#### 注意事项

* 切换摄像头需要重新绑定所有用例
* 切换过程可能有短暂黑屏，需要提示用户
* 前后摄像头的参数可能不同，需要重新获取

---

## 6. AI 识别接口

### 6.1 场景识别

#### 功能描述

实时分析相机预览帧，识别当前拍摄场景类型（人像/风景/美食/夜景等），并在界面顶部显示。

#### 接口定义

```kotlin
/**
 * 场景类型枚举
 */
enum class SceneType {
    PORTRAIT,   // 人像
    LANDSCAPE,  // 风景
    FOOD,       // 美食
    NIGHT,      // 夜景
    ARCHITECTURE, // 建筑
    AUTO        // 通用
}

/**
 * 场景识别结果
 */
data class SceneDetectionResult(
    val sceneType: SceneType,
    val confidence: Float,        // 置信度 0.0-1.0
    val detectedObjects: List<String>, // 检测到的对象
    val recommendedSettings: CameraSettings // 推荐的相机设置
)

/**
 * 相机设置建议
 */
data class CameraSettings(
    val iso: Int?,
    val shutterSpeed: String?,
    val aperture: String?
)

/**
 * 场景识别
 *
 * @param imageProxy 相机预览帧
 * @return 场景识别结果
 */
fun detectScene(imageProxy: ImageProxy): SceneDetectionResult
```

#### 实现示例（方案 A：使用 ML Kit）

```kotlin
fun detectScene(imageProxy: ImageProxy): SceneDetectionResult {
    val image = InputImage.fromMediaImage(
        imageProxy.image!!,
        imageProxy.imageInfo.rotationDegrees
    )

    val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
    )

    var detectedScene = SceneType.AUTO
    var maxConfidence = 0f
    val detectedObjects = mutableListOf<String>()

    labeler.process(image)
        .addOnSuccessListener { labels ->
            labels.forEach { label ->
                detectedObjects.add(label.text)
                when {
                    label.text.contains("Person", ignoreCase = true) && 
                    label.confidence > maxConfidence -> {
                        detectedScene = SceneType.PORTRAIT
                        maxConfidence = label.confidence
                    }
                    label.text.contains("Food", ignoreCase = true) && 
                    label.confidence > maxConfidence -> {
                        detectedScene = SceneType.FOOD
                        maxConfidence = label.confidence
                    }
                    label.text.contains("Landscape", ignoreCase = true) && 
                    label.confidence > maxConfidence -> {
                        detectedScene = SceneType.LANDSCAPE
                        maxConfidence = label.confidence
                    }
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("SceneDetection", "识别失败", e)
        }

    val settings = when (detectedScene) {
        SceneType.PORTRAIT -> CameraSettings(100, "1/125s", "f/1.8")
        SceneType.LANDSCAPE -> CameraSettings(200, "1/250s", "f/8.0")
        SceneType.NIGHT -> CameraSettings(800, "1/60s", "f/1.8")
        else -> CameraSettings(null, null, null)
    }

    return SceneDetectionResult(
        sceneType = detectedScene,
        confidence = maxConfidence,
        detectedObjects = detectedObjects,
        recommendedSettings = settings
    )
}
```

#### 实现示例（方案 B：使用 ONNX 模型）

```kotlin
class SceneDetector(context: Context) {
    private val interpreter: Interpreter
    private val labels = listOf(
        "portrait", "landscape", "food", "night", "architecture"
    )

    init {
        val model = loadModelFile(context, "scene_classifier.onnx")
        val options = OrtSession.SessionOptions()
        interpreter = OrtEnvironment.getEnvironment().createSession(model, options)
    }

    fun detect(imageProxy: ImageProxy): SceneDetectionResult {
        val bitmap = imageProxy.toBitmap()
        val input = preprocessImage(bitmap, 224, 224)

        val inputTensor = OrtTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            input,
            longArrayOf(1, 3, 224, 224)
        )

        val inputs = mapOf("input" to inputTensor)
        val outputs = interpreter.run(inputs)

        val probabilities = outputs[0].value as Array<FloatArray>
        val maxIndex = probabilities[0].indices.maxByOrNull { probabilities[0][it] } ?: 0
        val confidence = probabilities[0][maxIndex]

        val sceneType = when (maxIndex) {
            0 -> SceneType.PORTRAIT
            1 -> SceneType.LANDSCAPE
            2 -> SceneType.FOOD
            3 -> SceneType.NIGHT
            4 -> SceneType.ARCHITECTURE
            else -> SceneType.AUTO
        }

        return SceneDetectionResult(
            sceneType = sceneType,
            confidence = confidence,
            detectedObjects = listOf(labels[maxIndex]),
            recommendedSettings = getRecommendedSettings(sceneType)
        )
    }

    private fun preprocessImage(bitmap: Bitmap, width: Int, height: Int): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        val input = FloatArray(3 * width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            input[i * 3] = ((pixel shr 16 and 0xFF) / 255.0f)
            input[i * 3 + 1] = ((pixel shr 8 and 0xFF) / 255.0f)
            input[i * 3 + 2] = ((pixel and 0xFF) / 255.0f)
        }

        return input
    }
}
```

#### 调用位置

在 `CameraScreen.kt` 中定期检测场景：

```kotlin
var sceneType by remember { mutableStateOf("通用拍摄") }

LaunchedEffect(Unit) {
    while (true) {
        val frame = getCurrentPreviewFrame()
        val result = detectScene(frame)
        
        sceneType = when (result.sceneType) {
            SceneType.PORTRAIT -> "人像拍摄"
            SceneType.LANDSCAPE -> "风景拍摄"
            SceneType.FOOD -> "美食拍摄"
            SceneType.NIGHT -> "夜景拍摄"
            else -> "通用拍摄"
        }
        
        delay(500)
    }
}
```

#### 注意事项

* 检测频率不宜过高，建议 2-5 FPS
* 使用低分辨率图片（如 224x224）提高速度
* 异步处理，避免阻塞 UI
* 置信度阈值建议设置为 0.7

---

### 6.2 AI 构图分析

#### 功能描述

实时分析相机预览帧，检测人脸/主体位置，提供构图建议和评分。

#### 接口定义

```kotlin
/**
 * 构图建议类型
 */
enum class SuggestionType {
    POSITION,  // 位置调整
    ANGLE,     // 角度调整
    DISTANCE,  // 距离调整
    LIGHTING   // 光线调整
}

/**
 * 构图建议优先级
 */
enum class SuggestionPriority {
    HIGH,    // 高
    MEDIUM,  // 中
    LOW      // 低
}

/**
 * 构图建议
 */
data class CompositionSuggestion(
    val type: SuggestionType,
    val message: String,
    val confidence: Float,        // 置信度 0.0-1.0
    val priority: SuggestionPriority
)

/**
 * 检测到的人脸信息
 */
data class DetectedFace(
    val x: Float,      // 相对位置 0.0-1.0
    val y: Float,
    val width: Float,
    val height: Float,
    val eyeY: Float,   // 眼睛的 Y 坐标
    val confidence: Float
)

/**
 * 构图分析结果
 */
data class CompositionAnalysisResult(
    val success: Boolean,
    val suggestions: List<CompositionSuggestion>,
    val compositionScore: Float,  // 当前构图评分 0.0-1.0
    val idealScore: Float,        // 理想构图评分
    val detectedFaces: List<DetectedFace>
)

/**
 * AI 构图分析
 *
 * @param imageProxy 相机预览帧
 * @param sceneType 当前场景类型
 * @return 构图分析结果
 */
fun analyzeComposition(
    imageProxy: ImageProxy,
    sceneType: SceneType
): CompositionAnalysisResult
```

#### 实现示例（方案 A：使用 ML Kit 人脸检测）

```kotlin
fun analyzeComposition(
    imageProxy: ImageProxy,
    sceneType: SceneType
): CompositionAnalysisResult {
    val image = InputImage.fromMediaImage(
        imageProxy.image!!,
        imageProxy.imageInfo.rotationDegrees
    )

    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(options)

    var suggestions = mutableListOf<CompositionSuggestion>()
    var detectedFaces = mutableListOf<DetectedFace>()

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val eyeY = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.y ?: 0f
                val rightEyeY = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.y ?: 0f
                val avgEyeY = (eyeY + rightEyeY) / 2f

                val relativeEyeY = avgEyeY / imageProxy.height
                val idealY = 0.33f
                val diff = idealY - relativeEyeY

                val faceWidth = face.boundingBox.width()
                val faceHeight = face.boundingBox.height()
                val relativeFaceWidth = faceWidth / imageProxy.width

                detectedFaces.add(
                    DetectedFace(
                        x = face.boundingBox.left.toFloat() / imageProxy.width,
                        y = face.boundingBox.top.toFloat() / imageProxy.height,
                        width = relativeFaceWidth,
                        height = faceHeight / imageProxy.height,
                        eyeY = relativeEyeY,
                        confidence = face.trackingId?.toFloat() ?: 0f
                    )
                )

                when {
                    diff > 0.05 -> {
                        suggestions.add(
                            CompositionSuggestion(
                                type = SuggestionType.POSITION,
                                message = "📍 向上移动，将人物眼睛对准上方第一条线",
                                confidence = 0.85f,
                                priority = SuggestionPriority.HIGH
                            )
                        )
                    }
                    diff < -0.05 -> {
                        suggestions.add(
                            CompositionSuggestion(
                                type = SuggestionType.POSITION,
                                message = "📍 向下移动，将人物眼睛对准上方第一条线",
                                confidence = 0.85f,
                                priority = SuggestionPriority.HIGH
                            )
                        )
                    }
                    relativeFaceWidth < 0.3 -> {
                        suggestions.add(
                            CompositionSuggestion(
                                type = SuggestionType.DISTANCE,
                                message = "稍微靠近一点，填充更多画面",
                                confidence = 0.72f,
                                priority = SuggestionPriority.MEDIUM
                            )
                        )
                    }
                    relativeFaceWidth > 0.6 -> {
                        suggestions.add(
                            CompositionSuggestion(
                                type = SuggestionType.DISTANCE,
                                message = "稍微远一点，留出背景空间",
                                confidence = 0.72f,
                                priority = SuggestionPriority.MEDIUM
                            )
                        )
                    }
                }

                val score = 1.0f - minOf(abs(diff) * 2f, 1.0f)
                return CompositionAnalysisResult(
                    success = true,
                    suggestions = suggestions,
                    compositionScore = score,
                    idealScore = 0.90f,
                    detectedFaces = detectedFaces
                )
            } else {
                suggestions.add(
                    CompositionSuggestion(
                        type = SuggestionType.POSITION,
                        message = "将主体放在画面中心",
                        confidence = 0.5f,
                        priority = SuggestionPriority.LOW
                    )
                )

                return CompositionAnalysisResult(
                    success = true,
                    suggestions = suggestions,
                    compositionScore = 0.5f,
                    idealScore = 0.90f,
                    detectedFaces = emptyList()
                )
            }
        }
        .addOnFailureListener { e ->
            Log.e("Composition", "分析失败", e)
            return CompositionAnalysisResult(
                success = false,
                suggestions = emptyList(),
                compositionScore = 0f,
                idealScore = 0.90f,
                detectedFaces = emptyList()
            )
        }

    return CompositionAnalysisResult(
        success = false,
        suggestions = emptyList(),
        compositionScore = 0f,
        idealScore = 0.90f,
        detectedFaces = emptyList()
    )
}
```

#### 实现示例（方案 B：使用 ONNX 模型）

```kotlin
class CompositionAnalyzer(context: Context) {
    private val session: OrtSession

    init {
        val model = loadModelFile(context, "composition_analyzer.onnx")
        val options = OrtSession.SessionOptions()
        session = OrtEnvironment.getEnvironment().createSession(model, options)
    }

    fun analyze(imageProxy: ImageProxy): CompositionAnalysisResult {
        val bitmap = imageProxy.toBitmap()
        val input = preprocessImage(bitmap, 640, 480)

        val inputTensor = OrtTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            input,
            longArrayOf(1, 3, 640, 480)
        )

        val inputs = mapOf("input" to inputTensor)
        val outputs = session.run(inputs)

        val suggestions = mutableListOf<CompositionSuggestion>()
        val score = (outputs["score"]?.value as FloatArray)[0]

        val positionScore = (outputs["position_score"]?.value as FloatArray)[0]
        val distanceScore = (outputs["distance_score"]?.value as FloatArray)[0]

        if (positionScore < 0.7) {
            suggestions.add(
                CompositionSuggestion(
                    type = SuggestionType.POSITION,
                    message = "调整主体位置，使其位于画面三分之一处",
                    confidence = 1.0f - positionScore,
                    priority = SuggestionPriority.HIGH
                )
            )
        }

        if (distanceScore < 0.7) {
            suggestions.add(
                CompositionSuggestion(
                    type = SuggestionType.DISTANCE,
                    message = "调整拍摄距离，优化画面构图",
                    confidence = 1.0f - distanceScore,
                    priority = SuggestionPriority.MEDIUM
                )
            )
        }

        return CompositionAnalysisResult(
            success = true,
            suggestions = suggestions,
            compositionScore = score,
            idealScore = 0.90f,
            detectedFaces = emptyList()
        )
    }
}
```

#### 调用位置

在 `CameraScreen.kt` 中定期分析构图：

```kotlin
var compositionTip by remember { mutableStateOf("") }
var showCompositionTip by remember { mutableStateOf(false) }

LaunchedEffect(showGuides) {
    if (showGuides) {
        while (true) {
            delay(2000)

            val frame = getCurrentPreviewFrame()
            val result = analyzeComposition(frame, sceneType)

            if (result.success && result.suggestions.isNotEmpty()) {
                val highPrioritySuggestion = result.suggestions
                    .firstOrNull { it.priority == SuggestionPriority.HIGH }
                    ?: result.suggestions.first()

                compositionTip = highPrioritySuggestion.message
                showCompositionTip = true

                delay(3000)
                showCompositionTip = false
            }
        }
    }
}
```

#### 注意事项

* 分析频率建议 2-3 秒一次，避免过度消耗性能
* 使用低分辨率图片（如 640x480）提高速度
* 异步处理，不阻塞 UI
* 缓存结果，如果构图变化不大，复用上一次的建议
* 只显示高优先级建议，避免信息过载

---

## 7. 智能裁剪接口

### 7.1 AI 裁剪识别

#### 功能描述

进入裁剪界面后，自动分析照片主体（人脸、物体、文字等），建议最佳裁剪框。

#### 接口定义

```kotlin
/**
 * 裁剪模式
 */
enum class CropMode {
    AUTO,       // 自动
    PORTRAIT,   // 人像
    LANDSCAPE,  // 风景
    SQUARE      // 方形
}

/**
 * 检测到的主体类型
 */
enum class SubjectType {
    FACE,
    UPPER_BODY,
    FULL_BODY,
    OBJECT,
    TEXT,
    UNKNOWN
}

/**
 * 裁剪框坐标（相对位置 0.0-1.0）
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * AI 裁剪识别结果
 */
data class SmartCropResult(
    val success: Boolean,
    val cropRect: CropRect,
    val confidence: Float,        // 识别置信度 0.0-1.0
    val suggestion: String,        // 建议说明
    val detectedSubjects: List<SubjectType>,
    val aspectRatio: String        // 建议的宽高比
)

/**
 * AI 智能裁剪识别
 *
 * @param imageUri 图片路径
 * @param cropMode 裁剪模式
 * @return 裁剪识别结果
 */
fun analyzeSmartCrop(
    imageUri: String,
    cropMode: CropMode = CropMode.AUTO
): SmartCropResult
```

#### 实现示例（方案 A：使用 ML Kit 对象检测）

```kotlin
fun analyzeSmartCrop(
    imageUri: String,
    cropMode: CropMode = CropMode.AUTO
): SmartCropResult {
    val bitmap = BitmapFactory.decodeFile(imageUri)
    val image = InputImage.fromBitmap(bitmap, 0)

    val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    val detector = ObjectDetection.getClient(options)

    return try {
        val detectedObjects = Tasks.await(detector.process(image))

        if (detectedObjects.isNotEmpty()) {
            val mainObject = detectedObjects.maxByOrNull { 
                it.boundingBox.width() * it.boundingBox.height()
            }

            mainObject?.let { obj ->
                val padding = 0.1f
                val box = obj.boundingBox

                val left = (box.left.toFloat() / bitmap.width - padding).coerceIn(0f, 1f)
                val top = (box.top.toFloat() / bitmap.height - padding).coerceIn(0f, 1f)
                val width = ((box.width().toFloat() / bitmap.width) + padding * 2)
                    .coerceIn(0f, 1f - left)
                val height = ((box.height().toFloat() / bitmap.height) + padding * 2)
                    .coerceIn(0f, 1f - top)

                val cropRect = CropRect(left, top, width, height)
                val subjects = obj.labels.mapNotNull { label ->
                    when (label.text.lowercase()) {
                        "person" -> SubjectType.FACE
                        "food" -> SubjectType.OBJECT
                        else -> SubjectType.UNKNOWN
                    }
                }

                val suggestion = when {
                    subjects.contains(SubjectType.FACE) -> 
                        "✨ AI 建议：检测到人像主体，已优化构图"
                    subjects.contains(SubjectType.OBJECT) -> 
                        "✨ AI 建议：检测到主体，已优化裁剪"
                    else -> 
                        "✨ AI 建议：已优化构图"
                }

                val aspectRatio = when (cropMode) {
                    CropMode.SQUARE -> "1:1"
                    CropMode.PORTRAIT -> "3:4"
                    CropMode.LANDSCAPE -> "4:3"
                    else -> "4:3"
                }

                SmartCropResult(
                    success = true,
                    cropRect = cropRect,
                    confidence = 0.92f,
                    suggestion = suggestion,
                    detectedSubjects = subjects,
                    aspectRatio = aspectRatio
                )
            } ?: getDefaultCropResult()
        } else {
            getDefaultCropResult()
        }
    } catch (e: Exception) {
        Log.e("SmartCrop", "分析失败", e)
        getDefaultCropResult()
    }
}

private fun getDefaultCropResult() = SmartCropResult(
    success = false,
    cropRect = CropRect(0.1f, 0.2f, 0.8f, 0.6f),
    confidence = 0f,
    suggestion = "AI 分析失败，请手动调整",
    detectedSubjects = emptyList(),
    aspectRatio = "4:3"
)
```

#### 实现示例（方案 B：使用 ML Kit 人脸检测）

```kotlin
fun analyzePortraitCrop(imageUri: String): SmartCropResult {
    val bitmap = BitmapFactory.decodeFile(imageUri)
    val image = InputImage.fromBitmap(bitmap, 0)

    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(options)

    return try {
        val faces = Tasks.await(detector.process(image))

        if (faces.isNotEmpty()) {
            val mainFace = faces.maxByOrNull { 
                it.boundingBox.width() * it.boundingBox.height()
            }

            mainFace?.let { face ->
                val box = face.boundingBox

                val eyeY = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.y 
                    ?: box.centerY().toFloat()

                val cropHeight = bitmap.height * 0.8f
                val cropTop = (eyeY - cropHeight / 3)
                    .coerceIn(0f, bitmap.height - cropHeight)

                val cropWidth = (cropHeight * 0.75f)
                    .coerceIn(0f, bitmap.width.toFloat())
                val cropLeft = (box.centerX() - cropWidth / 2)
                    .coerceIn(0f, bitmap.width - cropWidth)

                val cropRect = CropRect(
                    left = cropLeft / bitmap.width,
                    top = cropTop / bitmap.height,
                    width = cropWidth / bitmap.width,
                    height = cropHeight / bitmap.height
                )

                SmartCropResult(
                    success = true,
                    cropRect = cropRect,
                    confidence = 0.95f,
                    suggestion = "✨ AI 建议：检测到人像主体，已优化构图",
                    detectedSubjects = listOf(SubjectType.FACE),
                    aspectRatio = "3:4"
                )
            } ?: getDefaultCropResult()
        } else {
            getDefaultCropResult()
        }
    } catch (e: Exception) {
        Log.e("SmartCrop", "人脸检测失败", e)
        getDefaultCropResult()
    }
}
```

#### 调用位置

在 `CropScreen.kt` 中调用 AI 分析：

```kotlin
var isAIProcessing by remember { mutableStateOf(true) }
var aiSuggestion by remember { mutableStateOf("AI 正在分析图片...") }
var cropRect by remember { mutableStateOf(CropRect(0.1f, 0.2f, 0.8f, 0.6f)) }

LaunchedEffect(Unit) {
    try {
        val result = analyzeSmartCrop(imageUri, CropMode.AUTO)
        cropRect = result.cropRect
        aiSuggestion = result.suggestion
    } catch (e: Exception) {
        Log.e("CropScreen", "AI 分析失败", e)
        aiSuggestion = "AI 分析失败，请手动调整"
    } finally {
        isAIProcessing = false
    }
}
```

#### 注意事项

* 图片压缩到合理尺寸（如 1024x768）加快分析
* 缓存分析结果，避免重复计算
* 异步处理，显示加载动画
* 设置超时（建议 5 秒），超时后使用默认裁剪框
* 提供手动调整功能，用户可以修改 AI 建议的裁剪框

---

### 7.2 图片裁剪执行

#### 功能描述

根据裁剪框坐标，执行图片裁剪并保存结果。

#### 接口定义

```kotlin
/**
 * 裁剪图片
 *
 * @param imageUri 原始图片路径
 * @param cropRect 裁剪框坐标（相对位置 0.0-1.0）
 * @param outputQuality 输出质量 0-100
 * @return 裁剪后的图片路径
 */
fun cropImage(
    imageUri: String,
    cropRect: CropRect,
    outputQuality: Int = 95
): String
```

#### 实现示例

```kotlin
fun cropImage(
    imageUri: String,
    cropRect: CropRect,
    outputQuality: Int = 95
): String {
    val originalBitmap = BitmapFactory.decodeFile(imageUri)
    
    val left = (cropRect.left * originalBitmap.width).toInt()
    val top = (cropRect.top * originalBitmap.height).toInt()
    val width = (cropRect.width * originalBitmap.width).toInt()
    val height = (cropRect.height * originalBitmap.height).toInt()

    val croppedBitmap = Bitmap.createBitmap(
        originalBitmap,
        left,
        top,
        width,
        height
    )

    val outputFile = File(
        originalBitmap.outputFile?.parentFile,
        "cropped_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(outputFile).use { out ->
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, outputQuality, out)
    }

    Log.d("Crop", "裁剪成功: ${outputFile.absolutePath}")
    return outputFile.absolutePath
}
```

#### 调用位置

在 `CropScreen.kt` 的确认按钮回调中：

```kotlin
onConfirm = {
    try {
        val croppedUri = cropImage(imageUri, cropRect, 95)
        onConfirm()
    } catch (e: Exception) {
        Log.e("CropScreen", "裁剪失败", e)
    }
}
```

#### 注意事项

* 确保裁剪框坐标在有效范围内
* 输出质量建议 90-95，平衡质量和文件大小
* 保留 Exif 信息（可选）
* 处理裁剪失败的情况，使用原图降级

---

## 8. 调色处理接口

### 8.1 AI 智能调色

#### 功能描述

基于美学模型自动评估照片的曝光、对比度、色彩平衡等维度，输出优化后的调色参数。

#### 接口定义

```kotlin
/**
 * 调色参数
 */
data class ColorAdjustmentParams(
    val exposure: Float,    // 曝光度 -1.0 到 +1.0
    val contrast: Float,    // 对比度 -1.0 到 +1.0
    val saturation: Float,  // 饱和度 -1.0 到 +1.0
    val sharpness: Float,   // 锐化 -1.0 到 +1.0
    val temperature: Float, // 色温 -1.0 到 +1.0
    val highlights: Float   // 高光 -1.0 到 +1.0
)

/**
 * AI 调色结果
 */
data class AIEnhanceResult(
    val success: Boolean,
    val params: ColorAdjustmentParams,
    val detectedInfo: String,  // 检测到的主体信息
    val confidence: Float
)

/**
 * AI 智能调色
 *
 * @param imageUri 图片路径
 * @return 调色结果
 */
fun analyzeColorEnhancement(imageUri: String): AIEnhanceResult
```

#### 实现示例（方案 A：使用 ONNX 模型）

```kotlin
class ColorEnhancer(context: Context) {
    private val session: OrtSession

    init {
        val model = loadModelFile(context, "color_enhancer.onnx")
        val options = OrtSession.SessionOptions()
        session = OrtEnvironment.getEnvironment().createSession(model, options)
    }

    fun enhance(imageUri: String): AIEnhanceResult {
        val bitmap = BitmapFactory.decodeFile(imageUri)
        val input = preprocessImage(bitmap, 224, 224)

        val inputTensor = OrtTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            input,
            longArrayOf(1, 3, 224, 224)
        )

        val inputs = mapOf("input" to inputTensor)
        val outputs = session.run(inputs)

        val paramsArray = outputs["params"]?.value as FloatArray
        val params = ColorAdjustmentParams(
            exposure = paramsArray[0],
            contrast = paramsArray[1],
            saturation = paramsArray[2],
            sharpness = paramsArray[3],
            temperature = paramsArray[4],
            highlights = paramsArray[5]
        )

        val detectedInfo = when {
            paramsArray[6] > 0.5 -> "人物肖像主体"
            paramsArray[7] > 0.5 -> "风景场景"
            else -> "通用场景"
        }

        return AIEnhanceResult(
            success = true,
            params = params,
            detectedInfo = detectedInfo,
            confidence = 0.88f
        )
    }

    private fun preprocessImage(bitmap: Bitmap, width: Int, height: Int): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        val input = FloatArray(3 * width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            input[i * 3] = ((pixel shr 16 and 0xFF) / 255.0f - 0.5f) * 2f
            input[i * 3 + 1] = ((pixel shr 8 and 0xFF) / 255.0f - 0.5f) * 2f
            input[i * 3 + 2] = ((pixel and 0xFF) / 255.0f - 0.5f) * 2f
        }

        return input
    }
}
```

#### 实现示例（方案 B：使用 ML Kit 图像分析）

```kotlin
fun analyzeColorEnhancement(imageUri: String): AIEnhanceResult {
    val bitmap = BitmapFactory.decodeFile(imageUri)
    val image = InputImage.fromBitmap(bitmap, 0)

    val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
    )

    var detectedInfo = "通用场景"
    var exposure = 0f
    var contrast = 0f
    var saturation = 0f

    labeler.process(image)
        .addOnSuccessListener { labels ->
            labels.forEach { label ->
                when {
                    label.text.contains("Person", ignoreCase = true) -> {
                        detectedInfo = "人物肖像主体"
                        exposure = 0.15f
                        contrast = 0.12f
                        saturation = 0.10f
                    }
                    label.text.contains("Landscape", ignoreCase = true) -> {
                        detectedInfo = "风景场景"
                        exposure = 0.05f
                        contrast = 0.20f
                        saturation = 0.15f
                    }
                    label.text.contains("Food", ignoreCase = true) -> {
                        detectedInfo = "美食场景"
                        exposure = 0.10f
                        contrast = 0.15f
                        saturation = 0.25f
                    }
                }
            }
        }

    val params = ColorAdjustmentParams(
        exposure = exposure,
        contrast = contrast,
        saturation = saturation,
        sharpness = 0f,
        temperature = 0f,
        highlights = 0f
    )

    return AIEnhanceResult(
        success = true,
        params = params,
        detectedInfo = detectedInfo,
        confidence = 0.75f
    )
}
```

#### 调用位置

在 `ColorAdjustScreen.kt` 中调用 AI 增强：

```kotlin
var isAIApplied by remember { mutableStateOf(false) }
var exposure by remember { mutableStateOf(0f) }
var contrast by remember { mutableStateOf(0f) }
var saturation by remember { mutableStateOf(0f) }

AIEnhanceCard(
    detectedInfo = "人物肖像主体",
    onApplyEnhance = {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = analyzeColorEnhancement(imageUri)
                withContext(Dispatchers.Main) {
                    exposure = result.params.exposure
                    contrast = result.params.contrast
                    saturation = result.params.saturation
                    isAIApplied = true
                }
            } catch (e: Exception) {
                Log.e("ColorAdjust", "AI 增强失败", e)
            }
        }
    },
    isApplied = isAIApplied
)
```

#### 注意事项

* 参数范围限制在 -1.0 到 +1.0
* 使用低分辨率图片（如 224x224）提高速度
* 异步处理，避免阻塞 UI
* 提供手动微调功能，用户可以调整 AI 建议的参数

---

### 8.2 滤镜应用

#### 功能描述

根据调色参数，应用滤镜到图片并生成结果。

#### 接口定义

```kotlin
/**
 * 应用调色滤镜
 *
 * @param imageUri 原始图片路径
 * @param params 调色参数
 * @return 处理后的图片路径
 */
fun applyColorAdjustments(
    imageUri: String,
    params: ColorAdjustmentParams
): String
```

#### 实现示例（方案 A：使用 GPUImage）

```kotlin
fun applyColorAdjustments(
    imageUri: String,
    params: ColorAdjustmentParams
): String {
    val bitmap = BitmapFactory.decodeFile(imageUri)
    val gpuImage = GPUImage(context)

    gpuImage.setImage(bitmap)

    val filterGroup = GPUImageFilterGroup()

    if (params.exposure != 0f) {
        filterGroup.addFilter(
            GPUImageExposureFilter(params.exposure * 2f)
        )
    }

    if (params.contrast != 0f) {
        filterGroup.addFilter(
            GPUImageContrastFilter(1.0f + params.contrast * 0.5f)
        )
    }

    if (params.saturation != 0f) {
        filterGroup.addFilter(
            GPUImageSaturationFilter(1.0f + params.saturation * 0.5f)
        )
    }

    if (params.temperature != 0f) {
        filterGroup.addFilter(
            GPUImageWhiteBalanceFilter(
                temperature = params.temperature * 5000f,
                tint = 0f
            )
        )
    }

    if (params.sharpness != 0f) {
        filterGroup.addFilter(
            GPUImageSharpenFilter(
                sharpness = params.sharpness * 2f
            )
        )
    }

    if (params.highlights != 0f) {
        filterGroup.addFilter(
            GPUImageHighlightShadowFilter(
                shadows = params.highlights * 0.5f,
                highlights = params.highlights * 0.5f
            )
        )
    }

    gpuImage.setFilter(filterGroup)
    val resultBitmap = gpuImage.bitmapWithFilterApplied

    val outputFile = File(
        File(imageUri).parentFile,
        "edited_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(outputFile).use { out ->
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }

    Log.d("ColorAdjust", "调色成功: ${outputFile.absolutePath}")
    return outputFile.absolutePath
}
```

#### 实现示例（方案 B：使用 ColorMatrix）

```kotlin
fun applyColorAdjustments(
    imageUri: String,
    params: ColorAdjustmentParams
): String {
    val bitmap = BitmapFactory.decodeFile(imageUri)

    val colorMatrix = ColorMatrix()

    colorMatrix.setSaturation(1.0f + params.saturation * 0.5f)

    val contrast = 1.0f + params.contrast * 0.5f
    val contrastMatrix = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, 0f,
        0f, contrast, 0f, 0f, 0f,
        0f, 0f, contrast, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    colorMatrix.postConcat(contrastMatrix)

    if (params.exposure != 0f) {
        val exposure = params.exposure * 50f
        val exposureMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, exposure,
            0f, 1f, 0f, 0f, exposure,
            0f, 0f, 1f, 0f, exposure,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(exposureMatrix)
    }

    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }

    val resultBitmap = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height,
        bitmap.config
    )

    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    val outputFile = File(
        File(imageUri).parentFile,
        "edited_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(outputFile).use { out ->
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }

    Log.d("ColorAdjust", "调色成功: ${outputFile.absolutePath}")
    return outputFile.absolutePath
}
```

#### 调用位置

在 `ColorAdjustScreen.kt` 的确认按钮回调中：

```kotlin
onConfirm = {
    val params = ColorAdjustmentParams(
        exposure = exposure,
        contrast = contrast,
        saturation = saturation,
        sharpness = sharpness,
        temperature = temperature,
        highlights = highlights
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val editedUri = applyColorAdjustments(imageUri, params)
            withContext(Dispatchers.Main) {
                onConfirm()
            }
        } catch (e: Exception) {
            Log.e("ColorAdjust", "调色失败", e)
        }
    }
}
```

#### 注意事项

* 使用 GPU 加速提高处理速度
* 输出质量建议 90-95
* 处理大图时考虑缩放
* 提供实时预览功能

---

## 9. 数据存储接口

### 9.1 图片保存到相册

#### 功能描述

将处理后的图片保存到系统相册，使其可以在相册应用中查看。

#### 接口定义

```kotlin
/**
 * 保存图片到相册
 *
 * @param context 应用上下文
 * @param imageUri 图片路径
 * @param onSuccess 成功回调
 * @param onError 失败回调
 */
fun saveToGallery(
    context: Context,
    imageUri: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)
```

#### 实现示例

```kotlin
fun saveToGallery(
    context: Context,
    imageUri: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    try {
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                FileInputStream(File(imageUri)).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)

            Log.d("Gallery", "保存成功: $it")
            onSuccess()
        } ?: onError("无法创建相册条目")
    } catch (e: Exception) {
        Log.e("Gallery", "保存失败", e)
        onError("保存失败: ${e.message}")
    }
}
```

#### 调用位置

在编辑界面的确认按钮回调中：

```kotlin
onConfirm = {
    saveToGallery(context, imageUri,
        onSuccess = {
            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        },
        onError = { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    )
}
```

#### 注意事项

* 需要 WRITE_EXTERNAL_STORAGE 权限（Android 10 以下）
* Android 10+ 使用 Scoped Storage
* 保存成功后通知系统扫描媒体库
* 处理保存失败的情况

---

### 9.2 缓存管理

#### 功能描述

管理应用缓存，定期清理临时文件，释放存储空间。

#### 接口定义

```kotlin
/**
 * 清理缓存
 *
 * @param context 应用上下文
 * @return 清理的文件大小（字节）
 */
fun clearCache(context: Context): Long

/**
 * 获取缓存大小
 *
 * @param context 应用上下文
 * @return 缓存大小（字节）
 */
fun getCacheSize(context: Context): Long
```

#### 实现示例

```kotlin
fun clearCache(context: Context): Long {
    var totalSize = 0L

    try {
        val cacheDir = context.cacheDir
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { file ->
                totalSize += file.length()
                file.delete()
            }
        }

        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir.exists() && externalCacheDir.isDirectory) {
            externalCacheDir.listFiles()?.forEach { file ->
                totalSize += file.length()
                file.delete()
            }
        }

        Log.d("Cache", "清理缓存: ${totalSize / 1024} KB")
    } catch (e: Exception) {
        Log.e("Cache", "清理失败", e)
    }

    return totalSize
}

fun getCacheSize(context: Context): Long {
    var totalSize = 0L

    try {
        val cacheDir = context.cacheDir
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { file ->
                totalSize += file.length()
            }
        }

        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir.exists() && externalCacheDir.isDirectory) {
            externalCacheDir.listFiles()?.forEach { file ->
                totalSize += file.length()
            }
        }
    } catch (e: Exception) {
        Log.e("Cache", "获取失败", e)
    }

    return totalSize
}
```

#### 调用位置

在设置界面提供清理缓存选项：

```kotlin
Button(
    onClick = {
        val size = clearCache(context)
        Toast.makeText(
            context,
            "已清理 ${size / 1024} KB",
            Toast.LENGTH_SHORT
        ).show()
    }
) {
    Text("清理缓存")
}
```

#### 注意事项

* 清理前确认用户操作
* 显示清理进度
* 保留重要文件
* 定期自动清理

---

## 10. 实现指南

### 10.1 依赖库清单

在 `app/build.gradle.kts` 中添加以下依赖：

```kotlin
dependencies {
    // CameraX
    val cameraxVersion = "1.1.0-beta01"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")

    // ML Kit
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:object-detection:17.0.1")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:latest")

    // OpenCV
    implementation("org.opencv:opencv-android:4.8.0")

    // GPUImage
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // 图片处理
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 网络请求（可选）
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
```

### 10.2 关键代码示例

#### 10.2.1 相机初始化

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (Camera) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                onCameraReady(camera)
            } catch (e: Exception) {
                Log.e("Camera", "初始化失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
```

#### 10.2.2 AI 模型加载

```kotlin
class ModelLoader(private val context: Context) {
    
    fun loadONNXModel(assetPath: String): OrtSession {
        val modelFile = copyAssetToCache(assetPath)
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setGraphOptimizationLevel(
                GraphOptimizationLevel.ORT_ENABLE_EXTENDED
            )
        }
        return OrtEnvironment.getEnvironment()
            .createSession(modelFile.absolutePath, options)
    }

    private fun copyAssetToCache(assetPath: String): File {
        val outputFile = File(context.cacheDir, assetPath)
        if (!outputFile.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outputFile
    }
}
```

#### 10.2.3 异步处理

```kotlin
@Composable
fun AsyncImageProcessor(
    imageUri: String,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        if (!isProcessing) {
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    val result = processImage(imageUri)
                    withContext(Dispatchers.Main) {
                        onResult(result)
                    }
                } catch (e: Exception) {
                    Log.e("Processor", "处理失败", e)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    if (isProcessing) {
        CircularProgressIndicator()
    }
}
```

### 10.3 错误处理

```kotlin
sealed class AppError {
    data class CameraError(val message: String) : AppError()
    data class AIError(val message: String) : AppError()
    data class StorageError(val message: String) : AppError()
    data class UnknownError(val message: String) : AppError()
}

fun <T> safeExecute(
    onError: (AppError) -> Unit = {},
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: SecurityException) {
        onError(AppError.CameraError("权限不足"))
        null
    } catch (e: IOException) {
        onError(AppError.StorageError("存储失败"))
        null
    } catch (e: Exception) {
        onError(AppError.UnknownError("未知错误: ${e.message}"))
        null
    }
}
```

---

## 11. 性能优化

### 11.1 分析频率控制

```kotlin
class AnalysisController {
    private var lastAnalysisTime = 0L
    private val minInterval = 200L

    fun shouldAnalyze(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime >= minInterval) {
            lastAnalysisTime = currentTime
            return true
        }
        return false
    }
}
```

### 11.2 图片压缩

```kotlin
fun compressImage(
    bitmap: Bitmap,
    maxWidth: Int = 1024,
    maxHeight: Int = 768,
    quality: Int = 85
): Bitmap {
    val widthRatio = maxWidth.toFloat() / bitmap.width
    val heightRatio = maxHeight.toFloat() / bitmap.height
    val ratio = minOf(widthRatio, heightRatio, 1f)

    val newWidth = (bitmap.width * ratio).toInt()
    val newHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
```

### 11.3 模型缓存

```kotlin
class ModelCache {
    private val cache = mutableMapOf<String, OrtSession>()

    fun getModel(key: String, loader: () -> OrtSession): OrtSession {
        return cache.getOrPut(key) {
            loader()
        }
    }

    fun clear() {
        cache.values.forEach { it.close() }
        cache.clear()
    }
}
```

### 11.4 GPU 加速

```kotlin
fun processWithGPU(bitmap: Bitmap): Bitmap {
    val gpuImage = GPUImage(context)
    gpuImage.setImage(bitmap)

    val filterGroup = GPUImageFilterGroup()
    filterGroup.addFilter(GPUImageContrastFilter(1.2f))
    filterGroup.addFilter(GPUImageSaturationFilter(1.1f))

    gpuImage.setFilter(filterGroup)
    return gpuImage.bitmapWithFilterApplied
}
```

---

## 12. 测试验收标准

### 12.1 功能测试清单

#### 相机功能

* [ ] 拍照功能正常，图片保存成功
* [ ] 闪光灯开关有效
* [ ] 前后摄像头切换正常
* [ ] 预览画面流畅（≥ 30 FPS）
* [ ] 相机参数显示正确

#### AI 功能

* [ ] 场景识别准确率 ≥ 80%
* [ ] 识别延迟 < 1 秒
* [ ] 不同光线条件下都能识别
* [ ] 构图分析准确率 ≥ 75%
* [ ] 构图建议合理且有帮助

#### 图片处理

* [ ] 裁剪功能正常
* [ ] 滤镜应用正确
* [ ] 处理速度可接受（< 3 秒）
* [ ] 图片质量良好（无明显失真）
* [ ] AI 调色效果自然

#### 数据存储

* [ ] 图片保存到正确路径
* [ ] 图片可以被相册访问
* [ ] 清理缓存功能正常
* [ ] Exif 信息保留（可选）

### 12.2 性能指标要求

| 指标 | 要求 | 测试方法 |
|------|------|----------|
| 相机预览帧率 | ≥ 30 FPS | 使用性能监控工具 |
| 场景识别延迟 | < 1 秒 | 从获取帧到显示结果的时间 |
| 构图分析延迟 | < 2 秒 | 从获取帧到显示建议的时间 |
| 裁剪识别延迟 | < 3 秒 | 从加载图片到显示裁剪框的时间 |
| 调色处理延迟 | < 2 秒 | 从应用参数到显示结果的时间 |
| 内存占用 | < 300 MB | 使用 Android Profiler |
| CPU 占用 | < 50% | 使用 Android Profiler |

### 12.3 兼容性要求

* Android 版本：7.0 (API 24) 及以上
* 设备要求：支持 Camera2 API
* 最小内存：2 GB RAM
* 存储空间：至少 100 MB 可用空间

---

## 附录

### A. 错误码定义

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 1001 | 相机未初始化 | 检查相机权限和设备支持 |
| 1002 | 拍照失败 | 检查存储权限和空间 |
| 1003 | 闪光灯控制失败 | 检查设备支持 |
| 2001 | AI 模型加载失败 | 检查模型文件和内存 |
| 2002 | 场景识别失败 | 降级到默认场景 |
| 2003 | 构图分析失败 | 静默处理，不显示建议 |
| 3001 | 图片处理失败 | 使用原图降级 |
| 3002 | 裁剪失败 | 使用原图降级 |
| 3003 | 调色失败 | 使用原图降级 |
| 4001 | 存储空间不足 | 提示用户清理空间 |
| 4002 | 保存到相册失败 | 检查权限和设置 |

### B. 常见问题

**Q: 相机预览卡顿怎么办？**

A: 降低预览分辨率，减少 AI 分析频率，使用 GPU 加速。

**Q: AI 识别不准确怎么办？**

A: 调整置信度阈值，使用更好的模型，增加训练数据。

**Q: 图片处理速度慢怎么办？**

A: 压缩图片尺寸，使用 GPU 加速，优化算法。

**Q: 内存占用过高怎么办？**

A: 及时释放 Bitmap，使用对象池，优化模型加载。

### C. 参考资源

* [CameraX 官方文档](https://developer.android.com/training/camerax)
* [ML Kit 官方文档](https://developers.google.com/ml-kit)
* [ONNX Runtime 文档](https://onnxruntime.ai/docs/)
* [Jetpack Compose 文档](https://developer.android.com/jetpack/compose)
* [Android 性能优化指南](https://developer.android.com/topic/performance)

---

**文档结束**

