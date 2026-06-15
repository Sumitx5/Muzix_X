package com.sumit.muzixx.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MuzixBottomBar(
    currentScreen: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {

        NavigationBarItem(
            selected = currentScreen == "Home",
            onClick = { onTabSelected("Home") },
            icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = currentScreen == "Search",
            onClick = { onTabSelected("Search") },
            icon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )

        NavigationBarItem(
            selected = currentScreen == "Library",
            onClick = { onTabSelected("Library") },
            icon = { Icon(imageVector = Icons.Rounded.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library") }
        )
    }
}