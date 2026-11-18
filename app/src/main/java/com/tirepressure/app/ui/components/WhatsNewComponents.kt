package com.tirepressure.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tirepressure.app.R
import com.tirepressure.app.core.AppVersion
import io.github.angelomarzocchi.lumen.rememberBounceHaptics

private data class WhatsNewHighlight(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int
)

private val LATEST_HIGHLIGHTS = listOf(
    WhatsNewHighlight(
        icon = Icons.Default.VerticalSplit,
        titleRes = R.string.whats_new_item_adaptive_title,
        descRes = R.string.whats_new_item_adaptive_desc
    ),
    WhatsNewHighlight(
        icon = Icons.Default.WbSunny,
        titleRes = R.string.whats_new_item_surfaces_title,
        descRes = R.string.whats_new_item_surfaces_desc
    ),
    WhatsNewHighlight(
        icon = Icons.Default.Star,
        titleRes = R.string.whats_new_item_default_setup_title,
        descRes = R.string.whats_new_item_default_setup_desc
    ),
    WhatsNewHighlight(
        icon = Icons.Default.Bookmarks,
        titleRes = R.string.whats_new_item_favorites_title,
        descRes = R.string.whats_new_item_favorites_desc
    ),
    WhatsNewHighlight(
        icon = Icons.Default.TouchApp,
        titleRes = R.string.whats_new_item_lightmenu_title,
        descRes = R.string.whats_new_item_lightmenu_desc
    ),
    WhatsNewHighlight(
        icon = Icons.Default.Tune,
        titleRes = R.string.whats_new_item_setup_edit_title,
        descRes = R.string.whats_new_item_setup_edit_desc
    )
)

@Composable
fun WhatsNewBanner(
    versionText: String,
    onOpenClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onOpenClick),
        shape = RoundedCornerShape(14.dp),
        color = primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.whats_new_banner_title, versionText),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.whats_new_banner_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val haptics = rememberBounceHaptics()
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(id = R.string.whats_new_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = AppVersion.DISPLAY_VERSION,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LATEST_HIGHLIGHTS.forEach { item ->
                    WhatsNewHighlightRow(item = item)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.press()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.whats_new_got_it),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun WhatsNewHighlightRow(item: WhatsNewHighlight) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(id = item.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = item.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
