package com.example.folderpruner.domain.pipeline

import com.example.folderpruner.domain.model.FileEntry
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.domain.model.NormalizedName
import com.example.folderpruner.domain.model.ScanResult
import com.example.folderpruner.domain.pipeline.NameNormalizerStage.Companion.extractVersion
import com.example.folderpruner.domain.pipeline.NameNormalizerStage.Companion.normalize
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NameNormalizerTest {

    private fun entry(
        displayName: String,
        directory: String = "/",
        sizeBytes: Long = 1000,
    ) = FileEntry(
        uri = "content://example/$directory/$displayName",
        displayName = displayName,
        directory = directory,
        extension = displayName.substringAfterLast('.'),
        sizeBytes = sizeBytes,
        lastModified = Instant.now(),
    )

    @Test
    fun `report paren one pdf normalizes same base as report pdf`() {
        assertEquals(normalize("report (1).pdf"), normalize("report.pdf"))
    }

    @Test
    fun `backup 2025 zip and backup 2026 zip normalize to same base`() {
        assertEquals(normalize("backup_2025.zip"), normalize("backup_2026.zip"))
    }

    @Test
    fun `app v1 dot 2 apk and app v1 dot 10 apk normalize to same base`() {
        assertEquals(normalize("app-v1.2.apk"), normalize("app-v1.10.apk"))
    }

    @Test
    fun `final and copy tokens are stripped`() {
        assertEquals(normalize("document_final.pdf"), normalize("document.pdf"))
        assertEquals(normalize("image_copy.jpg"), normalize("image.jpg"))
    }

    @Test
    fun `paren two token is stripped`() {
        assertEquals(normalize("vacation (2).jpg"), normalize("vacation.jpg"))
    }

    @Test
    fun `semantic version extracted from app v1 dot 2 apk`() {
        assertNotNull(extractVersion("app-v1.2.apk"))
        assertEquals(1, extractVersion("app-v1.2.apk")!!.major)
        assertEquals(2, extractVersion("app-v1.2.apk")!!.minor)
    }

    @Test
    fun `semantic version extracted from app v1 dot 10 apk`() {
        assertNotNull(extractVersion("app-v1.10.apk"))
        assertEquals(1, extractVersion("app-v1.10.apk")!!.major)
        assertEquals(10, extractVersion("app-v1.10.apk")!!.minor)
    }

    @Test
    fun `v1 dot 10 sorts after v1 dot 2 semantic not lexicographic`() {
        val v12 = extractVersion("app-v1.2.apk")!!
        val v110 = extractVersion("app-v1.10.apk")!!
        assertTrue(v110 > v12)
    }

    @Test
    fun `no extension file normalizes without crashing`() {
        assertEquals(NormalizedName("readme", ""), normalize("README"))
    }

    @Test
    fun `same directory plus extension groups together`() {
        val a = entry("report (1).pdf", "/docs")
        val b = entry("report.pdf", "/docs")
        assertEquals(normalize(a.displayName), normalize(b.displayName))
        assertEquals(a.extension, b.extension)
        assertEquals(a.directory, b.directory)
    }
}
