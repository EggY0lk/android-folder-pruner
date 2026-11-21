package com.example.folderpruner.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.folderpruner.data.apk.ApkMetadataReader
import com.example.folderpruner.data.deletion.UriDeletionRepository
import com.example.folderpruner.data.hasher.Sha256Hasher
import com.example.folderpruner.data.uri.SafUriRepository
import com.example.folderpruner.domain.model.ScanProgress
import com.example.folderpruner.domain.model.ScanResult
import com.example.folderpruner.domain.pipeline.DuplicateEngine
import com.example.folderpruner.domain.model.DuplicateGroup
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val contentResolver: ContentResolver = context.contentResolver

    private val safUriRepository = SafUriRepository(context)
    private val sha256Hasher = Sha256Hasher(contentResolver)
    private val apkMetadataReader = ApkMetadataReader(context)
    private val deletionRepository = UriDeletionRepository(contentResolver)

    private val engine = DuplicateEngine.defaultPipeline(
        hasher = { entry -> sha256Hasher.hash(Uri.parse(entry.uri)) },
        apkMetadata = { entry -> apkMetadataReader.readMetadata(Uri.parse(entry.uri)) },
    )

    private val _progress = MutableStateFlow(ScanProgress(0, null, 0L))
    val progress: StateFlow<ScanProgress> = _progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ScanProgress(0, null, 0L))

    private val _groups = MutableStateFlow(emptyList<com.example.folderpruner.domain.model.DuplicateGroup>())
    val groups: StateFlow<List<com.example.folderpruner.domain.model.DuplicateGroup>> = _groups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _selected = MutableStateFlow(emptySet<String>())
    val selectedUris: StateFlow<Set<String>> = _selected.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    private val _includeSubfolders = MutableStateFlow(false)
    val includeSubfolders: StateFlow<Boolean> = _includeSubfolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setIncludeSubfolders(value: Boolean) {
        _includeSubfolders.value = value
    }

    fun toggleFile(uri: String) {
        val current = _selected.value
        _selected.value = if (current.contains(uri)) current - uri else current + uri
    }

    fun pickFolder(treeUri: String, flags: Int) {
        viewModelScope.launch {
            _error.value = null
            _groups.value = emptyList<DuplicateGroup>()
            safUriRepository.takePersistableUriPermission(Uri.parse(treeUri), flags)
            scan(treeUri)
        }
    }

    private fun scan(treeUri: String) {
        val handler = CoroutineExceptionHandler { _, exception ->
            _error.value = exception.message ?: "Scan failed"
            _progress.value = _progress.value.copy(filesScanned = 0, currentFolder = null, totalRecoverableBytes = 0L)
        }
        viewModelScope.launch(handler) {
            val entries = withContext(Dispatchers.IO) {
                safUriRepository.buildFileTree(Uri.parse(treeUri), _includeSubfolders.value)
            }
            _progress.value = _progress.value.copy(filesScanned = entries.size, currentFolder = treeUri)
            val result: ScanResult = withContext(Dispatchers.IO) {
                engine.scan(entries, treeUri)
            }
            _groups.value = result.groups
            _progress.value = _progress.value.copy(totalRecoverableBytes = result.totalRecoverableBytes)
        }
    }

    fun deleteSelected() {
        val uriStrings = _selected.value.toList()
        val uris = uriStrings.map { Uri.parse(it) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                deletionRepository.delete(uris)
            }
            val failed = results.values.count { !it }
            if (failed == 0) {
                _selected.value = emptySet()
                _error.value = null
                val folder = _progress.value.currentFolder
                if (folder != null) scan(folder)
            } else {
                _error.value = "$failed file(s) could not be deleted."
            }
        }
    }
}
