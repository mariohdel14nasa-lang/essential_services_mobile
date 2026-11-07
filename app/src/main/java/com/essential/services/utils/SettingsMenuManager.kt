package com.essential.services.utils

import android.content.Context
import android.content.SharedPreferences
import com.essential.services.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.HttpsURLConnection
import java.net.URL

data class SettingsMenuItem(
    val id: String = "",
    val type: String = "toggle",
    val icon: String = "",
    val title: String = "",
    val description: String? = null,
    val action: String = "",
    val minSdk: Int = 0,
    val colorKey: String? = null,
    val targetUrl: String? = null,
    val fileName: String? = null,
    val items: List<SettingsMenuItem>? = null,
    val translationsURLfetch: String? = null,
)

data class SettingsMenuSection(
    val id: String = "",
    val title: String = "",
    val items: List<SettingsMenuItem> = emptyList(),
)

data class SettingsMenuData(
    val appVersion: String = "",
    val version: String = "1.0",
    val title: String = "Ajustes",
    val sections: List<SettingsMenuSection> = emptyList(),
)

data class MultiVersionSettingsContainer(
    val versions: List<SettingsMenuData> = emptyList(),
)

object SettingsMenuManager {
    private const val PREFS_NAME = "settings_menu_prefs"
    private const val KEY_SETTINGS_MENU_JSON = "cached_settings_menu_json"
    const val SETTINGS_MENU_URL = "https://mariohdel14nasa-lang.github.io/essential-app-catalog/appsettings.json"

    val DEFAULT_FALLBACK_JSON = """
    {
      "versions": [
        {
          "appVersion": "1.4",
          "title": "Ajustes v1.4",
          "sections": [
            {
              "id": "language_section",
              "title": "Idioma",
              "items": [
                {
                  "id": "language_button",
                  "type": "selector_card",
                  "icon": "rounded_blur_on_24",
                  "title": "Language",
                  "colorKey": "green",
                  "items": [
                    {
                      "title": "English",
                      "colorKey": "green",
                      "action": "SET_LANGUAGE",
                      "translationsURLfetch": "to be added"
                    },
                    {
                      "title": "Spanish",
                      "colorKey": "green",
                      "action": "SET_LANGUAGE",
                      "translationsURLfetch": "to be added"
                    }
                  ]
                }
              ]
            },
            {
              "id": "profiles_section",
              "title": "Más perfiles",
              "items": [
                {
                  "id": "custom_profile_toggle",
                  "type": "toggle",
                  "icon": "ic_apps_24",
                  "title": "Apps",
                  "description": "Todas las Apps Esenciales",
                  "action": "TOGGLE_CUSTOM_PROFILE"
                }
              ]
            },
            {
              "id": "general_section",
              "title": "General",
              "items": [
                {
                  "id": "deeplink_sharing_toggle",
                  "type": "toggle",
                  "icon": "rounded_globe_24",
                  "title": "Compartir Datos de Cuenta Essential con otras apps",
                  "description": "Permitir que otras aplicaciones accedan a tus datos de cuenta Essential vía URIs y Deep Links",
                  "action": "TOGGLE_DEEPLINK_SHARING"
                },
                {
                  "id": "allow_locked_toggle",
                  "type": "toggle",
                  "icon": "rounded_lock_24",
                  "title": "Permitir cuando está bloqueado",
                  "description": "Abrir Essential Services desde el tile rápido incluso cuando la pantalla está bloqueada",
                  "action": "TOGGLE_ALLOW_WHEN_LOCKED"
                },
                {
                  "id": "show_ripple_toggle",
                  "type": "toggle",
                  "icon": "rounded_blur_on_24",
                  "title": "Mostrar efecto onda",
                  "action": "TOGGLE_SHOW_RIPPLE",
                  "minSdk": 33
                }
              ]
            },
            {
              "id": "update_section",
              "title": "",
              "items": [
                {
                  "id": "update_button",
                  "type": "feature_card",
                  "icon": "ic_download_new",
                  "title": "Update",
                  "colorKey": "green",
                  "action": "CHECK_UPDATE",
                  "targetUrl": "https://github.com/mariohdel14nasa-lang/essential_services_mobile/releases/latest/download/essentialservices.apk"
                }
              ]
            },
            {
              "id": "appearance_section",
              "title": "Apariencia",
              "items": [
                {
                  "id": "pitch_black_toggle",
                  "type": "toggle",
                  "icon": "rounded_palette_24",
                  "title": "Tema negro puro",
                  "description": "Usar fondo negro puro en modo oscuro",
                  "action": "TOGGLE_PITCH_BLACK"
                },
                {
                  "id": "use_blur_toggle",
                  "type": "toggle",
                  "icon": "rounded_blur_on_24",
                  "title": "Usar desenfoque",
                  "description": "Habilitar elementos de desenfoque progresivo en la UI",
                  "action": "TOGGLE_USE_BLUR"
                }
              ]
            },
            {
              "id": "credits_section",
              "title": "Créditos a...",
              "items": [
                {
                  "id": "credits_markdown",
                  "type": "credits",
                  "fileName": "credits.md"
                }
              ]
            },
            {
              "id": "about_section",
              "title": "Acerca de",
              "items": [
                {
                  "id": "about_app_info",
                  "type": "about"
                }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    fun getTargetAppVersion(context: Context): String {
        return try {
            context.getString(R.string.settings_menu_version)
        } catch (_: Exception) {
            "1.0"
        }
    }

    fun getCachedSettingsMenu(context: Context): SettingsMenuData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawStr = prefs.getString(KEY_SETTINGS_MENU_JSON, null) ?: DEFAULT_FALLBACK_JSON
        val targetVersion = getTargetAppVersion(context)
        return parseSettingsJsonForVersion(rawStr, targetVersion)
    }

    suspend fun fetchAndCacheSettingsMenu(context: Context, urlString: String = SETTINGS_MENU_URL): SettingsMenuData? {
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
                        prefs.edit().putString(KEY_SETTINGS_MENU_JSON, jsonString).apply()
                        val targetVersion = getTargetAppVersion(context)
                        return@withContext parseSettingsJsonForVersion(jsonString, targetVersion)
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun parseSettingsJsonForVersion(jsonStr: String, targetVersion: String): SettingsMenuData {
        try {
            val gson = Gson()
            val trimmed = jsonStr.trim()

            // 1. Try object containing "versions": [ { "appVersion": "1.0", ... }, ... ]
            if (trimmed.startsWith("{")) {
                val container = gson.fromJson(trimmed, MultiVersionSettingsContainer::class.java)
                if (container != null && !container.versions.isNullOrEmpty()) {
                    val matched = container.versions.find {
                        it.appVersion.equals(targetVersion, ignoreCase = true) || it.version.equals(targetVersion, ignoreCase = true)
                    } ?: container.versions.first()
                    return matched
                }
            }

            // 2. Try array [ { "appVersion": "1.0", ... }, ... ]
            if (trimmed.startsWith("[")) {
                val type = object : TypeToken<List<SettingsMenuData>>() {}.type
                val list: List<SettingsMenuData>? = gson.fromJson(trimmed, type)
                if (!list.isNullOrEmpty()) {
                    val matched = list.find {
                        it.appVersion.equals(targetVersion, ignoreCase = true) || it.version.equals(targetVersion, ignoreCase = true)
                    } ?: list.first()
                    return matched
                }
            }

            // 3. Try direct single object
            val single = gson.fromJson(trimmed, SettingsMenuData::class.java)
            if (single != null && !single.sections.isNullOrEmpty()) {
                return single
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to default
        return try {
            val gson = Gson()
            val container = gson.fromJson(DEFAULT_FALLBACK_JSON, MultiVersionSettingsContainer::class.java)
            container.versions.first()
        } catch (_: Exception) {
            SettingsMenuData()
        }
    }

    fun getDrawableResId(context: Context, iconName: String): Int {
        if (iconName.isBlank()) return R.drawable.rounded_info_24
        val id = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.rounded_info_24
    }
}
