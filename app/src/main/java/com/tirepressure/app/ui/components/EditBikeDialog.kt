package com.tirepressure.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.domain.model.BikeCategory
import com.tirepressure.app.domain.model.BikeProfile

@Composable
fun EditBikeDialog(
    bike: BikeProfile,
    onDismiss: () -> Unit,
    onConfirm: (BikeProfile) -> Unit
) {
    var name by remember { mutableStateOf(bike.name) }
    var brand by remember { mutableStateOf(bike.brand) }
    var model by remember { mutableStateOf(bike.model) }
    var yearInput by remember { mutableStateOf(bike.year?.toString() ?: "") }
    var frameSize by remember { mutableStateOf(bike.frameSize) }
    var weightInput by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", bike.bikeWeightKg).replace(',', '.')) }
    var selectedCategory by remember { mutableStateOf(bike.category) }
    var isDefault by remember { mutableStateOf(bike.isDefault) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.edit_bike_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Nom du vélo (Obligatoire)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(id = R.string.bike_name_label)) },
                    placeholder = { Text("Ex : Mon VTT XC, Route Aéro") },
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Marque & Modèle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text(stringResource(id = R.string.bike_brand_label)) },
                        placeholder = { Text("Ex : Trek") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(stringResource(id = R.string.bike_model_label)) },
                        placeholder = { Text("Ex : Supercaliber") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Année & Taille du cadre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { yearInput = it.filter { char -> char.isDigit() }.take(4) },
                        label = { Text(stringResource(id = R.string.bike_year_label)) },
                        placeholder = { Text("Ex : 2024") },
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

                // 4. Poids du vélo
                NumberInputField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = stringResource(id = R.string.bike_weight),
                    suffix = "kg"
                )

                // 5. Catégorie principale du vélo
                Column {
                    Text(
                        text = stringResource(id = R.string.bike_category_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryMenuExpanded = true },
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
                                text = stringResource(id = selectedCategory.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        BikeCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(id = cat.labelRes),
                                        fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 6. Vélo par défaut
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
                    if (isDefault) {
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
                    if (name.isBlank()) {
                        nameError = nameRequiredError
                        return@Button
                    }
                    val weight = weightInput.toDoubleOrNull() ?: bike.bikeWeightKg
                    val year = yearInput.toIntOrNull()
                    val updated = bike.copy(
                        name = name.trim(),
                        brand = brand.trim(),
                        model = model.trim(),
                        year = year,
                        frameSize = frameSize.trim(),
                        category = selectedCategory,
                        bikeWeightKg = weight,
                        isDefault = isDefault
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
