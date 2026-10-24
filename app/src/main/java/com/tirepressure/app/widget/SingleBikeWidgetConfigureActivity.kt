package com.tirepressure.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tirepressure.app.R
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.ui.components.ScrollBottomFade
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.appGlossCardColors
import com.tirepressure.app.ui.theme.TirePressureTheme
import com.tirepressure.app.ui.theme.getCategoryColor
import com.tirepressure.app.ui.theme.setupEdgeToEdge
import io.github.angelomarzocchi.lumen.GlossCard
import io.github.angelomarzocchi.lumen.rememberBounceHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleBikeWidgetConfigureActivity : ComponentActivity() {

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

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
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
        val initialBikeId = WidgetPreferencesHelper.getSingleBikeWidgetBikeId(this, appWidgetId)
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
                    SingleBikeConfigContent(
                        initialBikeId = initialBikeId,
                        initialTheme = initialTheme,
                        onSave = { selectedBikeId, selectedTheme ->
                            saveAndFinish(selectedBikeId, selectedTheme)
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun saveAndFinish(bikeId: String, theme: WidgetTheme) {
        lifecycleScope.launch(Dispatchers.IO) {
            WidgetPreferencesHelper.setSingleBikeWidgetBikeId(this@SingleBikeWidgetConfigureActivity, appWidgetId, bikeId)
            WidgetPreferencesHelper.setWidgetTheme(this@SingleBikeWidgetConfigureActivity, appWidgetId, theme)

            val appWidgetManager = AppWidgetManager.getInstance(this@SingleBikeWidgetConfigureActivity)
            SingleBikeWidgetReceiver.updateWidget(this@SingleBikeWidgetConfigureActivity, appWidgetManager, appWidgetId)

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
private fun SingleBikeConfigContent(
    initialBikeId: String?,
    initialTheme: WidgetTheme,
    onSave: (bikeId: String, theme: WidgetTheme) -> Unit,
    onCancel: () -> Unit
) {
    var bikes by remember { mutableStateOf<List<BikeProfile>>(emptyList()) }
    var selectedBikeId by remember { mutableStateOf(initialBikeId) }
    var selectedTheme by remember { mutableStateOf(initialTheme) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = rememberBounceHaptics()
    val listState = rememberLazyListState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val entities = AppDatabase.getInstance(context).bikeDao().getAllBikesList()
            val list = entities.map { it.toDomain() }
            bikes = list
            if (selectedBikeId == null && list.isNotEmpty()) {
                selectedBikeId = list.find { it.isDefault }?.id ?: list.first().id
            }
        }
    }

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
                                selectedBikeId?.let { onSave(it, selectedTheme) }
                            },
                            enabled = selectedBikeId != null,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_config_choose_bike),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (bikes.isEmpty()) {
                    item {
                        GlossCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = appGlossCardColors(isDark = isDark),
                            tiltOnTouch = true
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.widget_no_bikes), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.widget_no_bikes_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(bikes, key = { it.id }) { bike ->
                        val isSelected = bike.id == selectedBikeId
                        val catColor = getCategoryColor(bike.category)
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
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    haptics.press()
                                    selectedBikeId = bike.id
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = appGlossCardColors(base = baseCardColor, isDark = isDark),
                            tiltOnTouch = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (bike.isDefault) "★ ${bike.name}" else bike.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = catColor.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = stringResource(bike.category.labelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        val setupsCountText = if (bike.setups.size <= 1) {
                                            stringResource(R.string.overview_config_count_one)
                                        } else {
                                            stringResource(R.string.overview_config_count_many, bike.setups.size)
                                        }
                                        Text(
                                            text = "• $setupsCountText",
                                            fontSize = 12.sp,
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
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            val isSelected = theme == selectedTheme
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
                                    .clickable {
                                        haptics.press()
                                        selectedTheme = theme
                                    },
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
                                        val label = when (theme) {
                                            WidgetTheme.SYSTEM -> stringResource(R.string.widget_theme_system)
                                            WidgetTheme.LIGHT -> stringResource(R.string.widget_theme_light)
                                            WidgetTheme.DARK -> stringResource(R.string.widget_theme_dark)
                                        }
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
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
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            ScrollTopFade(visible = listState.canScrollBackward)
            ScrollBottomFade(visible = listState.canScrollForward)
        }
    }
}
