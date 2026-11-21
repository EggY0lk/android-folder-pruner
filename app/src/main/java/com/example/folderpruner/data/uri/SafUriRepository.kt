package com.example.folderpruner.data.uri

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.folderpruner.domain.model.FileEntry
import java.time.Instant

/**
 * Data access over the Storage Access Framework (SAF).
 *
 * Builds a flat list of [FileEntry]s from a tree URI obtained via
 * [Intent.ACTION_OPEN_DOCUMENT_TREE], recursing into subfolders when requested.
 * No [android.Manifest.permission.MANAGE_EXTERNAL_STORAGE] is required — access is
 * gated entirely by the user's tree grant and [ContentResolver.takePersistableUriPermission].
 */
class SafUriRepository(
    private val context: Context,
    private val resolveTree: (Uri) -> DocumentFile? = {
        DocumentFile.fromTreeUri(context, it)
    },
) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Persists read permission for [treeUri] so the picked tree remains accessible
     * across process restarts. [flags] should come from the OPEN_DOCUMENT_TREE
     * result intent (e.g. Intent.FLAG_GRANT_READ_URI_PERMISSION).
     */
    fun takePersistableUriPermission(treeUri: Uri, flags: Int) {
        contentResolver.takePersistableUriPermission(treeUri, flags)
    }

    /**
     * Builds a flat list of [FileEntry]s for every file under [treeUri].
     * When [includeSubfolders] is false only direct children are returned;
     * directories themselves are never returned as entries.
     *
     * Zero-byte and unreadable files are still reported (with accurate [FileEntry.sizeBytes]
     * / [FileEntry.isReadable] flags) so the caller can decide; any single file that
     * throws while being read is skipped, never aborting the whole tree scan.
     */
    fun buildFileTree(treeUri: Uri, includeSubfolders: Boolean): List<FileEntry> {
        val root = resolveTree(treeUri) ?: return emptyList()
        return buildTree(root, root.name ?: "", includeSubfolders)
    }

    private fun buildTree(
        folder: DocumentFile,
        directory: String,
        includeSubfolders: Boolean,
    ): List<FileEntry> {
        val entries = mutableListOf<FileEntry>()
        val children = folder.listFiles() ?: return entries
        for (child in children) {
            try {
                if (child.isDirectory) {
                    if (includeSubfolders) {
                        entries.addAll(buildTree(child, childDirectory(directory, child.name), true))
                    }
                    continue
                }
                val entry = toFileEntry(child, directory) ?: continue
                entries += entry
            } catch (_: Exception) {
                // One unreadable file must not abort the whole tree scan.
                continue
            }
        }
        return entries
    }

    private fun toFileEntry(child: DocumentFile, directory: String): FileEntry? {
        val name = child.name ?: return null
        return FileEntry(
            uri = child.uri.toString(),
            displayName = name,
            directory = directory,
            extension = extensionOf(name),
            sizeBytes = child.length(),
            lastModified = Instant.ofEpochMilli(child.lastModified()),
            isReadable = child.canRead(),
        )
    }

    private fun childDirectory(parent: String, name: String?): String =
        if (parent.isEmpty()) name ?: "" else "$parent/${name ?: ""}"

    private fun extensionOf(name: String?): String {
        val n = name ?: return ""
        val dot = n.lastIndexOf('.')
        return if (dot <= 0 || dot == n.lastIndex) "" else n.substring(dot + 1).lowercase()
    }
}
