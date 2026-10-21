package com.tirepressure.app

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.util.LocaleHelper
import com.tirepressure.app.ui.navigation.AppNavigation
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.bikes.BikeEditViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tirepressure.app.ui.settings.SettingsViewModel
import com.tirepressure.app.ui.theme.TirePressureTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var systemConfigState by mutableStateOf<Configuration?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = LocaleHelper.getSavedLocale(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, locale))
    }

    override fun onResume() {
        super.onResume()
        val locale = LocaleHelper.getSavedLocale(this)
        LocaleHelper.applyLocale(this, locale)
        systemConfigState = Configuration(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        systemConfigState = Configuration(newConfig)
        val locale = LocaleHelper.getSavedLocale(this)
        LocaleHelper.applyLocale(this, locale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialLocale = LocaleHelper.getSavedLocale(this)
        LocaleHelper.applyLocale(this, initialLocale)
        systemConfigState = Configuration(resources.configuration)

        enableEdgeToEdge()

        val app = application as TirePressureApp

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(
                            preferencesRepository = app.preferencesRepository,
                            bikeRepository = app.bikeRepository,
                            backupManager = app.backupManager
                        ) as T
                    }
                }
            )

            val overviewViewModel: OverviewViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return OverviewViewModel(
                            bikeRepository = app.bikeRepository,
                            preferencesRepository = app.preferencesRepository,
                            calculator = app.calculator
                        ) as T
                    }
                }
            )

            val bikeEditViewModel: BikeEditViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return BikeEditViewModel(
                            bikeRepository = app.bikeRepository
                        ) as T
                    }
                }
            )

            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val isOnboardingCompleted by app.preferencesRepository.isOnboardingCompletedFlow.collectAsStateWithLifecycle(initialValue = null)

            // Résolution dynamique de la locale pour le support bilingue (EN / FR)
            val currentLocale = remember(settingsState.language) {
                LocaleHelper.resolveLocale(this@MainActivity, settingsState.language)
            }

            androidx.compose.runtime.LaunchedEffect(currentLocale) {
                LocaleHelper.applyLocale(this@MainActivity, currentLocale)
            }

            val activeConfig = systemConfigState ?: LocalConfiguration.current
            val isSystemDark = (activeConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val isAppDark = when (settingsState.theme) {
                com.tirepressure.app.core.model.AppTheme.LIGHT -> false
                com.tirepressure.app.core.model.AppTheme.DARK -> true
                com.tirepressure.app.core.model.AppTheme.SYSTEM -> isSystemDark
            }

            val configuration = remember(
                currentLocale,
                activeConfig.uiMode,
                isAppDark,
                activeConfig.orientation,
                activeConfig.screenWidthDp,
                activeConfig.screenHeightDp,
                activeConfig.smallestScreenWidthDp,
                activeConfig.screenLayout,
                activeConfig.densityDpi
            ) {
                Configuration(activeConfig).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                    val nightMask = if (isAppDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMask
                }
            }

            val context = LocalContext.current
            val localizedContext = remember(context, configuration) {
                val configCtx = context.createConfigurationContext(configuration)
                object : android.content.ContextWrapper(context) {
                    override fun getResources(): Resources = configCtx.resources
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext
            ) {
                TirePressureTheme(
                    appTheme = settingsState.theme,
                    accentColor = settingsState.accentColor,
                    isSystemDark = isSystemDark
                ) {
                    val completed = isOnboardingCompleted
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (completed != null) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AppNavigation(
                                    overviewViewModel = overviewViewModel,
                                    settingsViewModel = settingsViewModel,
                                    bikeEditViewModel = bikeEditViewModel,
                                    bikeRepository = app.bikeRepository,
                                    preferencesRepository = app.preferencesRepository,
                                    isOnboardingCompleted = completed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
