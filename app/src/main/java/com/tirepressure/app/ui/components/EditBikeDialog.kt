package com.tirepressure.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.ui.overview.EditBikeDraft

private val BikeCategorySaver: Saver<BikeCategory, String> = Saver(
    save = { it.name },
    restore = {
        try {
            BikeCategory.valueOf(it)
        } catch (_: Exception) {
            BikeCategory.ROAD
        }
    }
)

@Composable
fun EditBikeDialog(
    draft: EditBikeDraft,
    originalBike: BikeProfile,
    massUnit: MassUnit = MassUnit.KG,
    onDraftChange: (EditBikeDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (BikeProfile) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.edit_bike_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.edit_bike_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Nom du vélo (Obligatoire)
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = {
                        onDraftChange(draft.copy(name = it, nameError = null))
                    },
                    label = { Text(stringResource(id = R.string.bike_name_label)) },
                    placeholder = { Text(stringResource(id = R.string.bike_name_placeholder)) },
                    isError = draft.nameError != null,
                    supportingText = { draft.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Marque & Modèle (50% / 50%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft.brand,
                        onValueChange = { onDraftChange(draft.copy(brand = it)) },
                        label = { Text(stringResource(id = R.string.bike_brand_label)) },
                        placeholder = { Text(stringResource(id = R.string.bike_brand_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.model,
                        onValueChange = { onDraftChange(draft.copy(model = it)) },
                        label = { Text(stringResource(id = R.string.bike_model_label)) },
                        placeholder = { Text(stringResource(id = R.string.bike_model_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Année & Taille du cadre (50% / 50% avec YearInputField)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YearInputField(
                        value = draft.yearInput,
                        onValueChange = { onDraftChange(draft.copy(yearInput = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.frameSize,
                        onValueChange = { onDraftChange(draft.copy(frameSize = it)) },
                        label = { Text(stringResource(id = R.string.bike_size_label)) },
                        placeholder = { Text(stringResource(id = R.string.bike_size_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Catégorie principale du vélo (PillSelector rapide)
                Text(
                    text = stringResource(id = R.string.bike_category_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillSelector(
                    items = BikeCategory.entries,
                    selectedItem = draft.category,
                    onItemSelected = { onDraftChange(draft.copy(category = it)) },
                    labelProvider = { context.getString(it.labelRes) }
                )

                // 5. Poids du vélo
                NumberInputField(
                    value = draft.weightInput,
                    onValueChange = { onDraftChange(draft.copy(weightInput = it)) },
                    label = stringResource(id = R.string.bike_weight),
                    suffix = massUnit.symbol
                )

                // 6. Vélo par défaut
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = draft.isDefault,
                        onCheckedChange = { onDraftChange(draft.copy(isDefault = it)) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.bike_is_default_checkbox),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (draft.isDefault) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val nameRequiredError = stringResource(id = R.string.bike_name_required_error)
            Button(
                onClick = {
                    if (draft.name.isBlank()) {
                        onDraftChange(draft.copy(nameError = nameRequiredError))
                        return@Button
                    }
                    val enteredWeight = draft.weightInput.toDoubleOrNull()
                    val weightKg = if (enteredWeight != null) UnitConverter.toKg(enteredWeight, massUnit) else originalBike.bikeWeightKg
                    val year = draft.yearInput.toIntOrNull()
                    val updated = originalBike.copy(
                        name = draft.name.trim(),
                        brand = draft.brand.trim(),
                        model = draft.model.trim(),
                        year = year,
                        frameSize = draft.frameSize.trim(),
                        category = draft.category,
                        bikeWeightKg = weightKg,
                        isDefault = draft.isDefault
                    )
                    onConfirm(updated)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(id = R.string.action_save))
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
fun EditBikeDialog(
    bike: BikeProfile,
    massUnit: MassUnit = MassUnit.KG,
    onDismiss: () -> Unit,
    onConfirm: (BikeProfile) -> Unit
) {
    val initialWeightInUserUnit = UnitConverter.fromKg(bike.bikeWeightKg, massUnit)
    val weightStr = String.format(java.util.Locale.US, "%.1f", initialWeightInUserUnit).replace(',', '.')
    var localDraft by remember(bike.id) {
        mutableStateOf(
            EditBikeDraft(
                bikeId = bike.id,
                name = bike.name,
                brand = bike.brand,
                model = bike.model,
                yearInput = bike.year?.toString() ?: "",
                frameSize = bike.frameSize,
                weightInput = weightStr,
                category = bike.category,
                isDefault = bike.isDefault
            )
        )
    }
    EditBikeDialog(
        draft = localDraft,
        originalBike = bike,
        massUnit = massUnit,
        onDraftChange = { localDraft = it },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
