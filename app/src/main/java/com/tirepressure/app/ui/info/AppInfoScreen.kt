package com.tirepressure.app.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tirepressure.app.ui.components.AboutAndPrivacyCard
import com.tirepressure.app.ui.components.ScrollTopFade
import com.tirepressure.app.ui.components.ScrollBottomFade
import com.tirepressure.app.ui.components.TirePressurePulseLogo
import com.tirepressure.app.ui.components.appGlossCardColors
import io.github.angelomarzocchi.flight.RecordedPaint
import io.github.angelomarzocchi.flight.recordedFlightAnchor
import io.github.angelomarzocchi.flight.rememberFlightAnchor
import io.github.angelomarzocchi.lumen.GlossCard
import io.github.angelomarzocchi.lumen.GlossCardDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val appLogoAnchor = rememberFlightAnchor(
        key = "app_logo",
        paint = remember { RecordedPaint(single = false) }
    )
    val appTitleAnchor = rememberFlightAnchor(
        key = "app_title",
        paint = remember { RecordedPaint(single = false) }
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColors = appGlossCardColors(
        base = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        content = MaterialTheme.colorScheme.onSurface,
        isDark = isDark
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_info_title), fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo avec animation héro partagée et impulsion de gonflage tactile (Option C)
            TirePressurePulseLogo(
                size = 96.dp,
                shape = RoundedCornerShape(22.dp),
                imageModifier = Modifier.recordedFlightAnchor(appLogoAnchor),
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
                    modifier = Modifier.recordedFlightAnchor(appTitleAnchor)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = com.tirepressure.app.core.AppVersion.DISPLAY_VERSION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Carte lien GitHub en GlossCard réactive au toucher
            GlossCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = cardColors,
                tiltOnTouch = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/melvinmajor/TirePressure"))
                    context.startActivity(intent)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                fontWeight = FontWeight.Bold
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
            }

            // Carte Adaptations Algorithme Frank Berto en GlossCard
            GlossCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = cardColors,
                tiltOnTouch = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
            }

            // Section À propos & Confidentialité (identique à celle des paramètres, sans doublon de version)
            AboutAndPrivacyCard(modifier = Modifier.fillMaxWidth(), showVersion = false)

            Spacer(modifier = Modifier.height(24.dp))
        }

        ScrollTopFade(visible = scrollState.canScrollBackward)
        ScrollBottomFade(
            visible = scrollState.canScrollForward,
            showIndicator = true,
            indicatorText = stringResource(id = R.string.settings_scroll_more)
        )
    }
}
}

