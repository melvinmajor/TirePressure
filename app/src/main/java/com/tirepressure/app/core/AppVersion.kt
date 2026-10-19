package com.tirepressure.app.core

import com.tirepressure.app.BuildConfig

/**
 * Centralized global access to the application version, driven automatically by BuildConfig.VERSION_NAME.
 */
object AppVersion {
    const val VERSION_NAME: String = BuildConfig.VERSION_NAME
    val DISPLAY_VERSION: String = "v${BuildConfig.VERSION_NAME}"
}

