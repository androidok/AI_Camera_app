package com.aicamera.app.backend.hdr.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import com.aicamera.app.backend.hdr.model.ProcessingParameters
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PostPipeline(private val context: Context) {
    companion object {
        private const val TAG = "PostPipeline"
        
        const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """
        
        const val PASS_THROUGH_FRAGMENT = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D InputBuffer;
            
            void main() {
                gl_FragColor = texture2D(InputBuffer, vTexCoord);
            }
        """
        
        const val DEMOSAIC_FALLBACK = """
            precision highp float;
            varying vec2 vTexCoord;
            uniform sampler2D RawBuffer;
            uniform ivec2 size;
            uniform vec4 blackLevel;
            uniform float whiteLevel;
            
            void main() {
                vec2 pixelCoord = vTexCoord * vec2(size);
                ivec2 coord = ivec2(pixelCoord);
                
                float raw = texelFetch(RawBuffer, coord, 0).r;
                float normalized = (raw - blackLevel.r) / (whiteLevel - blackLevel.r);
                
                gl_FragColor = vec4(vec3(normalized), 1.0);
            }
        """
        
        const val DENOISE_FALLBACK = """
            precision highp float;
            varying vec2 vTexCoord;
            uniform sampler2D InputBuffer;
            uniform float noiseS;
            uniform float noiseO;
            
            void main() {
                gl_FragColor = texture2D(InputBuffer, vTexCoord);
            }
        """
        
        const val SHARPEN_FALLBACK = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D InputBuffer;
            uniform float strength;
            
            void main() {
                vec3 color = texture2D(InputBuffer, vTexCoord).rgb;
                gl_FragColor = vec4(color, 1.0);
            }
        """
        
        const val TONEMAP_FALLBACK = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D InputBuffer;
            uniform float strength;
            
            void main() {
                vec3 color = texture2D(InputBuffer, vTexCoord).rgb;
                gl_FragColor = vec4(color, 1.0);
            }
        """
    }
    
    private var program: Int = 0
    private var textureId: Int = 0
    private var outputTextureId: Int = 0
    private var framebufferId: Int = 0
    
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer
    
    private var width: Int = 0
    private var height: Int = 0
    
    fun process(
        inputBuffer: ByteBuffer,
        parameters: ProcessingParameters,
        imageWidth: Int,
        imageHeight: Int
    ): Bitmap {
        width = imageWidth
        height = imageHeight
        
        Log.d(TAG, "Starting post pipeline: ${width}x${height}")
        
        if (!EGLContextManager.makeCurrent()) {
            Log.e(TAG, "Failed to initialize EGL context, using fallback processing")
            return processFallback(inputBuffer, parameters, imageWidth, imageHeight)
        }
        
        try {
            initGL()
            createBuffers()
            
            val inputTexture = createTexture(inputBuffer, imageWidth, imageHeight)
            
            var currentTexture = inputTexture
            
            currentTexture = processDemosaic(currentTexture, parameters)
            
            currentTexture = processDenoise(currentTexture, parameters)
            
            currentTexture = processSharpen(currentTexture, parameters)
            
            currentTexture = processToneMapping(currentTexture, parameters)
            
            val bitmap = readTextureToBitmap(currentTexture, imageWidth, imageHeight)
            
            cleanup()
            
            Log.d(TAG, "Post pipeline completed")
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "OpenGL processing failed, using fallback", e)
            return processFallback(inputBuffer, parameters, imageWidth, imageHeight)
        }
    }
    
    private fun processFallback(
        inputBuffer: ByteBuffer,
        parameters: ProcessingParameters,
        width: Int,
        height: Int
    ): Bitmap {
        Log.d(TAG, "Using fallback processing")
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        inputBuffer.rewind()
        
        val blackLevel = parameters.blackLevel[0]
        val whiteLevel = parameters.whiteLevel.toFloat()
        
        val pixels = IntArray(width * height)
        
        for (i in 0 until width * height) {
            if (inputBuffer.remaining() >= 2) {
                val rawValue = (inputBuffer.short.toInt() and 0xFFFF)
                val normalized = ((rawValue - blackLevel) / (whiteLevel - blackLevel)).coerceIn(0.0f, 1.0f)
                val gray = (normalized * 255).toInt()
                pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    private fun initGL() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, PASS_THROUGH_FRAGMENT)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Failed to link program: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Failed to link program")
        }
    }
    
    private fun createBuffers() {
        val vertices = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )
        
        val texCoords = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
        
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices).position(0)
        
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texCoordBuffer.put(texCoords).position(0)
    }
    
    private fun createTexture(buffer: ByteBuffer, width: Int, height: Int): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        buffer.rewind()
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            width, height, 0,
            GLES20.GL_RGBA, GLES30.GL_HALF_FLOAT, buffer
        )
        
        return textures[0]
    }
    
    private fun processDemosaic(inputTexture: Int, params: ProcessingParameters): Int {
        Log.d(TAG, "Processing demosaic")
        
        val cfaPattern = params.cfaPattern.toInt()
        val shader = when (cfaPattern) {
            0 -> loadShaderFromAsset("shaders/demosaic/demosaic_rggb.glsl")
            1 -> loadShaderFromAsset("shaders/demosaic/demosaic_grbg.glsl")
            2 -> loadShaderFromAsset("shaders/demosaic/demosaic_gbrg.glsl")
            3 -> loadShaderFromAsset("shaders/demosaic/demosaic_bggr.glsl")
            else -> loadShaderFromAsset("shaders/demosaic/demosaic_rggb.glsl")
        }
        
        val program = createProgram(VERTEX_SHADER, shader)
        
        val outputTexture = createOutputTexture(width, height)
        val framebuffer = createFramebuffer(outputTexture)
        
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)
        
        GLES20.glUseProgram(program)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "RawBuffer"), 0)
        
        GLES20.glUniform2i(GLES20.glGetUniformLocation(program, "size"), width, height)
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(program, "blackLevel"), 1, params.blackLevel, 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "whiteLevel"), params.whiteLevel.toFloat())
        
        drawQuad(program)
        
        GLES20.glDeleteProgram(program)
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(inputTexture), 0)
        
        return outputTexture
    }
    
    /**
     * 降噪处理 - 使用ISO自适应参数
     * 根据ISO和噪声模型动态调整降噪强度
     */
    private fun processDenoise(inputTexture: Int, params: ProcessingParameters): Int {
        Log.d(TAG, "Processing denoise")

        val noiseModeler = params.noiseModeler
        if (noiseModeler == null) {
            Log.d(TAG, "No noise modeler, skipping denoise")
            return inputTexture
        }

        // 检查是否应该跳过降噪（正常光线且噪点很低）
        if (!noiseModeler.shouldApplyDenoise()) {
            Log.d(TAG, "Noise levels too low for current ISO (${params.iso}), skipping denoise")
            return inputTexture
        }

        val noiseS = noiseModeler.getNoiseS()
        val noiseO = noiseModeler.getNoiseO()

        // 获取ISO自适应参数
        val kernelSize = noiseModeler.getAdaptiveKernelSize()
        val denoiseStrength = noiseModeler.getDenoiseStrength()

        Log.d(TAG, "Denoise params: ISO=${params.iso}, kernel=$kernelSize, strength=$denoiseStrength")

        val shader = loadShaderFromAsset("shaders/denoise/esd3d.glsl")
        val program = createProgram(VERTEX_SHADER, shader)

        val outputTexture = createOutputTexture(width, height)
        val framebuffer = createFramebuffer(outputTexture)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "InputBuffer"), 0)

        // 根据降噪强度调整噪声参数
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "noiseS"), noiseS * denoiseStrength)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "noiseO"), noiseO * denoiseStrength)
        GLES20.glUniform2i(GLES20.glGetUniformLocation(program, "size"), width, height)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "MSIZE"), kernelSize)

        drawQuad(program)

        GLES20.glDeleteProgram(program)
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(inputTexture), 0)

        return outputTexture
    }
    
    /**
     * 锐化处理 - 使用ISO自适应锐化强度
     * 高ISO时自动降低锐化以避免噪点放大
     */
    private fun processSharpen(inputTexture: Int, params: ProcessingParameters): Int {
        Log.d(TAG, "Processing sharpen")

        val noiseModeler = params.noiseModeler

        // 计算自适应锐化强度
        // 基础强度 0.5，根据ISO自动调整
        val baseStrength = 0.5f
        val adaptiveStrength = noiseModeler?.getAdaptiveSharpenStrength(baseStrength) ?: baseStrength

        // 估计噪声水平用于着色器内的局部调整
        val noiseLevel = noiseModeler?.getCombinedNoise() ?: 0.01f

        Log.d(TAG, "Sharpen params: ISO=${params.iso}, strength=$adaptiveStrength, noise=$noiseLevel")

        val shader = loadShaderFromAsset("shaders/sharpening/sharpen.glsl")
        val program = createProgram(VERTEX_SHADER, shader)

        val outputTexture = createOutputTexture(width, height)
        val framebuffer = createFramebuffer(outputTexture)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "InputBuffer"), 0)

        // 设置自适应锐化参数
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "strength"), adaptiveStrength)
        GLES20.glUniform2i(GLES20.glGetUniformLocation(program, "size"), width, height)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "noiseLevel"), noiseLevel)

        drawQuad(program)

        GLES20.glDeleteProgram(program)
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(inputTexture), 0)

        return outputTexture
    }
    
    /**
     * 色调映射处理 - 应用带暗部保护的色调映射
     * 减少暗部噪点放大，保持高光细节
     */
    private fun processToneMapping(inputTexture: Int, params: ProcessingParameters): Int {
        Log.d(TAG, "Processing tone mapping")

        val shader = loadShaderFromAsset("shaders/tonemap/tonemap.glsl")
        val program = createProgram(VERTEX_SHADER, shader)

        val outputTexture = createOutputTexture(width, height)
        val framebuffer = createFramebuffer(outputTexture)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "InputBuffer"), 0)

        // 设置色调映射强度参数
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "strength"), params.tonemapStrength)
        // 使用自适应Gamma - 高ISO时稍微提高Gamma以减少暗部噪点
        val adaptiveGamma = when {
            params.iso < 200 -> 2.2f
            params.iso < 400 -> 2.3f
            params.iso < 800 -> 2.4f
            else -> 2.5f
        }
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "gamma"), adaptiveGamma)

        drawQuad(program)

        GLES20.glDeleteProgram(program)
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(inputTexture), 0)

        return outputTexture
    }
    
    private fun drawQuad(program: Int) {
        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)
        
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }
    
    private fun createOutputTexture(width: Int, height: Int): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            width, height, 0,
            GLES20.GL_RGBA, GLES30.GL_HALF_FLOAT, null
        )
        
        return textures[0]
    }
    
    private fun createFramebuffer(texture: Int): Int {
        val framebuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, framebuffers, 0)
        
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, texture, 0
        )
        
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer not complete: $status")
        }
        
        return framebuffers[0]
    }
    
    private fun readTextureToBitmap(texture: Int, width: Int, height: Int): Bitmap {
        val framebuffer = createFramebuffer(texture)
        
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)
        
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        
        return bitmap
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compilation failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compilation failed")
        }
        
        return shader
    }
    
    private fun createProgram(vertexShader: String, fragmentShader: String): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            Log.e(TAG, "Program linking failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program linking failed")
        }
        
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        
        return program
    }
    
    private fun loadShaderFromAsset(path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load shader from asset: $path, using fallback")
            getFallbackShader(path)
        }
    }
    
    private fun getFallbackShader(path: String): String {
        return when {
            path.contains("demosaic") -> DEMOSAIC_FALLBACK
            path.contains("denoise") -> DENOISE_FALLBACK
            path.contains("sharpen") -> SHARPEN_FALLBACK
            path.contains("tonemap") -> TONEMAP_FALLBACK
            else -> PASS_THROUGH_FRAGMENT
        }
    }
    
    private fun cleanup() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (outputTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(outputTextureId), 0)
            outputTextureId = 0
        }
        if (framebufferId != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
        }
    }
}
