package com.example.yapenotifier

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import android.content.Intent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.yapenotifier.data.service.KeepAliveService
import com.example.yapenotifier.presentation.contacts.ContactsScreen
import com.example.yapenotifier.presentation.dashboard.DashboardScreen
import com.example.yapenotifier.presentation.navigation.Screen
import com.example.yapenotifier.presentation.packages.PackagesScreen
import com.example.yapenotifier.presentation.settings.SettingsScreen
import com.example.yapenotifier.presentation.theme.YapeNotifierTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startKeepAliveService()
        setContent {
            YapeNotifierTheme {
                AppNavigation()
            }
        }
    }

    private fun startKeepAliveService() {
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val animDuration = 300

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            slideInHorizontally(tween(animDuration, easing = FastOutSlowInEasing)) { it }
        },
        exitTransition = {
            slideOutHorizontally(tween(animDuration, easing = FastOutSlowInEasing)) { -it / 4 }
        },
        popEnterTransition = {
            slideInHorizontally(tween(animDuration, easing = FastOutSlowInEasing)) { -it / 4 }
        },
        popExitTransition = {
            slideOutHorizontally(tween(animDuration, easing = FastOutSlowInEasing)) { it }
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenContacts = { navController.navigate(Screen.Contacts.route) }
            )
        }
        composable(Screen.Contacts.route) {
            ContactsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Packages.route) {
            PackagesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPackages = { navController.navigate(Screen.Packages.route) }
            )
        }
    }
}
