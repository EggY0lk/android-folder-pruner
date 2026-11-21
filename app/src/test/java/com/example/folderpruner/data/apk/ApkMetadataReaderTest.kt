package com.example.folderpruner.data.apk

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.mockk.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class ApkMetadataReaderTest {

    private fun info(packageName: String, versionCode: Int): PackageInfo {
        return PackageInfo().apply {
            this.packageName = packageName
            this.versionCode = versionCode
        }
    }

    @Test
    fun `readMetadata returns package name and long version code`() {
        val cacheDir = Files.createTempDirectory("folderpruner-test").toFile()
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        val resolver = mockk<ContentResolver>()
        every { context.cacheDir } returns cacheDir
        every { context.packageManager } returns pm
        every { context.contentResolver } returns resolver
        every { pm.getPackageArchiveInfo(any(), any<Int>()) } returns info("com.example.app", 42)
        every { resolver.openInputStream(any()) } returns ByteArrayInputStream("fake-apk".toByteArray())

        val reader = ApkMetadataReader(context)
        assertEquals(Pair("com.example.app", 42L), reader.readMetadata(Uri.parse("content://example/app.apk")))
    }

    @Test
    fun `readMetadata returns null when the archive is not a valid APK`() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        val resolver = mockk<ContentResolver>()
        every { context.cacheDir } returns Files.createTempDirectory("folderpruner-test").toFile()
        every { context.packageManager } returns pm
        every { context.contentResolver } returns resolver
        every { pm.getPackageArchiveInfo(any(), any<Int>()) } returns null
        every { resolver.openInputStream(any()) } returns ByteArrayInputStream("not-an-apk".toByteArray())

        val reader = ApkMetadataReader(context)
        assertNull(reader.readMetadata(Uri.parse("content://example/app.apk")))
    }

    @Test
    fun `readMetadata returns null when the content stream cannot be opened`() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        val resolver = mockk<ContentResolver>()
        every { context.cacheDir } returns Files.createTempDirectory("folderpruner-test").toFile()
        every { context.packageManager } returns pm
        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(any()) } returns null

        val reader = ApkMetadataReader(context)
        assertNull(reader.readMetadata(Uri.parse("content://example/app.apk")))
    }
}
