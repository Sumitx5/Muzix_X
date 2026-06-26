package com.sumit.muzixx.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun MuzixBottomBar(
    currentScreen: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent.copy(alpha = 0.8f)
    ) {
        val primaryTheme = MaterialTheme.colorScheme.primary
        val cS = NavigationBarItemColors(
            primaryTheme,
            primaryTheme,
            primaryTheme.copy(alpha = 0.2f),
            Color.White,
            Color.White,
            Color.LightGray,
            Color.LightGray
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