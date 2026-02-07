package com.example.yapenotifier.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Contacts : Screen("contacts")
    data object Packages : Screen("packages")
    data object Settings : Screen("settings")
}
