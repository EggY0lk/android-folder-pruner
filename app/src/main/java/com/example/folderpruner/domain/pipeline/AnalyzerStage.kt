package com.example.folderpruner.domain.pipeline

import com.example.folderpruner.domain.model.FileEntry
import com.example.folderpruner.domain.model.ScanResult

/** A pluggable analyzer stage. Pure Kotlin — no Android types. */
interface AnalyzerStage {
    /** Name shown in progress/UI. */
    val name: String

    /**
     * Analyze [entries] and return an updated [ScanResult].
     * Implementations must be side-effect-free and thread-safe.
     */
    fun analyze(entries: List<FileEntry>, prior: ScanResult): ScanResult
}
