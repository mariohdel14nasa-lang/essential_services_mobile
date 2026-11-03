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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.UUID

data class SerializedAppItem(
    val id: String = UUID.randomUUID().toString(),
    val appName: String = "",
    val name: String? = null,
    val title: String? = null,
    val downloadUrl: String? = null,
    val url: String? = null,
    val apkUrl: String? = null,
    val link: String? = null,
    val iconUrl: String? = null,
    val icon: String? = null,
    val imageUrl: String? = null,
    val categoryId: String? = null,
    val category: String? = null,
    val versionName: String = "1.0",
    val version: String? = null,
    val packageName: String = "",
    val description: String? = null,
    val desc: String? = null,
    val isUserAdded: Boolean = false,
) {
    fun getEffectiveName(): String = (appName.ifBlank { name ?: title ?: "" }).ifBlank { "App" }
    fun getEffectiveDownloadUrl(): String? = (downloadUrl ?: url ?: apkUrl ?: link)?.takeIf { it.isNotBlank() }
    fun getEffectiveIconUrl(): String? = (iconUrl ?: icon ?: imageUrl)?.takeIf { it.isNotBlank() }
    fun getEffectiveCategoryId(): String = categoryId ?: category ?: "utilities"
    fun getEffectiveVersion(): String = (versionName.ifBlank { version ?: "1.0" })
    fun getEffectiveDescription(): String? = description ?: desc
}

class AppRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun fetchRemoteDatabase(urlString: String = REMOTE_DATABASE_URL): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = (if (urlString.startsWith("https", ignoreCase = true)) {
                    url.openConnection() as HttpsURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    if (jsonString.isNotBlank()) {
                        prefs.edit().putString(KEY_CACHED_REMOTE_JSON, jsonString).apply()
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getAllApps(): List<AppItem> {
        val resultList = mutableListOf<AppItem>()

        // 1. Read apps from cached remote database JSON (or asset apps.json if not cached yet)
        val catalogApps = loadCatalogApps()
        resultList.addAll(catalogApps)

        // 2. Read user added apps from SharedPreferences
        val userApps = getUserAddedApps()
        resultList.addAll(userApps)

        // 3. Read local scanned APK files
        val localApks = ApkScanner.scanApks(context)
        for (apk in localApks) {
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

    private fun loadCatalogApps(): List<AppItem> {
        // Try cached remote JSON first
        val cachedRemoteStr = prefs.getString(KEY_CACHED_REMOTE_JSON, null)
        if (!cachedRemoteStr.isNullOrBlank()) {
            val parsed = parseJsonToAppItems(cachedRemoteStr)
            if (parsed.isNotEmpty()) return parsed
        }

        // Fallback to assets/apks/apps.json or assets/apps.json
        val possiblePaths = listOf("apks/apps.json", "apps.json")
        for (assetPath in possiblePaths) {
            try {
                context.assets.open(assetPath).use { input ->
                    val jsonStr = input.bufferedReader().use { it.readText() }
                    val parsed = parseJsonToAppItems(jsonStr)
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    private fun parseJsonToAppItems(jsonStr: String): List<AppItem> {
        val list = mutableListOf<AppItem>()
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("[")) {
                val type = object : TypeToken<List<SerializedAppItem>>() {}.type
                val items: List<SerializedAppItem>? = gson.fromJson(jsonStr, type)
                items?.forEach { s ->
                    list.add(mapSerializedToAppItem(s))
                }
            } else if (trimmed.startsWith("{")) {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                val mapObj: Map<String, Any>? = gson.fromJson(jsonStr, mapType)
                val rawItems = mapObj?.get("items") ?: mapObj?.get("apps") ?: mapObj?.get("data")
                if (rawItems != null) {
                    val rawJson = gson.toJson(rawItems)
                    val type = object : TypeToken<List<SerializedAppItem>>() {}.type
                    val items: List<SerializedAppItem>? = gson.fromJson(rawJson, type)
                    items?.forEach { s ->
                        list.add(mapSerializedToAppItem(s))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun mapSerializedToAppItem(s: SerializedAppItem): AppItem {
        val downloadUrl = s.getEffectiveDownloadUrl()
        return AppItem(
            id = s.id,
            appName = s.getEffectiveName(),
            downloadUrl = downloadUrl,
            iconUrl = s.getEffectiveIconUrl(),
            category = AppCategory.fromId(s.getEffectiveCategoryId()),
            versionName = s.getEffectiveVersion(),
            packageName = s.packageName,
            description = s.getEffectiveDescription(),
            isWebLink = !downloadUrl.isNullOrBlank(),
            isUserAdded = s.isUserAdded,
        )
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
        return getUserAddedSerializedApps().map { s -> mapSerializedToAppItem(s) }
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
        private const val KEY_CACHED_REMOTE_JSON = "cached_remote_apps_json"
        const val REMOTE_DATABASE_URL = "https://mariohdel14nasa-lang.github.io/essential-app-catalog/database.json"
    }
}
