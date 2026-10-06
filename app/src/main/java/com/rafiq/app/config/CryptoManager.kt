package com.rafiq.app.config

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.annotation.RequiresApi
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val KEYSTORE_ALIAS = "rafiq_master_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val FIXED_SALT = "Raf1q_@#_2024"
    private const val GCM = "AES/GCM/NoPadding"
    private const val CBC = "AES/CBC/PKCS5Padding"

    fun encrypt(context: Context, plain: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cipher = Cipher.getInstance(GCM)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } else {
            val cipher = Cipher.getInstance(CBC)
            cipher.init(Cipher.ENCRYPT_MODE, legacyKey(context))
            Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        }
    } catch (e: Exception) { null }

    fun decrypt(context: Context, encoded: String): String? = try {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cipher = Cipher.getInstance(GCM)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, data.copyOf(12)))
            String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
        } else {
            val cipher = Cipher.getInstance(CBC)
            cipher.init(Cipher.DECRYPT_MODE, legacyKey(context), IvParameterSpec(data.copyOf(16)))
            String(cipher.doFinal(data.copyOfRange(16, data.size)), Charsets.UTF_8)
        }
    } catch (e: Exception) { null }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        gen.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun legacyKey(context: Context): SecretKey {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "rafiq"
        val digest = MessageDigest.getInstance("SHA-256").digest((id + FIXED_SALT).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }
}