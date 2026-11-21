package com.example.folderpruner.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.folderpruner.R

enum class Filter {
    All,
    ExactDuplicates,
    OlderVersions,
}

/** Filter tab row: All / Exact Duplicates / Older Versions. */
@Composable
fun FilterTab(
    selected: Filter,
    onFilterSelected: (Filter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            onClick = { onFilterSelected(Filter.All) },
            selected = selected == Filter.All,
            label = { Text(text = stringResource(R.string.filter_all)) },
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            onClick = { onFilterSelected(Filter.ExactDuplicates) },
            selected = selected == Filter.ExactDuplicates,
            label = { Text(text = stringResource(R.string.filter_exact)) },
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            onClick = { onFilterSelected(Filter.OlderVersions) },
            selected = selected == Filter.OlderVersions,
            label = { Text(text = stringResource(R.string.filter_older)) },
        )
    }
}
