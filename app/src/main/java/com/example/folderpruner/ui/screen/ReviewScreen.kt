package com.example.folderpruner.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.folderpruner.R
import com.example.folderpruner.domain.model.DuplicateGroup
import com.example.folderpruner.domain.model.FileDecision
import com.example.folderpruner.domain.model.GroupType
import com.example.folderpruner.ui.component.DeletionConfirmationBar
import com.example.folderpruner.ui.component.FilterTab
import com.example.folderpruner.ui.component.Filter
import com.example.folderpruner.ui.component.GroupCard
import com.example.folderpruner.ui.dialog.ConfirmDialog
import com.example.folderpruner.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ScanViewModel, onBack: () -> Unit) {
    val groupsState = viewModel.groups.collectAsState()
    val groups = groupsState.value
    val selectedUrisState = viewModel.selectedUris.collectAsState()
    val selectedUris = selectedUrisState.value
    val errorState = viewModel.error.collectAsState()
    val error = errorState.value

    var filter by remember { mutableStateOf(Filter.All) }
    var confirmOpen by remember { mutableStateOf(false) }

    val visibleGroups = when (filter) {
        Filter.All -> groups
        Filter.ExactDuplicates -> groups.filter { it.groupType == GroupType.EXACT_DUPLICATE }
        Filter.OlderVersions -> groups.filter { it.groupType == GroupType.OLDER_VERSION }
    }

    val selectedDelete = groups.flatMap { it.rankedFiles }
        .filter { it.decision == FileDecision.DELETE && it.entry.uri in selectedUris }
    val count = selectedDelete.size
    val bytes = selectedDelete.sumOf { it.entry.sizeBytes }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.review)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "\u2190")
                    }
                },
            )
        },
        bottomBar = {
            DeletionConfirmationBar(count = count, bytes = bytes, onConfirm = { confirmOpen = true })
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            FilterTab(selected = filter, onFilterSelected = { filter = it })
            Spacer(modifier = Modifier.height(16.dp))
            if (groups.isEmpty()) {
                Text(text = stringResource(R.string.no_groups))
            } else if (visibleGroups.isEmpty()) {
                Text(text = stringResource(R.string.no_groups))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(visibleGroups.size) { index ->
                        val item = visibleGroups[index]
                        GroupCard(
                            group = item,
                            onFileToggle = { uri -> viewModel.toggleFile(uri) },
                            isSelected = { uri -> selectedUris.contains(uri) },
                        )
                    }
                }
            }
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = error)
            }
        }
    }

    ConfirmDialog(
        visible = confirmOpen,
        count = count,
        bytes = bytes,
        onDismiss = { confirmOpen = false },
        onConfirm = {
            confirmOpen = false
            viewModel.deleteSelected()
        },
    )
}
