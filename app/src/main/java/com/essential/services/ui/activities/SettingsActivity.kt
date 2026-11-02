package com.essential.services.ui.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.essential.services.R
import com.essential.services.domain.model.MeDropProfileType
import com.essential.services.ui.components.CreditsMarkdownUI
import com.essential.services.ui.components.MeDropFloatingToolbar
import com.essential.services.ui.components.dialogs.AboutSection
import com.essential.services.ui.core.cards.FeatureCard
import com.essential.services.ui.core.cards.IconToggleItem
import com.essential.services.ui.core.containers.RoundedCardContainer
import com.essential.services.ui.core.sheets.MeDropHelpBottomSheet
import com.essential.services.ui.core.sheets.PermissionItem
import com.essential.services.ui.core.sheets.PermissionsBottomSheet
import com.essential.services.ui.modifiers.BlurDirection
import com.essential.services.ui.modifiers.progressiveBlur
import com.essential.services.ui.theme.MeDropTheme
import com.essential.services.utils.MeDropContactPickerHelper
import com.essential.services.utils.PermissionUtils
import com.essential.services.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

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
            val viewModel: MeDropViewModel = viewModel()
            val scope = rememberCoroutineScope()

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

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.check(context)
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.check(context)
            }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled
            val isAllowWhenLocked by viewModel.isMeDropAllowWhenLocked
            val settings by viewModel.meDropSettings
            val safeSettings = settings ?: com.essential.services.domain.model.MeDropSettings()
            val density = LocalDensity.current

            MeDropTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
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

                            Text(
                                text = stringResource(R.string.settings_section_more_profiles),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                IconToggleItem(
                                    iconRes = R.drawable.ic_apps_24,
                                    title = stringResource(R.string.feat_medrop_profile_custom),
                                    description = stringResource(R.string.feat_medrop_profile_custom_desc),
                                    isChecked = safeSettings.customProfile.enabled,
                                    onCheckedChange = { viewModel.setMeDropProfileEnabled(context, MeDropProfileType.CUSTOM, it) },
                                )
                            }

                            Text(
                                text = stringResource(R.string.settings_section_general),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_globe_24,
                                    title = stringResource(R.string.setting_deeplink_sharing_title),
                                    description = stringResource(R.string.setting_deeplink_sharing_desc),
                                    isChecked = safeSettings.enableDeepLinkSharing,
                                    onCheckedChange = { viewModel.setEnableDeepLinkSharing(context, it) },
                                )
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_lock_24,
                                    title = stringResource(R.string.feat_medrop_allow_when_locked),
                                    description = stringResource(R.string.feat_medrop_allow_when_locked_desc),
                                    isChecked = isAllowWhenLocked,
                                    onCheckedChange = { viewModel.setMeDropAllowWhenLocked(context, it) },
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    IconToggleItem(
                                        iconRes = R.drawable.rounded_blur_on_24,
                                        title = stringResource(R.string.feat_medrop_show_ripple),
                                        isChecked = safeSettings.showRipple,
                                        onCheckedChange = { viewModel.setMeDropShowRipple(context, it) },
                                    )
                                }
                            }

                            FeatureCard(
                                title = stringResource(R.string.setting_download_web_button_text),
                                description = null,
                                iconRes = R.drawable.ic_download_new,
                                containerColor = androidx.compose.ui.graphics.Color(0xFF1A73E8),
                                accentColor = androidx.compose.ui.graphics.Color(0xFF1557B0),
                                onClick = {
                                    val webUrl = context.getString(R.string.setting_download_web_url)
                                    if (webUrl.endsWith(".apk", ignoreCase = true)) {
                                        com.essential.services.utils.ApkInstaller.downloadAndInstallApk(context, webUrl)
                                    } else {
                                        android.widget.Toast.makeText(context, "Checking for updates...", android.widget.Toast.LENGTH_SHORT).show()
                                        @android.annotation.SuppressLint("SetJavaScriptEnabled")
                                        android.webkit.WebView(context).apply {
                                            this.settings.javaScriptEnabled = true
                                            this.settings.domStorageEnabled = true

                                            setDownloadListener { downloadUrl, _, _, mimetype, _ ->
                                                if (downloadUrl.endsWith(".apk", ignoreCase = true) || mimetype == "application/vnd.android.package-archive") {
                                                    com.essential.services.utils.ApkInstaller.downloadAndInstallApk(context, downloadUrl)
                                                }
                                            }

                                            webViewClient = object : android.webkit.WebViewClient() {
                                                private var isHandled = false

                                                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                    val requestUrl = request?.url?.toString() ?: ""
                                                    if (requestUrl.endsWith(".apk", ignoreCase = true)) {
                                                        com.essential.services.utils.ApkInstaller.downloadAndInstallApk(context, requestUrl)
                                                        return true
                                                    }
                                                    return super.shouldOverrideUrlLoading(view, request)
                                                }

                                                override fun onPageFinished(view: android.webkit.WebView?, loadedUrl: String?) {
                                                    super.onPageFinished(view, loadedUrl)
                                                    if (!isHandled) {
                                                        isHandled = true
                                                        view?.evaluateJavascript("(function() { return JSON.stringify(localStorage); })();") { result ->
                                                            if (!result.isNullOrEmpty() && result != "null") {
                                                                val cleanJson = if (result.startsWith("\"") && result.endsWith("\"")) {
                                                                    try {
                                                                        com.google.gson.Gson().fromJson(result, String::class.java)
                                                                    } catch (_: Exception) { result }
                                                                } else result
                                                                com.essential.services.data.repository.MeDropRepository(context).saveWebViewLocalStorage(cleanJson)
                                                            }
                                                            viewModel.setMeDropActiveProfile(context, com.essential.services.domain.model.MeDropProfileType.CONTACT)
                                                            val intent = Intent(context, com.essential.services.MainActivity::class.java).apply {
                                                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                            }
                                                            context.startActivity(intent)
                                                            (context as? Activity)?.finish()
                                                        }
                                                    }
                                                }
                                            }
                                            loadUrl(webUrl)
                                        }
                                    }
                                },
                            )

                            Text(
                                text = stringResource(R.string.settings_section_appearance),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_palette_24,
                                    title = stringResource(R.string.setting_pitch_black_theme_title),
                                    description = stringResource(R.string.setting_pitch_black_theme_desc),
                                    isChecked = isPitchBlackThemeEnabled,
                                    onCheckedChange = { viewModel.setPitchBlackTheme(context, it) },
                                )
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_blur_on_24,
                                    title = stringResource(R.string.label_use_blur),
                                    description = stringResource(R.string.desc_use_blur),
                                    isChecked = isBlurEnabled,
                                    onCheckedChange = { viewModel.setBlurEnabled(context, it) },
                                )
                            }

                            val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled

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

                            Text(
                                text = stringResource(R.string.settings_section_credits),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                CreditsMarkdownUI(fileName = "credits.md")
                            }

                            RoundedCardContainer {
                                AboutSection(
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
                            title = stringResource(R.string.settings_title),
                            onBackClick = { goBackToContact() },
                            onHelpClick = { showHelpSheet = true },
                            modifier =
                                Modifier
                                    .align(androidx.compose.ui.Alignment.BottomCenter)
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
