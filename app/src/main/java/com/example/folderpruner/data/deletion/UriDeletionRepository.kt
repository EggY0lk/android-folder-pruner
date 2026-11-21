package com.example.folderpruner.data.deletion

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Deletes documents via the Storage Access Framework.
 *
 * Wraps [DocumentsContract.deleteDocument] so every selected URI gets an independent
 * success/failure result — one URI's failure never aborts the remaining deletions.
 * No [android.Manifest.permission.MANAGE_EXTERNAL_STORAGE] is required.
 *
 * @param documentDeleter injectable for tests; defaults to DocumentsContract.deleteDocument.
 */
class UriDeletionRepository(
    private val contentResolver: ContentResolver,
    private val documentDeleter: (ContentResolver, Uri) -> Boolean = { resolver, uri ->
        DocumentsContract.deleteDocument(resolver, uri)
    },
) {

    /**
     * Deletes every URI in [uris] and returns a [uri → success] map.
     * Exceptions on a single URI are caught and reported as failure for that URI only.
     */
    fun delete(uris: List<Uri>): Map<Uri, Boolean> =
        uris.associateWith { uri ->
            try {
                documentDeleter(contentResolver, uri)
            } catch (_: Exception) {
                false
            }
        }
}
