package com.example.snapswipe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_PERMISSIONS = "permissions"
private const val ROUTE_MAIN = "main"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun SnapSwipeApp() {
    val navController = rememberNavController()

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_PERMISSIONS
        ) {
            permissionsRoute(
                onGrantAccess = {
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                    }
                },
                onSkip = { /* no-op placeholder */ }
            )
            mainRoute(
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onBackToPermissions = {
                    navController.navigate(ROUTE_PERMISSIONS) {
                        popUpTo(ROUTE_MAIN) { inclusive = true }
                    }
                }
            )
            settingsRoute(
                onDone = { navController.popBackStack() }
            )
        }
    }
}

private fun NavGraphBuilder.permissionsRoute(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit
) {
    composable(ROUTE_PERMISSIONS) {
        PermissionsScreen(
            onGrantAccess = onGrantAccess,
            onSkip = onSkip
        )
    }
}

private fun NavGraphBuilder.mainRoute(
    onOpenSettings: () -> Unit,
    onBackToPermissions: () -> Unit
) {
    composable(ROUTE_MAIN) {
        MainPlaceholderScreen(
            onOpenSettings = onOpenSettings,
            onRequestPermissions = onBackToPermissions
        )
    }
}

private fun NavGraphBuilder.settingsRoute(
    onDone: () -> Unit
) {
    composable(ROUTE_SETTINGS) {
        SettingsPlaceholderScreen(onDone = onDone)
    }
}

@Composable
private fun PermissionsScreen(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Snap Swipe needs photo access to help you clean up your gallery.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrantAccess) {
            Text("Grant photo access")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSkip) {
            Text("Continue without access")
        }
    }
}

@Composable
private fun MainPlaceholderScreen(
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Main swipe screen placeholder",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("Go to settings")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermissions) {
            Text("Back to permissions")
        }
    }
}

@Composable
private fun SettingsPlaceholderScreen(
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings placeholder",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("About Snap Swipe: Clean up your photos by swiping.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDone) {
            Text("Back")
        }
    }
}
