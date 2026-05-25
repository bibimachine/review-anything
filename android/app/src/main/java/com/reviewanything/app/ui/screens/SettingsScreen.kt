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
    val testResult by viewModel.testResult.collectAsState()

    var apiBaseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var dailyCount by remember { mutableStateOf("10") }
    var selectedProvider by remember { mutableStateOf(viewModel.providers.first()) }
    var providerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        config?.let {
            apiBaseUrl = it.apiBaseUrl ?: ""
            apiKey = it.apiKey ?: ""
            modelName = it.modelName ?: ""
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

        // LLM 提供商选择
        Text("选择 LLM 提供商", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedProvider.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("提供商") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false }
            ) {
                viewModel.providers.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.name) },
                        onClick = {
                            selectedProvider = provider
                            providerExpanded = false
                            if (provider.key != "custom") {
                                apiBaseUrl = provider.apiBaseUrl
                                modelName = provider.modelName
                            }
                            viewModel.clearTestResult()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API 配置
        OutlinedTextField(
            value = apiBaseUrl,
            onValueChange = { apiBaseUrl = it; viewModel.clearTestResult() },
            label = { Text("API Base URL") },
            placeholder = { Text("https://api.deepseek.com") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; viewModel.clearTestResult() },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it; viewModel.clearTestResult() },
            label = { Text("模型名称") },
            placeholder = { Text("deepseek-chat") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 测试连接
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.testConnection(apiBaseUrl, apiKey, modelName) },
                modifier = Modifier.weight(1f)
            ) {
                Text("测试连接")
            }
        }

        testResult?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (it.startsWith("✅"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API 申请指南
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📖 ${selectedProvider.name} API 申请指南",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                selectedProvider.guide.forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

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
                viewModel.saveConfig(apiBaseUrl, apiKey, modelName, dailyCount.toIntOrNull() ?: 10)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存配置")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                viewModel.saveEmptyConfig(dailyCount.toIntOrNull() ?: 10)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("暂不配置 LLM，使用本地规则")
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
