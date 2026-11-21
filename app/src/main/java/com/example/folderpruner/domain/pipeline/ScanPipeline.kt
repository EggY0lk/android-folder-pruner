package com.example.folderpruner.domain.pipeline

import com.example.folderpruner.domain.model.DuplicateGroup
import com.example.folderpruner.domain.model.FileEntry
import com.example.folderpruner.domain.model.FileDecision
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.domain.model.NormalizedName
import com.example.folderpruner.domain.model.RankedFile
import com.example.folderpruner.domain.model.SemanticVersion
import com.example.folderpruner.domain.model.ScanResult
import kotlin.math.max

/**
 * Pluggable pipeline that runs ordered [AnalyzerStage]s.
 * Stages are pure functions over [ScanResult]; each stage sees the output of the previous one.
 */
class ScanPipeline(private val stages: List<AnalyzerStage>) {
    fun run(entries: List<FileEntry>, currentFolder: String?): ScanResult {
        var result = ScanResult(
            filesScanned = entries.size,
            currentFolder = currentFolder,
            totalRecoverableBytes = 0L,
            groups = emptyList(),
        )
        stages.forEach { stage ->
            result = stage.analyze(entries, result)
        }
        return result
    }
}

/**
 * Decides keep/delete within each group:
 *   - Rank by semantic version / longVersionCode descending (when parsed).
 *   - Tie-break by LAST_MODIFIED descending.
 *   - Highest-ranked = KEEP; everything else = DELETE.
 */
fun rankGroup(
    files: List<FileEntry>,
    groupType: GroupType,
    normalizedName: NormalizedName,
    directory: String,
): DuplicateGroup {
    val ranked = files.map { entry ->
        val versionScore = entry.semanticVersion?.let { (it.major.toLong() * 1_000_000 + it.minor.toLong() * 1_000 + it.patch) * 1000 }
            ?: entry.longVersionCode ?: 0L
        val timeScore = entry.lastModified.toEpochMilli()
        // Combine: version dominates; timestamp breaks ties. Multiply version by a large
        // constant so any version difference outweighs any timestamp difference.
        val rankScore = versionScore * 1_000_000_000_000L + timeScore
        RankedFile(
            entry = entry,
            semanticVersion = entry.semanticVersion,
            longVersionCode = entry.longVersionCode,
            rankScore = rankScore,
            decision = FileDecision.KEEP, // placeholder; set below
        )
    }
        .sortedByDescending { it.rankScore }

    val withDecisions = ranked.mapIndexed { index, rf ->
        rf.copy(decision = if (index == 0) FileDecision.KEEP else FileDecision.DELETE)
    }

    val totalRecoverable = withDecisions
        .filter { it.decision == FileDecision.DELETE }
        .sumOf { it.entry.sizeBytes }

    return DuplicateGroup(
        groupId = "$directory|${normalizedName.base}.${normalizedName.extension}",
        groupType = groupType,
        normalizedName = normalizedName,
        directory = directory,
        rankedFiles = withDecisions,
        totalRecoverableBytes = totalRecoverable,
    )
}

/**
 * Stage 1 — Exact Byte Duplicates.
 * Groups files by exact byte size; for size collisions, computes SHA-256 and flags
 * identical hashes as EXACT_DUPLICATE. Keeps newest/cleanest-named copy.
 */
class Sha256DuplicateStage(private val hasher: (FileEntry) -> String?) : AnalyzerStage {
    override val name: String = "Exact duplicate detection (SHA-256)"

    override fun analyze(entries: List<FileEntry>, prior: ScanResult): ScanResult {
        val readable = entries.filter { it.isReadable && it.sizeBytes > 0 }
        val bySize = readable.groupBy { it.sizeBytes }

        val collisions = bySize.values.filter { it.size > 1 }
        val hashGroups = mutableMapOf<String, MutableList<FileEntry>>()

        collisions.forEach { sameSize ->
            sameSize.forEach { entry ->
                val hash = hasher(entry) ?: return@forEach
                hashGroups.getOrPut(hash) { mutableListOf() }.add(entry)
            }
        }

        val groups = hashGroups.values
            .filter { it.size > 1 }
            .map { members ->
                rankGroup(members, GroupType.EXACT_DUPLICATE, NameNormalizerStage.normalize(members[0].displayName), members[0].directory)
            }

        val recoverable = groups.sumOf { it.totalRecoverableBytes }
        return prior.copy(groups = prior.groups + groups, totalRecoverableBytes = prior.totalRecoverableBytes + recoverable)
    }
}

/**
 * Stage 2 — Smart Filename Version Grouping.
 * Normalizes filenames (same directory + extension) by stripping version/copy tokens
 * and groups related versions together as OLDER_VERSION.
 */
class NameNormalizerStage : AnalyzerStage {
    override val name: String = "Filename version grouping"

    override fun analyze(entries: List<FileEntry>, prior: ScanResult): ScanResult {
        // Group by (directory, extension, normalized base) where more than one file shares the key
        val byKey = entries.groupBy { entry ->
            val norm = normalize(entry.displayName)
            Triple(entry.directory, norm.extension, norm.base)
        }

        val versionGroups = byKey.values
            .filter { it.size > 1 }
            .map { members ->
                val norm = normalize(members[0].displayName)
                rankGroup(members, GroupType.OLDER_VERSION, norm, members[0].directory)
            }

        val recoverable = versionGroups.sumOf { it.totalRecoverableBytes }
        return prior.copy(groups = prior.groups + versionGroups, totalRecoverableBytes = prior.totalRecoverableBytes + recoverable)
    }

    companion object {
        // Tokens stripped when normalizing a filename for version grouping.
        private val versionTokens = listOf(
            "_final", "_copy", "(1)", "(2)", "(3)", "(4)", "(5)",
            "_v1", "_v2", "_v3", "-v1", "-v2", "-v3",
            " final", " copy", " draft",
        )

        private val dateTokenRegex = Regex("""(_|-)?\d{4}([-]?\d{2}[-]?\d{2})?$""")
        private val semverTokenRegex = Regex("""[-_]v?\d+\.\d+(?:\.\d+)?$""")

        fun normalize(displayName: String): NormalizedName {
            val name = displayName
            val dotIndex = name.lastIndexOf('.')
            val (base, ext) = if (dotIndex > 0) {
                Pair(name.substring(0, dotIndex), name.substring(dotIndex + 1))
            } else {
                Pair(name, "")
            }

            var normalized = base.lowercase()
            // Strip semver patterns FIRST (e.g. -v1.2, _v2.1.3) so they aren't
            // partially consumed by shorter version tokens like -v1.
            normalized = normalized.replace(semverTokenRegex, "")
            versionTokens.forEach { token ->
                normalized = normalized.replace(Regex(Regex.escape(token)), "")
            }
            normalized = normalized.replace(dateTokenRegex, "")
            // Collapse repeated separators/spaces and trim
            normalized = normalized.replace(Regex("""[ _-]+"""), "_").trim { it == '_' }

            return NormalizedName(base = normalized, extension = ext.lowercase())
        }

        /** Extract a SemanticVersion from a filename like "app-v1.2.apk" or "report-2.1.pdf". */
        fun extractVersion(displayName: String): SemanticVersion? {
            val dotIndex = displayName.lastIndexOf('.')
            val base = if (dotIndex > 0) displayName.substring(0, dotIndex) else displayName
            val match = semverTokenRegex.find(base) ?: return null
            var versionPart = match.value.lowercase()
            versionPart = versionPart.removePrefix("_").removePrefix("-").removePrefix("v")
            val parts = versionPart.split(".")
            if (parts.size < 2) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = if (parts.size > 2) parts[2].toIntOrNull() ?: 0 else 0
            return SemanticVersion(major = major, minor = minor, patch = patch)
        }
    }
}

/**
 * Extension: derive a naive SemanticVersion from a FileEntry's displayName.
 */
private val FileEntry.semanticVersion: com.example.folderpruner.domain.model.SemanticVersion?
    get() = NameNormalizerStage.extractVersion(displayName)

/** Extension: APK long version code, populated only by APK metadata enrichment (else null). */
private var apkLongVersionCode: Map<String, Long> = emptyMap()
private val FileEntry.longVersionCode: Long?
    get() = apkLongVersionCode[uri]
internal fun setApkLongVersionCode(uri: String, code: Long) { apkLongVersionCode += uri to code }

/**
 * Stage 3 — Optional APK Metadata Enrichment.
 * Inspects packageName + longVersionCode from a temp copy (strict try/finally cleanup)
 * so APKs with different filenames group by package + version code.
 */
class ApkMetadataStage(private val apkMetadata: (FileEntry) -> Pair<String, Long>?) : AnalyzerStage {
    override val name: String = "APK metadata enrichment"

    override fun analyze(entries: List<FileEntry>, prior: ScanResult): ScanResult {
        entries.filter { it.extension.equals("apk", ignoreCase = true) && it.isReadable }.forEach { entry ->
            val meta = apkMetadata(entry) ?: return@forEach
            val (_, versionCode) = meta
            setApkLongVersionCode(entry.uri, versionCode)
        }
        // Enrichment annotates entries; it does not create new groups or change recoverable bytes.
        return prior
    }
}

/**
 * Pure-Kotlin domain engine. Composes stages and produces keep/delete decisions.
 * Zero Android dependencies — runnable on plain JVM.
 */
class DuplicateEngine(
    private val pipeline: ScanPipeline,
) {
    fun scan(entries: List<FileEntry>, currentFolder: String?): ScanResult {
        return pipeline.run(entries, currentFolder)
    }

    companion object {
        fun defaultPipeline(hasher: (FileEntry) -> String?, apkMetadata: (FileEntry) -> Pair<String, Long>?): DuplicateEngine {
            val stages = listOf<AnalyzerStage>(
                Sha256DuplicateStage(hasher),
                NameNormalizerStage(),
                ApkMetadataStage(apkMetadata),
            )
            return DuplicateEngine(ScanPipeline(stages))
        }
    }
}
