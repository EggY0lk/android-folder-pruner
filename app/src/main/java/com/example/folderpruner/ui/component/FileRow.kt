package com.example.folderpruner.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.folderpruner.R
import com.example.folderpruner.domain.model.FileDecision
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.domain.model.RankedFile
import com.example.folderpruner.ui.util.fileTypeIcon
import com.example.folderpruner.ui.util.formatBytes
import com.example.folderpruner.ui.util.statusText
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun FileRow(
    rankedFile: RankedFile,
    groupType: GroupType,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = rankedFile.entry
    val status = statusText(rankedFile.decision, groupType)
    val dateLabel =
        if (entry.lastModified == Instant.EPOCH) stringResource(R.string.unknown_date)
        else entry.lastModified.atZone(ZoneOffset.UTC).toLocalDate().toString()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = fileTypeIcon(entry.extension).uppercase().first().toString(),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatBytes(entry.sizeBytes)}  •  $dateLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = if (rankedFile.decision == FileDecision.DELETE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

private fun fileTypeLabel(key: String): String = when (key) {
    "archive" -> "A"
    "installer" -> "I"
    "media" -> "M"
    else -> "D"
}
