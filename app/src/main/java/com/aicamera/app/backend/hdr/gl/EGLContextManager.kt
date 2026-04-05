package com.aicamera.app.backend.hdr.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log

object EGLContextManager {
    private const val TAG = "EGLContextManager"
    
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglConfig: EGLConfig? = null
    
    @Synchronized
    fun init(): Boolean {
        if (eglContext != null) {
            return true
        }
        
        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                Log.e(TAG, "Failed to get EGL display")
                return false
            }
            
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                Log.e(TAG, "Failed to initialize EGL")
                return false
            }
            
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
            )
            
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)) {
                Log.e(TAG, "Failed to choose EGL config")
                return false
            }
            eglConfig = configs[0]
            
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            
            eglContext = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0
            )
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                Log.e(TAG, "Failed to create EGL context")
                return false
            }
            
            Log.d(TAG, "EGL context initialized successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init EGL context", e)
            return false
        }
    }
    
    @Synchronized
    fun makeCurrent(): Boolean {
        if (eglDisplay == null || eglContext == null) {
            if (!init()) {
                return false
            }
        }
        
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0)
        
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "Failed to create pbuffer surface")
            return false
        }
        
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "Failed to make EGL context current")
            return false
        }
        
        return true
    }
    
    @Synchronized
    fun release() {
        try {
            if (eglDisplay != null && eglContext != null) {
                EGL14.eglMakeCurrent(
                    eglDisplay, 
                    EGL14.EGL_NO_SURFACE, 
                    EGL14.EGL_NO_SURFACE, 
                    EGL14.EGL_NO_CONTEXT
                )
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
            eglDisplay = null
            eglContext = null
            eglConfig = null
            Log.d(TAG, "EGL context released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release EGL context", e)
        }
    }
}
