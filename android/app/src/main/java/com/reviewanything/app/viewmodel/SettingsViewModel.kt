package com.reviewanything.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewanything.app.data.db.AppDatabase
import com.reviewanything.app.data.model.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LlmProvider(
    val key: String,
    val name: String,
    val apiBaseUrl: String,
    val modelName: String,
    val guide: List<String>
)

class SettingsViewModel(private val db: AppDatabase) : ViewModel() {

    val providers = listOf(
        LlmProvider(
            "deepseek", "DeepSeek",
            "https://api.deepseek.com/chat/completions",
            "deepseek-chat",
            listOf(
                "1. 访问 https://platform.deepseek.com/",
                "2. 注册/登录后进入「API Keys」",
                "3. 点击「创建 API Key」",
                "4. 复制密钥填入上方 API Key 栏"
            )
        ),
        LlmProvider(
            "openai", "OpenAI",
            "https://api.openai.com/v1/chat/completions",
            "gpt-3.5-turbo",
            listOf(
                "1. 访问 https://platform.openai.com/api-keys",
                "2. 登录后点击 Create new secret key",
                "3. 复制 sk- 开头的密钥填入上方"
            )
        ),
        LlmProvider(
            "kimi", "Kimi",
            "https://api.moonshot.cn/v1/chat/completions",
            "moonshot-v1-8k",
            listOf(
                "1. 访问 https://platform.moonshot.cn/",
                "2. 进入「API Key 管理」",
                "3. 点击「新建」生成 API Key"
            )
        ),
        LlmProvider(
            "custom", "自定义",
            "", "",
            listOf("1. 输入你的 API Base URL", "2. 输入 API Key", "3. 输入模型名称")
        )
    )

    private val _config = MutableStateFlow<Config?>(null)
    val config: StateFlow<Config?> = _config

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _config.value = db.configDao().getConfigSync()
        }
    }

    fun saveConfig(apiBaseUrl: String, apiKey: String, modelName: String, dailyCount: Int) {
        viewModelScope.launch {
            val existing = db.configDao().getConfigSync()
            val newConfig = Config(
                id = existing?.id ?: 0,
                apiBaseUrl = apiBaseUrl.ifBlank { null },
                apiKey = apiKey.ifBlank { null },
                modelName = modelName.ifBlank { "deepseek-chat" },
                dailyReviewCount = dailyCount.coerceIn(1, 100)
            )
            if (existing == null) {
                db.configDao().insert(newConfig)
            } else {
                db.configDao().update(newConfig)
            }
            _config.value = newConfig
            _message.value = "配置已保存"
        }
    }

    fun saveEmptyConfig(dailyCount: Int) {
        viewModelScope.launch {
            val existing = db.configDao().getConfigSync()
            val newConfig = Config(
                id = existing?.id ?: 0,
                apiBaseUrl = null,
                apiKey = null,
                modelName = "deepseek-chat",
                dailyReviewCount = dailyCount.coerceIn(1, 100)
            )
            if (existing == null) {
                db.configDao().insert(newConfig)
            } else {
                db.configDao().update(newConfig)
            }
            _config.value = newConfig
            _message.value = "已跳过 LLM 配置，使用本地规则"
        }
    }

    fun testConnection(apiBaseUrl: String, apiKey: String, modelName: String) {
        viewModelScope.launch {
            _testResult.value = "测试中..."
            try {
                // 简单验证：URL 和 Key 不为空
                if (apiBaseUrl.isBlank() || apiKey.isBlank()) {
                    _testResult.value = "❌ 请填写 API Base URL 和 API Key"
                    return@launch
                }
                _testResult.value = "✅ 配置格式正确（运行时验证）"
            } catch (e: Exception) {
                _testResult.value = "❌ ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearTestResult() {
        _testResult.value = null
    }
}
