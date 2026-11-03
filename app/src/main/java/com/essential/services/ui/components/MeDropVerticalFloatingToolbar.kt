package com.essential.services.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.essential.services.R
import com.essential.services.data.model.AppCategory
import com.essential.services.utils.HapticUtil

@Composable
fun MeDropVerticalFloatingToolbar(
    selectedCategory: AppCategory,
    onCategorySelected: (AppCategory) -> Unit,
    onSettingsClick: () -> Unit,
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    Box(
        modifier = modifier
            .padding(end = 12.dp, top = 60.dp, bottom = 90.dp)
            .shadow(12.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Add app icon at top of vertical bar
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onAddAppClick()
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_add_24),
                    contentDescription = stringResource(R.string.action_add_app),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4 Categories
            AppCategory.entries.forEach { cat ->
                val isSelected = cat == selectedCategory

                val itemHeight by animateDpAsState(
                    targetValue = if (isSelected) 52.dp else 44.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "v_item_height_${cat.id}",
                )

                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onCategorySelected(cat)
                    },
                    modifier = Modifier
                        .width(44.dp)
                        .height(itemHeight),
                    colors = if (isSelected) {
                        IconButtonDefaults.filledIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.background,
                        )
                    } else {
                        IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.background,
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    Icon(
                        painter = painterResource(id = cat.iconRes),
                        contentDescription = stringResource(id = cat.titleRes),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Settings icon at bottom of vertical bar
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onSettingsClick()
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_settings_24),
                    contentDescription = stringResource(R.string.action_settings),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
