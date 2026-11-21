package com.example.folderpruner.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.folderpruner.R
import com.example.folderpruner.ui.util.formatBytes
import com.example.folderpruner.ui.viewmodel.ScanViewModel

@Composable
fun ScanScreen(viewModel: ScanViewModel, onChooseFolder: () -> Unit) {
    val progressState = viewModel.progress.collectAsState()
    val progress = progressState.value
    val includeSubfoldersState = viewModel.includeSubfolders.collectAsState()
    val includeSubfolders = includeSubfoldersState.value
    val errorState = viewModel.error.collectAsState()
    val error = errorState.value

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onChooseFolder,
            enabled = progress.currentFolder == null,
        ) {
            Text(text = stringResource(R.string.choose_folder))
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = includeSubfolders,
                onCheckedChange = { viewModel.setIncludeSubfolders(it) },
            )
            Text(text = stringResource(R.string.include_subfolders))
        }
        if (progress.currentFolder != null) {
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.scanning),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = stringResource(R.string.files_scanned, progress.filesScanned))
                    if (progress.currentFolder != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(R.string.current_folder, progress.currentFolder))
                    }
                    Text(
                        text = stringResource(R.string.recoverable_bytes, formatBytes(progress.totalRecoverableBytes)),
                    )
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
    }
}
