package com.aicamera.app.backend.hdr.model

import android.graphics.Point
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.BlackLevelPattern
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.LensShadingMap
import android.util.Log
import android.util.Rational
import android.util.SizeF
import java.nio.ByteBuffer

class ProcessingParameters {
    companion object {
        private const val TAG = "ProcessingParameters"
    }
    
    var rawSize: Point = Point(0, 0)
    var iso: Int = 100
    var exposureTime: Double = 1.0 / 30.0
    var cfaPattern: Byte = 0  // 0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR
    
    var blackLevel: FloatArray = floatArrayOf(64f, 64f, 64f, 64f)
    var whiteLevel: Int = 1023
    var rawWhiteLevel: Int = 1023
    
    var hasGainMap: Boolean = false
    var mapSize: Point = Point(1, 1)
    var gainMap: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    
    var sensorPix: Rect = Rect(0, 0, 0, 0)
    var focalLength: Float = 4.75f
    var aperture: Float = 1.8f
    var cameraRotation: Int = 0
    
    var noiseModeler: NoiseModeler? = null
    var sensorSize: SizeF? = null
    
    var whitePoint: FloatArray = floatArrayOf(1f, 1f, 1f)
    var sensorToProPhoto: FloatArray = FloatArray(9)
    var proPhotoToSRGB: FloatArray = FloatArray(9)
    
    var tonemapStrength: Float = 1.4f
    var customTonemap: FloatArray = floatArrayOf(-2f, 3f, 1f, 0f)
    
    var hotPixels: Array<Point>? = null
    
    var calibrationIlluminant1: Int = -1
    var calibrationIlluminant2: Int = -1
    var calibrationTransform1: FloatArray = FloatArray(9)
    var calibrationTransform2: FloatArray = FloatArray(9)
    var forwardTransform1: FloatArray = FloatArray(9)
    var forwardTransform2: FloatArray = FloatArray(9)
    var colorMatrix1: FloatArray = FloatArray(9)
    var colorMatrix2: FloatArray = FloatArray(9)
    
    var analogISO: Int = 100
    
    fun FillFromCharacteristics(
        characteristics: CameraCharacteristics,
        size: Point
    ) {
        rawSize = size
        
        val analogue = characteristics.get(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY)
        if (analogue != null) {
            analogISO = analogue
        }
        
        val cfa = characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
        if (cfa != null) {
            cfaPattern = (cfa as Int).toByte()
        }
        
        val whiteLevelObj = characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)
        if (whiteLevelObj != null) {
            whiteLevel = whiteLevelObj as Int
            rawWhiteLevel = whiteLevel
        }
        
        val blackLevelPattern = characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
        if (blackLevelPattern != null) {
            val blArr = IntArray(4)
            blackLevelPattern.copyTo(blArr, 0)
            for (i in 0 until 4) {
                blackLevel[i] = blArr[i].toFloat()
            }
        }
        
        val flen = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        if (flen != null && flen.isNotEmpty()) {
            focalLength = flen[0]
        }
        
        val apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
        if (apertures != null && apertures.isNotEmpty()) {
            aperture = apertures[0]
        }
        
        sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        
        sensorPix = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            ?: Rect(0, 0, rawSize.x, rawSize.y)
        
        initColorMatrices(characteristics)
    }
    
    private fun initColorMatrices(characteristics: CameraCharacteristics) {
        val ref1 = characteristics.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1) ?: 0
        val ref2Obj = characteristics.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2)
        calibrationIlluminant1 = ref1
        calibrationIlluminant2 = if (ref2Obj != null) ref2Obj as Int else ref1
        
        val calib1 = characteristics.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1)
        val calib2 = characteristics.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM2)
        val colorMat1 = characteristics.get(CameraCharacteristics.SENSOR_COLOR_TRANSFORM1)
        val colorMat2 = characteristics.get(CameraCharacteristics.SENSOR_COLOR_TRANSFORM2)
        val fwd1 = characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX1)
        val fwd2 = characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX2)
        
        calib1?.let { convertColorSpaceTransform(it, calibrationTransform1) }
        calib2?.let { convertColorSpaceTransform(it, calibrationTransform2) }
        colorMat1?.let { convertColorSpaceTransform(it, colorMatrix1) }
        colorMat2?.let { convertColorSpaceTransform(it, colorMatrix2) }
        fwd1?.let { convertColorSpaceTransform(it, forwardTransform1) }
        fwd2?.let { convertColorSpaceTransform(it, forwardTransform2) }
        
        initIdentityMatrix(sensorToProPhoto)
        initIdentityMatrix(proPhotoToSRGB)
    }
    
    private fun convertColorSpaceTransform(transform: ColorSpaceTransform, out: FloatArray) {
        val rational = arrayOfNulls<Rational>(9)
        transform.copyElements(rational, 0)
        for (i in 0 until 9) {
            out[i] = rational[i]?.toFloat() ?: 0f
        }
    }
    
    private fun initIdentityMatrix(matrix: FloatArray) {
        matrix[0] = 1f; matrix[1] = 0f; matrix[2] = 0f
        matrix[3] = 0f; matrix[4] = 1f; matrix[5] = 0f
        matrix[6] = 0f; matrix[7] = 0f; matrix[8] = 1f
    }
    
    fun FillFromCaptureResult(result: CaptureResult?) {
        if (result == null) return
        
        val sensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY)
        if (sensitivity != null) {
            iso = sensitivity
        }
        
        val exposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        if (exposure != null) {
            exposureTime = exposure / 1_000_000_000.0
        }
        
        val dynWhiteLevel = result.get(CaptureResult.SENSOR_DYNAMIC_WHITE_LEVEL)
        if (dynWhiteLevel != null) {
            whiteLevel = dynWhiteLevel
        }
        
        val dynBlackLevel = result.get(CaptureResult.SENSOR_DYNAMIC_BLACK_LEVEL)
        if (dynBlackLevel != null && dynBlackLevel.size >= 4) {
            for (i in 0 until 4) {
                blackLevel[i] = dynBlackLevel[i]
            }
        }
        
        val lensMap = result.get(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP)
        if (lensMap != null) {
            gainMap = FloatArray(lensMap.gainFactorCount)
            lensMap.copyGainFactors(gainMap, 0)
            mapSize = Point(lensMap.columnCount, lensMap.rowCount)
            hasGainMap = true
        }
        
        val neutralColor = result.get(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT)
        if (neutralColor != null && neutralColor.size >= 3) {
            for (i in 0 until 3) {
                whitePoint[i] = neutralColor[i].toFloat()
            }
        }
        
        hotPixels = result.get(CaptureResult.STATISTICS_HOT_PIXEL_MAP)
        
        val colorCorrection = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)
        if (colorCorrection != null) {
            convertColorSpaceTransform(colorCorrection, proPhotoToSRGB)
        }
        
        noiseModeler = NoiseModeler(
            result.get(CaptureResult.SENSOR_NOISE_PROFILE),
            analogISO,
            iso,
            cfaPattern
        )
    }
    
    override fun toString(): String {
        return """
            ProcessingParameters:
              rawSize: ${rawSize.x}x${rawSize.y}
              iso: $iso
              exposureTime: ${exposureTime}s
              cfaPattern: $cfaPattern
              blackLevel: ${blackLevel.contentToString()}
              whiteLevel: $whiteLevel
              focalLength: ${focalLength}mm
              aperture: f/$aperture
        """.trimIndent()
    }
}
