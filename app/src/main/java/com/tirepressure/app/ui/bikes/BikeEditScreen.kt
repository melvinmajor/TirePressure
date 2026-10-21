package com.tirepressure.app.ui.bikes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.ScrollBottomFade
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.*
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.components.LocalGlassBackdrop
import io.github.angelomarzocchi.glass.glassBackdrop
import io.github.angelomarzocchi.glass.rememberGlassBackdrop
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import io.github.angelomarzocchi.flight.rememberFlightAnchor
import io.github.angelomarzocchi.flight.recordedFlightAnchor
import io.github.angelomarzocchi.flight.RecordedPaint
import io.github.angelomarzocchi.lumen.GlossCard
import io.github.angelomarzocchi.lumen.GlossCardColors
import io.github.angelomarzocchi.lumen.GlossCardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tirepressure.app.ui.components.AddSetupDialog
import com.tirepressure.app.ui.components.GlassAlertDialog
import com.tirepressure.app.ui.components.NumberInputField
import com.tirepressure.app.ui.components.PillSelector
import androidx.compose.foundation.BorderStroke
import com.tirepressure.app.ui.components.SectionCard
import com.tirepressure.app.ui.components.YearInputField
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tirepressure.app.ui.components.formatDecimal
import com.tirepressure.app.ui.theme.getCategoryColor
import com.tirepressure.app.ui.theme.getRideStyleColor
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import com.tirepressure.app.ui.overview.AddSetupDraft

private class VmState<T>(
    private val getter: () -> T,
    private val setter: (T) -> Unit
) : MutableState<T> {
    override var value: T
        get() = getter()
        set(v) = setter(v)
    override fun component1(): T = getter()
    override fun component2(): (T) -> Unit = setter
}

private fun <T> vmState(getter: () -> T, setter: (T) -> Unit): MutableState<T> =
    VmState(getter, setter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeEditScreen(
    bikeId: String,
    viewModel: BikeEditViewModel,
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val massUnit by preferencesRepository.massUnitFlow.collectAsStateWithLifecycle(initialValue = MassUnit.KG)

    val defaultSetupName = stringResource(id = R.string.config_default_name)
    val defaultSetupPrefix = stringResource(id = R.string.setup_default_prefix)
    val riderWeightKg by preferencesRepository.riderWeightFlow.collectAsStateWithLifecycle(initialValue = 75.0)
    val pressureUnit by preferencesRepository.pressureUnitFlow.collectAsStateWithLifecycle(initialValue = PressureUnit.BAR)

    LaunchedEffect(bikeId, massUnit) {
        viewModel.loadBikeIfNeeded(bikeId, massUnit, defaultSetupName)
    }

    val isLoaded = state.isLoaded
    val currentId = state.currentId

    var name by remember(state, viewModel) { vmState({ state.name }, { viewModel.setName(it) }) }
    var brand by remember(state, viewModel) { vmState({ state.brand }, { viewModel.setBrand(it) }) }
    var model by remember(state, viewModel) { vmState({ state.model }, { viewModel.setModel(it) }) }
    var yearInput by remember(state, viewModel) { vmState({ state.yearInput }, { viewModel.setYearInput(it) }) }
    var frameSize by remember(state, viewModel) { vmState({ state.frameSize }, { viewModel.setFrameSize(it) }) }
    var bikeCategory by remember(state, viewModel) { vmState({ state.bikeCategory }, { viewModel.setBikeCategory(it) }) }
    var bikeWeightInput by remember(state, viewModel) { vmState({ state.bikeWeightInput }, { viewModel.setBikeWeightInput(it) }) }
    var isDefault by remember(state, viewModel) { vmState({ state.isDefault }, { viewModel.setIsDefault(it) }) }

    var isGeneralInfoExpanded by remember(state, viewModel) { vmState({ state.isGeneralInfoExpanded }, { viewModel.toggleGeneralInfoExpanded() }) }
    var isSetupsExpanded by remember(state, viewModel) { vmState({ state.isSetupsExpanded }, { viewModel.toggleSetupsExpanded() }) }
    var isDiameterExpanded by remember(state, viewModel) { vmState({ state.isDiameterExpanded }, { viewModel.toggleDiameterExpanded() }) }
    var isFrontWheelExpanded by remember(state, viewModel) { vmState({ state.isFrontWheelExpanded }, { viewModel.toggleFrontWheelExpanded() }) }
    var isRearWheelExpanded by remember(state, viewModel) { vmState({ state.isRearWheelExpanded }, { viewModel.toggleRearWheelExpanded() }) }

    var setups by remember(state, viewModel) { vmState({ state.setups }, { viewModel.updateSetups(it) }) }
    var selectedSetupIndex by remember(state, viewModel) { vmState({ state.selectedSetupIndex }, { viewModel.setSelectedSetupIndex(it) }) }
    var isSameWheels by remember(state, viewModel) { vmState({ state.isSameWheels }, { viewModel.setIsSameWheels(it) }) }

    var showAddSetupDialog by remember(state, viewModel) { vmState({ state.showAddSetupDialog }, { viewModel.setShowAddSetupDialog(it) }) }
    var showDeleteBikeConfirmation by remember(state, viewModel) { vmState({ state.showDeleteBikeConfirmation }, { viewModel.setShowDeleteBikeConfirmation(it) }) }
    var setupToDeleteId by remember(state, viewModel) { vmState({ state.setupToDeleteId }, { viewModel.setSetupToDeleteId(it) }) }
    val setupToDeleteConfirmation = setups.find { it.id == setupToDeleteId }

    var nameError by remember(state, viewModel) { vmState({ state.nameError }, { viewModel.setNameError(it) }) }

    val nameRequiredErrorMsg = stringResource(id = R.string.bike_name_required_error)
    val isDeleteDialogOpen = showDeleteBikeConfirmation || setupToDeleteConfirmation != null || showAddSetupDialog
    val blurRadius by animateDpAsState(
        targetValue = if (isDeleteDialogOpen) 14.dp else 0.dp,
        animationSpec = tween(250),
        label = "EditScreenDialogBlur"
    )

    BackHandler(enabled = !isDeleteDialogOpen) {
        viewModel.reset()
        onNavigateBack()
    }

    val glassBackdrop = rememberGlassBackdrop()
    val bikeHeroAnchor = rememberFlightAnchor(
        key = "bike_hero_${bikeId}",
        paint = remember { RecordedPaint(single = false) }
    )

    val activeSetup = setups.getOrNull(selectedSetupIndex) ?: setups.firstOrNull() ?: WheelSetup(id = "default", name = "Default")
    val calculator = remember { BertoTirePressureCalculator() }
    val liveCalculationResult = remember(bikeCategory, bikeWeightInput, massUnit, activeSetup, riderWeightKg) {
        try {
            val weightKg = UnitConverter.toKg(bikeWeightInput.toDoubleOrNull() ?: 10.0, massUnit)
            val effectiveStyle = activeSetup.getEffectiveRideStyle(bikeCategory)
            val input = PressureCalculationInput(
                riderWeightKg = riderWeightKg,
                bikeWeightKg = weightKg,
                luggageWeightKg = activeSetup.luggageWeightKg,
                rideStyle = effectiveStyle,
                frontTire = activeSetup.frontTire,
                rearTire = activeSetup.rearTire,
                frontWheel = activeSetup.frontWheel,
                rearWheel = activeSetup.rearWheel,
                surfaceCondition = SurfaceCondition.DRY
            )
            calculator.calculate(input)
        } catch (e: Exception) {
            null
        }
    }

    CompositionLocalProvider(LocalGlassBackdrop provides glassBackdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdrop(glassBackdrop)
                    .background(MaterialTheme.colorScheme.background)
                    .blur(blurRadius),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(if (bikeId == "new") R.string.bike_edit_title_new else R.string.bike_edit_title_edit),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .recordedFlightAnchor(bikeHeroAnchor)
                                    .semantics { heading() }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_cancel))
                    }
                },
                actions = {
                    if (bikeId != "new") {
                        IconButton(onClick = { showDeleteBikeConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.delete_bike_button),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = nameRequiredErrorMsg
                                return@IconButton
                            }
                            viewModel.saveBike(
                                massUnit = massUnit,
                                nameRequiredError = nameRequiredErrorMsg,
                                onSaved = onNavigateBack
                            )
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (isLoaded) {
                liveCalculationResult?.let { result ->
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

                    val accentGlossColors = GlossCardColors(
                        top = primaryColor,
                        middle = primaryColor,
                        bottom = primaryColor.copy(alpha = if (isDark) 0.88f else 0.82f),
                        slab = Color.White.copy(alpha = if (isDark) 0.35f else 0.50f),
                        content = onPrimaryColor,
                        shadow = primaryColor.copy(alpha = if (isDark) 0.45f else 0.25f),
                        contactShadow = Color.Black.copy(alpha = if (isDark) 0.30f else 0.15f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        GlossCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDark) 0.40f else 0.55f),
                                            Color.White.copy(alpha = if (isDark) 0.15f else 0.20f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            colors = accentGlossColors,
                            tiltOnTouch = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.bike_edit_live_preview_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = onPrimaryColor.copy(alpha = 0.82f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${activeSetup.name} • ☀️ ${stringResource(R.string.overview_dry)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = onPrimaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stringResource(R.string.widget_front_abbr),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onPrimaryColor.copy(alpha = 0.82f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${pressureUnit.format(result.front.pressureMbar)} ${pressureUnit.symbol}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = onPrimaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stringResource(R.string.widget_rear_abbr),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onPrimaryColor.copy(alpha = 0.82f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${pressureUnit.format(result.rear.pressureMbar)} ${pressureUnit.symbol}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = onPrimaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (!isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
            // 1. Informations Générales (Réductible par défaut et non éditable pour un vélo existant)
            item {
                SectionCard(
                    title = stringResource(id = R.string.bike_general_info_section),
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    isCollapsible = true,
                    isExpanded = isGeneralInfoExpanded,
                    onToggleExpand = { isGeneralInfoExpanded = !isGeneralInfoExpanded },
                    collapsedSummary = {
                        Text(
                            text = "${name.ifBlank { "..." }} • ${stringResource(id = bikeCategory.labelRes)} • $bikeWeightInput ${massUnit.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    if (bikeId != "new") {
                        // Notice expliquant que les caractéristiques partagées du vélo s'éditent via le menu d'accueil
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(id = R.string.bike_general_info_readonly_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text(stringResource(id = R.string.bike_name_label)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = brand.ifBlank { "—" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(stringResource(id = R.string.bike_brand_label)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = model.ifBlank { "—" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(stringResource(id = R.string.bike_model_label)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = yearInput.ifBlank { "—" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(stringResource(id = R.string.bike_year_label)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = frameSize.ifBlank { "—" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(stringResource(id = R.string.bike_size_label)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = stringResource(id = bikeCategory.labelRes),
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(stringResource(id = R.string.bike_category_label)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "$bikeWeightInput ${massUnit.symbol}",
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(stringResource(id = R.string.bike_weight)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Vélo neuf : Champs éditables
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = null
                            },
                            label = { Text(stringResource(id = R.string.bike_name_label)) },
                            placeholder = { Text(stringResource(id = R.string.bike_name_placeholder)) },
                            isError = nameError != null,
                            supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Catégorie
                        Text(
                            text = stringResource(id = R.string.bike_category_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PillSelector(
                            items = BikeCategory.entries,
                            selectedItem = bikeCategory,
                            onItemSelected = { newCat ->
                                bikeCategory = newCat
                                val catWeightInUserUnit = UnitConverter.fromKg(newCat.defaultWeightKg, massUnit)
                                bikeWeightInput = String.format(java.util.Locale.US, "%.1f", catWeightInUserUnit)
                                val updatedSetups = setups.mapIndexed { idx, s ->
                                    if (idx == 0 && s.rideStyle == null) {
                                        val defStyle = newCat.defaultRideStyle
                                        val validDiameters = WheelDiameter.getOptionsForStyle(defStyle)
                                        s.copy(
                                            frontTire = s.frontTire.copy(widthMm = defStyle.defaultTireWidthMm),
                                            rearTire = s.rearTire.copy(widthMm = defStyle.defaultTireWidthMm),
                                            frontWheel = s.frontWheel.copy(
                                                diameter = validDiameters.first(),
                                                innerRimWidthMm = defStyle.defaultInnerRimWidthMm,
                                                rimType = defStyle.defaultRimType
                                            ),
                                            rearWheel = s.rearWheel.copy(
                                                diameter = validDiameters.first(),
                                                innerRimWidthMm = defStyle.defaultInnerRimWidthMm,
                                                rimType = defStyle.defaultRimType
                                            )
                                        )
                                    } else s
                                }
                                setups = updatedSetups
                            },
                            labelProvider = { context.getString(it.labelRes) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                label = { Text(stringResource(id = R.string.bike_brand_label)) },
                                placeholder = { Text(stringResource(id = R.string.bike_brand_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                label = { Text(stringResource(id = R.string.bike_model_label)) },
                                placeholder = { Text(stringResource(id = R.string.bike_model_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            YearInputField(
                                value = yearInput,
                                onValueChange = { yearInput = it },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = frameSize,
                                onValueChange = { frameSize = it },
                                label = { Text(stringResource(id = R.string.bike_size_label)) },
                                placeholder = { Text(stringResource(id = R.string.bike_size_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        NumberInputField(
                            value = bikeWeightInput,
                            onValueChange = { bikeWeightInput = it },
                            label = stringResource(id = R.string.bike_weight),
                            suffix = massUnit.symbol
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isDefault,
                                onCheckedChange = { isDefault = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.bike_is_default_checkbox),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 2. Gestion des Configurations Multiples (Réductible)
            item {
                SectionCard(
                    title = stringResource(id = R.string.bike_setups_section_title, setups.size),
                    icon = Icons.Default.Settings,
                    subtitle = stringResource(id = R.string.bike_setups_section_desc),
                    isCollapsible = true,
                    isExpanded = isSetupsExpanded,
                    onToggleExpand = { isSetupsExpanded = !isSetupsExpanded },
                    collapsedSummary = {
                        Text(
                            text = "${activeSetup.name} (${stringResource(activeSetup.getEffectiveRideStyle(bikeCategory).labelRes)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    // Onglets de sélection de configuration en LightTabs (Option 2A)
                    PillSelector(
                        items = setups.indices.toList(),
                        selectedItem = selectedSetupIndex,
                        onItemSelected = { selectedSetupIndex = it },
                        labelProvider = { setups.getOrNull(it)?.name ?: "" },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddSetupDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(id = R.string.action_add), maxLines = 1)
                        }

                        if (setups.size > 1) {
                            OutlinedButton(
                                onClick = { setupToDeleteId = activeSetup.id },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(id = R.string.action_delete), color = MaterialTheme.colorScheme.error, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = activeSetup.name,
                        onValueChange = { newName ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(name = newName) else s
                            }
                        },
                        label = { Text(stringResource(id = R.string.bike_setup_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Discipline / Usage spécifique à ce profil
                    var setupStyleMenuExpanded by remember { mutableStateOf(false) }
                    val currentSetupStyle = activeSetup.getEffectiveRideStyle(bikeCategory)
                    Column {
                        Text(
                            text = stringResource(id = R.string.bike_setup_style_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { setupStyleMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val styleColor = getRideStyleColor(currentSetupStyle, bikeCategory)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = styleColor.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, styleColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = stringResource(id = currentSetupStyle.labelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = styleColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val defaultCategoryName = stringResource(id = bikeCategory.labelRes)
                                    val defaultStyleName = stringResource(id = bikeCategory.defaultRideStyle.labelRes)
                                    val isDefaultStyle = activeSetup.rideStyle == null || activeSetup.rideStyle == bikeCategory.defaultRideStyle
                                    Text(
                                        text = if (isDefaultStyle) {
                                            stringResource(id = R.string.bike_setup_style_default, defaultCategoryName, defaultStyleName)
                                        } else {
                                            stringResource(id = R.string.bike_setup_style_custom, stringResource(id = currentSetupStyle.labelRes))
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = setupStyleMenuExpanded,
                            onDismissRequest = { setupStyleMenuExpanded = false }
                        ) {
                            val defaultCategoryName = stringResource(id = bikeCategory.labelRes)
                            val defaultStyleName = stringResource(id = bikeCategory.defaultRideStyle.labelRes)
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.bike_setup_style_default, defaultCategoryName, defaultStyleName)) },
                                onClick = {
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) s.copy(rideStyle = null) else s
                                    }
                                    setupStyleMenuExpanded = false
                                }
                            )
                            HorizontalDivider()
                            RideStyle.entries.forEach { st ->
                                val stColor = getRideStyleColor(st, bikeCategory)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(stColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(id = st.labelRes),
                                                fontWeight = if (st == currentSetupStyle) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        setups = setups.mapIndexed { idx, s ->
                                            if (idx == selectedSetupIndex) s.copy(rideStyle = st) else s
                                        }
                                        setupStyleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var luggageWeightInput by rememberSaveable(selectedSetupIndex, activeSetup.id, massUnit) {
                        val currentLuggage = UnitConverter.fromKg(activeSetup.luggageWeightKg, massUnit)
                        mutableStateOf(formatDecimal(currentLuggage, 1))
                    }
                    LaunchedEffect(activeSetup.luggageWeightKg, massUnit) {
                        val typed = luggageWeightInput.toDoubleOrNull() ?: 0.0
                        val expected = UnitConverter.fromKg(activeSetup.luggageWeightKg, massUnit)
                        if (Math.abs(typed - expected) > 0.05) {
                            luggageWeightInput = formatDecimal(expected, 1)
                        }
                    }

                    NumberInputField(
                        value = luggageWeightInput,
                        onValueChange = { wStr ->
                            luggageWeightInput = wStr
                            val typedVal = wStr.toDoubleOrNull() ?: 0.0
                            val wKg = UnitConverter.toKg(typedVal, massUnit)
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(luggageWeightKg = wKg) else s
                            }
                        },
                        label = stringResource(id = R.string.luggage_weight),
                        suffix = massUnit.symbol
                    )
                }
            }


            // 3. Diamètre de Roue (Réductible, de la plus petite à la plus grande taille)
            item {
                SectionCard(
                    title = stringResource(id = R.string.wheel_diameter),
                    icon = Icons.Default.DonutLarge,
                    isCollapsible = true,
                    isExpanded = isDiameterExpanded,
                    onToggleExpand = { isDiameterExpanded = !isDiameterExpanded },
                    collapsedSummary = {
                        val currentSetupStyle = activeSetup.getEffectiveRideStyle(bikeCategory)
                        val fLabel = activeSetup.frontWheel.diameter.getLabelForStyle(currentSetupStyle)
                        val rLabel = activeSetup.rearWheel.diameter.getLabelForStyle(currentSetupStyle)
                        Text(
                            text = if (fLabel == rLabel) fLabel else "$fLabel / $rLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    val currentSetupStyle = activeSetup.getEffectiveRideStyle(bikeCategory)
                    val availableDiameters = WheelDiameter.getOptionsForStyle(currentSetupStyle)
                    val frontDiameter = if (activeSetup.frontWheel.diameter in availableDiameters)
                        activeSetup.frontWheel.diameter
                    else
                        availableDiameters.first()
                    val rearDiameter = if (activeSetup.rearWheel.diameter in availableDiameters)
                        activeSetup.rearWheel.diameter
                    else
                        availableDiameters.first()

                    val isMulletAllowed = (bikeCategory == BikeCategory.MTB || bikeCategory == BikeCategory.COMMUTE) && !isSameWheels

                    var isMulletMode by remember(selectedSetupIndex, activeSetup.frontWheel.diameter, activeSetup.rearWheel.diameter) {
                        mutableStateOf(isMulletAllowed && activeSetup.frontWheel.diameter != activeSetup.rearWheel.diameter)
                    }

                    if (isMulletAllowed) {
                        // Bascule Même taille vs Mullet / MX
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isMulletMode,
                                onClick = {
                                    isMulletMode = false
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) {
                                            s.copy(rearWheel = s.rearWheel.copy(diameter = s.frontWheel.diameter))
                                        } else s
                                    }
                                },
                                label = { Text(stringResource(id = R.string.mullet_mode_same)) }
                            )
                            FilterChip(
                                selected = isMulletMode,
                                onClick = {
                                    isMulletMode = true
                                    if (frontDiameter == rearDiameter && availableDiameters.size > 1) {
                                        val secondDiam = availableDiameters.getOrNull(1) ?: rearDiameter
                                        setups = setups.mapIndexed { idx, s ->
                                            if (idx == selectedSetupIndex) {
                                                s.copy(rearWheel = s.rearWheel.copy(diameter = secondDiam))
                                            } else s
                                        }
                                    }
                                },
                                label = { Text(stringResource(id = R.string.mullet_mode_mixed)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    AnimatedContent(
                        targetState = isMulletMode && isMulletAllowed,
                        transitionSpec = {
                            expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)) togetherWith
                            shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        },
                        label = "MulletDiameterTransition"
                    ) { mulletActive ->
                        if (!mulletActive) {
                            PillSelector(
                                items = availableDiameters,
                                selectedItem = frontDiameter,
                                onItemSelected = { newDiam ->
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) {
                                            s.copy(
                                                frontWheel = s.frontWheel.copy(diameter = newDiam),
                                                rearWheel = s.rearWheel.copy(diameter = newDiam)
                                            )
                                        } else s
                                    }
                                },
                                labelProvider = { it.getLabelForStyle(currentSetupStyle) }
                            )
                        } else {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.mullet_front_diameter),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PillSelector(
                                    items = availableDiameters,
                                    selectedItem = frontDiameter,
                                    onItemSelected = { newDiam ->
                                        setups = setups.mapIndexed { idx, s ->
                                            if (idx == selectedSetupIndex) {
                                                s.copy(frontWheel = s.frontWheel.copy(diameter = newDiam))
                                            } else s
                                        }
                                    },
                                    labelProvider = { it.getLabelForStyle(currentSetupStyle) }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = stringResource(id = R.string.mullet_rear_diameter),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PillSelector(
                                    items = availableDiameters,
                                    selectedItem = rearDiameter,
                                    onItemSelected = { newDiam ->
                                        setups = setups.mapIndexed { idx, s ->
                                            if (idx == selectedSetupIndex) {
                                                s.copy(rearWheel = s.rearWheel.copy(diameter = newDiam))
                                            } else s
                                        }
                                    },
                                    labelProvider = { it.getLabelForStyle(currentSetupStyle) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(id = R.string.mullet_badge),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Roue Avant (Réductible)
            item {
                SectionCard(
                    title = stringResource(id = R.string.wheel_front),
                    icon = Icons.Default.DonutLarge,
                    isCollapsible = true,
                    isExpanded = isFrontWheelExpanded,
                    onToggleExpand = { isFrontWheelExpanded = !isFrontWheelExpanded },
                    collapsedSummary = {
                        val cStr = stringResource(id = activeSetup.frontTire.casing.labelRes)
                        val rStr = stringResource(id = activeSetup.frontWheel.rimType.labelRes)
                        Text(
                            text = "${formatDecimal(activeSetup.frontTire.widthMm)}mm • $rStr • $cStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    WheelTireFormBlock(
                        wheelKey = "${activeSetup.id}_front",
                        tireWidthMm = activeSetup.frontTire.widthMm,
                        onTireWidthChangeMm = { w ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) {
                                    s.copy(
                                        frontTire = s.frontTire.copy(widthMm = w),
                                        rearTire = if (isSameWheels) s.rearTire.copy(widthMm = w) else s.rearTire
                                    )
                                } else s
                            }
                        },
                        innerRimWidthMm = activeSetup.frontWheel.innerRimWidthMm,
                        onInnerRimWidthChangeMm = { r ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) {
                                    s.copy(
                                        frontWheel = s.frontWheel.copy(innerRimWidthMm = r),
                                        rearWheel = if (isSameWheels) s.rearWheel.copy(innerRimWidthMm = r) else s.rearWheel
                                    )
                                } else s
                            }
                        },
                        rimType = activeSetup.frontWheel.rimType,
                        onRimTypeChange = { rt ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) {
                                    s.copy(
                                        frontWheel = s.frontWheel.copy(rimType = rt),
                                        rearWheel = if (isSameWheels) s.rearWheel.copy(rimType = rt) else s.rearWheel
                                    )
                                } else s
                            }
                        },
                        casing = activeSetup.frontTire.casing,
                        onCasingChange = { c ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) {
                                    s.copy(
                                        frontTire = s.frontTire.copy(casing = c),
                                        rearTire = if (isSameWheels) s.rearTire.copy(casing = c) else s.rearTire
                                    )
                                } else s
                            }
                        }
                    )
                }
            }

            // Bascule Roue arrière identique à l'avant
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.bike_same_wheels_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(id = R.string.bike_same_wheels_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = isSameWheels,
                            onCheckedChange = { checked ->
                                isSameWheels = checked
                                if (checked) {
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) {
                                            s.copy(
                                                rearTire = s.frontTire,
                                                rearWheel = s.frontWheel
                                            )
                                        } else s
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 5. Roue Arrière (Réductible si indépendante, ou badge synchronisé)
            item {
                AnimatedVisibility(
                    visible = !isSameWheels,
                    enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
                    exit = shrinkVertically(tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                setups = setups.mapIndexed { idx, s ->
                                    if (idx == selectedSetupIndex) {
                                        s.copy(
                                            rearTire = s.frontTire,
                                            rearWheel = s.frontWheel
                                        )
                                    } else s
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(id = R.string.copy_front_to_rear))
                        }

                        SectionCard(
                            title = stringResource(id = R.string.wheel_rear),
                            icon = Icons.Default.DonutLarge,
                            isCollapsible = true,
                            isExpanded = isRearWheelExpanded,
                            onToggleExpand = { isRearWheelExpanded = !isRearWheelExpanded },
                            collapsedSummary = {
                                val cStr = stringResource(id = activeSetup.rearTire.casing.labelRes)
                                val rStr = stringResource(id = activeSetup.rearWheel.rimType.labelRes)
                                Text(
                                    text = "${formatDecimal(activeSetup.rearTire.widthMm)}mm • $rStr • $cStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        ) {
                            WheelTireFormBlock(
                                wheelKey = "${activeSetup.id}_rear",
                                tireWidthMm = activeSetup.rearTire.widthMm,
                                onTireWidthChangeMm = { w ->
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) s.copy(rearTire = s.rearTire.copy(widthMm = w)) else s
                                    }
                                },
                                innerRimWidthMm = activeSetup.rearWheel.innerRimWidthMm,
                                onInnerRimWidthChangeMm = { r ->
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) s.copy(rearWheel = s.rearWheel.copy(innerRimWidthMm = r)) else s
                                    }
                                },
                                rimType = activeSetup.rearWheel.rimType,
                                onRimTypeChange = { rt ->
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) s.copy(rearWheel = s.rearWheel.copy(rimType = rt)) else s
                                    }
                                },
                                casing = activeSetup.rearTire.casing,
                                onCasingChange = { c ->
                                    setups = setups.mapIndexed { idx, s ->
                                        if (idx == selectedSetupIndex) s.copy(rearTire = s.rearTire.copy(casing = c)) else s
                                    }
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isSameWheels,
                    enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
                    exit = shrinkVertically(tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.bike_same_wheels_badge),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Bouton Enregistrer
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = nameRequiredErrorMsg
                            return@Button
                        }
                        viewModel.saveBike(
                            massUnit = massUnit,
                            nameRequiredError = nameRequiredErrorMsg,
                            onSaved = onNavigateBack
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.action_save))
                }
            }
        }

        ScrollTopFade(visible = listState.canScrollBackward)
        ScrollBottomFade(
            visible = listState.canScrollForward,
            showIndicator = true,
            indicatorText = stringResource(id = R.string.settings_scroll_more)
        )
    }
}

    if (showDeleteBikeConfirmation) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteBikeConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.delete_bike_dialog_title)) },
            text = { Text(stringResource(R.string.delete_bike_dialog_msg, name.ifBlank { stringResource(R.string.overview_title) })) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBike(onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteBikeConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    setupToDeleteConfirmation?.let { setupToDel ->
        GlassAlertDialog(
            onDismissRequest = { setupToDeleteId = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.delete_setup_dialog_title)) },
            text = { Text(stringResource(R.string.delete_setup_dialog_msg, setupToDel.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDeleteSetup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { setupToDeleteId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAddSetupDialog) {
        val currentBikeWeightKg = UnitConverter.toKg(bikeWeightInput.toDoubleOrNull() ?: UnitConverter.fromKg(8.0, massUnit), massUnit)
        val draft = state.addSetupDraft ?: AddSetupDraft(
            bikeId = currentId,
            bikeName = name.ifBlank { stringResource(R.string.overview_title) },
            bikeWeightKg = currentBikeWeightKg,
            bikeCategory = bikeCategory,
            setupName = "$defaultSetupPrefix ${setups.size + 1}",
            useExistingTemplate = setups.isNotEmpty(),
            selectedTemplateIndex = 0,
            setupRideStyle = if (setups.isNotEmpty()) setups[0].getEffectiveRideStyle(bikeCategory) else bikeCategory.defaultRideStyle
        )
        AddSetupDialog(
            draft = draft,
            existingSetups = setups,
            massUnit = massUnit,
            onDraftChange = { viewModel.updateAddSetupDraft(it) },
            onDismiss = { viewModel.cancelAddSetup() },
            onConfirm = { newSetupName, templateSetup, overrideRideStyle ->
                viewModel.confirmAddSetup(newSetupName, templateSetup, overrideRideStyle, defaultSetupPrefix)
            }
        )
    }
        }
    }
}

@Composable
fun BikeEditScreen(
    bikeId: String,
    bikeRepository: com.tirepressure.app.domain.repository.BikeRepository,
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: BikeEditViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "BikeEditViewModel_$bikeId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return BikeEditViewModel(bikeRepository) as T
            }
        }
    )
    BikeEditScreen(
        bikeId = bikeId,
        viewModel = viewModel,
        preferencesRepository = preferencesRepository,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun WheelTireFormBlock(
    wheelKey: String,
    tireWidthMm: Double,
    onTireWidthChangeMm: (Double) -> Unit,
    innerRimWidthMm: Double,
    onInnerRimWidthChangeMm: (Double) -> Unit,
    rimType: RimType,
    onRimTypeChange: (RimType) -> Unit,
    casing: TireCasing,
    onCasingChange: (TireCasing) -> Unit
) {
    // Mode d'unité : mm ou pouces
    // Initialise une seule fois par configuration de roue la préférence d'unité sans bascule automatique lors de la saisie
    val defaultToInches = remember(wheelKey) {
        val inInches = tireWidthMm / 25.4
        val nearestTenth = Math.round(inInches * 10.0) / 10.0
        tireWidthMm >= 50.0 && Math.abs(inInches - nearestTenth) < 0.08
    }

    var isInchUnit by rememberSaveable(wheelKey) {
        mutableStateOf(defaultToInches)
    }

    var tireTextInput by rememberSaveable(wheelKey) {
        val str = if (isInchUnit) {
            formatDecimal(tireWidthMm / 25.4, 2)
        } else {
            formatDecimal(tireWidthMm, 2)
        }
        mutableStateOf(str)
    }

    var innerRimTextInput by rememberSaveable(wheelKey) {
        mutableStateOf(formatDecimal(innerRimWidthMm, 2))
    }

    // Synchronisation de la valeur de pneu externe uniquement lors d'un vrai changement externe (ex: preset, copie)
    LaunchedEffect(tireWidthMm, isInchUnit) {
        val typedNum = tireTextInput.toDoubleOrNull()
        val typedMm = if (typedNum != null) {
            if (isInchUnit) typedNum * 25.4 else typedNum
        } else null

        if (typedMm == null || Math.abs(typedMm - tireWidthMm) > 0.02) {
            tireTextInput = if (isInchUnit) {
                formatDecimal(tireWidthMm / 25.4, 2)
            } else {
                formatDecimal(tireWidthMm, 2)
            }
        }
    }

    // Synchronisation de la largeur interne de jante externe
    LaunchedEffect(innerRimWidthMm) {
        val typedNum = innerRimTextInput.toDoubleOrNull()
        if (typedNum == null || Math.abs(typedNum - innerRimWidthMm) > 0.02) {
            innerRimTextInput = formatDecimal(innerRimWidthMm, 2)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // En-tête avec sélecteur d'unité de largeur (mm ou pouces) - Bascule manuelle stricte
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.tire_width),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = !isInchUnit,
                    onClick = {
                        if (isInchUnit) {
                            isInchUnit = false
                            tireTextInput = formatDecimal(tireWidthMm, 2)
                        }
                    },
                    label = { Text(stringResource(id = R.string.unit_mm), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = isInchUnit,
                    onClick = {
                        if (!isInchUnit) {
                            isInchUnit = true
                            tireTextInput = formatDecimal(tireWidthMm / 25.4, 2)
                        }
                    },
                    label = { Text(stringResource(id = R.string.unit_inch), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberInputField(
                value = tireTextInput,
                onValueChange = { newVal ->
                    tireTextInput = newVal
                    val num = newVal.toDoubleOrNull()
                    if (num != null && num > 0.0) {
                        val mm = if (isInchUnit) num * 25.4 else num
                        onTireWidthChangeMm(mm)
                    }
                },
                label = if (isInchUnit) "${stringResource(id = R.string.tire_width)} (${stringResource(id = R.string.unit_inch_abbr)})" else "${stringResource(id = R.string.tire_width)} (mm)",
                suffix = if (isInchUnit) "\"" else "mm",
                modifier = Modifier.weight(1f)
            )
            NumberInputField(
                value = innerRimTextInput,
                onValueChange = { newVal ->
                    innerRimTextInput = newVal
                    val num = newVal.toDoubleOrNull()
                    if (num != null && num > 0.0) {
                        onInnerRimWidthChangeMm(num)
                    }
                },
                label = stringResource(id = R.string.inner_rim_width),
                suffix = "mm",
                modifier = Modifier.weight(1f)
            )
        }

        // Type de jante
        Column {
            Text(
                text = stringResource(id = R.string.rim_type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            var rimMenuExpanded by remember { mutableStateOf(false) }
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rimMenuExpanded = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = rimType.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = rimMenuExpanded,
                    onDismissRequest = { rimMenuExpanded = false }
                ) {
                    RimType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = type.labelRes)) },
                            onClick = {
                                onRimTypeChange(type)
                                rimMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Carcasse du pneu
        Column {
            Text(
                text = stringResource(id = R.string.tire_casing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            var casingMenuExpanded by remember { mutableStateOf(false) }
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { casingMenuExpanded = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = casing.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = casingMenuExpanded,
                    onDismissRequest = { casingMenuExpanded = false }
                ) {
                    TireCasing.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = c.labelRes)) },
                            onClick = {
                                onCasingChange(c)
                                casingMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
