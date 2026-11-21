package com.example.folderpruner.ui

import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.folderpruner.ui.screen.ReviewScreen
import com.example.folderpruner.ui.screen.ScanScreen
import com.example.folderpruner.ui.theme.FolderPrunerTheme
import com.example.folderpruner.ui.viewmodel.ScanViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FolderPrunerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showReview by remember { mutableStateOf(false) }
                    var treeUri by remember { mutableStateOf<String?>(null) }

                    val openDocumentTree = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result: ActivityResult ->
                    val uri = result.data?.data
                    uri?.let { selected ->
                        treeUri = selected.toString()
                        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        viewModel.pickFolder(selected.toString(), flags)
                        showReview = true
                    }
                }
                    if (showReview && treeUri != null) {
                        ReviewScreen(
                            viewModel = viewModel,
                            onBack = { showReview = false; treeUri = null },
                        )
                    } else {
                        ScanScreen(
                            viewModel = viewModel,
                            onChooseFolder = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addCategory(Intent.CATEGORY_OPENABLE)
                        openDocumentTree.launch(intent)
                    },
                        )
                    }
                }
            }
        }
    }
}
