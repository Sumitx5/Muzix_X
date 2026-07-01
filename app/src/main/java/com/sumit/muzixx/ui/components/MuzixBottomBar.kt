package com.sumit.muzixx.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MuzixBottomBar(
    currentScreen: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        val cS = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        NavigationBarItem(
            selected = currentScreen == "Home",
            onClick = { onTabSelected("Home") },
            icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = cS
        )

        NavigationBarItem(
            selected = currentScreen == "Search",
            onClick = { onTabSelected("Search") },
            icon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search") },
            label = { Text("Search") },
            colors = cS
        )

        NavigationBarItem(
            selected = currentScreen == "Library",
            onClick = { onTabSelected("Library") },
            icon = { Icon(imageVector = Icons.Rounded.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library") },
            colors = cS
        )
    }
}