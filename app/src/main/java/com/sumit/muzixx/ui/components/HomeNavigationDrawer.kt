package com.sumit.muzixx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f),
                drawerContainerColor = Color.Black
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(20.dp)
                ) {
                    DrawerProfileItem(userName = userName, onClick = onProfileClick)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFF222222)
                    )

                    DrawerItem(
                        text = "Check Updates",
                        icon = Icons.Default.Info,
                        onClick = onCheckUpdatesClick
                    )

                    DrawerItem(
                        text = "Settings",
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick
                    )
                }
            }
        },
        content = content
    )
}

// Internal Drawer Items

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
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "User profile icon",
            modifier = Modifier.size(48.dp),
            tint = Color.White
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(userName, fontWeight = FontWeight.Bold, color = Color.White)
            Text("View Profile", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB3B3B3))
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
        label = { Text(text, color = Color.White, fontWeight = FontWeight.Medium) },
        icon = { Icon(icon, null, tint = Color.White) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = Color(0xFF1A1A1A)
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}