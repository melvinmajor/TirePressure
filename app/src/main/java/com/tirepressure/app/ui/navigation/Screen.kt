package com.tirepressure.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Overview : Screen("overview")
    object BikeEdit : Screen("bike_edit/{bikeId}") {
        fun createRoute(bikeId: String? = "new") = "bike_edit/$bikeId"
    }
    object Settings : Screen("settings")
    object AppInfo : Screen("app_info")
}

