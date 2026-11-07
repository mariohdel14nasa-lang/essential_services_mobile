package com.essential.services.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.HttpsURLConnection
import java.net.URL

data class RemoteTranslationData(
    val langCode: String = "",
    val langName: String = "",
    val translations: Map<String, String> = emptyMap(),
)

object TranslationManager {
    private const val PREFS_NAME = "translation_prefs"
    private const val KEY_CURRENT_LANG = "current_language_code"
    private const val KEY_ACTIVE_TRANSLATION_MAP = "active_translation_map_json"
    private const val PREFIX_CACHED_LANG = "cached_lang_"

    val translationVersionState = mutableStateOf(0)

    private val gson = Gson()

    val LOCAL_SPANISH_NUMERICAL_TRANSLATIONS = mapOf(
        "1" to "Essential Services",
        "2" to "Ajustes",
        "3" to "Más perfiles",
        "4" to "General",
        "5" to "Apariencia",
        "6" to "Créditos a...",
        "7" to "Acerca de",
        "8" to "Inicio",
        "9" to "Apps",
        "10" to "Todas las Apps Esenciales",
        "11" to "Compartir Datos de Cuenta Essential con otras apps",
        "12" to "Permitir que otras aplicaciones accedan a tus datos de cuenta Essential vía URIs y Deep Links",
        "13" to "Permitir cuando está bloqueado",
        "14" to "Abrir Essential Services desde el tile rápido incluso cuando la pantalla está bloqueada",
        "15" to "Update",
        "16" to "Tema negro puro",
        "17" to "Usar fondo negro puro en modo oscuro",
        "18" to "Usar desenfoque",
        "19" to "Habilitar elementos de desenfoque progresivo en la UI",
        "20" to "Desarrollador y Mantenedor de Essential Services",
        "21" to "Creador original de MeDrop y Essentials UI",
        "22" to "Essential Services permite compartir tu tarjeta de contacto y perfiles sin problemas mediante emulación de tarjeta Host NFC.",
        "23" to "Sitio Web",
        "24" to "Ver en GitHub",
        "25" to "Contacto",
        "26" to "Videos y Pelis",
        "27" to "Utilidades",
        "28" to "Juegos",
        "29" to "Música",
        "30" to "Añadir App por Enlace",
        "31" to "Descargar",
        "32" to "Instalar App",
        "33" to "Creadores de Calculator",
        "34" to "Creador de Draft y base de la app",
        "35" to "Creador de Koda",
        "36" to "Creador de Sefirah",
        "37" to "Desarrollado por mariohdel14nasa-lang con ❤\uFE0F desde 🇪🇸",
    )

    val LOCAL_ENGLISH_NUMERICAL_TRANSLATIONS = mapOf(
        "1" to "Essential Services",
        "2" to "Settings",
        "3" to "More profiles",
        "4" to "General",
        "5" to "Appearance",
        "6" to "Credits To...",
        "7" to "About",
        "8" to "Home",
        "9" to "Apps",
        "10" to "All Essential Apps",
        "11" to "Share Essential Account Data with other apps",
        "12" to "Allow other apps to access your Essential Account data via URIs and Deep Links",
        "13" to "Allow when Locked",
        "14" to "Open Essential Services from the quick tile even when the screen is locked",
        "15" to "Update",
        "16" to "Pitch Black Mode",
        "17" to "Use Pitch Black mode in Dark Mode",
        "18" to "Use Blur",
        "19" to "Enable progressive blur elements across the UI",
        "20" to "Developer and Maintainer of Essential Services",
        "21" to "Original creator of MeDrop and Essentials UI",
        "22" to "Essential Services allows seamless sharing of your contact card and profiles using NFC Host Card Emulation.",
        "23" to "Website",
        "24" to "View on GitHub",
        "25" to "Contact",
        "26" to "Videos & Movies",
        "27" to "Utilities",
        "28" to "Games",
        "29" to "Music",
        "30" to "Add App by Link",
        "31" to "Download",
        "32" to "Install App",
        "33" to "Creators of Calculator",
        "34" to "Creator of Draft and app base",
        "35" to "Creator of Koda",
        "36" to "Creator of Sefirah",
        "37" to "Developed by mariohdel14nasa-lang with ❤\uFE0F from 🇪🇸",
    )

    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_LANG, "es") ?: "es"
    }

    fun setCurrentLanguage(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_LANG, langCode).apply()
        translationVersionState.value++
    }

    fun getActiveTranslationMap(context: Context): Map<String, String> {
        translationVersionState.value

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLang = getCurrentLanguage(context)

        val cachedJson = prefs.getString(PREFIX_CACHED_LANG + currentLang, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val map: Map<String, String>? = gson.fromJson(cachedJson, type)
                if (!map.isNullOrEmpty()) return map
            } catch (_: Exception) {}
        }

        val activeJson = prefs.getString(KEY_ACTIVE_TRANSLATION_MAP, null)
        if (!activeJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val map: Map<String, String>? = gson.fromJson(activeJson, type)
                if (!map.isNullOrEmpty()) return map
            } catch (_: Exception) {}
        }

        return when (currentLang.lowercase()) {
            "en" -> LOCAL_ENGLISH_NUMERICAL_TRANSLATIONS
            else -> LOCAL_SPANISH_NUMERICAL_TRANSLATIONS
        }
    }

    suspend fun fetchAndApplyLanguage(context: Context, langCode: String, fetchUrl: String?): Boolean {
        setCurrentLanguage(context, langCode)
        if (fetchUrl.isNullOrBlank() || !fetchUrl.startsWith("http", ignoreCase = true)) {
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(fetchUrl)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    if (jsonString.isNotBlank()) {
                        var mapToSave: Map<String, String> = emptyMap()

                        if (jsonString.trim().startsWith("{")) {
                            try {
                                val type = object : TypeToken<Map<String, Any>>() {}.type
                                val objMap: Map<String, Any>? = gson.fromJson(jsonString, type)
                                val transObj = objMap?.get("translations")
                                if (transObj != null) {
                                    val transJson = gson.toJson(transObj)
                                    val mapType = object : TypeToken<Map<String, String>>() {}.type
                                    val parsed: Map<String, String>? = gson.fromJson(transJson, mapType)
                                    if (parsed != null) mapToSave = parsed
                                } else {
                                    val mapType = object : TypeToken<Map<String, String>>() {}.type
                                    val parsed: Map<String, String>? = gson.fromJson(jsonString, type)
                                    if (parsed != null) mapToSave = parsed
                                }
                            } catch (_: Exception) {}
                        }

                        if (mapToSave.isNotEmpty()) {
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val jsonMapStr = gson.toJson(mapToSave)
                            prefs.edit()
                                .putString(KEY_ACTIVE_TRANSLATION_MAP, jsonMapStr)
                                .putString(PREFIX_CACHED_LANG + langCode, jsonMapStr)
                                .apply()

                            withContext(Dispatchers.Main) {
                                translationVersionState.value++
                            }
                            return@withContext true
                        }
                    }
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getString(context: Context, keyOrNum: String, fallback: String): String {
        val map = getActiveTranslationMap(context)

        // 1. Direct lookup
        map[keyOrNum]?.let { if (it.isNotBlank() && !it.all { c -> c.isDigit() }) return it }
        map[keyOrNum.lowercase()]?.let { if (it.isNotBlank() && !it.all { c -> c.isDigit() }) return it }

        // 2. Resolve numerical ID from string resource name or text
        val numId = resolveNumericalIdFromResources(context, keyOrNum)
        if (!numId.isNullOrBlank()) {
            map[numId]?.let { if (it.isNotBlank() && !it.all { c -> c.isDigit() }) return it }
            LOCAL_ENGLISH_NUMERICAL_TRANSLATIONS[numId]?.let { return it }
            LOCAL_SPANISH_NUMERICAL_TRANSLATIONS[numId]?.let { return it }
        }

        // 3. If keyOrNum is a number
        if (keyOrNum.toIntOrNull() != null) {
            map[keyOrNum]?.let { if (it.isNotBlank()) return it }
            LOCAL_ENGLISH_NUMERICAL_TRANSLATIONS[keyOrNum]?.let { return it }
            LOCAL_SPANISH_NUMERICAL_TRANSLATIONS[keyOrNum]?.let { return it }
        }

        // 4. Trimmed lookup
        map[keyOrNum.trim()]?.let { if (it.isNotBlank() && !it.all { c -> c.isDigit() }) return it }

        return if (fallback.isNotBlank() && !fallback.all { c -> c.isDigit() }) fallback else keyOrNum
    }

    fun getString(context: Context, resId: Int): String {
        return try {
            val entryName = context.resources.getResourceEntryName(resId)
            val fallback = context.getString(resId)
            getString(context, entryName, fallback)
        } catch (_: Exception) {
            try {
                context.getString(resId)
            } catch (_: Exception) {
                ""
            }
        }
    }

    @Composable
    fun stringResource(resId: Int): String {
        val context = LocalContext.current
        return getString(context, resId)
    }

    private fun resolveNumericalIdFromResources(context: Context, keyStr: String): String? {
        val mappedResName = when (keyStr) {
            "app_name" -> "num_key_app_name"
            "settings_title", "Settings", "Ajustes" -> "num_key_settings_title"
            "settings_section_more_profiles", "More Sections", "Más perfiles", "More profiles", "More Profiles" -> "num_key_settings_section_more_profiles"
            "settings_section_general", "General" -> "num_key_settings_section_general"
            "settings_section_appearance", "Appearence", "Apariencia", "Appearance" -> "num_key_settings_section_appearance"
            "settings_section_credits", "Credits To...", "Créditos a..." -> "num_key_settings_section_credits"
            "about_title", "About", "Acerca de" -> "num_key_about_title"
            "feat_medrop_profile_contact", "Home", "Inicio" -> "num_key_feat_medrop_profile_contact"
            "feat_medrop_profile_custom", "Apps" -> "num_key_feat_medrop_profile_custom"
            "feat_medrop_profile_custom_desc" -> "num_key_feat_medrop_profile_custom_desc"
            "setting_deeplink_sharing_title" -> "num_key_setting_deeplink_sharing_title"
            "setting_deeplink_sharing_desc" -> "num_key_setting_deeplink_sharing_desc"
            "feat_medrop_allow_when_locked" -> "num_key_feat_medrop_allow_when_locked"
            "feat_medrop_allow_when_locked_desc" -> "num_key_feat_medrop_allow_when_locked_desc"
            "setting_download_web_button_text", "Update", "Actualizar" -> "num_key_setting_download_web_button_text"
            "setting_pitch_black_theme_title" -> "num_key_setting_pitch_black_theme_title"
            "setting_pitch_black_theme_desc" -> "num_key_setting_pitch_black_theme_desc"
            "label_use_blur" -> "num_key_label_use_blur"
            "desc_use_blur" -> "num_key_desc_use_blur"
            "credits_item_1_desc" -> "num_key_credits_item_1_desc"
            "credits_item_2_desc" -> "num_key_credits_item_2_desc"
            "about_app_desc" -> "num_key_about_app_desc"
            "action_website" -> "num_key_action_website"
            "action_view_on_github" -> "num_key_action_view_on_github"
            "action_contact" -> "num_key_action_contact"
            "category_videos" -> "num_key_category_videos"
            "category_utilities" -> "num_key_category_utilities"
            "category_games" -> "num_key_category_games"
            "category_music" -> "num_key_category_music"
            "action_add_app" -> "num_key_action_add_app"
            "action_download_update" -> "num_key_action_download_update"
            "action_install_app" -> "num_key_action_install_app"
            "developed_by_format" -> "num_key_developed_by_format"
            else -> null
        } ?: return null

        return try {
            val id = context.resources.getIdentifier(mappedResName, "string", context.packageName)
            if (id != 0) context.getString(id) else null
        } catch (_: Exception) {
            null
        }
    }
}
