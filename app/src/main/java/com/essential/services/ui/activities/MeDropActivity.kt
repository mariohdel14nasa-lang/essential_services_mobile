package com.essential.services.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.essential.services.ui.sheets.MeDropBottomSheet
import com.essential.services.ui.theme.MeDropTheme
import com.essential.services.viewmodels.MeDropViewModel

class MeDropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            val mainViewModel: MeDropViewModel = viewModel()
            val isPitchBlackThemeEnabled by mainViewModel.isPitchBlackThemeEnabled
            val context = LocalContext.current

            val initialProfileTypeStr = intent?.getStringExtra("extra_profile_type")
            val initialProfileType = initialProfileTypeStr?.let {
                try {
                    com.essential.services.domain.model.MeDropProfileType.valueOf(it)
                } catch (_: Exception) {
                    null
                }
            }

            LaunchedEffect(initialProfileType) {
                if (initialProfileType != null) {
                    mainViewModel.setMeDropActiveProfile(context, initialProfileType)
                }
            }

            MeDropTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                MeDropBottomSheet(
                    viewModel = mainViewModel,
                    initialProfileType = initialProfileType,
                    onDismissRequest = { finish() }
                )
            }
        }
    }
}
