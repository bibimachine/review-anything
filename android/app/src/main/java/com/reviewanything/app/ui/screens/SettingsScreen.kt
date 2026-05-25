package com.reviewanything.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reviewanything.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val config by viewModel.config.collectAsState()
    val message by viewModel.message.collectAsState()

    var apiBaseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("deepseek-v4-pro") }
    var dailyCount by remember { mutableStateOf("10") }

    LaunchedEffect(config) {
        config?.let {
            apiBaseUrl = it.apiBaseUrl ?: ""
            apiKey = it.apiKey ?: ""
            modelName = it.modelName
            dailyCount = it.dailyReviewCount.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("⚙️ 设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // LLM 配置
        Text("模型配置", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiBaseUrl,
            onValueChange = { apiBaseUrl = it },
            label = { Text("API Base URL") },
            placeholder = { Text("https://api.deepseek.com") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            label = { Text("模型名称") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        // 复习设置
        Text("复习设置", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dailyCount,
            onValueChange = { dailyCount = it.filter { c -> c.isDigit() } },
            label = { Text("每日复习数量") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveConfig(
                    apiBaseUrl = apiBaseUrl,
                    apiKey = apiKey,
                    modelName = modelName,
                    dailyCount = dailyCount.toIntOrNull() ?: 10
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存设置")
        }
    }

    // Snackbar
    message?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
}
