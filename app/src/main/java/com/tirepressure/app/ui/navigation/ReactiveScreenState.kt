package com.tirepressure.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.distinctUntilChanged

enum class DeviceFoldPosture {
    NORMAL,                // Standard non-foldable or standard phone
    FOLDED,                // Foldable in folded/cover screen mode
    UNFOLDED_FLAT,         // Foldable opened completely flat
    HALF_OPENED_BOOK,      // Vertical hinge, book posture
    HALF_OPENED_TABLETOP   // Horizontal hinge, tabletop posture
}

data class ReactiveScreenState(
    val isFoldableDevice: Boolean,
    val posture: DeviceFoldPosture,
    val isUnfolded: Boolean,
    val isLargerThanHigh: Boolean,    // width > height (landscape)
    val isHigherThanLarge: Boolean,   // height >= width (portrait)
    val isDualPaneSupported: Boolean  // strictly only when larger than high and width >= 600dp
)

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun rememberReactiveScreenState(): ReactiveScreenState {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val activity = remember(context) { context.findActivity() }

    // Detect system hardware features for foldables
    val hasHingeSensor = remember(context) {
        try {
            context.packageManager.hasSystemFeature("android.hardware.sensor.hinge_angle")
        } catch (_: Exception) {
            false
        }
    }

    // Reactive WindowLayoutInfo stream for foldable hinges and posture changes
    val windowLayoutInfo by produceState<WindowLayoutInfo?>(initialValue = null, activity) {
        val act = activity
        if (act != null) {
            try {
                WindowInfoTracker.getOrCreate(act)
                    .windowLayoutInfo(act)
                    .distinctUntilChanged()
                    .collect { value = it }
            } catch (_: Exception) {
                value = null
            }
        }
    }

    val foldingFeature = windowLayoutInfo?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull()

    val isFoldableDevice = hasHingeSensor || foldingFeature != null

    val posture = when {
        foldingFeature != null -> {
            when (foldingFeature.state) {
                FoldingFeature.State.HALF_OPENED -> {
                    if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
                        DeviceFoldPosture.HALF_OPENED_BOOK
                    } else {
                        DeviceFoldPosture.HALF_OPENED_TABLETOP
                    }
                }
                FoldingFeature.State.FLAT -> DeviceFoldPosture.UNFOLDED_FLAT
                else -> DeviceFoldPosture.UNFOLDED_FLAT
            }
        }
        hasHingeSensor -> {
            if (configuration.smallestScreenWidthDp < 600) {
                DeviceFoldPosture.FOLDED
            } else {
                DeviceFoldPosture.UNFOLDED_FLAT
            }
        }
        else -> DeviceFoldPosture.NORMAL
    }

    val isUnfolded = posture == DeviceFoldPosture.UNFOLDED_FLAT ||
            posture == DeviceFoldPosture.HALF_OPENED_BOOK ||
            posture == DeviceFoldPosture.HALF_OPENED_TABLETOP

    // Screen geometry: higher than large (portrait) vs larger than high (landscape)
    val isLargerThanHigh = configuration.screenWidthDp > configuration.screenHeightDp
    val isHigherThanLarge = !isLargerThanHigh

    // Policy:
    // 1. If higher than large (height >= width), stay strictly full screen (never split screen).
    // 2. If larger than high (width > height) and screenWidthDp >= 600dp, support split screen.
    val isDualPaneSupported = isLargerThanHigh && configuration.screenWidthDp >= 600

    return ReactiveScreenState(
        isFoldableDevice = isFoldableDevice,
        posture = posture,
        isUnfolded = isUnfolded,
        isLargerThanHigh = isLargerThanHigh,
        isHigherThanLarge = isHigherThanLarge,
        isDualPaneSupported = isDualPaneSupported
    )
}

