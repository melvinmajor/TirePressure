package com.tirepressure.app.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.AppVersion
import com.tirepressure.app.ui.components.AboutAndPrivacyCard
import com.tirepressure.app.ui.components.LocalGlassBackdrop
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.ScrollBottomFade
import com.tirepressure.app.ui.components.TirePressurePulseLogo
import com.tirepressure.app.ui.components.WhatsNewDialog
import io.github.angelomarzocchi.flight.RecordedPaint
import io.github.angelomarzocchi.flight.recordedFlightAnchor
import io.github.angelomarzocchi.flight.rememberFlightAnchor
import io.github.angelomarzocchi.glass.glassBackdrop
import io.github.angelomarzocchi.glass.rememberGlassBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    isDualPane: Boolean = false,
    onPopupActiveChanged: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showWhatsNewDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showWhatsNewDialog) {
        onPopupActiveChanged(showWhatsNewDialog)
    }
    DisposableEffect(Unit) {
        onDispose { onPopupActiveChanged(false) }
    }

    val parentGlassBackdrop = LocalGlassBackdrop.current
    val localGlassBackdrop = rememberGlassBackdrop()
    val effectiveGlassBackdrop = parentGlassBackdrop ?: localGlassBackdrop
    val blurRadius by animateDpAsState(
        targetValue = if (showWhatsNewDialog && parentGlassBackdrop == null) 14.dp else 0.dp,
        animationSpec = tween(250),
        label = "AppInfoDialogBlur"
    )

    val logoAnchorModifier = if (!isDualPane) {
        val appLogoAnchor = rememberFlightAnchor(
            key = "app_logo",
            paint = remember { RecordedPaint(single = false, overflow = true) }
        )
        Modifier.recordedFlightAnchor(appLogoAnchor, spill = 4.dp)
    } else {
        Modifier
    }

    val titleAnchorModifier = if (!isDualPane) {
        val appTitleAnchor = rememberFlightAnchor(
            key = "app_title",
            paint = remember { RecordedPaint(single = false, overflow = true) }
        )
        Modifier.recordedFlightAnchor(appTitleAnchor, spill = 4.dp)
    } else {
        Modifier
    }

    // Unified single-piece screen background so the logo's sonic pressure waves radiate
    // freely behind the transparent header all the way to the top of the display without clipping.
    CompositionLocalProvider(LocalGlassBackdrop provides effectiveGlassBackdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (parentGlassBackdrop == null) {
                            Modifier.glassBackdrop(localGlassBackdrop)
                        } else {
                            Modifier
                        }
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .blur(blurRadius),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Spacer(modifier = Modifier.height(52.dp))

                    // Logo avec animation héro partagée et impulsion de gonflage tactile
                    TirePressurePulseLogo(
                        size = 96.dp,
                        shape = RoundedCornerShape(22.dp),
                        imageModifier = logoAnchorModifier,
                        contentDescription = stringResource(id = R.string.app_info_title)
                    )

                    // Nom et version avec animation héro partagée
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = titleAnchorModifier
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppVersion.DISPLAY_VERSION,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { showWhatsNewDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(role = Role.Button) { showWhatsNewDialog = true },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.whats_new_button_open, AppVersion.DISPLAY_VERSION),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

            // Lien GitHub direct sans contour de carte ni effet Lumen/Flight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/melvinmajor/TirePressure"))
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_info_github_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.app_info_github_url),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Section Adaptations Algorithme Frank Berto directe (sans contour de carte)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.app_info_algorithm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.app_info_algorithm_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section À propos & Confidentialité (identique à celle des paramètres, sans doublon de version)
            AboutAndPrivacyCard(modifier = Modifier.fillMaxWidth(), showVersion = false)

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Transparent header overlaid on the unified background
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.app_info_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.action_cancel)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ScrollTopFade(visible = scrollState.canScrollBackward)
        ScrollBottomFade(
            visible = scrollState.canScrollForward,
            showIndicator = true,
            indicatorText = stringResource(id = R.string.settings_scroll_more)
        )
            }

            if (showWhatsNewDialog) {
                WhatsNewDialog(
                    onDismiss = { showWhatsNewDialog = false }
                )
            }
        }
    }
}

