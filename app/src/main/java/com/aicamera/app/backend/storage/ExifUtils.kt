package com.aicamera.app.backend.storage

import androidx.exifinterface.media.ExifInterface

object ExifUtils {
    /**
     * Copy selected Exif attributes from src -> dst. Best-effort.
     */
    fun copyExif(srcPath: String, dstPath: String) {
        try {
            val src = ExifInterface(srcPath)
            val dst = ExifInterface(dstPath)

            val tags = listOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_ISO_SPEED_RATINGS,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_ORIENTATION
            )

            tags.forEach { tag ->
                val v = src.getAttribute(tag)
                if (v != null) dst.setAttribute(tag, v)
            }
            dst.saveAttributes()
        } catch (_: Throwable) {
        }
    }
}

