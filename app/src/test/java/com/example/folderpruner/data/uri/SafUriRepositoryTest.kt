package com.example.folderpruner.data.uri

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.folderpruner.domain.model.FileEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class SafUriRepositoryTest {

    private fun child(
        name: String,
        uriString: String,
        length: Long = 0,
        lastModified: Long = 0,
        canRead: Boolean = true,
    ): DocumentFile {
        val df = mockk<DocumentFile>()
        every { df.name } returns name
        every { df.uri } returns Uri.parse(uriString)
        every { df.isDirectory } returns false
        every { df.length() } returns length
        every { df.lastModified() } returns lastModified
        every { df.canRead() } returns canRead
        every { df.listFiles() } returns null
        return df
    }

    private fun folder(
        name: String,
        uriString: String,
        vararg children: DocumentFile,
    ): DocumentFile {
        val df = mockk<DocumentFile>()
        every { df.name } returns name
        every { df.uri } returns Uri.parse(uriString)
        every { df.isDirectory } returns true
        every { df.listFiles() } returns children
        return df
    }

    @Test
    fun `buildFileTree reports direct children with metadata`() {
        val context = mockk<Context>()
        every { context.contentResolver } returns mockk<ContentResolver>()
        val root = folder("root", "content://tree",
            child("a.txt", "content://tree/a.txt", 100, 1000),
            child("b.JPG", "content://tree/b.JPG", 200, 2000, canRead = false),
        )
        val repo = SafUriRepository(context, { _ -> root })
        val entries = repo.buildFileTree(Uri.parse("content://tree"), includeSubfolders = false)

        assertEquals(2, entries.size)
        assertEquals("a.txt", entries[0].displayName)
        assertEquals("content://tree/a.txt", entries[0].uri)
        assertEquals("root", entries[0].directory)
        assertEquals("txt", entries[0].extension)
        assertEquals(100L, entries[0].sizeBytes)
        assertEquals(1000L, entries[0].lastModified.toEpochMilli())
        assertTrue(entries[0].isReadable)

        assertEquals("b.JPG", entries[1].displayName)
        assertEquals("jpg", entries[1].extension)
        assertEquals(200L, entries[1].sizeBytes)
        assertFalse(entries[1].isReadable)
    }

    @Test
    fun `include subfolders recurses into nested folders`() {
        val context = mockk<Context>()
        every { context.contentResolver } returns mockk<ContentResolver>()
        val inner = folder("inbox", "content://tree/inbox",
            child("note.txt", "content://tree/inbox/note.txt", 50, 3000),
        )
        val root = folder("root", "content://tree",
            child("top.txt", "content://tree/top.txt", 10, 1000),
            inner,
        )
        val repo = SafUriRepository(context, { _ -> root })
        val entries = repo.buildFileTree(Uri.parse("content://tree"), includeSubfolders = true)

        assertEquals(2, entries.size)
        assertEquals("top.txt", entries[0].displayName)
        assertEquals("root", entries[0].directory)
        assertEquals("note.txt", entries[1].displayName)
        assertEquals("root/inbox", entries[1].directory)
    }

    @Test
    fun `subfolders are skipped when includeSubfolders is false`() {
        val context = mockk<Context>()
        every { context.contentResolver } returns mockk<ContentResolver>()
        val inner = folder("inbox", "content://tree/inbox",
            child("note.txt", "content://tree/inbox/note.txt", 50, 3000),
        )
        val root = folder("root", "content://tree",
            child("top.txt", "content://tree/top.txt", 10, 1000),
            inner,
        )
        val repo = SafUriRepository(context, { _ -> root })
        val entries = repo.buildFileTree(Uri.parse("content://tree"), includeSubfolders = false)

        assertEquals(1, entries.size)
        assertEquals("top.txt", entries[0].displayName)
    }

    @Test
    fun `null tree root returns empty list without crashing`() {
        val context = mockk<Context>()
        every { context.contentResolver } returns mockk<ContentResolver>()
        val repo = SafUriRepository(context, { null })
        assertTrue(repo.buildFileTree(Uri.parse("content://tree"), true).isEmpty())
    }

    @Test
    fun `extension is empty for files without extension or dotfiles`() {
        val context = mockk<Context>()
        every { context.contentResolver } returns mockk<ContentResolver>()
        val root = folder("root", "content://tree",
            child("README", "content://tree/README"),
            child(".bashrc", "content://tree/.bashrc"),
            child("archive.tar.gz", "content://tree/archive.tar.gz"),
        )
        val repo = SafUriRepository(context, { _ -> root })
        val entries = repo.buildFileTree(Uri.parse("content://tree"), false)

        assertEquals("", entries[0].extension)
        assertEquals("", entries[1].extension)
        assertEquals("gz", entries[2].extension)
    }

    @Test
    fun `takePersistableUriPermission forwards to content resolver`() {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val context = mockk<Context>()
        every { context.contentResolver } returns resolver
        val repo = SafUriRepository(context)
        val uri = Uri.parse("content://tree")
        repo.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        verify(exactly = 1) { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}
