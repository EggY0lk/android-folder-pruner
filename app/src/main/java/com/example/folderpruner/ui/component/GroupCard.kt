package com.example.folderpruner.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.folderpruner.R
import com.example.folderpruner.domain.model.DuplicateGroup
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.ui.util.fileTypeIcon
import com.example.folderpruner.ui.util.formatBytes

@Composable
fun GroupCard(
    group: DuplicateGroup,
    onFileToggle: (String) -> Unit,
    isSelected: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val extension = group.normalizedName.extension
    val title = if (extension.isNotEmpty()) "${group.normalizedName.base}.$extension" else group.normalizedName.base
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = fileTypeLabel(extension),
                    style = MaterialTheme.typography.bodySmall,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.group_recoverable,
                            formatBytes(group.totalRecoverableBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = when (group.groupType) {
                        GroupType.EXACT_DUPLICATE -> stringResource(R.string.filter_exact)
                        GroupType.OLDER_VERSION -> stringResource(R.string.filter_older)
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(group.rankedFiles.size) { index ->
                    val ranked = group.rankedFiles[index]
                    FileRow(
                        rankedFile = ranked,
                        groupType = group.groupType,
                        checked = isSelected(ranked.entry.uri),
                        onCheckedChange = { onFileToggle(ranked.entry.uri) },
                    )
                }
            }
        }
    }
}

private fun fileTypeLabel(key: String): String = when (key) {
    "archive" -> "A"
    "installer" -> "I"
    "media" -> "M"
    else -> "D"
}
