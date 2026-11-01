package com.essential.services.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream

data class ApkItem(
    val fileName: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkFile: File,
)

object ApkScanner {

    fun scanApks(context: Context): List<ApkItem> {
        val apkItems = mutableListOf<ApkItem>()
        val apkDir = File(context.cacheDir, "apks").apply { mkdirs() }

        // 1. Scan assets/apks/ folder
        try {
            val assetFiles = context.assets.list("apks") ?: emptyArray()
            for (fileName in assetFiles) {
                if (fileName.endsWith(".apk", ignoreCase = true)) {
                    val destFile = File(apkDir, fileName)
                    if (!destFile.exists() || destFile.length() == 0L) {
                        try {
                            context.assets.open("apks/$fileName").use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    parseApkFile(context, destFile)?.let { apkItems.add(it) }
                }
            }
        } catch (_: Exception) {}

        // 2. Scan internal cache apks/ directory for any manually copied APKs
        try {
            val localFiles = apkDir.listFiles { file -> file.isFile && file.name.endsWith(".apk", ignoreCase = true) } ?: emptyArray()
            for (file in localFiles) {
                if (apkItems.none { it.apkFile.name == file.name }) {
                    parseApkFile(context, file)?.let { apkItems.add(it) }
                }
            }
        } catch (_: Exception) {}

        return apkItems
    }

    private fun parseApkFile(context: Context, file: File): ApkItem? {
        if (!file.exists() || !file.name.endsWith(".apk", ignoreCase = true)) return null
        return try {
            val pm = context.packageManager
            val packageInfo: PackageInfo? = pm.getPackageArchiveInfo(file.absolutePath, 0)
            val appInfo: ApplicationInfo? = packageInfo?.applicationInfo

            var appName = file.nameWithoutExtension
            var icon: Drawable? = null

            if (appInfo != null) {
                appInfo.sourceDir = file.absolutePath
                appInfo.publicSourceDir = file.absolutePath
                appName = try {
                    appInfo.loadLabel(pm).toString()
                } catch (_: Exception) {
                    file.nameWithoutExtension
                }
                icon = try {
                    appInfo.loadIcon(pm)
                } catch (_: Exception) {
                    null
                }
            }

            ApkItem(
                fileName = file.name,
                appName = appName,
                packageName = packageInfo?.packageName ?: "",
                versionName = packageInfo?.versionName ?: "1.0",
                icon = icon,
                apkFile = file,
            )
        } catch (_: Exception) {
            null
        }
    }
}
