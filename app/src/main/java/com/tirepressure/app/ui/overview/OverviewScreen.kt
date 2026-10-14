package com.tirepressure.app.ui.overview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tirepressure.app.R
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.domain.model.*
import com.tirepressure.app.ui.components.AddSetupDialog
import com.tirepressure.app.ui.components.EditBikeDialog
import com.tirepressure.app.ui.components.PillSelector
import com.tirepressure.app.ui.theme.ElectricLime
import com.tirepressure.app.ui.theme.ElectricLimeContainerDark
import com.tirepressure.app.ui.theme.getCategoryColor
import com.tirepressure.app.ui.theme.getRideStyleColor
import com.tirepressure.app.ui.theme.pressureReading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onNavigateToAddBike: () -> Unit,
    onNavigateToEditBike: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppInfo: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var bikeForNewSetup by remember { mutableStateOf<BikeProfile?>(null) }
    var bikeToEditInfo by remember { mutableStateOf<BikeProfile?>(null) }
    var userSelectedSimplified by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val isSimplifiedView = userSelectedSimplified ?: state.defaultOverviewCompact

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToAppInfo() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = stringResource(id = R.string.app_info_title),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val tireColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = tireColor)) {
                                    append("Tire")
                                }
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("Pressure")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { userSelectedSimplified = !isSimplifiedView }) {
                        Icon(
                            imageVector = if (isSimplifiedView) Icons.Default.ViewAgenda else Icons.Default.TableRows,
                            contentDescription = if (isSimplifiedView) {
                                stringResource(id = R.string.overview_view_detailed)
                            } else {
                                stringResource(id = R.string.overview_view_simplified)
                            }
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.nav_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val columnsCount = if (maxWidth >= 600.dp) 2 else 1
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
            // Bandeau de réglages rapides : Poids cycliste & Bascule d'unité de pression
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(id = R.string.overview_rider_label)} ${state.massUnit.format(state.riderWeightKg)} ${state.massUnit.symbol}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Sélecteur d'unité de pression instantané
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.overview_pressures_display_in),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            PillSelector(
                                items = PressureUnit.entries,
                                selectedItem = state.pressureUnit,
                                onItemSelected = { viewModel.setPressureUnit(it) },
                                labelProvider = { it.symbol },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }

            // Si aucun vélo enregistré
            if (state.bikesWithPressures.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = stringResource(id = R.string.overview_no_bikes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.overview_no_bikes_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onNavigateToAddBike,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(id = R.string.overview_add_bike))
                            }
                        }
                    }
                }
            } else {
                items(state.bikesWithPressures, key = { it.bike.id }) { bikeOverview ->
                    val copyPressuresAction: (SetupMultiSurfaceResult) -> Unit = { setupResult ->
                        val dryLabel = context.getString(R.string.overview_dry)
                        val wetLabel = context.getString(R.string.overview_wet)
                        val snowLabel = context.getString(R.string.overview_snow)
                        val frontAbbr = context.getString(R.string.widget_front_abbr)
                        val rearAbbr = context.getString(R.string.widget_rear_abbr)

                        val textToCopy = buildString {
                            val effStyle = setupResult.setup.getEffectiveRideStyle(bikeOverview.bike.category)
                            val catName = context.getString(bikeOverview.bike.category.labelRes)
                            val styleName = context.getString(effStyle.labelRes)
                            appendLine("🚴‍♂️ ${bikeOverview.bike.name} ($catName) - ${setupResult.setup.name} [$styleName]")
                            appendLine("☀️ $dryLabel : $frontAbbr ${state.pressureUnit.format(setupResult.dryResult.front.pressureMbar)} / $rearAbbr ${state.pressureUnit.format(setupResult.dryResult.rear.pressureMbar)} ${state.pressureUnit.symbol}")
                            if (state.showWetSurface) {
                                appendLine("🌧️ $wetLabel : $frontAbbr ${state.pressureUnit.format(setupResult.wetResult.front.pressureMbar)} / $rearAbbr ${state.pressureUnit.format(setupResult.wetResult.rear.pressureMbar)} ${state.pressureUnit.symbol}")
                            }
                            if (state.showSnowSurface) {
                                appendLine("❄️ $snowLabel : $frontAbbr ${state.pressureUnit.format(setupResult.snowResult.front.pressureMbar)} / $rearAbbr ${state.pressureUnit.format(setupResult.snowResult.rear.pressureMbar)} ${state.pressureUnit.symbol}")
                            }
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Pressions", textToCopy.trimEnd()))
                        Toast.makeText(context, context.getString(R.string.overview_toast_copied), Toast.LENGTH_SHORT).show()
                    }

                    val copySuffix = stringResource(id = R.string.overview_duplicate_suffix)
                    AnimatedContent(
                        targetState = isSimplifiedView,
                        transitionSpec = {
                            if (targetState) {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(250)) togetherWith
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(200))
                            } else {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(250)) togetherWith
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(200))
                            }
                        },
                        label = "ViewModeCardTransition"
                    ) { simplified ->
                        if (simplified) {
                            BikeOverviewCardSimplified(
                                bikeOverview = bikeOverview,
                                pressureUnit = state.pressureUnit,
                                showWetSurface = state.showWetSurface,
                                showSnowSurface = state.showSnowSurface,
                                onEditBike = { onNavigateToEditBike(bikeOverview.bike.id) },
                                onEditBikeInfo = { bikeToEditInfo = bikeOverview.bike },
                                onDuplicateBike = { viewModel.duplicateBike(bikeOverview.bike, copySuffix) },
                                onDeleteBike = { viewModel.requestDeleteBike(bikeOverview.bike) },
                                onAddSetup = { bikeForNewSetup = bikeOverview.bike },
                                onDuplicateSetup = { setup -> viewModel.duplicateSetup(bikeOverview.bike, setup, copySuffix) },
                                onDeleteSetup = { setup -> viewModel.requestDeleteSetup(bikeOverview.bike, setup) },
                                onCopyPressures = copyPressuresAction
                            )
                        } else {
                            BikeOverviewCard(
                                bikeOverview = bikeOverview,
                                pressureUnit = state.pressureUnit,
                                showWetSurface = state.showWetSurface,
                                showSnowSurface = state.showSnowSurface,
                                onEditBike = { onNavigateToEditBike(bikeOverview.bike.id) },
                                onEditBikeInfo = { bikeToEditInfo = bikeOverview.bike },
                                onDuplicateBike = { viewModel.duplicateBike(bikeOverview.bike, copySuffix) },
                                onDeleteBike = { viewModel.requestDeleteBike(bikeOverview.bike) },
                                onAddSetup = { bikeForNewSetup = bikeOverview.bike },
                                onDuplicateSetup = { setup -> viewModel.duplicateSetup(bikeOverview.bike, setup, copySuffix) },
                                onDeleteSetup = { setup -> viewModel.requestDeleteSetup(bikeOverview.bike, setup) },
                                onCopyPressures = copyPressuresAction
                            )
                        }
                    }
                }

                // Bouton visible après la liste des vélos pour ajouter un vélo
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Button(
                        onClick = onNavigateToAddBike,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.overview_add_bike),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        }

        // Dialogue confirmation suppression vélo avec message d'avertissement
        state.bikeToDelete?.let { bike ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteBike() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text(stringResource(R.string.delete_bike_dialog_title)) },
                text = { Text(stringResource(R.string.delete_bike_dialog_msg, bike.name)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeleteBike() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.cancelDeleteBike() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Dialogue confirmation suppression profil / configuration (déclenché via le bouton '...' du profil)
        state.setupToDelete?.let { (bike, setup) ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteSetup() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text(stringResource(R.string.delete_setup_dialog_title)) },
                text = { Text(stringResource(R.string.delete_setup_dialog_msg, setup.name)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeleteSetup() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.cancelDeleteSetup() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Dialogue d'ajout de configuration avec sélection de template et discipline
        bikeForNewSetup?.let { bike ->
            AddSetupDialog(
                bikeName = bike.name,
                bikeWeightKg = bike.bikeWeightKg,
                bikeCategory = bike.category,
                existingSetups = bike.setups,
                onDismiss = { bikeForNewSetup = null },
                onConfirm = { setupName, templateSetup, overrideRideStyle ->
                    val defaultSetupPrefix = context.getString(R.string.setup_default_prefix)
                    viewModel.addSetupToBike(bike, setupName, templateSetup, overrideRideStyle, defaultSetupPrefix)
                    bikeForNewSetup = null
                    Toast.makeText(context, context.getString(R.string.overview_toast_setup_created, setupName), Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dialogue d'édition des informations du vélo (nom, marque, modèle, année, taille, poids)
        bikeToEditInfo?.let { bike ->
            EditBikeDialog(
                bike = bike,
                onDismiss = { bikeToEditInfo = null },
                onConfirm = { updatedBike ->
                    viewModel.updateBike(updatedBike)
                    bikeToEditInfo = null
                    Toast.makeText(context, context.getString(R.string.overview_toast_bike_updated), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun BikeOverviewCard(
    bikeOverview: BikeOverviewData,
    pressureUnit: PressureUnit,
    showWetSurface: Boolean,
    showSnowSurface: Boolean,
    onEditBike: () -> Unit,
    onEditBikeInfo: () -> Unit,
    onDuplicateBike: () -> Unit,
    onDeleteBike: () -> Unit,
    onAddSetup: () -> Unit,
    onDuplicateSetup: (WheelSetup) -> Unit,
    onDeleteSetup: (WheelSetup) -> Unit,
    onCopyPressures: (SetupMultiSurfaceResult) -> Unit
) {
    val bike = bikeOverview.bike
    var bikeMenuExpanded by remember { mutableStateOf(false) }
    var isExpanded by remember(bike.id, bike.isDefault) { mutableStateOf(bike.isDefault) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En-tête du vélo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Text(
                            text = bike.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.padding(end = if (bike.isDefault) 26.dp else 0.dp)) {
                            IconButton(onClick = { bikeMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = bikeMenuExpanded,
                                onDismissRequest = { bikeMenuExpanded = false }
                            ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.overview_edit_bike_info)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    bikeMenuExpanded = false
                                    onEditBikeInfo()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.overview_configure_wheels)) },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    bikeMenuExpanded = false
                                    onEditBike()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.overview_add_config)) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    bikeMenuExpanded = false
                                    onAddSetup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.overview_duplicate_bike)) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    bikeMenuExpanded = false
                                    onDuplicateBike()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_bike_button), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    bikeMenuExpanded = false
                                    onDeleteBike()
                                }
                            )
                        }
                    }

                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "OverviewBikeChevronRotation"
                    )
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) stringResource(R.string.overview_collapse_bike) else stringResource(R.string.overview_expand_bike),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }
            }

            // Marque, Modèle, Année et Taille si renseignés
            val sizePrefix = stringResource(R.string.bike_size_label)
            val bikeMeta = buildList {
                if (bike.brand.isNotBlank() || bike.model.isNotBlank()) {
                    val bm = listOf(bike.brand, bike.model).filter { it.isNotBlank() }.joinToString(" ")
                    val withYear = if (bike.year != null) "$bm (${bike.year})" else bm
                    add(withYear)
                } else if (bike.year != null) {
                    add("${bike.year}")
                }
                if (bike.frameSize.isNotBlank()) {
                    add("$sizePrefix ${bike.frameSize}")
                }
            }.joinToString(" • ")

            if (bikeMeta.isNotBlank()) {
                Text(
                    text = bikeMeta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Sous-titre vélo : Discipline & Poids
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val catColor = getCategoryColor(bike.category)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = stringResource(id = bike.category.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.overview_bike_weight, String.format(java.util.Locale.US, "%.1f", bike.bikeWeightKg)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isExpanded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        val countText = if (bikeOverview.setupsWithPressures.size > 1) {
                            stringResource(R.string.overview_config_count_many, bikeOverview.setupsWithPressures.size)
                        } else {
                            stringResource(R.string.overview_config_count_one)
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    // Affichage de chaque configuration de roue
                    bikeOverview.setupsWithPressures.forEach { setupResult ->
                        val setup = setupResult.setup
                        var setupMenuExpanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Titre de la configuration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DonutLarge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = setup.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val effStyle = setup.getEffectiveRideStyle(bike.category)
                                    val styleColor = getRideStyleColor(effStyle, bike.category)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = styleColor.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, styleColor.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = stringResource(id = effStyle.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = styleColor.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { setupMenuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.MoreHoriz, contentDescription = "Options", modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(
                                        expanded = setupMenuExpanded,
                                        onDismissRequest = { setupMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.overview_edit_setup)) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                setupMenuExpanded = false
                                                onEditBike()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.action_copy_pressures)) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                setupMenuExpanded = false
                                                onCopyPressures(setupResult)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.overview_duplicate_setup)) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                setupMenuExpanded = false
                                                onDuplicateSetup(setup)
                                            }
                                        )
                                        if (bikeOverview.setupsWithPressures.size > 1) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.delete_setup_button), color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    setupMenuExpanded = false
                                                    onDeleteSetup(setup)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Caractéristiques roues & pneus (selon exigence : pas de largeur de pneu, diamètre affiché et tubeless si tubeless)
                            val effStyle = setup.getEffectiveRideStyle(bike.category)
                            val frontDiam = setup.frontWheel.diameter.getLabelForStyle(effStyle)
                            val rearDiam = setup.rearWheel.diameter.getLabelForStyle(effStyle)
                            val diamLabel = if (setup.frontWheel.diameter == setup.rearWheel.diameter) {
                                frontDiam
                            } else {
                                "$frontDiam/$rearDiam"
                            }

                            val isTubeless = setup.frontWheel.rimType == RimType.TUBELESS_STRAIGHT_SIDE ||
                                    setup.frontWheel.rimType == RimType.TUBELESS_CROCHET ||
                                    setup.frontWheel.rimType == RimType.WIDE_AERO_HOOKLESS

                            val specLabel = if (isTubeless) {
                                "$diamLabel • ${stringResource(R.string.rim_tubeless_badge)}"
                            } else {
                                when (setup.frontWheel.rimType) {
                                    RimType.TUBULAR -> "$diamLabel • ${stringResource(R.string.rim_tubular)}"
                                    RimType.TUBES -> "$diamLabel • ${stringResource(R.string.rim_tubes_short)}"
                                    else -> diamLabel
                                }
                            }

                            Text(
                                text = specLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Cartes des conditions de surface calculées globales : Sec (toujours), Mouillé (si actif dans les réglages), Neige (si actif dans les réglages)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SurfacePressureBadge(
                                    surfaceName = "☀️ " + stringResource(R.string.overview_dry),
                                    frontPressure = pressureUnit.format(setupResult.dryResult.front.pressureMbar),
                                    rearPressure = pressureUnit.format(setupResult.dryResult.rear.pressureMbar),
                                    unitSymbol = pressureUnit.symbol,
                                    isWarning = setupResult.dryResult.hasHooklessWarning,
                                    modifier = Modifier.weight(1f)
                                )

                                if (showWetSurface) {
                                    SurfacePressureBadge(
                                        surfaceName = "🌧️ " + stringResource(R.string.overview_wet),
                                        frontPressure = pressureUnit.format(setupResult.wetResult.front.pressureMbar),
                                        rearPressure = pressureUnit.format(setupResult.wetResult.rear.pressureMbar),
                                        unitSymbol = pressureUnit.symbol,
                                        isWarning = setupResult.wetResult.hasHooklessWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (showSnowSurface) {
                                    SurfacePressureBadge(
                                        surfaceName = "❄️ " + stringResource(R.string.overview_snow),
                                        frontPressure = pressureUnit.format(setupResult.snowResult.front.pressureMbar),
                                        rearPressure = pressureUnit.format(setupResult.snowResult.rear.pressureMbar),
                                        unitSymbol = pressureUnit.symbol,
                                        isWarning = setupResult.snowResult.hasHooklessWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Bouton pour ajouter directement une nouvelle configuration / profil
                    OutlinedButton(
                        onClick = onAddSetup,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.overview_add_config))
                    }
                }
            }
        }

        if (bike.isDefault) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(id = R.string.overview_badge_default),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfacePressureBadge(
    surfaceName: String,
    frontPressure: String,
    rearPressure: String,
    unitSymbol: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isWarning) 1.5.dp else 1.dp,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = surfaceName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            val frontAbbr = stringResource(R.string.widget_front_abbr)
            val rearAbbr = stringResource(R.string.widget_rear_abbr)

            // AVANT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = frontAbbr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$frontPressure $unitSymbol",
                    style = MaterialTheme.typography.pressureReading
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ARRIÈRE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rearAbbr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$rearPressure $unitSymbol",
                    style = MaterialTheme.typography.pressureReading
                )
            }

            if (isWarning) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ Max Hookless",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BikeOverviewCardSimplified(
    bikeOverview: BikeOverviewData,
    pressureUnit: PressureUnit,
    showWetSurface: Boolean,
    showSnowSurface: Boolean,
    onEditBike: () -> Unit,
    onEditBikeInfo: () -> Unit,
    onDuplicateBike: () -> Unit,
    onDeleteBike: () -> Unit,
    onAddSetup: () -> Unit,
    onDuplicateSetup: (WheelSetup) -> Unit,
    onDeleteSetup: (WheelSetup) -> Unit,
    onCopyPressures: (SetupMultiSurfaceResult) -> Unit
) {
    val bike = bikeOverview.bike
    var bikeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // En-tête compact du vélo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = bike.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val catColor = getCategoryColor(bike.category)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = stringResource(id = bike.category.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${String.format(java.util.Locale.US, "%.1f", bike.bikeWeightKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(modifier = Modifier.padding(end = if (bike.isDefault) 24.dp else 0.dp)) {
                        IconButton(
                            onClick = { bikeMenuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = bikeMenuExpanded,
                        onDismissRequest = { bikeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.overview_edit_bike_info)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                bikeMenuExpanded = false
                                onEditBikeInfo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.overview_configure_wheels)) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            onClick = {
                                bikeMenuExpanded = false
                                onEditBike()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.overview_add_config)) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                bikeMenuExpanded = false
                                onAddSetup()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.overview_duplicate_bike)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                bikeMenuExpanded = false
                                onDuplicateBike()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_bike_button), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                bikeMenuExpanded = false
                                onDeleteBike()
                            }
                        )
                    }
                }
            }

            // Liste compacte des configurations et calculs de pression météo
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bikeOverview.setupsWithPressures.forEach { setupResult ->
                    val setup = setupResult.setup
                    var setupMenuExpanded by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyPressures(setupResult) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // En-tête profil : nom, discipline, options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DonutLarge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = setup.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val effStyle = setup.getEffectiveRideStyle(bike.category)
                                    val styleColor = getRideStyleColor(effStyle, bike.category)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = styleColor.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, styleColor.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = stringResource(id = effStyle.labelRes),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = styleColor.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { setupMenuExpanded = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "Options",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = setupMenuExpanded,
                                        onDismissRequest = { setupMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.overview_edit_setup)) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                setupMenuExpanded = false
                                                onEditBike()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.action_copy_pressures)) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                setupMenuExpanded = false
                                                onCopyPressures(setupResult)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.overview_duplicate_setup)) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                setupMenuExpanded = false
                                                onDuplicateSetup(setup)
                                            }
                                        )
                                        if (bikeOverview.setupsWithPressures.size > 1) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.delete_setup_button), color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    setupMenuExpanded = false
                                                    onDeleteSetup(setup)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Pressions calculées selon les conditions de surface choisies dans les paramètres
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SimplifiedPressureBadge(
                                    surfaceLabel = "☀️ " + stringResource(R.string.overview_dry),
                                    frontPressure = pressureUnit.format(setupResult.dryResult.front.pressureMbar),
                                    rearPressure = pressureUnit.format(setupResult.dryResult.rear.pressureMbar),
                                    unitSymbol = pressureUnit.symbol,
                                    isWarning = setupResult.dryResult.hasHooklessWarning,
                                    modifier = Modifier.weight(1f)
                                )

                                if (showWetSurface) {
                                    SimplifiedPressureBadge(
                                        surfaceLabel = "🌧️ " + stringResource(R.string.overview_wet),
                                        frontPressure = pressureUnit.format(setupResult.wetResult.front.pressureMbar),
                                        rearPressure = pressureUnit.format(setupResult.wetResult.rear.pressureMbar),
                                        unitSymbol = pressureUnit.symbol,
                                        isWarning = setupResult.wetResult.hasHooklessWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (showSnowSurface) {
                                    SimplifiedPressureBadge(
                                        surfaceLabel = "❄️ " + stringResource(R.string.overview_snow),
                                        frontPressure = pressureUnit.format(setupResult.snowResult.front.pressureMbar),
                                        rearPressure = pressureUnit.format(setupResult.snowResult.rear.pressureMbar),
                                        unitSymbol = pressureUnit.symbol,
                                        isWarning = setupResult.snowResult.hasHooklessWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bike.isDefault) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 14.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(id = R.string.overview_badge_default),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimplifiedPressureBadge(
    surfaceLabel: String,
    frontPressure: String,
    rearPressure: String,
    unitSymbol: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    SurfacePressureBadge(
        surfaceName = surfaceLabel,
        frontPressure = frontPressure,
        rearPressure = rearPressure,
        unitSymbol = unitSymbol,
        isWarning = isWarning,
        modifier = modifier
    )
}

