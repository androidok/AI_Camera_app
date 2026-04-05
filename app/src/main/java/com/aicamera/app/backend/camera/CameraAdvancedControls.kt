@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)

package com.aicamera.app.backend.camera

import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera

object CameraAdvancedControls {
    fun setLinearZoom(camera: Camera?, linearZoom: Float) {
        val c = camera ?: return
        try {
            c.cameraControl.setLinearZoom(linearZoom.coerceIn(0f, 1f))
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "setLinearZoom failed", e)
        }
    }

    fun setExposureCompensationIndex(camera: Camera?, index: Int) {
        val c = camera ?: return
        try {
            Log.d("CameraAdvancedControls", "setExposureCompensationIndex: $index")
            c.cameraControl.setExposureCompensationIndex(index)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "setExposureCompensationIndex failed", e)
        }
    }

    /**
     * Enable manual mode for ISO and shutter speed control.
     * This sets CONTROL_MODE = CONTROL_MODE_OFF and CONTROL_AE_MODE = CONTROL_AE_MODE_OFF.
     */
    fun enableManualMode(camera: Camera?) {
        val c = camera ?: return
        val control = try {
            Camera2CameraControl.from(c.cameraControl)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "Camera2CameraControl.from failed", e)
            return
        }
        try {
            val builder = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            control.setCaptureRequestOptions(builder.build())
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "enableManualMode failed", e)
        }
    }

    /**
     * Restore auto exposure mode for preview.
     */
    fun restoreAutoMode(camera: Camera?) {
        val c = camera ?: return
        val control = try {
            Camera2CameraControl.from(c.cameraControl)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "Camera2CameraControl.from failed", e)
            return
        }
        try {
            val builder = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            control.setCaptureRequestOptions(builder.build())
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "restoreAutoMode failed", e)
        }
    }

    /**
     * Best-effort manual ISO. Some devices/auto modes may ignore this.
     */
    fun setManualIso(camera: Camera?, iso: Int?) {
        val c = camera ?: return
        val control = try {
            Camera2CameraControl.from(c.cameraControl)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "Camera2CameraControl.from failed", e)
            return
        }
        try {
            val builder = CaptureRequestOptions.Builder()
            if (iso != null) {
                // Enable manual mode when setting ISO
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
            } else {
                // Clear is not directly supported; using null means we simply avoid setting.
                // Note: To return to auto mode, you'd need to clear CONTROL_MODE and CONTROL_AE_MODE
            }
            control.setCaptureRequestOptions(builder.build())
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "setManualIso failed", e)
        }
    }

    /**
     * Best-effort manual exposure time (ns). Some devices/auto modes may ignore this.
     */
    fun setManualExposureTimeNs(camera: Camera?, exposureTimeNs: Long?) {
        val c = camera ?: return
        val control = try {
            Camera2CameraControl.from(c.cameraControl)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "Camera2CameraControl.from failed", e)
            return
        }
        try {
            Log.d("CameraAdvancedControls", "setManualExposureTimeNs: $exposureTimeNs ns")
            val builder = CaptureRequestOptions.Builder()
            if (exposureTimeNs != null) {
                // Enable manual mode when setting exposure time
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs)
            }
            control.setCaptureRequestOptions(builder.build())
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "setManualExposureTimeNs failed", e)
        }
    }

    /**
     * Apply manual exposure settings with both ISO and exposure time.
     * If either parameter is null, a reasonable default will be used.
     */
    fun applyManualExposure(camera: Camera?, iso: Int?, exposureTimeNs: Long?) {
        val c = camera ?: return
        val control = try {
            Camera2CameraControl.from(c.cameraControl)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "Camera2CameraControl.from failed", e)
            return
        }
        try {
            Log.d("CameraAdvancedControls", "applyManualExposure: iso=$iso, exposureTimeNs=$exposureTimeNs")
            val builder = CaptureRequestOptions.Builder()

            if (iso != null || exposureTimeNs != null) {
                // Enable manual mode
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

                // Set ISO if provided, otherwise use a reasonable default (e.g., 100)
                val isoToSet = iso ?: 100
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoToSet)

                // Set exposure time if provided, otherwise use a reasonable default (e.g., 1/100s = 10,000,000 ns)
                val exposureToSet = exposureTimeNs ?: 10_000_000L // 1/100 second
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureToSet)
            } else {
                // Both null, restore auto mode
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }

            control.setCaptureRequestOptions(builder.build())
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "applyManualExposure failed", e)
        }
    }

    /**
     * Get the exposure compensation range supported by the camera.
     * Returns a Pair(min, max) or null if not available.
     */
    fun getExposureCompensationRange(camera: Camera?): Pair<Int, Int>? {
        val c = camera ?: return null
        return try {
            val range = c.cameraInfo.exposureState.exposureCompensationRange
            Pair(range.lower, range.upper)
        } catch (e: Throwable) {
            Log.e("CameraAdvancedControls", "getExposureCompensationRange failed", e)
            null
        }
    }
}

