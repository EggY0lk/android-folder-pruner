package com.example.folderpruner.domain.pipeline

import com.example.folderpruner.domain.model.FileDecision
import com.example.folderpruner.domain.model.FileEntry
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.domain.model.NormalizedName
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuplicateEngineTest {

    private fun file(
        displayName: String,
        sizeBytes: Long,
        content: String,
        lastModified: Instant = Instant.EPOCH,
    ) = FileEntry(
        uri = "content://example/$displayName",
        displayName = displayName,
        directory = "/",
        extension = "bin",
        sizeBytes = sizeBytes,
        lastModified = lastModified,
    )

    private fun sha256(text: String): String {
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `exact duplicates with same content are grouped and one is kept`() {
        val entries = listOf(
            file("copy1.bin", 10, "hello", Instant.parse("2025-01-01T00:00:00Z")),
            file("copy2.bin", 10, "hello", Instant.parse("2026-01-01T00:00:00Z")),
        )
        val engine = DuplicateEngine.defaultPipeline(
            hasher = { sha256("hello") },
            apkMetadata = { null },
        )
        val result = engine.scan(entries, "/")
        assertEquals(1, result.groups.size)
        assertEquals(GroupType.EXACT_DUPLICATE, result.groups[0].groupType)
        assertEquals(1, result.groups[0].rankedFiles.count { it.decision == FileDecision.KEEP })
    }

    @Test
    fun `different content with same size is not a false positive`() {
        val entries = listOf(
            file("a.bin", 10, "aaaaaaaaaa"),
            file("b.bin", 10, "bbbbbbbbbb"),
        )
        val engine = DuplicateEngine.defaultPipeline(
            hasher = { sha256(it.displayName) },
            apkMetadata = { null },
        )
        val result = engine.scan(entries, "/")
        // Different content → different hashes → no exact-duplicate groups
        assertEquals(0, result.groups.count { it.groupType == GroupType.EXACT_DUPLICATE })
    }

    @Test
    fun `zero-byte file is ignored (no crash)`() {
        val entries = listOf(file("empty.bin", 0, ""))
        val engine = DuplicateEngine.defaultPipeline(
            hasher = { sha256("") },
            apkMetadata = { null },
        )
        val result = engine.scan(entries, "/")
        assertEquals(0, result.groups.size)
    }

    @Test
    fun `unreadable file is ignored (no crash)`() {
        val entries = listOf(
            FileEntry(
                uri = "content://example/unreadable.bin",
                displayName = "unreadable.bin",
                directory = "/",
                extension = "bin",
                sizeBytes = 100,
                lastModified = Instant.EPOCH,
                isReadable = false,
            ),
        )
        val engine = DuplicateEngine.defaultPipeline(
            hasher = { throw AssertionError("should not hash unreadable file") },
            apkMetadata = { null },
        )
        val result = engine.scan(entries, "/")
        assertEquals(0, result.groups.size)
    }

    @Test
    fun `apk metadata does not throw when metadata provider returns null`() {
        val entries = listOf(
            FileEntry(
                uri = "content://example/app.apk",
                displayName = "app.apk",
                directory = "/",
                extension = "apk",
                sizeBytes = 1000,
                lastModified = Instant.EPOCH,
                isReadable = true,
            ),
        )
        val engine = DuplicateEngine.defaultPipeline(
            hasher = { "" },
            apkMetadata = { null },
        )
        val result = engine.scan(entries, "/")
        assertTrue(result.groups.isEmpty())
    }
}


