package com.example.folderpruner.data.deletion

import android.content.ContentResolver
import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class UriDeletionRepositoryTest {

    @Test
    fun `delete returns success per uri`() {
        val resolver = mockk<ContentResolver>()
        val repo = UriDeletionRepository(resolver) { _, _ -> true }
        val uris = listOf(Uri.parse("content://example/a"), Uri.parse("content://example/b"))
        assertEquals(mapOf(uris[0] to true, uris[1] to true), repo.delete(uris))
    }

    @Test
    fun `delete returns failure per uri without aborting the batch`() {
        val resolver = mockk<ContentResolver>()
        val repo = UriDeletionRepository(resolver) { _, _ -> throw RuntimeException("gone") }
        val uris = listOf(Uri.parse("content://example/a"), Uri.parse("content://example/b"))
        assertEquals(mapOf(uris[0] to false, uris[1] to false), repo.delete(uris))
    }

    @Test
    fun `empty input returns empty map`() {
        val resolver = mockk<ContentResolver>()
        val repo = UriDeletionRepository(resolver) { _, _ -> true }
        assertEquals(emptyMap(), repo.delete(emptyList()))
    }
}
