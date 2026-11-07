package com.essential.services.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.HttpsURLConnection
import java.net.URL
import java.util.regex.Pattern

object AppColorManager {
    private const val PREFS_NAME = "app_colors_prefs"
    private const val KEY_COLORS_JSON = "cached_colors_json"
    const val COLORS_URL = "https://mariohdel14nasa-lang.github.io/essential-app-catalog/appscolors.json"

    fun getCachedColors(context: Context): List<Color> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawStr = prefs.getString(KEY_COLORS_JSON, null) ?: return emptyList()
        return parseColorsFromString(rawStr)
    }

    fun getNamedColor(context: Context, colorName: String, defaultColor: Color): Color {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawStr = prefs.getString(KEY_COLORS_JSON, null) ?: return defaultColor
        try {
            val trimmed = rawStr.trim()
            // 1. Check JSON Map
            if (trimmed.startsWith("{")) {
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any>? = gson.fromJson(trimmed, mapType)
                map?.forEach { (k, v) ->
                    if (k.equals(colorName, ignoreCase = true) && v is String) {
                        parseHex(v)?.let { return it }
                    }
                }
            }
            // 2. Check XML tag format <color name="green">#HEX</color>
            val xmlPattern = Pattern.compile("<color\\s+name=[\"']$colorName[\"'][^>]*>\\s*(#[A-Fa-f0-9]{6,8})\\s*</color>", Pattern.CASE_INSENSITIVE)
            val matcher = xmlPattern.matcher(rawStr)
            if (matcher.find()) {
                parseHex(matcher.group(1))?.let { return it }
            }
        } catch (_: Exception) {}

        // Fallback to indexed colors or default
        val colors = getCachedColors(context)
        return when (colorName.lowercase()) {
            "green" -> colors.getOrNull(6) ?: colors.getOrNull(0) ?: defaultColor
            else -> defaultColor
        }
    }

    suspend fun fetchAndCacheColors(context: Context, urlString: String = COLORS_URL): List<Color> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    if (jsonString.isNotBlank()) {
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_COLORS_JSON, jsonString).apply()
                        return@withContext parseColorsFromString(jsonString)
                    }
                }
                emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun parseColorsFromString(content: String): List<Color> {
        val colors = mutableListOf<Color>()
        try {
            val trimmed = content.trim()
            // 1. Try parsing as JSON Map or JSON Array first
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    val gson = Gson()
                    if (trimmed.startsWith("{")) {
                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        val map: Map<String, Any>? = gson.fromJson(trimmed, mapType)
                        map?.values?.forEach { v ->
                            if (v is String) {
                                parseHex(v)?.let { colors.add(it) }
                            }
                        }
                    } else {
                        val listType = object : TypeToken<List<Any>>() {}.type
                        val list: List<Any>? = gson.fromJson(trimmed, listType)
                        list?.forEach { item ->
                            if (item is String) {
                                parseHex(item)?.let { colors.add(it) }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. If no colors parsed yet, or for XML tags like <color name="color">#C0LORR</color> / plain text, use Regex
            if (colors.isEmpty()) {
                val pattern = Pattern.compile("#([A-Fa-f0-9]{6,8})")
                val matcher = pattern.matcher(content)
                while (matcher.find()) {
                    val hex = matcher.group(1)
                    parseHex(hex)?.let { colors.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return colors
    }

    private fun parseHex(hexStr: String?): Color? {
        if (hexStr.isNullOrBlank()) return null
        return try {
            val clean = hexStr.trim().removePrefix("#")
            val colorLong = when (clean.length) {
                6 -> "FF$clean".toLong(16)
                8 -> clean.toLong(16)
                else -> return null
            }
            Color(colorLong)
        } catch (_: Exception) {
            null
        }
    }
}
