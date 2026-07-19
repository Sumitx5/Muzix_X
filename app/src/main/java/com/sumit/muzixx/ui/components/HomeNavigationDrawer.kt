package com.sumit.muzixx.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.utils.glassEffect

@Composable
fun HomeNavigationDrawer(
    drawerState: DrawerState,
    userName: String,
    onProfileClick: () -> Unit,
    onCheckUpdatesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onIntegrationsClick: () -> Unit,
    onListenTogetherClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .padding(end = 24.dp, bottom = 8.dp)
                    .glassEffect(shape = MaterialTheme.shapes.extraLarge),
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    DrawerProfileItem(userName = userName, onClick = onProfileClick)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 0.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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
                        text = "Listen Together",
                        icon = Icons.Rounded.Headphones,
                        onClick = { Toast.makeText(context, "Coming Soon...", Toast.LENGTH_SHORT).show() }
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
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = "User profile icon",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        icon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}