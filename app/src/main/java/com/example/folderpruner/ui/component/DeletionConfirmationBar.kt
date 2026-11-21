package com.example.folderpruner.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.folderpruner.R
import com.example.folderpruner.ui.util.formatBytes

/**
 * Bottom confirmation bar: exact selected file count and MB/GB to free.
 * Tapping the bar opens the confirmation dialog — deletion never happens
 * automatically.
 */
@Composable
fun DeletionConfirmationBar(
    count: Int,
    bytes: Long,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .clickable(onClick = onConfirm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.confirmation_bar_label, count, formatBytes(bytes)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.confirmation_bar_tap),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}
