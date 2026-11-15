package com.tirepressure.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tirepressure.app.R
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.ui.components.ScrollBottomFade
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.appGlossCardColors
import com.tirepressure.app.ui.theme.TirePressureTheme
import com.tirepressure.app.ui.theme.setupEdgeToEdge
import io.github.angelomarzocchi.lumen.GlossCard
import io.github.angelomarzocchi.lumen.rememberBounceHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllBikesWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = com.tirepressure.app.core.util.LocaleHelper.getSavedLocale(newBase)
        super.attachBaseContext(com.tirepressure.app.core.util.LocaleHelper.wrapContext(newBase, locale))
    }

    override fun onResume() {
        super.onResume()
        val locale = com.tirepressure.app.core.util.LocaleHelper.getSavedLocale(this)
        com.tirepressure.app.core.util.LocaleHelper.applyLocale(this, locale)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val locale = com.tirepressure.app.core.util.LocaleHelper.getSavedLocale(this)
        com.tirepressure.app.core.util.LocaleHelper.applyLocale(this, locale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialLocale = com.tirepressure.app.core.util.LocaleHelper.getSavedLocale(this)
        com.tirepressure.app.core.util.LocaleHelper.applyLocale(this, initialLocale)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val initialTheme = WidgetPreferencesHelper.getWidgetTheme(this, appWidgetId)
        val initialFavoritesOnly = WidgetPreferencesHelper.isAllBikesFavoritesOnly(this, appWidgetId)
        val prefs = PreferencesRepositoryImpl(this)

        setContent {
            val appTheme by prefs.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val accentColor by prefs.accentColorFlow.collectAsStateWithLifecycle(initialValue = AppAccentColor.ELECTRIC_LIME)

            TirePressureTheme(
                appTheme = appTheme,
                accentColor = accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AllBikesConfigContent(
                        initialTheme = initialTheme,
                        initialFavoritesOnly = initialFavoritesOnly,
                        onSave = { selectedTheme, favoritesOnly ->
                            saveAndFinish(selectedTheme, favoritesOnly)
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun saveAndFinish(theme: WidgetTheme, favoritesOnly: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            WidgetPreferencesHelper.setWidgetTheme(this@AllBikesWidgetConfigureActivity, appWidgetId, theme)
            WidgetPreferencesHelper.setAllBikesFavoritesOnly(this@AllBikesWidgetConfigureActivity, appWidgetId, favoritesOnly)

            val appWidgetManager = AppWidgetManager.getInstance(this@AllBikesWidgetConfigureActivity)
            AllBikesWidgetReceiver.updateWidget(this@AllBikesWidgetConfigureActivity, appWidgetManager, appWidgetId)

            withContext(Dispatchers.Main) {
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(Activity.RESULT_OK, resultValue)
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllBikesConfigContent(
    initialTheme: WidgetTheme,
    initialFavoritesOnly: Boolean,
    onSave: (theme: WidgetTheme, favoritesOnly: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(initialTheme) }
    var favoritesOnly by remember { mutableStateOf(initialFavoritesOnly) }
    val haptics = rememberBounceHaptics()
    val listState = rememberLazyListState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.press()
                                onCancel()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(
                            onClick = {
                                haptics.press()
                                onSave(selectedTheme, favoritesOnly)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.widget_config_apply), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_all_bikes_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_all_bikes_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_config_filter_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectableWidgetOptionCard(
                            title = stringResource(R.string.widget_filter_all_bikes),
                            subtitle = stringResource(R.string.widget_filter_all_bikes_desc),
                            isSelected = !favoritesOnly,
                            isDark = isDark,
                            onClick = {
                                haptics.press()
                                favoritesOnly = false
                            }
                        )
                        SelectableWidgetOptionCard(
                            title = "★ ${stringResource(R.string.widget_filter_favorites_only)}",
                            subtitle = stringResource(R.string.widget_filter_favorites_only_desc),
                            isSelected = favoritesOnly,
                            isDark = isDark,
                            onClick = {
                                haptics.press()
                                favoritesOnly = true
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_config_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetTheme.values().forEach { theme ->
                            val label = when (theme) {
                                WidgetTheme.SYSTEM -> stringResource(R.string.widget_theme_system)
                                WidgetTheme.LIGHT -> stringResource(R.string.widget_theme_light)
                                WidgetTheme.DARK -> stringResource(R.string.widget_theme_dark)
                            }
                            SelectableWidgetOptionCard(
                                title = label,
                                subtitle = null,
                                isSelected = theme == selectedTheme,
                                isDark = isDark,
                                onClick = {
                                    haptics.press()
                                    selectedTheme = theme
                                }
                            )
                        }
                    }
                }
            }

            ScrollTopFade(visible = listState.canScrollBackward)
            ScrollBottomFade(visible = listState.canScrollForward)
        }
    }
}

@Composable
private fun SelectableWidgetOptionCard(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val baseCardColor = if (isSelected) {
        androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primary,
            if (isDark) 0.14f else 0.08f
        )
    } else {
        MaterialTheme.colorScheme.surface
    }
    GlossCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = appGlossCardColors(base = baseCardColor, isDark = isDark),
        tiltOnTouch = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
