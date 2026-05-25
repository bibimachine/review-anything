package com.reviewanything.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewanything.app.data.db.AppDatabase
import com.reviewanything.app.data.model.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val db: AppDatabase) : ViewModel() {

    private val _config = MutableStateFlow<Config?>(null)
    val config: StateFlow<Config?> = _config

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

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
                modelName = modelName.ifBlank { "deepseek-v4-pro" },
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

    fun clearMessage() {
        _message.value = null
    }
}
