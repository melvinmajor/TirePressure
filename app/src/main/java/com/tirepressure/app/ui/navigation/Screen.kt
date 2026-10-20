package com.tirepressure.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Overview : Screen

    @Serializable
    data class BikeEdit(val bikeId: String = "new") : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object AppInfo : Screen
}
