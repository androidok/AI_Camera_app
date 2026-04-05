package com.aicamera.app.backend.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    private const val PREFS_FILE_NAME = "secure_cloud_ai_prefs"

    @Volatile
    private var encryptedPrefs: androidx.security.crypto.EncryptedSharedPreferences? = null

    fun getEncryptedPrefs(context: Context): androidx.security.crypto.EncryptedSharedPreferences {
        return encryptedPrefs ?: synchronized(this) {
            encryptedPrefs ?: createEncryptedPrefs(context).also { encryptedPrefs = it }
        }
    }

    private fun createEncryptedPrefs(context: Context): androidx.security.crypto.EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as androidx.security.crypto.EncryptedSharedPreferences
    }
}
