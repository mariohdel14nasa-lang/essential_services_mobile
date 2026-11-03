package com.essential.services.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.essential.services.R
import com.essential.services.data.model.AppCategory
import com.essential.services.utils.HapticUtil

@Composable
fun MeDropCategoryFloatingToolbar(
    selectedCategory: AppCategory,
    onCategorySelected: (AppCategory) -> Unit,
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Categories Container Pill
        Box(
            modifier = Modifier
                .shadow(12.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppCategory.entries.forEach { cat ->
                    val isSelected = cat == selectedCategory

                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onCategorySelected(cat)
                        },
                        modifier = Modifier.size(44.dp),
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
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // Independent Add App Button on the RIGHT (a la derecha)
        Box(
            modifier = Modifier
                .shadow(12.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
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
        }
    }
}
