package com.reviewanything.app.service

import com.reviewanything.app.data.model.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LlmService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun generateQA(content: String, headingPath: String, config: Config): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val systemPrompt = """你是一个帮助用户复习笔记的助手。请根据提供的笔记内容生成3个不同的复习问题及其答案。
要求：1. 每个问题考察不同的知识点或角度 2. 问题应该考察理解，而不是简单记忆 3. 答案简洁准确，覆盖核心要点
4. 请用JSON格式返回数组：[{"question": "问题1", "answer": "答案1"}]"""

        val context = "笔记标题路径: $headingPath\n\n笔记内容:\n$content"
        val baseUrl = config.apiBaseUrl?.trimEnd('/') ?: "https://api.deepseek.com"
        val url = "$baseUrl/chat/completions"

        val body = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", context))
            })
            put("temperature", 0.8)
            put("reasoning_effort", "high")
            put("thinking", JSONObject().put("type", "enabled"))
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("LLM API error: ${response.code}")
            val json = JSONObject(response.body!!.string())
            val content = json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            parseQA(content)
        }
    }

    private fun parseQA(content: String): List<Pair<String, String>> {
        return try {
            val array = JSONArray(content)
            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                obj.getString("question") to obj.getString("answer")
            }
        } catch (_: Exception) {
            val start = content.indexOf('[')
            val end = content.lastIndexOf(']')
            if (start != -1 && end != -1) {
                val array = JSONArray(content.substring(start, end + 1))
                (0 until array.length()).map {
                    val obj = array.getJSONObject(it)
                    obj.getString("question") to obj.getString("answer")
                }
            } else emptyList()
        }
    }
}
