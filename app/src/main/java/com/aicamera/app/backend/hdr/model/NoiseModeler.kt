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
}
