package com.tirepressure.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.FavoriteMode
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.model.TwoPaneSplitRatio
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.ui.components.AboutAndPrivacyCard
import com.tirepressure.app.ui.components.GlassAlertDialog
import com.tirepressure.app.ui.components.NumberInputField
import com.tirepressure.app.ui.components.PillSelector
import com.tirepressure.app.ui.components.SectionCard
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.ScrollBottomFade
import com.tirepressure.app.ui.components.appGlossCardColors
import io.github.angelomarzocchi.lumen.GlossCard
import io.github.angelomarzocchi.lumen.GlossCardDefaults
import io.github.angelomarzocchi.lumen.rememberBounceHaptics
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    supportsTwoPaneLayout: Boolean = false,
    onNavigateToOnboarding: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val haptics = rememberBounceHaptics()

    // Gestion SAF : Export JSON
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                viewModel.exportBackup(outputStream)
            }
        }
    }

    // Gestion SAF : Import JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                viewModel.readAndValidateImport(inputStream)
            }
        }
    }

    // Affichage des messages Snack/Toast
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title), fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Profil Cycliste (Poids de référence) - En tête, non réductible (Option 3B GlossCard)
                var riderWeightInput by rememberSaveable(state.massUnit) {
                    mutableStateOf(state.massUnit.format(state.riderWeightKg).replace(',', '.'))
                }
                var riderWeightError by rememberSaveable { mutableStateOf<String?>(null) }

                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val riderGlossColors = appGlossCardColors(
                    base = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    content = MaterialTheme.colorScheme.onSurface,
                    isDark = isDark
                )
                GlossCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = riderGlossColors,
                    tiltOnTouch = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.settings_rider_profile),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(id = R.string.onboarding_weight_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        NumberInputField(
                            value = riderWeightInput,
                            onValueChange = { input ->
                                riderWeightInput = input
                                val num = input.toDoubleOrNull()
                                if (num != null) {
                                    val kg = UnitConverter.toKg(num, state.massUnit)
                                    if (kg in 30.0..250.0) {
                                        riderWeightError = null
                                        viewModel.setRiderWeight(kg)
                                    } else {
                                        riderWeightError = "30 - 250 kg"
                                    }
                                } else {
                                    riderWeightError = "..."
                                }
                            },
                            label = stringResource(id = R.string.settings_rider_weight),
                            suffix = state.massUnit.symbol,
                            isError = riderWeightError != null,
                            errorMessage = riderWeightError
                        )
                    }
                }

                // 2. Vélos favoris (Non réductible, juste après Profil Cycliste)
                SectionCard(
                    title = stringResource(id = R.string.settings_favorites_title),
                    icon = Icons.Default.Star,
                    subtitle = stringResource(id = R.string.settings_favorites_desc),
                    isCollapsible = false
                ) {
                    PillSelector(
                        items = FavoriteMode.entries,
                        selectedItem = state.favoriteMode,
                        onItemSelected = { viewModel.setFavoriteMode(it) },
                        labelProvider = { context.getString(it.labelRes) }
                    )
                }

                // 3. Unités & Calculs (Regroupé, réductible)
            SectionCard(
                title = stringResource(id = R.string.settings_units_surfaces_title),
                icon = Icons.Default.SquareFoot,
                subtitle = stringResource(id = R.string.settings_units_surfaces_desc),
                isCollapsible = true,
                isExpanded = state.isUnitsExpanded,
                onToggleExpand = { viewModel.toggleUnitsExpanded() }
            ) {
                Text(
                    text = stringResource(id = R.string.settings_mass_unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                PillSelector(
                    items = MassUnit.entries,
                    selectedItem = state.massUnit,
                    onItemSelected = { viewModel.setMassUnit(it) },
                    labelProvider = { context.getString(it.labelRes) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.settings_pressure_unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                PillSelector(
                    items = PressureUnit.entries,
                    selectedItem = state.pressureUnit,
                    onItemSelected = { viewModel.setPressureUnit(it) },
                    labelProvider = { context.getString(it.labelRes) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.settings_surfaces_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Dry (☀️ Sec) - Always enabled / required
                    val dryBgColor = MaterialTheme.colorScheme.primary
                    val dryTextColor = MaterialTheme.colorScheme.onPrimary
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(dryBgColor)
                            .clickable { haptics.press() }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "☀️ " + stringResource(R.string.overview_dry),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = dryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Wet (🌧️ Mouillé) - Toggleable
                    val isWetSelected = state.showWetSurface
                    val wetBgColor by animateColorAsState(
                        targetValue = if (isWetSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(200),
                        label = "WetBgColor"
                    )
                    val wetTextColor by animateColorAsState(
                        targetValue = if (isWetSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "WetTextColor"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(wetBgColor)
                            .toggleable(
                                value = isWetSelected,
                                onValueChange = {
                                    haptics.bounce(0.8f, true)
                                    viewModel.setShowWetSurface(it)
                                },
                                role = Role.Checkbox
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "🌧️ " + stringResource(R.string.overview_wet),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isWetSelected) FontWeight.Bold else FontWeight.Normal,
                            color = wetTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. Snow (❄️ Neige) - Toggleable
                    val isSnowSelected = state.showSnowSurface
                    val snowBgColor by animateColorAsState(
                        targetValue = if (isSnowSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(200),
                        label = "SnowBgColor"
                    )
                    val snowTextColor by animateColorAsState(
                        targetValue = if (isSnowSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "SnowTextColor"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(snowBgColor)
                            .toggleable(
                                value = isSnowSelected,
                                onValueChange = {
                                    haptics.bounce(0.8f, true)
                                    viewModel.setShowSnowSurface(it)
                                },
                                role = Role.Checkbox
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "❄️ " + stringResource(R.string.overview_snow),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSnowSelected) FontWeight.Bold else FontWeight.Normal,
                            color = snowTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Affichage & Langue (Regroupé, réductible)
            SectionCard(
                title = stringResource(id = R.string.settings_display_language_title),
                icon = Icons.Default.Palette,
                subtitle = stringResource(id = R.string.settings_display_language_desc),
                isCollapsible = true,
                isExpanded = state.isDisplayExpanded,
                onToggleExpand = { viewModel.toggleDisplayExpanded() }
            ) {
                Text(
                    text = stringResource(id = R.string.settings_appearance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                PillSelector(
                    items = AppTheme.entries,
                    selectedItem = state.theme,
                    iconProvider = {
                        when (it) {
                            AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                            AppTheme.LIGHT -> Icons.Default.LightMode
                            AppTheme.DARK -> Icons.Default.DarkMode
                        }
                    },
                    onItemSelected = { viewModel.setTheme(it) },
                    labelProvider = { context.getString(it.labelRes) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.settings_accent_color_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppAccentColor.entries.forEach { accent ->
                        val isSelected = accent == state.accentColor
                        val circleColor = accent.getPrimary(isDark)
                        val onCircleColor = accent.getOnPrimary(isDark)
                        val colorLabel = stringResource(id = accent.labelRes)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = colorLabel
                                }
                                .selectable(
                                    selected = isSelected,
                                    onClick = { viewModel.setAccentColor(accent) },
                                    role = Role.RadioButton
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = onCircleColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.settings_language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                PillSelector(
                    items = AppLanguage.entries,
                    selectedItem = state.language,
                    onItemSelected = { lang ->
                        viewModel.setLanguage(lang)
                    },
                    labelProvider = { context.getString(it.labelRes) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.settings_default_overview_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val detailedLabel = stringResource(id = R.string.overview_mode_detailed)
                val compactLabel = stringResource(id = R.string.overview_mode_compact)
                PillSelector(
                    items = listOf(false, true),
                    selectedItem = state.defaultOverviewCompact,
                    iconProvider = { if (it) Icons.Default.ViewAgenda else Icons.Default.TableRows },
                    onItemSelected = { viewModel.setDefaultOverviewCompact(it) },
                    labelProvider = { if (it) compactLabel else detailedLabel }
                )

                if (supportsTwoPaneLayout) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(id = R.string.settings_two_pane_split_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PillSelector(
                        items = TwoPaneSplitRatio.entries,
                        selectedItem = state.twoPaneSplitRatio,
                        iconProvider = {
                            when (it) {
                                TwoPaneSplitRatio.FORTY_SIXTY -> Icons.AutoMirrored.Filled.ViewSidebar
                                TwoPaneSplitRatio.HALF_HALF -> Icons.Default.VerticalSplit
                            }
                        },
                        onItemSelected = { viewModel.setTwoPaneSplitRatio(it) },
                        labelProvider = { context.getString(it.labelRes) }
                    )
                }

                Spacer(modifier = Modifier.height(0.dp)
                )
            }

            // 4. Widgets d'écran d'accueil (Réductible)
            var defaultWidgetTheme by rememberSaveable {
                mutableStateOf(com.tirepressure.app.widget.WidgetPreferencesHelper.getDefaultWidgetTheme(context.applicationContext))
            }
            SectionCard(
                title = stringResource(id = R.string.settings_widget_section),
                icon = Icons.Default.Widgets,
                subtitle = stringResource(id = R.string.settings_widget_desc),
                isCollapsible = true,
                isExpanded = state.isWidgetsExpanded,
                onToggleExpand = { viewModel.toggleWidgetsExpanded() }
            ) {
                Text(
                    text = stringResource(id = R.string.widget_config_theme),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val sysStr = stringResource(R.string.widget_theme_system)
                val lightStr = stringResource(R.string.widget_theme_light)
                val darkStr = stringResource(R.string.widget_theme_dark)
                PillSelector(
                    items = com.tirepressure.app.widget.WidgetTheme.entries,
                    selectedItem = defaultWidgetTheme,
                    onItemSelected = { theme ->
                        defaultWidgetTheme = theme
                        com.tirepressure.app.widget.WidgetPreferencesHelper.setDefaultWidgetTheme(context.applicationContext, theme)
                        com.tirepressure.app.widget.WidgetUpdateHelper.updateAllWidgets(context.applicationContext)
                    },
                    labelProvider = {
                        when (it) {
                            com.tirepressure.app.widget.WidgetTheme.SYSTEM -> sysStr
                            com.tirepressure.app.widget.WidgetTheme.LIGHT -> lightStr
                            com.tirepressure.app.widget.WidgetTheme.DARK -> darkStr
                        }
                    }
                )
            }

            // 5. Sauvegarde et Données (Réductible, replié par défaut)
            SectionCard(
                title = stringResource(id = R.string.settings_backup),
                icon = Icons.Default.SaveAlt,
                subtitle = stringResource(id = R.string.settings_backup_desc),
                isCollapsible = true,
                isExpanded = state.isBackupExpanded,
                onToggleExpand = { viewModel.toggleBackupExpanded() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.press()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val filename = "bike-pressure-backup-${dateFormat.format(Date())}.json"
                            exportLauncher.launch(filename)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.settings_export_btn))
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.press()
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.settings_import_btn))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.requestReset() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.settings_reset_all_data))
                }
            }

            // 7. À propos & Confidentialité - En pied, non réductible
            AboutAndPrivacyCard()

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Estompage supérieur et inférieur fluide avec indicateur de défilement
        ScrollTopFade(visible = scrollState.canScrollBackward)
        ScrollBottomFade(
            visible = scrollState.canScrollForward,
            showIndicator = true,
            indicatorText = stringResource(id = R.string.settings_scroll_more)
        )
    }

        // Dialogue de réinitialisation
        if (state.showResetConfirmation) {
            GlassAlertDialog(
                onDismissRequest = { viewModel.cancelReset() },
                title = { Text(stringResource(id = R.string.settings_reset_dialog_title)) },
                text = { Text(stringResource(id = R.string.settings_reset_dialog_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmReset(onResetCompleted = onNavigateToOnboarding) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(id = R.string.settings_reset_all_data))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelReset() }) {
                        Text(stringResource(id = R.string.action_cancel))
                    }
                }
            )
        }

        // Dialogue d'importation : Remplacer ou Fusionner
        state.pendingImportPayload?.let { payload ->
            GlassAlertDialog(
                onDismissRequest = { viewModel.cancelImport() },
                title = { Text(stringResource(id = R.string.settings_import_dialog_title)) },
                text = {
                    Text(stringResource(id = R.string.settings_import_dialog_msg, payload.bikes.size, payload.exportDate))
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { viewModel.confirmImport(merge = true) }) {
                            Text(stringResource(id = R.string.settings_import_merge))
                        }
                        Button(onClick = { viewModel.confirmImport(merge = false) }) {
                            Text(stringResource(id = R.string.settings_import_replace))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelImport() }) {
                        Text(stringResource(id = R.string.action_cancel))
                    }
                }
            )
        }
    }
}
