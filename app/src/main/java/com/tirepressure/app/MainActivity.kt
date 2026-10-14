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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.util.LocaleHelper
import com.tirepressure.app.ui.navigation.AppNavigation
import com.tirepressure.app.ui.overview.OverviewViewModel
import com.tirepressure.app.ui.settings.SettingsViewModel
import com.tirepressure.app.ui.theme.TirePressureTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = LocaleHelper.getSavedLocale(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, locale))
    }

    override fun onResume() {
        super.onResume()
        val locale = LocaleHelper.getSavedLocale(this)
        LocaleHelper.applyLocale(this, locale)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val locale = LocaleHelper.getSavedLocale(this)
        LocaleHelper.applyLocale(this, locale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialLocale = LocaleHelper.getSavedLocale(this)
        LocaleHelper.applyLocale(this, initialLocale)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
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

            val settingsState by settingsViewModel.uiState.collectAsState()
            val isOnboardingCompleted by app.preferencesRepository.isOnboardingCompletedFlow.collectAsState(initial = null)

            // Résolution dynamique de la locale pour le support bilingue (EN / FR)
            val currentLocale = remember(settingsState.language) {
                LocaleHelper.resolveLocale(this@MainActivity, settingsState.language)
            }

            androidx.compose.runtime.LaunchedEffect(currentLocale) {
                LocaleHelper.applyLocale(this@MainActivity, currentLocale)
            }

            val localConfig = LocalConfiguration.current
            val configuration = remember(currentLocale, localConfig) {
                Configuration(localConfig).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
            }

            val context = LocalContext.current
            val localizedContext = remember(context, currentLocale) {
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
                    accentColor = settingsState.accentColor
                ) {
                    val completed = isOnboardingCompleted
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (completed != null) {
                            val navController = rememberNavController()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .safeDrawingPadding()
                            ) {
                                AppNavigation(
                                    navController = navController,
                                    overviewViewModel = overviewViewModel,
                                    settingsViewModel = settingsViewModel,
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
