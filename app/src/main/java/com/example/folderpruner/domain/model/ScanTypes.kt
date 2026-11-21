package com.example.folderpruner.domain.model

import java.net.URI
import java.time.Instant

/** A single discovered file, described only by pure-Kotlin data (no Android types). */
data class FileEntry(
    val uri: String,
    val displayName: String,
    val directory: String,
    val extension: String, // lowercase, without leading dot; empty string when no extension
    val sizeBytes: Long,
    val lastModified: Instant,
    val isReadable: Boolean = true,
)

/** The normalized base name after stripping version/copy/date tokens. */
data class NormalizedName(
    val base: String,
    val extension: String,
)

/** One candidate within a duplicate/version group. */
data class RankedFile(
    val entry: FileEntry,
    val semanticVersion: SemanticVersion?, // null when not parseable
    val longVersionCode: Long?,            // null when not an APK or unreadable
    val rankScore: Long,                   // higher = better (default Keep)
    val decision: FileDecision,            // Keep or Delete
)

enum class FileDecision { KEEP, DELETE }

/**
 * Semantic version parsed from a filename.
 * Null-safe ordering: null sorts below any concrete version.
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        return run {
            val c = major.compareTo(other.major)
            if (c != 0) return@run c
            val c2 = minor.compareTo(other.minor)
            if (c2 != 0) return@run c2
            val c3 = patch.compareTo(other.patch)
            if (c3 != 0) return@run c3
            (preRelease ?: "").compareTo(other.preRelease ?: "", ignoreCase = true)
        }
    }
}

/** A group of related files (exact duplicates or versions of the same logical file). */
data class DuplicateGroup(
    val groupId: String,
    val groupType: GroupType,
    val normalizedName: NormalizedName,
    val directory: String,
    val rankedFiles: List<RankedFile>,
    val totalRecoverableBytes: Long, // sum of sizes of files marked DELETE
)

enum class GroupType { EXACT_DUPLICATE, OLDER_VERSION }

/** Full result of a scan pass. */
data class ScanResult(
    val filesScanned: Int,
    val currentFolder: String?,
    val totalRecoverableBytes: Long,
    val groups: List<DuplicateGroup>,
)

/** Progress event emitted live during a scan. */
data class ScanProgress(
    val filesScanned: Int,
    val currentFolder: String?,
    val totalRecoverableBytes: Long,
)
