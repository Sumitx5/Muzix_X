package com.sumit.muzixx.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeNavigationDrawer(
    drawerState: DrawerState,
    userName: String,
    onProfileClick: () -> Unit,
    onCheckUpdatesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onIntegrationsClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    DrawerProfileItem(userName = userName, onClick = onProfileClick)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    DrawerItem(
                        text = "Check Updates",
                        icon = Icons.Rounded.Update,
                        onClick = onCheckUpdatesClick
                    )

                    DrawerItem(
                        text = "Integrations",
                        icon = Icons.Rounded.Api,
                        onClick = onIntegrationsClick
                    )

                    DrawerItem(
                        text = "Settings",
                        icon = Icons.Rounded.Settings,
                        onClick = onSettingsClick
                    )
                }
            }
        },
        content = content
    )
}

@Composable
private fun DrawerProfileItem(
    userName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = "User profile icon",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = userName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View Profile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        icon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}