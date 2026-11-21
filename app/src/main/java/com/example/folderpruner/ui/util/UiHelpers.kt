package com.example.folderpruner.ui.util

import com.example.folderpruner.domain.model.FileDecision
import com.example.folderpruner.domain.model.GroupType

/** Human-readable byte size with at most one decimal, using binary units (KB/MB/GB). */
fun formatBytes(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    }

/** Friendly icon key for a file extension (lowercase, no leading dot). */
fun fileTypeIcon(extension: String): String {
    val ext = extension.lowercase()
    return when (ext) {
        "apk" -> "installer"
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> "archive"
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico" -> "media"
        "mp3", "wav", "flac", "aac", "ogg", "m4a" -> "media"
        "mp4", "mkv", "mov", "avi", "wmv", "webm" -> "media"
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "rtf", "csv" -> "document"
        else -> "document"
    }
}

/** Status badge text for a ranked file within its group. */
fun statusText(decision: FileDecision, groupType: GroupType): String =
    when (decision) {
        FileDecision.KEEP -> "Keep"
        FileDecision.DELETE -> when (groupType) {
            GroupType.EXACT_DUPLICATE -> "Exact duplicate"
            GroupType.OLDER_VERSION -> "Older version"
        }
    }
