package com.tirepressure.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.RideStyle
import com.tirepressure.app.domain.model.WheelSetup
import com.tirepressure.app.ui.overview.AddSetupDraft
import java.util.Locale

private val RideStyleSaver: Saver<RideStyle, String> = Saver(
    save = { it.name },
    restore = {
        try {
            RideStyle.valueOf(it)
        } catch (_: Exception) {
            RideStyle.ROAD
        }
    }
)

@Composable
fun AddSetupDialog(
    draft: AddSetupDraft,
    existingSetups: List<WheelSetup>,
    massUnit: MassUnit = MassUnit.KG,
    onDraftChange: (AddSetupDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (setupName: String, templateSetup: WheelSetup?, overrideRideStyle: RideStyle?) -> Unit
) {
    val bikeName = draft.bikeName
    val bikeWeightKg = draft.bikeWeightKg
    val bikeCategory = draft.bikeCategory
    val setupName = draft.setupName
    val useExistingTemplate = draft.useExistingTemplate
    val selectedTemplateIndex = draft.selectedTemplateIndex
    val setupRideStyle = draft.setupRideStyle ?: bikeCategory.defaultRideStyle

    var styleMenuExpanded by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(id = R.string.add_setup_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bikeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nom de la configuration
                OutlinedTextField(
                    value = setupName,
                    onValueChange = { onDraftChange(draft.copy(setupName = it)) },
                    label = { Text(stringResource(id = R.string.add_setup_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Sélecteur de discipline / usage pour ce profil
                Column {
                    Text(
                        text = stringResource(id = R.string.add_setup_discipline_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { styleMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
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
                                    text = stringResource(id = setupRideStyle.labelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (setupRideStyle == bikeCategory.defaultRideStyle) {
                                        stringResource(
                                            id = R.string.bike_setup_style_default,
                                            stringResource(id = bikeCategory.labelRes),
                                            stringResource(id = bikeCategory.defaultRideStyle.labelRes)
                                        )
                                    } else {
                                        stringResource(
                                            id = R.string.bike_setup_style_custom,
                                            stringResource(id = setupRideStyle.labelRes)
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = styleMenuExpanded,
                        onDismissRequest = { styleMenuExpanded = false }
                    ) {
                        RideStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = stringResource(id = style.labelRes),
                                            fontWeight = if (style == setupRideStyle) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (style == bikeCategory.defaultRideStyle) {
                                            Text(
                                                text = "(${stringResource(R.string.overview_badge_default)} ${stringResource(bikeCategory.labelRes)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onDraftChange(draft.copy(setupRideStyle = style))
                                    styleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(id = R.string.add_setup_template_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Option 1 : Réglages par défaut pour ce vélo
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDraftChange(
                                draft.copy(
                                    useExistingTemplate = false,
                                    setupRideStyle = bikeCategory.defaultRideStyle
                                )
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!useExistingTemplate) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        width = if (!useExistingTemplate) 1.5.dp else 1.dp,
                        color = if (!useExistingTemplate) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = !useExistingTemplate,
                            onClick = {
                                onDraftChange(
                                    draft.copy(
                                        useExistingTemplate = false,
                                        setupRideStyle = bikeCategory.defaultRideStyle
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.add_setup_template_default, stringResource(id = bikeCategory.labelRes)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val convertedWeight = UnitConverter.fromKg(bikeWeightKg, massUnit)
                            val weightFormatted = String.format(Locale.US, "%.1f", convertedWeight)
                            Text(
                                text = stringResource(id = R.string.add_setup_template_default_desc, weightFormatted, massUnit.symbol),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2 : Partir d'une configuration existante comme template
                if (existingSetups.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val chosen = existingSetups.getOrNull(selectedTemplateIndex)
                                val style = chosen?.getEffectiveRideStyle(bikeCategory) ?: setupRideStyle
                                onDraftChange(
                                    draft.copy(
                                        useExistingTemplate = true,
                                        setupRideStyle = style
                                    )
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (useExistingTemplate) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            width = if (useExistingTemplate) 1.5.dp else 1.dp,
                            color = if (useExistingTemplate) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = useExistingTemplate,
                                    onClick = {
                                        val chosen = existingSetups.getOrNull(selectedTemplateIndex)
                                        val style = chosen?.getEffectiveRideStyle(bikeCategory) ?: setupRideStyle
                                        onDraftChange(
                                            draft.copy(
                                                useExistingTemplate = true,
                                                setupRideStyle = style
                                            )
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.add_setup_template_existing),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.add_setup_template_existing_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Choix parmi les configurations existantes
                            if (useExistingTemplate) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(id = R.string.add_setup_select_existing),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    existingSetups.forEachIndexed { index, setup ->
                                        val isSelected = selectedTemplateIndex == index
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onDraftChange(
                                                        draft.copy(
                                                            selectedTemplateIndex = index,
                                                            setupRideStyle = setup.getEffectiveRideStyle(bikeCategory)
                                                        )
                                                    )
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        onDraftChange(
                                                            draft.copy(
                                                                selectedTemplateIndex = index,
                                                                setupRideStyle = setup.getEffectiveRideStyle(bikeCategory)
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = setup.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    val effStyle = setup.getEffectiveRideStyle(bikeCategory)
                                                    val frontLabel = stringResource(id = R.string.overview_front)
                                                    val rearLabel = stringResource(id = R.string.overview_rear)
                                                    val rimLabel = stringResource(id = setup.frontWheel.rimType.labelRes).split(" ").first()
                                                    Text(
                                                        text = "${stringResource(id = effStyle.labelRes)} • $frontLabel ${setup.frontTire.widthMm}mm / $rearLabel ${setup.rearTire.widthMm}mm • $rimLabel",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosenTemplate = if (useExistingTemplate && existingSetups.isNotEmpty()) {
                        existingSetups.getOrNull(selectedTemplateIndex) ?: existingSetups.first()
                    } else null
                    val overrideStyle = setupRideStyle
                    onConfirm(setupName, chosenTemplate, overrideStyle)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(id = R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
fun AddSetupDialog(
    bikeName: String,
    bikeWeightKg: Double,
    bikeCategory: BikeCategory,
    existingSetups: List<WheelSetup>,
    massUnit: MassUnit = MassUnit.KG,
    onDismiss: () -> Unit,
    onConfirm: (setupName: String, templateSetup: WheelSetup?, overrideRideStyle: RideStyle?) -> Unit
) {
    val defaultPrefix = stringResource(id = R.string.setup_default_prefix)
    var localDraft by remember(bikeName, existingSetups.size) {
        mutableStateOf(
            AddSetupDraft(
                bikeId = "",
                bikeName = bikeName,
                bikeWeightKg = bikeWeightKg,
                bikeCategory = bikeCategory,
                setupName = "$defaultPrefix ${existingSetups.size + 1}",
                useExistingTemplate = existingSetups.isNotEmpty(),
                selectedTemplateIndex = 0,
                setupRideStyle = if (existingSetups.isNotEmpty()) existingSetups[0].getEffectiveRideStyle(bikeCategory)
                else bikeCategory.defaultRideStyle
            )
        )
    }
    AddSetupDialog(
        draft = localDraft,
        existingSetups = existingSetups,
        massUnit = massUnit,
        onDraftChange = { localDraft = it },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
