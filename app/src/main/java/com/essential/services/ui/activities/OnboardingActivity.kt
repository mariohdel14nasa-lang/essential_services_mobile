package com.essential.services.ui.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.essential.services.MainActivity
import com.essential.services.R
import com.essential.services.data.repository.MeDropRepository
import com.essential.services.ui.theme.MeDropTheme
import com.essential.services.utils.BackgroundMusicPlayer
import com.essential.services.utils.HapticUtil
import com.essential.services.utils.SettingsMenuManager
import com.essential.services.utils.TranslationManager
import com.essential.services.viewmodels.MeDropViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
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

        // Start background music playing A_Gentle_Bloom.mp3 persistently across activities
        BackgroundMusicPlayer.play(this, R.raw.a_gentle_bloom)

        setContent {
            val viewModel: MeDropViewModel = viewModel()
            val isPitchBlackThemeEnabled = viewModel.isPitchBlackThemeEnabled.value

            MeDropTheme(
                pitchBlackTheme = isPitchBlackThemeEnabled,
                remoteColors = viewModel.remoteColors.value,
            ) {
                SetupScreen(
                    onFinishOnboarding = {
                        MeDropRepository(this).setFirstLaunchCompleted(true)
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

class FullScreenVideoView(context: Context) : VideoView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(0, widthMeasureSpec)
        val height = getDefaultSize(0, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}

@Composable
fun SetupScreen(
    onFinishOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var currentStep by remember { mutableStateOf(1) } // Step 1: Video loop screen, Step 2: Language menu
    var activeLang by remember { mutableStateOf(TranslationManager.getCurrentLanguage(context)) }

    var languageOptions by remember { mutableStateOf<List<com.essential.services.utils.SettingsMenuItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val settingsData = SettingsMenuManager.fetchAndCacheSettingsMenu(context) ?: SettingsMenuManager.getCachedSettingsMenu(context)
            settingsData.sections.forEach { section ->
                section.items.forEach { item ->
                    if (item.type == "selector_card" && !item.items.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            languageOptions = item.items
                        }
                        TranslationManager.prefetchAndCacheAllLanguages(context, item.items)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Background loop video (bootvideo.mp4) occupying full screen
        AndroidView(
            factory = { ctx ->
                FullScreenVideoView(ctx).apply {
                    setVideoURI(Uri.parse("android.resource://${ctx.packageName}/raw/bootvideo"))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Dark semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp))

            // Center Content: Step 1 (empty, only video) or Step 2 (Language Selection Menu without extra text)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (currentStep == 2) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        if (languageOptions.isNotEmpty()) {
                            languageOptions.forEach { langItem ->
                                val code = TranslationManager.extractLangCodeFromItem(langItem)
                                val isSelected = activeLang.equals(code, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.85f)
                                        )
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            activeLang = code
                                            if (!langItem.translationsURLfetch.isNullOrBlank()) {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    TranslationManager.fetchAndApplyLanguage(context, code, langItem.translationsURLfetch)
                                                }
                                            } else {
                                                TranslationManager.setCurrentLanguage(context, code)
                                            }
                                        }
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = langItem.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        } else {
                            listOf("en" to "English", "es" to "Español").forEach { (code, title) ->
                                val isSelected = activeLang.equals(code, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.85f)
                                        )
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            activeLang = code
                                            TranslationManager.setCurrentLanguage(context, code)
                                        }
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Full-Width Bar Button (only an arrow, occupying the entire bottom bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (currentStep == 1) {
                            currentStep = 2
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_arrow_back_24),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(rotationZ = 180f),
                    )
                }
            }
        }
    }
}
