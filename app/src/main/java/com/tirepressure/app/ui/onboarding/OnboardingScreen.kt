package com.tirepressure.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.util.UnitConverter
import com.tirepressure.app.domain.repository.PreferencesRepository
import com.tirepressure.app.ui.components.NumberInputField
import com.tirepressure.app.ui.components.PillSelector
import com.tirepressure.app.ui.theme.ElectricLime
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    preferencesRepository: PreferencesRepository,
    onOnboardingFinished: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) } // Step 1: Weight, Step 2: Tutorial

    var weightInput by remember { mutableStateOf("75.0") }
    var selectedUnit by remember { mutableStateOf(MassUnit.KG) }
    var weightError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(250)) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(200))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(250)) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(200))
                }
            },
            label = "OnboardingStepTransition",
            modifier = Modifier.fillMaxSize()
        ) { step ->
            if (step == 1) {
            // Étape 1 : Saisie initiale du poids du cycliste
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )

                    Text(
                        text = stringResource(id = R.string.onboarding_welcome_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(id = R.string.onboarding_welcome_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        coroutineScope.launch {
                            preferencesRepository.setRiderWeight(kg)
                            preferencesRepository.setMassUnit(selectedUnit)
                            currentStep = 2 // Passer au swipe tutorial
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
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        } else {
            // Étape 2 : Mini tutoriel swipeable avec HorizontalPager
            val tutorialPages = listOf(
                TutorialPage(
                    titleRes = R.string.onboarding_step1_title,
                    descRes = R.string.onboarding_step1_desc,
                    icon = Icons.Default.DirectionsBike
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

            val pagerState = rememberPagerState(pageCount = { tutorialPages.size })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Barre supérieure avec saut possible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            preferencesRepository.setOnboardingCompleted(true)
                            onOnboardingFinished()
                        }
                    }) {
                        Text(stringResource(id = R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val page = tutorialPages[pageIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(id = page.titleRes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(id = page.descRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Indicateurs de pages (Dots)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(tutorialPages.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                            )
                        }
                    }

                    // Bouton final
                    Button(
                        onClick = {
                            if (pagerState.currentPage < tutorialPages.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                coroutineScope.launch {
                                    preferencesRepository.setOnboardingCompleted(true)
                                    onOnboardingFinished()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == tutorialPages.size - 1)
                                stringResource(id = R.string.onboarding_get_started)
                            else
                                stringResource(id = R.string.onboarding_continue),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
}

private data class TutorialPage(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

