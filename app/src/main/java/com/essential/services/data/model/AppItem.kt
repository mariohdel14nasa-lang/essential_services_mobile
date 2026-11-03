package com.essential.services.data.model

import android.graphics.drawable.Drawable
import java.io.File

data class AppItem(
    val id: String,
    val appName: String,
    val downloadUrl: String? = null,
    val iconUrl: String? = null,
    val iconDrawable: Drawable? = null,
    val category: AppCategory = AppCategory.UTILITIES,
    val versionName: String = "1.0",
    val packageName: String = "",
    val description: String? = null,
    val apkFile: File? = null,
    val isWebLink: Boolean = false,
    val isUserAdded: Boolean = false,
)
