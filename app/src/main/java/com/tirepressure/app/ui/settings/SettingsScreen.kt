package com.tirepressure.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.model.AppLanguage
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.ui.components.AboutAndPrivacyCard
import com.tirepressure.app.ui.components.NumberInputField
import com.tirepressure.app.ui.components.PillSelector
import com.tirepressure.app.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // États de réduction des sections
    var isUnitsExpanded by remember { mutableStateOf(true) }
    var isDisplayExpanded by remember { mutableStateOf(false) }
    var isWidgetsExpanded by remember { mutableStateOf(false) }
    var isBackupExpanded by remember { mutableStateOf(false) }

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
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.action_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profil Cycliste (Poids de référence) - En tête, non réductible
            var riderWeightInput by remember(state.riderWeightKg, state.massUnit) {
                mutableStateOf(state.massUnit.format(state.riderWeightKg).replace(',', '.'))
            }
            var riderWeightError by remember { mutableStateOf<String?>(null) }

            SectionCard(
                title = stringResource(id = R.string.settings_rider_profile),
                icon = Icons.Default.Person,
                subtitle = stringResource(id = R.string.onboarding_weight_desc),
                isCollapsible = false
            ) {
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

            // 2. Unités & Calculs (Regroupé, réductible)
            SectionCard(
                title = stringResource(id = R.string.settings_units_surfaces_title),
                icon = Icons.Default.SquareFoot,
                subtitle = stringResource(id = R.string.settings_units_surfaces_desc),
                isCollapsible = true,
                isExpanded = isUnitsExpanded,
                onToggleExpand = { isUnitsExpanded = !isUnitsExpanded }
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { /* Toujours obligatoire */ },
                        label = {
                            Text(
                                text = "☀️ " + stringResource(R.string.overview_dry),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = state.showWetSurface,
                        onClick = { viewModel.setShowWetSurface(!state.showWetSurface) },
                        label = {
                            Text(
                                text = "🌧️ " + stringResource(R.string.overview_wet),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = state.showSnowSurface,
                        onClick = { viewModel.setShowSnowSurface(!state.showSnowSurface) },
                        label = {
                            Text(
                                text = "❄️ " + stringResource(R.string.overview_snow),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        state.showWetSurface && state.showSnowSurface -> stringResource(R.string.surfaces_count_3)
                        state.showWetSurface -> stringResource(R.string.surfaces_count_2_wet)
                        state.showSnowSurface -> stringResource(R.string.surfaces_count_2_snow)
                        else -> stringResource(R.string.surfaces_count_1)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Affichage & Langue (Regroupé, réductible)
            SectionCard(
                title = stringResource(id = R.string.settings_display_language_title),
                icon = Icons.Default.Palette,
                subtitle = stringResource(id = R.string.settings_display_language_desc),
                isCollapsible = true,
                isExpanded = isDisplayExpanded,
                onToggleExpand = { isDisplayExpanded = !isDisplayExpanded }
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
                    onItemSelected = { viewModel.setTheme(it) },
                    labelProvider = { context.getString(it.labelRes) }
                )

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
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                            if (lang == AppLanguage.SYSTEM) {
                                localeManager?.applicationLocales = android.os.LocaleList.getEmptyLocaleList()
                            } else {
                                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(lang.code)
                            }
                        }
                    },
                    labelProvider = { context.getString(it.labelRes) }
                )
            }

            // 4. Widgets d'écran d'accueil (Réductible)
            var defaultWidgetTheme by remember {
                mutableStateOf(com.tirepressure.app.widget.WidgetPreferencesHelper.getDefaultWidgetTheme(context.applicationContext))
            }
            SectionCard(
                title = stringResource(id = R.string.settings_widget_section),
                icon = Icons.Default.Widgets,
                subtitle = stringResource(id = R.string.settings_widget_desc),
                isCollapsible = true,
                isExpanded = isWidgetsExpanded,
                onToggleExpand = { isWidgetsExpanded = !isWidgetsExpanded }
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

            // 5. Sauvegarde et Données (Réductible)
            SectionCard(
                title = stringResource(id = R.string.settings_backup),
                icon = Icons.Default.SaveAlt,
                subtitle = stringResource(id = R.string.settings_backup_desc),
                isCollapsible = true,
                isExpanded = isBackupExpanded,
                onToggleExpand = { isBackupExpanded = !isBackupExpanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
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
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
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

            // 6. À propos & Confidentialité - En pied, non réductible
            AboutAndPrivacyCard()

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dialogue de réinitialisation
        if (state.showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelReset() },
                title = { Text(stringResource(id = R.string.settings_reset_dialog_title)) },
                text = { Text(stringResource(id = R.string.settings_reset_dialog_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmReset() },
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
            AlertDialog(
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
