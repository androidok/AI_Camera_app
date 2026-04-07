package com.aicamera.app.backend.hdr.model

import android.hardware.camera2.CaptureResult
import android.util.Log
import android.util.Pair
import kotlin.math.pow
import kotlin.math.sqrt

class NoiseModeler(
    noiseProfile: Array<Pair<Double, Double>>?,
    private val analogISO: Int,
    private val sensitivityISO: Int,
    private val cfaPattern: Byte,
    private val customParams: FloatArray? = null
) {
    companion object {
        private const val TAG = "NoiseModeler"
        
        private val DEFAULT_S = Pair(0.0000025720647, 0.000028855721)
        private val DEFAULT_O = Pair(0.000000000039798506, 0.000000046578279)
    }
    
    val baseModel: Array<Pair<Double, Double>>
    val computeModel: Array<Pair<Double, Double>>
    
    init {
        baseModel = Array(3) { Pair(0.0, 0.0) }
        computeModel = Array(3) { Pair(0.0, 0.0) }
        
        if (noiseProfile == null || noiseProfile.isEmpty() || noiseProfile[0].first == 0.0) {
            val computedS = computeNoiseModelS(sensitivityISO.toDouble(), DEFAULT_S)
            val computedO = computeNoiseModelO(sensitivityISO.toDouble(), DEFAULT_O)
            
            for (i in 0 until 3) {
                baseModel[i] = Pair(computedS, computedO)
            }
        } else {
            when {
                noiseProfile.size == 1 -> {
                    for (i in 0 until 3) {
                        baseModel[i] = Pair(noiseProfile[0].first, noiseProfile[0].second)
                    }
                }
                noiseProfile.size == 3 -> {
                    for (i in 0 until 3) {
                        baseModel[i] = Pair(noiseProfile[i].first, noiseProfile[i].second)
                    }
                }
                noiseProfile.size == 4 -> {
                    baseModel[0] = Pair(noiseProfile[0].first, noiseProfile[0].second)
                    baseModel[1] = Pair(
                        (noiseProfile[1].first + noiseProfile[2].first) / 2.0,
                        (noiseProfile[1].second + noiseProfile[2].second) / 2.0
                    )
                    baseModel[2] = Pair(noiseProfile[3].first, noiseProfile[3].second)
                }
                else -> {
                    for (i in 0 until 3) {
                        baseModel[i] = Pair(noiseProfile[0].first, noiseProfile[0].second)
                    }
                }
            }
        }
        
        computeStackingNoiseModel(1)
        
        Log.d(TAG, "NoiseModel initialized:")
        Log.d(TAG, "  baseModel[0]: ${baseModel[0]}")
        Log.d(TAG, "  computeModel[0]: ${computeModel[0]}")
    }
    
    fun computeStackingNoiseModel(frameCount: Int) {
        val noiseRemove = frameCount.toDouble().pow(0.9)
        for (i in 0 until 3) {
            computeModel[i] = Pair(
                baseModel[i].first / noiseRemove,
                baseModel[i].second / noiseRemove
            )
        }
    }
    
    private fun computeNoiseModelS(
        sensitivity: Double,
        generator: Pair<Double, Double>
    ): Double {
        val result = generator.first * sensitivity + generator.second
        return if (result < 0.0) {
            Log.w(TAG, "Negative noise model S at sensitivity: $sensitivity")
            kotlin.math.abs(result)
        } else {
            result
        }
    }
    
    private fun computeNoiseModelO(
        sensitivity: Double,
        generator: Pair<Double, Double>
    ): Double {
        val dGain = maxOf(sensitivity / analogISO, 1.0)
        val result = generator.first * sensitivity * sensitivity + generator.second * dGain * dGain
        
        return if (result < 0.0) {
            Log.w(TAG, "Negative noise model O at sensitivity: $sensitivity")
            kotlin.math.abs(result)
        } else {
            result
        }
    }
    
    fun getNoiseS(): Float {
        return ((computeModel[0].first + computeModel[1].first + computeModel[2].first) / 3.0).toFloat()
    }
    
    fun getNoiseO(): Float {
        return ((computeModel[0].second + computeModel[1].second + computeModel[2].second) / 3.0).toFloat()
    }
    
    fun getCombinedNoise(): Float {
        return sqrt(getNoiseS() * getNoiseS() + getNoiseO() * getNoiseO())
    }

    /**
     * 计算ISO自适应降噪强度
     * 根据ISO值返回合适的降噪内核大小
     * 原理：
     * - 正常光线 (ISO < 200): 使用最小降噪，保留细节
     * - 中等光线 (ISO 200-800): 渐进增加降噪强度
     * - 暗光 (ISO > 800): 强力降噪
     *
     * @return 建议的降噪内核大小 (7-21)
     */
    fun getAdaptiveKernelSize(): Int {
        return when {
            sensitivityISO < 200 -> 7   // 正常光线：最小降噪
            sensitivityISO < 400 -> 9   // 轻微噪点
            sensitivityISO < 800 -> 13  // 中等噪点
            sensitivityISO < 1600 -> 17 // 较强噪点
            else -> 21                   // 暗光：最大降噪
        }
    }

    /**
     * 计算ISO自适应降噪强度系数
     * 用于控制降噪着色器中的强度参数
     * 优化：降低降噪强度以保护细节清晰度
     *
     * @return 降噪强度系数 (0.0 - 1.0)
     */
    fun getDenoiseStrength(): Float {
        val baseNoise = getCombinedNoise()
        return when {
            sensitivityISO < 200 -> 0.15f  // 极低降噪，保护细节
            sensitivityISO < 400 -> 0.25f  // 轻度降噪
            sensitivityISO < 800 -> 0.45f  // 中度降噪
            sensitivityISO < 1600 -> 0.65f // 较强降噪
            else -> 0.85f                  // 强力降噪但不过度
        } * (1.0f + baseNoise * 5f).coerceAtMost(1.2f)
    }

    /**
     * 判断是否需要进行降噪处理
     * 在正常光线且噪点水平很低时可以跳过降噪以提升性能
     *
     * @return true 如果需要进行降噪
     */
    fun shouldApplyDenoise(): Boolean {
        // ISO < 150 且噪声模型值很低时跳过降噪
        if (sensitivityISO < 150 && getCombinedNoise() < 0.001f) {
            return false
        }
        return true
    }

    /**
     * 计算自适应锐化强度
     * 高ISO时降低锐化以避免噪点放大
     *
     * @param baseStrength 基础锐化强度 (0.0 - 1.0)
     * @return 调整后的锐化强度
     */
    fun getAdaptiveSharpenStrength(baseStrength: Float = 0.5f): Float {
        // 高ISO时降低锐化强度
        val isoFactor = when {
            sensitivityISO < 200 -> 1.0f
            sensitivityISO < 400 -> 0.8f
            sensitivityISO < 800 -> 0.6f
            sensitivityISO < 1600 -> 0.4f
            else -> 0.25f
        }
        return baseStrength * isoFactor
    }
}
