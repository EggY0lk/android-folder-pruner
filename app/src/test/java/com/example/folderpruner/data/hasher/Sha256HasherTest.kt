package com.example.folderpruner.data.hasher

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class Sha256HasherTest {

    private fun sha256Hex(content: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(content)
            .joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `hash returns lowercase hex digest of streamed content`() {
        val resolver = mockk<ContentResolver>()
        val content = "hello world".toByteArray()
        every { resolver.openInputStream(any()) } returns ByteArrayInputStream(content)
        val hasher = Sha256Hasher(resolver)
        assertEquals(sha256Hex(content), hasher.hash(Uri.parse("content://example/file.bin")))
    }

    @Test
    fun `hash returns null when the stream cannot be opened`() {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(any()) } returns null
        val hasher = Sha256Hasher(resolver)
        assertNull(hasher.hash(Uri.parse("content://example/missing.bin")))
    }

    @Test
    fun `hash returns null when reading fails`() {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(any()) } answers { throw RuntimeException("boom") }
        val hasher = Sha256Hasher(resolver)
        assertNull(hasher.hash(Uri.parse("content://example/file.bin")))
    }

    @Test
    fun `zero-byte content yields the digest of empty input`() {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(any()) } returns ByteArrayInputStream(byteArrayOf())
        val hasher = Sha256Hasher(resolver)
        assertEquals(sha256Hex(byteArrayOf()), hasher.hash(Uri.parse("content://example/empty.bin")))
    }
}
