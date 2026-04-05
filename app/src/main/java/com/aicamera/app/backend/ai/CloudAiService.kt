package com.aicamera.app.backend.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CloudAiService {
    private const val TAG = "CloudAiService"
    private const val API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val MODEL_NAME = "qwen-vl-plus"
    private const val PREFS_NAME = "cloud_ai_prefs"
    private const val KEY_API_KEY = "api_key"

    private var cachedApiKey: String? = null

    fun setApiKey(context: Context, apiKey: String) {
        val encryptedPrefs = SecurePrefs.getEncryptedPrefs(context)
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
        cachedApiKey = apiKey
    }

    fun getApiKey(context: Context): String? {
        if (cachedApiKey != null) return cachedApiKey
        val encryptedPrefs = SecurePrefs.getEncryptedPrefs(context)
        cachedApiKey = encryptedPrefs.getString(KEY_API_KEY, null)
        return cachedApiKey
    }

    fun hasApiKey(context: Context): Boolean {
        return !getApiKey(context).isNullOrBlank()
    }

    fun clearApiKey(context: Context) {
        val encryptedPrefs = SecurePrefs.getEncryptedPrefs(context)
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
        cachedApiKey = null
    }

    suspend fun analyzeScene(
        context: Context,
        bitmap: Bitmap,
        detectedObjects: List<String>,
        currentSettings: CameraSettingsInfo
    ): CloudAiResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "[云端AI] API Key未配置，跳过云端分析")
            return@withContext CloudAiResult(
                success = false,
                suggestions = emptyList(),
                errorMessage = "请先在设置中配置API Key"
            )
        }

        Log.d(TAG, "[云端AI] 开始分析场景，模型: $MODEL_NAME")
        Log.d(TAG, "[云端AI] 检测到物体: ${detectedObjects.joinToString(", ")}")
        Log.d(TAG, "[云端AI] 当前设置: ISO=${currentSettings.iso ?: "自动"}, 快门=${currentSettings.shutterSpeed ?: "自动"}, EV=${currentSettings.ev ?: "自动"}")

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = buildPrompt(detectedObjects, currentSettings)
            
            Log.d(TAG, "[云端AI] 正在调用API...")
            val response = callApi(apiKey, prompt, base64Image)
            val result = parseResponse(response)
            
            if (result.success) {
                Log.i(TAG, "[云端AI] 分析成功，建议: ${result.suggestions.joinToString(", ")}")
            } else {
                Log.w(TAG, "[云端AI] 分析失败: ${result.errorMessage}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "[云端AI] 分析异常: ${e.message}", e)
            CloudAiResult(
                success = false,
                suggestions = emptyList(),
                errorMessage = "AI分析失败: ${e.message}"
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        val scaledBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
            val scale = 512f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildPrompt(detectedObjects: List<String>, currentSettings: CameraSettingsInfo): String {
        val objectsStr = if (detectedObjects.isNotEmpty()) {
            "检测到的物体: ${detectedObjects.joinToString(", ")}"
        } else {
            "未检测到特定物体"
        }

        return """
你是一个专业的摄影指导。请根据照片内容给出简短的拍摄指导。

当前场景信息:
$objectsStr

当前相机设置:
- ISO: ${currentSettings.iso ?: "自动"}
- 快门速度: ${currentSettings.shutterSpeed ?: "自动"}
- 曝光补偿: ${currentSettings.ev ?: "自动"}

请分析照片并给出1条具体的拍摄调整建议。

建议类型及格式要求:
1. 位置调整: "往左移一点"、"往右移一点"、"抬高镜头"、"降低镜头"、"靠近一点"、"后退一点"
2. 对焦调整: "对焦人脸"、"对焦背景"、"点击对焦"
3. 曝光调整(必须指明参数): 
   - 如需调亮: 说"调高ISO"或"调高曝光度"
   - 如需调暗: 说"降低ISO"或"降低曝光度"
   - 当前ISO为自动时优先说"曝光度"
4. 其他操作: "开启闪光灯"、"关闭闪光灯"、"稳定手持"

严格要求:
1. 必须是可立即执行的物理操作或参数调整
2. 不超过10个字
3. 不要描述画面，不要评价照片
4. 曝光类建议必须指明是调ISO还是曝光度

直接输出建议，不要添加任何其他内容。
        """.trimIndent()
    }

    private fun callApi(apiKey: String, prompt: String, base64Image: String): String {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val requestBody = JSONObject().apply {
                put("model", MODEL_NAME)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        })
                    })
                })
                put("max_tokens", 200)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw Exception("API请求失败 ($responseCode): $errorStream")
            }

            return connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(response: String): CloudAiResult {
        val json = JSONObject(response)
        val choices = json.optJSONArray("choices")
        
        if (choices == null || choices.length() == 0) {
            return CloudAiResult(
                success = false,
                suggestions = emptyList(),
                errorMessage = "API返回数据格式错误"
            )
        }

        val content = choices.getJSONObject(0)
            .getJSONObject("message")
            .optString("content", "")
            .trim()

        if (content.isBlank()) {
            return CloudAiResult(
                success = false,
                suggestions = emptyList(),
                errorMessage = "AI未返回有效建议"
            )
        }

        val suggestions = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)

        return CloudAiResult(
            success = true,
            suggestions = suggestions,
            errorMessage = null
        )
    }
}

data class CameraSettingsInfo(
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val ev: Int? = null
)

data class CloudAiResult(
    val success: Boolean,
    val suggestions: List<String>,
    val errorMessage: String?
)
