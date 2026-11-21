package com.example.folderpruner.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.folderpruner.R
import com.example.folderpruner.ui.util.formatBytes

/**
 * Confirmation dialog shown before any deletion. Nothing is deleted unless the user
 * taps "Delete files"; [onDismiss] is called on cancel/dismiss and [onConfirm] on
 * explicit confirmation.
 */
@Composable
fun ConfirmDialog(
    visible: Boolean,
    count: Int,
    bytes: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.confirm_delete_title)) },
            text = {
                Text(
                    text = stringResource(R.string.confirm_delete_message, count, formatBytes(bytes)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDismiss(); onConfirm() }) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }