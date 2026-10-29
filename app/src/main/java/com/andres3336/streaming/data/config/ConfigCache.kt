package com.andres3336.streaming.data.config

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Last-known-good config, encrypted with an Android Keystore AES-GCM key.
 * The file never contains the config in plaintext.
 */
class ConfigCache(context: Context) {

    private val file = File(context.applicationContext.filesDir, "config-cache.bin")

    suspend fun save(raw: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(raw.toByteArray(Charsets.UTF_8))

            val temp = File(file.parentFile, "${file.name}.tmp")
            DataOutputStream(temp.outputStream().buffered()).use { output ->
                output.writeInt(cipher.iv.size)
                output.write(cipher.iv)
                output.write(encrypted)
            }

            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun load(): String? = withContext(Dispatchers.IO) {
        val result = runCatching {
            if (!file.exists()) return@runCatching null

            DataInputStream(file.inputStream().buffered()).use { input ->
                val ivSize = input.readInt()
                if (ivSize !in 1..32) return@runCatching null

                val iv = ByteArray(ivSize).also(input::readFully)
                val encrypted = input.readBytes()

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(encrypted), Charsets.UTF_8)
            }
        }.getOrNull()

        if (result == null) file.delete()
        result
    }

    fun clear() {
        file.delete()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "streaming_config_cache"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
