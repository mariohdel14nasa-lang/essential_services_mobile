package com.essential.services.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.essential.services.utils.ColorUtil
import com.essential.services.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureCard(
    title: String,
    description: String? = null,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    accentColor: Color? = null,
) {
    val view = LocalView.current
    val effectiveContainerColor = containerColor ?: MaterialTheme.colorScheme.surfaceBright
    val effectiveIconBg = accentColor ?: ColorUtil.getPastelColorFor(title)
    val effectiveIconTint = if (containerColor != null) Color.White else ColorUtil.getVibrantColorFor(title)
    val effectiveTextColor = if (containerColor != null) Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = effectiveContainerColor,
            ),
    ) {
        ListItem(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                onClick()
            },
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            colors =
                ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = effectiveTextColor,
                    supportingColor = if (containerColor != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            leadingContent = {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(
                                color = effectiveIconBg,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = effectiveIconTint,
                    )
                }
            },
            supportingContent =
                if (description != null) {
                    {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (containerColor != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = effectiveTextColor,
                )
            },
        )
    }
}
