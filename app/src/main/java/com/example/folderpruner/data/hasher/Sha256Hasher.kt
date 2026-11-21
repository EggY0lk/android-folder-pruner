package com.example.folderpruner.data.hasher

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest

/**
 * Streaming SHA-256 hash of file content accessed through a [ContentResolver].
 *
 * Returns the lowercase hex digest, or [null] when the stream cannot be opened or
 * read (missing permission, not found, zero-byte, etc.). The input stream is always
 * closed, even on error.
 */
class Sha256Hasher(private val contentResolver: ContentResolver) {

    private companion object {
        private const val BUFFER_SIZE = 8192
    }

    fun hash(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(BUFFER_SIZE)
                var read = input.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) {
            null
        }
    }
}
