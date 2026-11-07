package com.essential.services.ui.activities

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.essential.services.R
import com.essential.services.domain.model.MeDropProfileType
import com.essential.services.domain.model.MeDropSettings
import com.essential.services.ui.components.CreditsMarkdownUI
import com.essential.services.ui.components.MeDropFloatingToolbar
import com.essential.services.ui.components.dialogs.AboutSection
import com.essential.services.ui.core.cards.FeatureCard
import com.essential.services.ui.core.cards.IconToggleItem
import com.essential.services.ui.core.containers.RoundedCardContainer
import com.essential.services.ui.core.sheets.MeDropHelpBottomSheet
import com.essential.services.ui.modifiers.BlurDirection
import com.essential.services.ui.modifiers.progressiveBlur
import com.essential.services.ui.theme.MeDropTheme
import com.essential.services.utils.ApkInstaller
import com.essential.services.utils.AppColorManager
import com.essential.services.utils.SettingsMenuManager
import com.essential.services.utils.TranslationManager
import com.essential.services.viewmodels.MeDropViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val isDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)

        setContent {
            val context = LocalContext.current
            val view = LocalView.current
            val viewModel: MeDropViewModel = viewModel()

            val goBackToContact = {
                viewModel.setMeDropActiveProfile(context, MeDropProfileType.CONTACT)
                val intent = Intent(context, com.essential.services.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
                finish()
            }

            BackHandler {
                goBackToContact()
            }

            val scope = rememberCoroutineScope()
            var settingsMenuData by remember { mutableStateOf(SettingsMenuManager.getCachedSettingsMenu(context)) }

            val fetchSettings = {
                scope.launch(Dispatchers.IO) {
                    val fresh = SettingsMenuManager.fetchAndCacheSettingsMenu(context)
                    if (fresh != null) {
                        withContext(Dispatchers.Main) {
                            settingsMenuData = fresh
                        }
                        fresh.sections.forEach { section ->
                            section.items.forEach { item ->
                                if (item.type == "selector_card" && !item.items.isNullOrEmpty()) {
                                    item.items.forEach { subItem ->
                                        if (!subItem.translationsURLfetch.isNullOrBlank() && subItem.translationsURLfetch.startsWith("http", ignoreCase = true)) {
                                            val code = when {
                                                subItem.title.contains("English", ignoreCase = true) -> "en"
                                                subItem.title.contains("Spanish", ignoreCase = true) || subItem.title.contains("Español", ignoreCase = true) -> "es"
                                                else -> subItem.title.lowercase().take(2)
                                            }
                                            TranslationManager.fetchAndApplyLanguage(context, code, subItem.translationsURLfetch)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.check(context)
                            fetchSettings()
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.check(context)
                fetchSettings()
            }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled
            val isAllowWhenLocked by viewModel.isMeDropAllowWhenLocked
            val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled
            val settings by viewModel.meDropSettings
            val safeSettings = settings ?: MeDropSettings()
            val density = LocalDensity.current

            MeDropTheme(
                pitchBlackTheme = isPitchBlackThemeEnabled,
                remoteColors = viewModel.remoteColors.value,
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) { innerPadding ->
                    val statusBarHeightPx =
                        with(density) {
                            WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding()
                                .toPx()
                        }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = statusBarHeightPx * 1.15f,
                                    direction = BlurDirection.TOP,
                                ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .progressiveBlur(
                                        blurRadius = if (isBlurEnabled) 40f else 0f,
                                        height = with(density) { 150.dp.toPx() },
                                        direction = BlurDirection.BOTTOM,
                                    )
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier.height(
                                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                                    ),
                            )

                            // Render Sections dynamically from remote/cached JSON
                            settingsMenuData.sections.forEach { section ->
                                val visibleItems = section.items.filter { item ->
                                    item.minSdk <= Build.VERSION.SDK_INT
                                }

                                if (visibleItems.isNotEmpty()) {
                                    val sectionTitle = TranslationManager.getString(context, section.title, section.title)
                                    if (sectionTitle.isNotBlank()) {
                                        Text(
                                            text = sectionTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }

                                    val toggleItems = visibleItems.filter { it.type == "toggle" }
                                    if (toggleItems.isNotEmpty()) {
                                        RoundedCardContainer {
                                            toggleItems.forEach { item ->
                                                val iconRes = SettingsMenuManager.getDrawableResId(context, item.icon)
                                                val (isChecked, onToggle) = when (item.action) {
                                                    "TOGGLE_CUSTOM_PROFILE" -> Pair(safeSettings.customProfile.enabled) { enabled: Boolean ->
                                                        viewModel.setMeDropProfileEnabled(context, MeDropProfileType.CUSTOM, enabled)
                                                    }
                                                    "TOGGLE_DEEPLINK_SHARING" -> Pair(safeSettings.enableDeepLinkSharing) { enabled: Boolean ->
                                                        viewModel.setEnableDeepLinkSharing(context, enabled)
                                                    }
                                                    "TOGGLE_ALLOW_WHEN_LOCKED" -> Pair(isAllowWhenLocked) { enabled: Boolean ->
                                                        viewModel.setMeDropAllowWhenLocked(context, enabled)
                                                    }
                                                    "TOGGLE_SHOW_RIPPLE" -> Pair(safeSettings.showRipple) { enabled: Boolean ->
                                                        viewModel.setMeDropShowRipple(context, enabled)
                                                    }
                                                    "TOGGLE_PITCH_BLACK" -> Pair(isPitchBlackThemeEnabled) { enabled: Boolean ->
                                                        viewModel.setPitchBlackTheme(context, enabled)
                                                    }
                                                    "TOGGLE_USE_BLUR" -> Pair(isBlurEnabled) { enabled: Boolean ->
                                                        viewModel.setBlurEnabled(context, enabled)
                                                    }
                                                    else -> Pair(false) { _: Boolean -> }
                                                }

                                                val itemTitle = TranslationManager.getString(context, item.title, item.title)
                                                val itemDesc = item.description?.let { TranslationManager.getString(context, it, it) }

                                                IconToggleItem(
                                                    iconRes = iconRes,
                                                    title = itemTitle,
                                                    description = itemDesc,
                                                    isChecked = isChecked,
                                                    onCheckedChange = onToggle,
                                                )
                                            }
                                        }
                                    }

                                    visibleItems.filter { it.type == "selector_card" }.forEach { item ->
                                        val iconRes = SettingsMenuManager.getDrawableResId(context, item.icon)
                                        val cardColor = AppColorManager.getNamedColor(context, item.colorKey ?: "green", androidx.compose.ui.graphics.Color(0xFF488066))
                                        val subItems = item.items ?: emptyList()

                                        RoundedCardContainer {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(iconRes),
                                                        contentDescription = null,
                                                        tint = cardColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = TranslationManager.getString(context, item.title, item.title),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (subItems.isNotEmpty()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        subItems.forEach { subItem ->
                                                            val subColor = AppColorManager.getNamedColor(context, subItem.colorKey ?: "green", cardColor)
                                                            Button(
                                                                onClick = {
                                                                    com.essential.services.utils.HapticUtil.performVirtualKeyHaptic(view)
                                                                    val langCode = when {
                                                                        subItem.title.contains("English", ignoreCase = true) -> "en"
                                                                        subItem.title.contains("Spanish", ignoreCase = true) || subItem.title.contains("Español", ignoreCase = true) -> "es"
                                                                        else -> subItem.title.lowercase().take(2)
                                                                    }
                                                                    val fetchUrl = subItem.translationsURLfetch ?: ""
                                                                    scope.launch(Dispatchers.IO) {
                                                                        TranslationManager.fetchAndApplyLanguage(context, langCode, fetchUrl)
                                                                        withContext(Dispatchers.Main) {
                                                                            android.widget.Toast.makeText(context, "Language set to ${subItem.title}", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = subColor,
                                                                    contentColor = androidx.compose.ui.graphics.Color.White
                                                                ),
                                                                shape = RoundedCornerShape(12.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text(
                                                                    text = TranslationManager.getString(context, subItem.title, subItem.title),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    visibleItems.filter { it.type == "feature_card" }.forEach { item ->
                                        val iconRes = SettingsMenuManager.getDrawableResId(context, item.icon)
                                        val containerColor = AppColorManager.getNamedColor(context, item.colorKey ?: "green", androidx.compose.ui.graphics.Color(0xFF488066))
                                        val accentColor = containerColor.copy(alpha = 0.8f)

                                        FeatureCard(
                                            title = TranslationManager.getString(context, item.title, item.title),
                                            description = item.description?.let { TranslationManager.getString(context, it, it) },
                                            iconRes = iconRes,
                                            containerColor = containerColor,
                                            accentColor = accentColor,
                                            onClick = {
                                                val targetUrl = item.targetUrl ?: context.getString(R.string.setting_download_web_url)
                                                if (targetUrl.endsWith(".apk", ignoreCase = true)) {
                                                    ApkInstaller.downloadAndInstallApk(context, targetUrl)
                                                } else {
                                                    android.widget.Toast.makeText(context, "Checking for updates...", android.widget.Toast.LENGTH_SHORT).show()
                                                    @android.annotation.SuppressLint("SetJavaScriptEnabled")
                                                    android.webkit.WebView(context).apply {
                                                        this.settings.javaScriptEnabled = true
                                                        this.settings.domStorageEnabled = true
                                                        setDownloadListener { downloadUrl, _, _, mimetype, _ ->
                                                            if (downloadUrl.endsWith(".apk", ignoreCase = true) || mimetype == "application/vnd.android.package-archive") {
                                                                ApkInstaller.downloadAndInstallApk(context, downloadUrl)
                                                            }
                                                        }
                                                        webViewClient = object : android.webkit.WebViewClient() {
                                                            private var isHandled = false
                                                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                                val requestUrl = request?.url?.toString() ?: ""
                                                                if (requestUrl.endsWith(".apk", ignoreCase = true)) {
                                                                    ApkInstaller.downloadAndInstallApk(context, requestUrl)
                                                                    return true
                                                                }
                                                                return super.shouldOverrideUrlLoading(view, request)
                                                            }
                                                            override fun onPageFinished(view: android.webkit.WebView?, loadedUrl: String?) {
                                                                super.onPageFinished(view, loadedUrl)
                                                                if (!isHandled) {
                                                                    isHandled = true
                                                                    viewModel.setMeDropActiveProfile(context, MeDropProfileType.CONTACT)
                                                                    val intent = Intent(context, com.essential.services.MainActivity::class.java).apply {
                                                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                                    }
                                                                    context.startActivity(intent)
                                                                    (context as? Activity)?.finish()
                                                                }
                                                            }
                                                        }
                                                        loadUrl(targetUrl)
                                                    }
                                                }
                                            },
                                        )
                                    }

                                    visibleItems.filter { it.type == "credits" }.forEach { item ->
                                        RoundedCardContainer {
                                            if (!item.creditsList.isNullOrEmpty()) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    item.creditsList.forEach { credit ->
                                                        Column {
                                                            Text(
                                                                text = TranslationManager.getString(context, credit.title, credit.title),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (!credit.description.isNullOrBlank()) {
                                                                Text(
                                                                    text = TranslationManager.getString(context, credit.description, credit.description),
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                CreditsMarkdownUI(fileName = item.fileName ?: "credits.md")
                                            }
                                        }
                                    }

                                    visibleItems.filter { it.type == "about" }.forEach { item ->
                                        RoundedCardContainer {
                                            AboutSection(
                                                appName = item.appName ?: stringResource(R.string.app_name),
                                                developerName = item.developer ?: stringResource(R.string.app_developer_name),
                                                description = item.description ?: stringResource(R.string.app_description),
                                                links = item.links,
                                                onAvatarLongClick = {
                                                    val newState = !isDeveloperModeEnabled
                                                    viewModel.setDeveloperModeEnabled(context, newState)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (newState) context.getString(R.string.developer_options_enabled_toast)
                                                        else context.getString(R.string.developer_options_disabled_toast),
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (isDeveloperModeEnabled) {
                                Text(
                                    text = stringResource(R.string.settings_section_developer_options),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp),
                                )

                                RoundedCardContainer {
                                    IconToggleItem(
                                        iconRes = R.drawable.rounded_info_24,
                                        title = stringResource(R.string.developer_options_empty_shrug),
                                        showToggle = false,
                                    )
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        WindowInsets.navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding() + 150.dp,
                                    ),
                            )
                        }

                        var showHelpSheet by remember { mutableStateOf(false) }

                        MeDropFloatingToolbar(
                            title = TranslationManager.getString(context, settingsMenuData.title, settingsMenuData.title).ifBlank { stringResource(R.string.settings_title) },
                            onBackClick = { goBackToContact() },
                            onHelpClick = { showHelpSheet = true },
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(1f),
                        )

                        if (showHelpSheet) {
                            MeDropHelpBottomSheet(
                                onDismissRequest = { showHelpSheet = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
