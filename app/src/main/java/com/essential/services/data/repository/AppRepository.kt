package com.essential.services.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.essential.services.data.model.AppCategory
import com.essential.services.data.model.AppItem
import com.essential.services.utils.ApkScanner
import java.io.File
import java.util.UUID

data class SerializedAppItem(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val downloadUrl: String? = null,
    val iconUrl: String? = null,
    val categoryId: String = "utilities",
    val versionName: String = "1.0",
    val packageName: String = "",
    val description: String? = null,
    val isUserAdded: Boolean = true,
)

class AppRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllApps(): List<AppItem> {
        val resultList = mutableListOf<AppItem>()

        // 1. Read apps from assets/apks/apps.json or assets/apps.json
        val jsonApps = loadAppsFromAssets()
        resultList.addAll(jsonApps)

        // 2. Read user added apps from SharedPreferences
        val userApps = getUserAddedApps()
        resultList.addAll(userApps)

        // 3. Read local scanned APK files
        val localApks = ApkScanner.scanApks(context)
        for (apk in localApks) {
            // Avoid duplicates if already present by appName or packageName
            if (resultList.none { it.appName.equals(apk.appName, ignoreCase = true) || (it.packageName.isNotBlank() && it.packageName == apk.packageName) }) {
                resultList.add(
                    AppItem(
                        id = "apk_${apk.fileName}",
                        appName = apk.appName,
                        iconDrawable = apk.icon,
                        category = AppCategory.UTILITIES,
                        versionName = apk.versionName,
                        packageName = apk.packageName,
                        apkFile = apk.apkFile,
                        isWebLink = false,
                        isUserAdded = false,
                    )
                )
            }
        }

        return resultList
    }

    fun addCustomApp(
        appName: String,
        downloadUrl: String,
        iconUrl: String?,
        category: AppCategory,
        description: String?,
        versionName: String?,
    ) {
        val serialized = SerializedAppItem(
            id = UUID.randomUUID().toString(),
            appName = appName.trim(),
            downloadUrl = downloadUrl.trim(),
            iconUrl = iconUrl?.trim()?.takeIf { it.isNotBlank() },
            categoryId = category.id,
            versionName = versionName?.trim()?.ifBlank { "1.0" } ?: "1.0",
            description = description?.trim()?.takeIf { it.isNotBlank() },
            isUserAdded = true,
        )

        val currentList = getUserAddedSerializedApps().toMutableList()
        currentList.add(serialized)
        saveUserAddedSerializedApps(currentList)
    }

    fun deleteCustomApp(appId: String) {
        val currentList = getUserAddedSerializedApps().toMutableList()
        currentList.removeAll { it.id == appId }
        saveUserAddedSerializedApps(currentList)
    }

    private fun loadAppsFromAssets(): List<AppItem> {
        val list = mutableListOf<AppItem>()
        val possiblePaths = listOf("apks/apps.json", "apps.json")

        for (assetPath in possiblePaths) {
            try {
                context.assets.open(assetPath).use { input ->
                    val jsonStr = input.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<SerializedAppItem>>() {}.type
                    val items: List<SerializedAppItem>? = gson.fromJson(jsonStr, type)
                    items?.forEach { s ->
                        list.add(
                            AppItem(
                                id = s.id,
                                appName = s.appName,
                                downloadUrl = s.downloadUrl,
                                iconUrl = s.iconUrl,
                                category = AppCategory.fromId(s.categoryId),
                                versionName = s.versionName,
                                packageName = s.packageName,
                                description = s.description,
                                isWebLink = !s.downloadUrl.isNullOrBlank(),
                                isUserAdded = false,
                            )
                        )
                    }
                }
                break // loaded successfully
            } catch (_: Exception) {}
        }
        return list
    }

    private fun getUserAddedSerializedApps(): List<SerializedAppItem> {
        val jsonStr = prefs.getString(KEY_USER_APPS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SerializedAppItem>>() {}.type
            gson.fromJson(jsonStr, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getUserAddedApps(): List<AppItem> {
        return getUserAddedSerializedApps().map { s ->
            AppItem(
                id = s.id,
                appName = s.appName,
                downloadUrl = s.downloadUrl,
                iconUrl = s.iconUrl,
                category = AppCategory.fromId(s.categoryId),
                versionName = s.versionName,
                packageName = s.packageName,
                description = s.description,
                isWebLink = !s.downloadUrl.isNullOrBlank(),
                isUserAdded = true,
            )
        }
    }

    private fun saveUserAddedSerializedApps(list: List<SerializedAppItem>) {
        val jsonStr = gson.toJson(list)
        prefs.edit().putString(KEY_USER_APPS, jsonStr).apply()
    }

    fun getCategoryBackgroundBitmap(category: AppCategory): Bitmap? {
        val fileNames = listOf(
            category.bgFileName,
            "bg_${category.id}.png",
            "bg_${category.id}.jpg",
            "${category.id}.png"
        )
        val assetFolders = listOf("backgrounds", "apks", "")

        for (folder in assetFolders) {
            for (fn in fileNames) {
                val fullPath = if (folder.isBlank()) fn else "$folder/$fn"
                try {
                    context.assets.open(fullPath).use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) return bmp
                    }
                } catch (_: Exception) {}
            }
        }

        // Check internal cache dir for uploaded PNG backgrounds
        val cacheBgDir = File(context.cacheDir, "backgrounds")
        for (fn in fileNames) {
            val bgFile = File(cacheBgDir, fn)
            if (bgFile.exists() && bgFile.length() > 0) {
                try {
                    val bmp = BitmapFactory.decodeFile(bgFile.absolutePath)
                    if (bmp != null) return bmp
                } catch (_: Exception) {}
            }
        }

        return null
    }

    companion object {
        private const val PREFS_NAME = "apps_repository_prefs"
        private const val KEY_USER_APPS = "user_added_apps_json"
    }
}
