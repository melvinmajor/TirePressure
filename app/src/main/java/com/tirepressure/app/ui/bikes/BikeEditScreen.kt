package com.tirepressure.app.ui.bikes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.domain.model.*
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.components.AddSetupDialog
import com.tirepressure.app.ui.components.NumberInputField
import com.tirepressure.app.ui.components.PillSelector
import com.tirepressure.app.ui.components.SectionCard
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeEditScreen(
    bikeId: String,
    bikeRepository: BikeRepository,
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoaded by remember { mutableStateOf(false) }

    var currentId by remember { mutableStateOf(if (bikeId == "new") UUID.randomUUID().toString() else bikeId) }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("") }
    var frameSize by remember { mutableStateOf("") }
    var bikeCategory by remember { mutableStateOf(BikeCategory.ROAD) }
    var bikeWeightInput by remember { mutableStateOf("8.0") }
    var isDefault by remember { mutableStateOf(false) }

    // Sections réductibles
    var isGeneralInfoExpanded by remember(bikeId) { mutableStateOf(bikeId == "new") }
    var isSetupsExpanded by remember { mutableStateOf(true) }
    var isDiameterExpanded by remember { mutableStateOf(true) }
    var isFrontWheelExpanded by remember { mutableStateOf(true) }
    var isRearWheelExpanded by remember { mutableStateOf(true) }

    // Liste des configurations du vélo
    val defaultSetupName = stringResource(id = R.string.config_default_name)
    val defaultSetupPrefix = stringResource(id = R.string.setup_default_prefix)
    var setups by remember {
        mutableStateOf(
            listOf(
                WheelSetup(
                    name = defaultSetupName,
                    isDefault = true
                )
            )
        )
    }
    var selectedSetupIndex by remember { mutableIntStateOf(0) }
    var showAddSetupDialog by remember { mutableStateOf(false) }
    var showDeleteBikeConfirmation by remember { mutableStateOf(false) }
    var setupToDeleteConfirmation by remember { mutableStateOf<WheelSetup?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bikeId) {
        if (bikeId != "new") {
            val existing = bikeRepository.getBikeById(bikeId)
            if (existing != null) {
                currentId = existing.id
                name = existing.name
                brand = existing.brand
                model = existing.model
                yearInput = existing.year?.toString() ?: ""
                frameSize = existing.frameSize
                bikeCategory = existing.category
                bikeWeightInput = existing.bikeWeightKg.toString()
                isDefault = existing.isDefault
                setups = if (existing.setups.isNotEmpty()) existing.setups else listOf(WheelSetup(name = defaultSetupName))
                selectedSetupIndex = 0
            }
        }
        isLoaded = true
    }

    val nameRequiredErrorMsg = stringResource(id = R.string.bike_name_required_error)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (bikeId == "new") R.string.bike_edit_title_new else R.string.bike_edit_title_edit),
                        fontWeight = FontWeight.Bold
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
                            coroutineScope.launch {
                                val bike = BikeProfile(
                                    id = currentId,
                                    name = name.trim(),
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    year = yearInput.toIntOrNull(),
                                    frameSize = frameSize.trim(),
                                    category = bikeCategory,
                                    bikeWeightKg = bikeWeightInput.toDoubleOrNull() ?: 8.0,
                                    setups = setups,
                                    isDefault = isDefault,
                                    updatedAt = System.currentTimeMillis()
                                )
                                val existing = bikeRepository.getBikeById(bike.id)
                                if (existing == null) {
                                    bikeRepository.insertBike(bike)
                                } else {
                                    bikeRepository.updateBike(bike)
                                }
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (!isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val activeSetup = setups.getOrNull(selectedSetupIndex) ?: setups.first()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // 1. Informations Générales (Réductible par défaut et non éditable pour un vélo existant)
            item {
                SectionCard(
                    title = stringResource(id = R.string.bike_general_info_section),
                    icon = Icons.Default.DirectionsBike,
                    isCollapsible = true,
                    isExpanded = isGeneralInfoExpanded,
                    onToggleExpand = { isGeneralInfoExpanded = !isGeneralInfoExpanded },
                    collapsedSummary = {
                        Text(
                            text = "${name.ifBlank { "..." }} • ${stringResource(id = bikeCategory.labelRes)} • $bikeWeightInput kg",
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
                                modifier = Modifier.weight(1.2f)
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
                                value = "$bikeWeightInput kg",
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
                                bikeWeightInput = String.format(java.util.Locale.US, "%.1f", newCat.defaultWeightKg)
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
                            OutlinedTextField(
                                value = yearInput,
                                onValueChange = { yearInput = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text(stringResource(id = R.string.bike_year_label)) },
                                placeholder = { Text(stringResource(id = R.string.bike_year_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = frameSize,
                                onValueChange = { frameSize = it },
                                label = { Text(stringResource(id = R.string.bike_size_label)) },
                                placeholder = { Text(stringResource(id = R.string.bike_size_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        NumberInputField(
                            value = bikeWeightInput,
                            onValueChange = { bikeWeightInput = it },
                            label = stringResource(id = R.string.bike_weight),
                            suffix = "kg"
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
                    // Onglets de sélection de configuration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        setups.forEachIndexed { index, setup ->
                            val isSelected = index == selectedSetupIndex
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { selectedSetupIndex = index }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = setup.name.take(15),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAddSetupDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(id = R.string.overview_add_config))
                        }

                        if (setups.size > 1) {
                            TextButton(
                                onClick = { setupToDeleteConfirmation = activeSetup },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(id = R.string.delete_setup_button))
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
                                    Text(
                                        text = stringResource(id = currentSetupStyle.labelRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(id = st.labelRes),
                                            fontWeight = if (st == currentSetupStyle) FontWeight.Bold else FontWeight.Normal
                                        )
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

                    NumberInputField(
                        value = activeSetup.luggageWeightKg.toString(),
                        onValueChange = { wStr ->
                            val w = wStr.toDoubleOrNull() ?: 0.0
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(luggageWeightKg = w) else s
                            }
                        },
                        label = stringResource(id = R.string.luggage_weight),
                        suffix = "kg"
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

                    var isMulletMode by remember(selectedSetupIndex, activeSetup.frontWheel.diameter, activeSetup.rearWheel.diameter) {
                        mutableStateOf(activeSetup.frontWheel.diameter != activeSetup.rearWheel.diameter)
                    }

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

                    if (!isMulletMode) {
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
                            text = "${activeSetup.frontTire.widthMm.toInt()}mm • $rStr • $cStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    WheelTireFormBlock(
                        tireWidthMm = activeSetup.frontTire.widthMm,
                        onTireWidthChangeMm = { w ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(frontTire = s.frontTire.copy(widthMm = w)) else s
                            }
                        },
                        innerRimWidth = activeSetup.frontWheel.innerRimWidthMm.toString(),
                        onInnerRimWidthChange = { rStr ->
                            val r = rStr.toDoubleOrNull() ?: activeSetup.frontWheel.innerRimWidthMm
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(frontWheel = s.frontWheel.copy(innerRimWidthMm = r)) else s
                            }
                        },
                        rimType = activeSetup.frontWheel.rimType,
                        onRimTypeChange = { rt ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(frontWheel = s.frontWheel.copy(rimType = rt)) else s
                            }
                        },
                        casing = activeSetup.frontTire.casing,
                        onCasingChange = { c ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(frontTire = s.frontTire.copy(casing = c)) else s
                            }
                        }
                    )
                }
            }

            // Bouton copier avant vers arrière
            item {
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
            }

            // 5. Roue Arrière (Réductible)
            item {
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
                            text = "${activeSetup.rearTire.widthMm.toInt()}mm • $rStr • $cStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    WheelTireFormBlock(
                        tireWidthMm = activeSetup.rearTire.widthMm,
                        onTireWidthChangeMm = { w ->
                            setups = setups.mapIndexed { idx, s ->
                                if (idx == selectedSetupIndex) s.copy(rearTire = s.rearTire.copy(widthMm = w)) else s
                            }
                        },
                        innerRimWidth = activeSetup.rearWheel.innerRimWidthMm.toString(),
                        onInnerRimWidthChange = { rStr ->
                            val r = rStr.toDoubleOrNull() ?: activeSetup.rearWheel.innerRimWidthMm
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

            // Bouton Enregistrer
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = nameRequiredErrorMsg
                            return@Button
                        }
                        coroutineScope.launch {
                            val bike = BikeProfile(
                                id = currentId,
                                name = name.trim(),
                                brand = brand.trim(),
                                model = model.trim(),
                                year = yearInput.toIntOrNull(),
                                frameSize = frameSize.trim(),
                                category = bikeCategory,
                                bikeWeightKg = bikeWeightInput.toDoubleOrNull() ?: 8.0,
                                setups = setups,
                                isDefault = isDefault,
                                updatedAt = System.currentTimeMillis()
                            )
                            val existing = bikeRepository.getBikeById(bike.id)
                            if (existing == null) {
                                bikeRepository.insertBike(bike)
                            } else {
                                bikeRepository.updateBike(bike)
                            }
                            onNavigateBack()
                        }
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
    }

    if (showDeleteBikeConfirmation) {
        AlertDialog(
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
                        coroutineScope.launch {
                            bikeRepository.deleteBike(currentId)
                            showDeleteBikeConfirmation = false
                            onNavigateBack()
                        }
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
        AlertDialog(
            onDismissRequest = { setupToDeleteConfirmation = null },
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
                        val updated = setups.filter { it.id != setupToDel.id }
                        setups = updated
                        selectedSetupIndex = 0
                        setupToDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { setupToDeleteConfirmation = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAddSetupDialog) {
        AddSetupDialog(
            bikeName = name.ifBlank { stringResource(R.string.overview_title) },
            bikeWeightKg = bikeWeightInput.toDoubleOrNull() ?: 8.0,
            bikeCategory = bikeCategory,
            existingSetups = setups,
            onDismiss = { showAddSetupDialog = false },
            onConfirm = { newSetupName, templateSetup, overrideRideStyle ->
                val newSetup = if (templateSetup != null) {
                    templateSetup.copy(
                        id = UUID.randomUUID().toString(),
                        name = newSetupName.ifBlank { "$defaultSetupPrefix ${setups.size + 1}" },
                        rideStyle = overrideRideStyle ?: templateSetup.rideStyle ?: bikeCategory.defaultRideStyle,
                        isDefault = false
                    )
                } else {
                    val targetStyle = overrideRideStyle ?: bikeCategory.defaultRideStyle
                    val validDiameters = WheelDiameter.getOptionsForStyle(targetStyle)
                    WheelSetup(
                        id = UUID.randomUUID().toString(),
                        name = newSetupName.ifBlank { "$defaultSetupPrefix ${setups.size + 1}" },
                        rideStyle = targetStyle,
                        frontTire = TireConfig(
                            widthMm = targetStyle.defaultTireWidthMm,
                            casing = targetStyle.defaultTireCasing
                        ),
                        rearTire = TireConfig(
                            widthMm = targetStyle.defaultTireWidthMm,
                            casing = targetStyle.defaultTireCasing
                        ),
                        frontWheel = WheelConfig(
                            diameter = validDiameters.first(),
                            innerRimWidthMm = targetStyle.defaultInnerRimWidthMm,
                            rimType = targetStyle.defaultRimType
                        ),
                        rearWheel = WheelConfig(
                            diameter = validDiameters.first(),
                            innerRimWidthMm = targetStyle.defaultInnerRimWidthMm,
                            rimType = targetStyle.defaultRimType
                        ),
                        luggageWeightKg = 0.0,
                        isDefault = false
                    )
                }
                setups = setups + newSetup
                selectedSetupIndex = setups.size - 1
                showAddSetupDialog = false
            }
        )
    }
}

@Composable
private fun WheelTireFormBlock(
    tireWidthMm: Double,
    onTireWidthChangeMm: (Double) -> Unit,
    innerRimWidth: String,
    onInnerRimWidthChange: (String) -> Unit,
    rimType: RimType,
    onRimTypeChange: (RimType) -> Unit,
    casing: TireCasing,
    onCasingChange: (TireCasing) -> Unit
) {
    // Mode d'unité : mm ou pouces
    // Détecte intelligemment si la valeur stockée correspond à une valeur ronde en pouces (ex: 60.96mm = 2.4")
    var isInchUnit by remember(tireWidthMm) {
        val inInches = tireWidthMm / 25.4
        val nearestTenth = Math.round(inInches * 10.0) / 10.0
        mutableStateOf(tireWidthMm >= 50.0 && Math.abs(inInches - nearestTenth) < 0.05)
    }

    var textInput by remember(tireWidthMm, isInchUnit) {
        val str = if (isInchUnit) {
            val inInches = tireWidthMm / 25.4
            String.format(java.util.Locale.US, "%.1f", inInches)
        } else {
            val rounded = Math.round(tireWidthMm)
            if (Math.abs(tireWidthMm - rounded) < 0.05) rounded.toString()
            else String.format(java.util.Locale.US, "%.1f", tireWidthMm)
        }
        mutableStateOf(str)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // En-tête avec sélecteur d'unité de largeur (mm ou pouces)
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
                    onClick = { isInchUnit = false },
                    label = { Text(stringResource(id = R.string.unit_mm), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = isInchUnit,
                    onClick = { isInchUnit = true },
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
                value = textInput,
                onValueChange = { newVal ->
                    textInput = newVal
                    val num = newVal.toDoubleOrNull()
                    if (num != null && num > 0.0) {
                        val mm = if (isInchUnit) num * 25.4 else num
                        onTireWidthChangeMm(mm)
                    }
                },
                label = if (isInchUnit) "${stringResource(id = R.string.tire_width)} (po)" else "${stringResource(id = R.string.tire_width)} (mm)",
                suffix = if (isInchUnit) "\"" else "mm",
                modifier = Modifier.weight(1f)
            )
            NumberInputField(
                value = innerRimWidth,
                onValueChange = onInnerRimWidthChange,
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
