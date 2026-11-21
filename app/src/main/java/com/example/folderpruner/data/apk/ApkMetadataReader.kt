package com.example.folderpruner.data.apk

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Reads an APK's [packageName] and [longVersionCode] without requiring
 * [android.Manifest.permission.MANAGE_EXTERNAL_STORAGE].
 *
 * The APK is copied from a content [Uri] to a temp file under the app's cache dir,
 * then inspected via [PackageManager.getPackageArchiveInfo]. The temp file is always
 * deleted (strict try/finally), with [File.deleteOnExit] as a backstop, so it can
 * never leak even when an exception is thrown.
 */
class ApkMetadataReader(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val contentResolver: ContentResolver = context.contentResolver

    /** Flag values for [PackageManager.getPackageArchiveInfo]. The named
     * [PackageManager] constants (GET_PACKAGES, GET_LONG_VERSION_CODES) were removed
     * from the API-33+ surface, so we keep the historical int values; the int overload
     * still honors them and keeps this runnable on minSdk 24 without loading the
     * API-33+ PackageInfoFlags class. */
    private companion object {
        private const val FLAG_GET_PACKAGES = 0x00000001
        private const val FLAG_GET_LONG_VERSION_CODES = 0x00000100
    }

    fun readMetadata(uri: Uri): Pair<String, Long>? {
        val tempFile = File.createTempFile("folderpruner_apk_", ".apk", context.cacheDir).apply {
            // Safety net: if explicit delete in `finally` ever fails, the VM removes it.
            deleteOnExit()
        }
        var input: InputStream? = null
        var output: FileOutputStream? = null
        return try {
            input = contentResolver.openInputStream(uri) ?: return null
            output = FileOutputStream(tempFile)
            input.copyTo(output)
            readArchive(tempFile.absolutePath) ?: return null
        } catch (_: Exception) {
            null
        } finally {
            output?.close()
            input?.close()
            // Explicit cleanup; deleteOnExit is the backstop.
            tempFile.delete()
        }
    }

    private fun readArchive(path: String): Pair<String, Long>? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            FLAG_GET_PACKAGES or FLAG_GET_LONG_VERSION_CODES
        } else {
            FLAG_GET_PACKAGES
        }
        return packageManager.getPackageArchiveInfo(path, flags)?.let { info ->
            val packageName = info.packageName ?: return@let null
            Pair(packageName, versionCodeOf(info))
        }
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
}
