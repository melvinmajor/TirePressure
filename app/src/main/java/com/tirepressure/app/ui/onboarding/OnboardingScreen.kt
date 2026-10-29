package com.tirepressure.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tirepressure.app.R
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.AppTheme
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.components.NumberInputField
import com.tirepressure.app.ui.components.PillSelector
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.ScrollBottomFade
import com.tirepressure.app.ui.components.TirePressurePulseLogo
import com.tirepressure.app.ui.components.appGlossCardColors
import io.github.angelomarzocchi.flight.RecordedPaint
import io.github.angelomarzocchi.flight.recordedFlightAnchor
import io.github.angelomarzocchi.flight.rememberFlightAnchor
import io.github.angelomarzocchi.lumen.GlossCard
import io.github.angelomarzocchi.lumen.rememberBounceHaptics
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    step: Int = 1,
    preferencesRepository: PreferencesRepository,
    onNavigateToStep2: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onOnboardingFinished: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberBounceHaptics()

    val currentTheme by preferencesRepository.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val currentAccentColor by preferencesRepository.accentColorFlow.collectAsStateWithLifecycle(initialValue = AppAccentColor.ELECTRIC_LIME)

    val appLogoAnchor = rememberFlightAnchor(
        key = "app_logo",
        paint = remember { RecordedPaint(single = false) }
    )
    val appTitleAnchor = rememberFlightAnchor(
        key = "app_title",
        paint = remember { RecordedPaint(single = false) }
    )
    val welcomePrefixAnchor = rememberFlightAnchor(
        key = "welcome_prefix",
        paint = remember { RecordedPaint(single = false) }
    )

    BackHandler(enabled = step == 2) {
        onNavigateBack()
    }

    val savedWeight by preferencesRepository.riderWeightFlow.collectAsStateWithLifecycle(initialValue = 75.0)
    val savedUnit by preferencesRepository.massUnitFlow.collectAsStateWithLifecycle(initialValue = MassUnit.KG)
    var weightInput by rememberSaveable(savedWeight, savedUnit) {
        mutableStateOf(
            if (savedUnit == MassUnit.KG) String.format(java.util.Locale.US, "%.1f", savedWeight)
            else String.format(java.util.Locale.US, "%.1f", UnitConverter.fromKg(savedWeight, MassUnit.LB))
        )
    }
    var selectedUnit by rememberSaveable(savedUnit) { mutableStateOf(savedUnit) }
    var weightError by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val fullTitle = stringResource(id = R.string.onboarding_welcome_title)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val normalTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary

    val welcomePrefix = remember(fullTitle) {
        val tpIndex = fullTitle.indexOf("TirePressure")
        if (tpIndex != -1) fullTitle.substring(0, tpIndex).trim() else ""
    }

    val tirePressureAnnotated = remember(normalTextColor, primaryColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = normalTextColor)) {
                append("Tire")
            }
            withStyle(SpanStyle(color = primaryColor)) {
                append("Pressure")
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (step == 1) {
                // Étape 1 : Saisie initiale du poids du cycliste
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize()
                            .consumeWindowInsets(WindowInsets.navigationBars)
                            .imePadding()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Logo avec animation héro partagée et impulsion de gonflage tactile (Option C)
                        TirePressurePulseLogo(
                            size = 100.dp,
                            shape = RoundedCornerShape(22.dp),
                            imageModifier = Modifier.recordedFlightAnchor(appLogoAnchor)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (welcomePrefix.isNotEmpty()) {
                                Text(
                                    text = welcomePrefix,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = normalTextColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.recordedFlightAnchor(welcomePrefixAnchor)
                                )
                            }
                            Text(
                                text = tirePressureAnnotated,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.recordedFlightAnchor(appTitleAnchor)
                            )
                        }

                        Text(
                            text = stringResource(id = R.string.onboarding_welcome_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val weightCardColors = appGlossCardColors(
                            base = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                            content = MaterialTheme.colorScheme.onSurface,
                            isDark = isDark
                        )
                        GlossCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = weightCardColors,
                            tiltOnTouch = true
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.onboarding_weight_prompt),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                PillSelector(
                                    items = MassUnit.entries,
                                    selectedItem = selectedUnit,
                                    onItemSelected = { newUnit ->
                                        haptics.press()
                                        val currentKg = UnitConverter.toKg(weightInput.toDoubleOrNull() ?: 75.0, selectedUnit)
                                        selectedUnit = newUnit
                                        weightInput = newUnit.format(currentKg).replace(',', '.')
                                    },
                                    labelProvider = { context.getString(it.labelRes) }
                                )

                                NumberInputField(
                                    value = weightInput,
                                    onValueChange = {
                                        weightInput = it
                                        weightError = null
                                    },
                                    label = stringResource(id = R.string.settings_rider_weight),
                                    suffix = selectedUnit.symbol,
                                    isError = weightError != null,
                                    errorMessage = weightError
                                )

                                Text(
                                    text = stringResource(id = R.string.onboarding_weight_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val num = weightInput.toDoubleOrNull()
                            val kg = if (num != null) UnitConverter.toKg(num, selectedUnit) else null
                            if (kg == null || kg < 30.0 || kg > 200.0) {
                                weightError = context.getString(R.string.onboarding_weight_error)
                                return@Button
                            }
                            haptics.press()
                            coroutineScope.launch {
                                preferencesRepository.setRiderWeight(kg)
                                preferencesRepository.setMassUnit(selectedUnit)
                                onNavigateToStep2()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_continue),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }

                ScrollTopFade(visible = scrollState.canScrollBackward)
                ScrollBottomFade(
                    visible = scrollState.canScrollForward,
                    showIndicator = true,
                    indicatorText = stringResource(id = R.string.settings_scroll_more)
                )
            }
        } else {
            // Étape 2 : Présentation simultanée des fonctionnalités et personnalisation
            val tutorialPages = listOf(
                TutorialPage(
                    titleRes = R.string.onboarding_step1_title,
                    descRes = R.string.onboarding_step1_desc,
                    icon = Icons.AutoMirrored.Filled.DirectionsBike
                ),
                TutorialPage(
                    titleRes = R.string.onboarding_step2_title,
                    descRes = R.string.onboarding_step2_desc,
                    icon = Icons.Default.WbSunny
                ),
                TutorialPage(
                    titleRes = R.string.onboarding_step3_title,
                    descRes = R.string.onboarding_step3_desc,
                    icon = Icons.Default.Shield
                )
            )

            val tutorialCardColors = appGlossCardColors(
                base = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                content = MaterialTheme.colorScheme.onSurface,
                isDark = isDark
            )
            val step2ScrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .verticalScroll(step2ScrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Barre supérieure avec retour à l'étape 1 et bouton ignorer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            haptics.press()
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.action_cancel)
                            )
                        }

                        TextButton(onClick = {
                            haptics.press()
                            coroutineScope.launch {
                                preferencesRepository.setOnboardingCompleted(true)
                                onOnboardingFinished()
                            }
                        }) {
                            Text(
                                text = stringResource(id = R.string.onboarding_skip),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Logo avec animation héro partagée et impulsion de gonflage tactile (Option C)
                    TirePressurePulseLogo(
                        size = 80.dp,
                        shape = RoundedCornerShape(18.dp),
                        imageModifier = Modifier.recordedFlightAnchor(appLogoAnchor)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (welcomePrefix.isNotEmpty()) {
                            Text(
                                text = welcomePrefix,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = normalTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.recordedFlightAnchor(welcomePrefixAnchor)
                            )
                        }
                        Text(
                            text = tirePressureAnnotated,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.recordedFlightAnchor(appTitleAnchor)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Les 3 cartes de fonctionnalités présentées simultanément
                    tutorialPages.forEach { page ->
                        GlossCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = tutorialCardColors,
                            tiltOnTouch = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = page.titleRes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = page.descRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 4ème carte : Choix du thème et de la couleur d'accentuation
                    GlossCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = tutorialCardColors,
                        tiltOnTouch = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.onboarding_step4_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.onboarding_step4_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = stringResource(id = R.string.settings_appearance),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            PillSelector(
                                items = AppTheme.entries,
                                selectedItem = currentTheme,
                                iconProvider = {
                                    when (it) {
                                        AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                                        AppTheme.LIGHT -> Icons.Default.LightMode
                                        AppTheme.DARK -> Icons.Default.DarkMode
                                    }
                                },
                                onItemSelected = { theme ->
                                    haptics.press()
                                    coroutineScope.launch {
                                        preferencesRepository.setTheme(theme)
                                    }
                                },
                                labelProvider = { context.getString(it.labelRes) }
                            )

                            Text(
                                text = stringResource(id = R.string.settings_accent_color_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppAccentColor.entries.forEach { accent ->
                                    val isSelected = accent == currentAccentColor
                                    val circleColor = accent.getPrimary(isDark)
                                    val onCircleColor = accent.getOnPrimary(isDark)
                                    val colorLabel = stringResource(id = accent.labelRes)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .semantics {
                                                contentDescription = colorLabel
                                            }
                                            .selectable(
                                                selected = isSelected,
                                                onClick = {
                                                    haptics.press()
                                                    coroutineScope.launch {
                                                        preferencesRepository.setAccentColor(accent)
                                                    }
                                                },
                                                role = Role.RadioButton
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
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
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bouton final
                    Button(
                        onClick = {
                            haptics.bounce(1f, true)
                            coroutineScope.launch {
                                preferencesRepository.setOnboardingCompleted(true)
                                onOnboardingFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_get_started),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                ScrollTopFade(visible = step2ScrollState.canScrollBackward)
                ScrollBottomFade(
                    visible = step2ScrollState.canScrollForward,
                    showIndicator = true,
                    indicatorText = stringResource(id = R.string.settings_scroll_more)
                )
            }
        }
    }
}

private data class TutorialPage(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)
