package com.andres3336.streaming.data.live

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
import org.json.JSONObject

/**
 * Live state encrypted with an Android Keystore AES-GCM key because stored
 * channel keys can be URLs that contain playlist tokens or credentials.
 */
class LiveStateStore(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "live-state.bin")

    fun load(): LiveState = runCatching {
        if (!file.exists()) return LiveState()

        val raw = DataInputStream(file.inputStream().buffered()).use { input ->
            val ivSize = input.readInt()
            if (ivSize !in 1..32) return LiveState()

            val iv = ByteArray(ivSize).also(input::readFully)
            val encrypted = input.readBytes()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }

        LiveState.fromJson(JSONObject(raw))
    }.getOrDefault(LiveState())

    suspend fun save(state: LiveState) = withContext(Dispatchers.IO) {
        runCatching {
            if (state.favorites.isEmpty() &&
                state.recents.isEmpty() &&
                state.lastChannel == null
            ) {
                file.delete()
                return@runCatching
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(state.toJson().toString().toByteArray(Charsets.UTF_8))

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
        const val ALIAS = "streaming_live_state"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
