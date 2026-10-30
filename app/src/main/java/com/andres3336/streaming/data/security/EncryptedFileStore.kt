package com.andres3336.streaming.data.security

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
 * Small encrypted text file backed by an Android Keystore AES-GCM key.
 * Used for caches whose payloads may contain provider tokens or credentials.
 */
class EncryptedFileStore(
    context: Context,
    fileName: String,
    private val alias: String,
) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, fileName)

    fun readBytes(): ByteArray? = runCatching {
        if (!file.exists()) return null

        DataInputStream(file.inputStream().buffered()).use { input ->
            val ivSize = input.readInt()
            if (ivSize !in 1..32) return null

            val iv = ByteArray(ivSize).also(input::readFully)
            val encrypted = input.readBytes()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted)
        }
    }.getOrNull()

    fun read(): String? = readBytes()?.toString(Charsets.UTF_8)

    suspend fun writeBytes(bytes: ByteArray) = withContext(Dispatchers.IO) {
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(bytes)

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
        }
    }

    suspend fun write(text: String) = writeBytes(text.toByteArray(Charsets.UTF_8))

    fun delete() {
        file.delete()
    }

    fun sizeBytes(): Long = if (file.exists()) file.length() else 0L

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
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
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
