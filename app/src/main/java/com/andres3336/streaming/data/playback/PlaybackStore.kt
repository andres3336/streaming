package com.andres3336.streaming.data.playback

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
import org.json.JSONArray

/**
 * Resume positions, encrypted with an Android Keystore AES-GCM key because the
 * stored URLs can contain Xtream credentials.
 */
class PlaybackStore(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "playback-entries.bin")
    private val legacyFile = File(appContext.filesDir, "playback-entries.json")

    fun load(): List<PlaybackEntry> {
        legacyFile.delete()

        return runCatching {
            if (!file.exists()) return emptyList()

            val raw = DataInputStream(file.inputStream().buffered()).use { input ->
                val ivSize = input.readInt()
                if (ivSize !in 1..32) return emptyList()

                val iv = ByteArray(ivSize).also(input::readFully)
                val encrypted = input.readBytes()

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(encrypted), Charsets.UTF_8)
            }

            val array = JSONArray(raw)
            (0 until array.length())
                .mapNotNull { index -> array.optJSONObject(index)?.let(PlaybackEntry::fromJson) }
                .filter(PlaybackEntry::isResumable)
                .sortedByDescending(PlaybackEntry::updatedAt)
                .take(50)
        }.getOrDefault(emptyList())
    }

    suspend fun save(entries: List<PlaybackEntry>) = withContext(Dispatchers.IO) {
        legacyFile.delete()

        runCatching {
            val array = JSONArray()
            entries.take(50).forEach { array.put(it.toJson()) }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))

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
        legacyFile.delete()
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
        const val ALIAS = "streaming_playback_store"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
