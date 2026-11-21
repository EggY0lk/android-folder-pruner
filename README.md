# Folder Pruner

Native Android app (Kotlin, Jetpack Compose, Material 3, Coroutines/Flow) that scans any folder for exact duplicate files and older versions of files, lets you review all matches, and safely deletes the selected files.

## Features

- **Folder picker** via Storage Access Framework (`OpenDocumentTree`) — works on modern Android without `MANAGE_EXTERNAL_STORAGE`
- **Include subfolders** toggle — recursive vs top-level scan
- **Pass 1 — Exact byte duplicates:** groups by byte size, then SHA-256 hashes only on size collisions; flags `"Exact Duplicate"`, keeps newest/cleanest-named copy
- **Pass 2 — Smart filename version grouping:** normalizes filenames (same directory + extension) by stripping `(1)`, `(2)`, `_v1`, `-v2.1.3`, `_final`, `_copy`, date/timestamp suffixes; groups related versions
- **Pass 3 — APK metadata enrichment:** inspects `packageName` and `longVersionCode` from a temp copy (strict `try/finally` cleanup) so differently-named APKs group by package + version code
- **Ranking rule:** within each group, rank by semantic version / `longVersionCode` descending (when parsed), tie-break by `LAST_MODIFIED` descending; highest-ranked = `Keep`, rest = `Delete`
- **Safe deletion:** filter tabs (`All`, `Exact Duplicates`, `Older Versions`), grouped cards with file-type icons, status badges, checkboxes; bottom confirmation bar shows exact file count and MB/GB to free; confirmation dialog before `DocumentsContract.deleteDocument()`; no auto-delete

## Architecture

```
com.example.folderpruner/
  domain/          # pure Kotlin, zero Android deps — JVM-testable
    model/         # FileEntry, RankedFile, DuplicateGroup, ScanResult, ScanProgress
    pipeline/      # pluggable AnalyzerStage pipeline (Pass 1/2/3), rankGroup, DuplicateEngine
  data/            # Android I/O layer
    uri/           # SAF file tree traversal, persisted URI permissions
    hasher/        # streaming SHA-256 via ContentResolver
    apk/           # temp-copy APK metadata reader (try/finally cleanup)
    deletion/      # DocumentsContract.deleteDocument
  ui/              # Jetpack Compose + Material 3
    screen/        # ScanScreen, ReviewScreen
    component/     # GroupCard, FileRow, FilterTab, DeletionConfirmationBar
    viewmodel/     # ScanViewModel (Flow progress, groups)
    dialog/        # ConfirmDialog
```

## Build & Test

```bash
./gradlew testDebugUnitTest assembleDebug
```

## CI

GitHub Actions runs `testDebugUnitTest assembleDebug` on every push/PR to `main` and uploads `app-debug.apk` as a workflow artifact (`.github/workflows/android-ci.yml`).

## Git

Clean, atomic commits per milestone. `.gitignore` excludes `.gradle/`, `build/`, `local.properties`, `*.apk`, `*.aab`, `.idea/`.
