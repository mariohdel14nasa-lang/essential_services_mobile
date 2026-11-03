package com.essential.services.data.model

import androidx.compose.ui.graphics.Color
import com.essential.services.R

enum class AppCategory(
    val id: String,
    val titleRes: Int,
    val defaultTitle: String,
    val iconRes: Int,
    val bgFileName: String,
    val gradientColors: List<Color>,
) {
    VIDEOS(
        id = "videos",
        titleRes = R.string.category_videos,
        defaultTitle = "Videos y Pelis",
        iconRes = R.drawable.ic_category_videos,
        bgFileName = "bg_videos.png",
        gradientColors = listOf(Color(0xFF2C1930), Color(0xFF5D2452), Color(0xFF190E1C)),
    ),
    UTILITIES(
        id = "utilities",
        titleRes = R.string.category_utilities,
        defaultTitle = "Utilidades",
        iconRes = R.drawable.ic_category_utilities,
        bgFileName = "bg_utilities.png",
        gradientColors = listOf(Color(0xFF102A38), Color(0xFF1E4D63), Color(0xFF091720)),
    ),
    GAMES(
        id = "games",
        titleRes = R.string.category_games,
        defaultTitle = "Juegos",
        iconRes = R.drawable.ic_category_games,
        bgFileName = "bg_games.png",
        gradientColors = listOf(Color(0xFF38152C), Color(0xFF75244E), Color(0xFF1F0B18)),
    ),
    MUSIC(
        id = "music",
        titleRes = R.string.category_music,
        defaultTitle = "Música",
        iconRes = R.drawable.ic_category_music,
        bgFileName = "bg_music.png",
        gradientColors = listOf(Color(0xFF28131C), Color(0xFF6B1F34), Color(0xFF16090E)),
    );

    companion object {
        fun fromId(id: String?): AppCategory {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: UTILITIES
        }
    }
}
