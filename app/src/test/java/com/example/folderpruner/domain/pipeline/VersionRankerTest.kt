package com.example.folderpruner.domain.pipeline

import com.example.folderpruner.domain.model.FileEntry
import com.example.folderpruner.domain.model.FileDecision
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.domain.model.NormalizedName
import com.example.folderpruner.domain.model.RankedFile
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VersionRankerTest {

    private fun file(
        displayName: String,
        sizeBytes: Long = 1000,
        lastModified: Instant = Instant.EPOCH,
    ) = FileEntry(
        uri = "content://example/$displayName",
        displayName = displayName,
        directory = "/",
        extension = "pdf",
        sizeBytes = sizeBytes,
        lastModified = lastModified,
    )

    @Test
    fun `highest semantic version defaults to Keep`() {
        val group = rankGroup(
            files = listOf(file("report-v1.0.pdf"), file("report-v1.2.pdf")),
            groupType = GroupType.OLDER_VERSION,
            normalizedName = NormalizedName("report", "pdf"),
            directory = "/",
        )
        val keep = group.rankedFiles.first { it.decision == FileDecision.KEEP }
        assertEquals("report-v1.2.pdf", keep.entry.displayName)
    }

    @Test
    fun `when versions equal, newer lastModified wins Keep`() {
        val older = file("report-v1.0.pdf", lastModified = Instant.parse("2025-01-01T00:00:00Z"))
        val newer = file("report-v1.0.pdf", lastModified = Instant.parse("2026-01-01T00:00:00Z"))
        val group = rankGroup(
            files = listOf(older, newer),
            groupType = GroupType.OLDER_VERSION,
            normalizedName = NormalizedName("report", "pdf"),
            directory = "/",
        )
        val keep = group.rankedFiles.first { it.decision == FileDecision.KEEP }
        assertSame(newer.uri, keep.entry.uri)
    }

    @Test
    fun `all non-top-ranked files default to Delete`() {
        val group = rankGroup(
            files = listOf(file("a-v1.0.pdf"), file("a-v1.1.pdf"), file("a-v1.2.pdf")),
            groupType = GroupType.OLDER_VERSION,
            normalizedName = NormalizedName("a", "pdf"),
            directory = "/",
        )
        assertEquals(1, group.rankedFiles.count { it.decision == FileDecision.KEEP })
        assertEquals(2, group.rankedFiles.count { it.decision == FileDecision.DELETE })
    }

    @Test
    fun `totalRecoverableBytes sums only deleted files`() {
        val group = rankGroup(
            files = listOf(
                file("keep.pdf", sizeBytes = 5000, lastModified = Instant.parse("2026-01-01T00:00:00Z")),
                file("delete.pdf", sizeBytes = 1000, lastModified = Instant.EPOCH),
            ),
            groupType = GroupType.EXACT_DUPLICATE,
            normalizedName = NormalizedName("doc", "pdf"),
            directory = "/",
        )
        assertEquals(1000L, group.totalRecoverableBytes)
    }

    @Test
    fun `single file group keeps that file`() {
        val group = rankGroup(
            files = listOf(file("solo.pdf")),
            groupType = GroupType.OLDER_VERSION,
            normalizedName = NormalizedName("solo", "pdf"),
            directory = "/",
        )
        assertEquals(1, group.rankedFiles.size)
        assertEquals(FileDecision.KEEP, group.rankedFiles[0].decision)
        assertEquals(0L, group.totalRecoverableBytes)
    }
}
