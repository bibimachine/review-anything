package com.reviewanything.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reviewanything.app.viewmodel.UploadViewModel

@Composable
fun UploadScreen(viewModel: UploadViewModel) {
    val state by viewModel.state.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadZip(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val s = state) {
            is UploadViewModel.UploadState.Idle -> {
                Text("📁", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("上传笔记", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "选择 ZIP 文件上传，支持 Markdown 笔记",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("选择 ZIP 文件")
                }
            }
            is UploadViewModel.UploadState.Processing -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在处理...", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { if (progress.total > 0) progress.current.toFloat() / progress.total else 0f },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${progress.current} / ${progress.total}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    progress.currentFile,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
            is UploadViewModel.UploadState.Success -> {
                Text("✅", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("上传成功！", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "处理了 ${s.files} 个文件，${s.chunks} 个段落",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("继续上传")
                }
            }
            is UploadViewModel.UploadState.Error -> {
                Text("❌", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("上传失败", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Text(s.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("重试")
                }
            }
        }
    }
}
