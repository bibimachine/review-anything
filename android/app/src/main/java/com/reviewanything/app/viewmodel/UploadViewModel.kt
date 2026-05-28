package com.reviewanything.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewanything.app.data.db.AppDatabase
import com.reviewanything.app.data.model.Chunk
import com.reviewanything.app.data.model.Note
import com.reviewanything.app.data.model.ReviewItem
import com.reviewanything.app.service.LlmService
import com.reviewanything.app.service.MarkdownParser
import com.reviewanything.app.service.ZipExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UploadViewModel(private val db: AppDatabase) : ViewModel() {

    private val llmService = LlmService()

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state

    private val _progress = MutableStateFlow(UploadProgress())
    val progress: StateFlow<UploadProgress> = _progress

    /** 上传 ZIP 文件 */
    fun uploadZip(context: Context, uri: Uri, section: String? = null) {
        viewModelScope.launch {
            _state.value = UploadState.Processing
            _progress.value = UploadProgress(currentFile = "解压中...")

            try {
                val files = ZipExtractor.extract(context, uri)
                val totalChunks = files.sumOf { MarkdownParser.parse(it.second).size }
                var processed = 0

                for ((fileName, content) in files) {
                    val fileSection = section?.takeIf { it.isNotBlank() }
                        ?: fileName.substringBeforeLast("/", "未分类")

                    saveNoteWithChunks(fileName, content, fileSection, totalChunks) { current, currentFile ->
                        processed = current
                        _progress.value = UploadProgress(
                            current = processed,
                            total = totalChunks,
                            currentFile = currentFile
                        )
                    }
                }

                _state.value = UploadState.Success(files.size, processed)
            } catch (e: Exception) {
                _state.value = UploadState.Error(e.message ?: "未知错误")
            }
        }
    }

    /** 上传单个 Markdown 文件到指定板块 */
    fun uploadSingleFile(context: Context, uri: Uri, section: String) {
        viewModelScope.launch {
            _state.value = UploadState.Processing
            _progress.value = UploadProgress(currentFile = "读取文件中...")

            try {
                val content = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: throw IllegalArgumentException("无法读取文件")

                val fileName = uri.lastPathSegment ?: "note.md"
                val chunks = MarkdownParser.parse(content)
                var processed = 0

                saveNoteWithChunks(fileName, content, section, chunks.size) { current, currentFile ->
                    processed = current
                    _progress.value = UploadProgress(
                        current = processed,
                        total = chunks.size,
                        currentFile = currentFile
                    )
                }

                _state.value = UploadState.Success(1, processed)
            } catch (e: Exception) {
                _state.value = UploadState.Error(e.message ?: "未知错误")
            }
        }
    }

    private suspend fun saveNoteWithChunks(
        fileName: String,
        content: String,
        section: String,
        totalChunks: Int,
        onProgress: (Int, String) -> Unit
    ) {
        val note = Note(
            filePath = fileName,
            fileName = fileName.substringAfterLast("/", fileName),
            section = section,
            content = content,
            contentHash = MarkdownParser.computeHash(content)
        )
        val noteId = db.noteDao().insert(note).toInt()

        val chunks = MarkdownParser.parse(content)
        chunks.forEachIndexed { index, chunkData ->
            onProgress(index + 1, "${fileName} - ${chunkData.headingPath}")

            val chunk = Chunk(
                noteId = noteId,
                content = chunkData.content,
                contentHash = MarkdownParser.computeHash(chunkData.content),
                headingPath = chunkData.headingPath
            )
            val chunkId = db.chunkDao().insert(chunk).toInt()

            generateReviewItem(chunkId, chunkData)
        }
    }

    private suspend fun generateReviewItem(chunkId: Int, chunkData: com.reviewanything.app.service.ParsedChunk) {
        val config = db.configDao().getConfigSync()
        val fallbackQuestion = "请解释「${chunkData.headingPath.ifEmpty { "这段内容" }}」的核心要点？"
        val fallbackAnswer = chunkData.content.take(300)

        if (config?.apiKey != null) {
            try {
                val qaList = llmService.generateQA(chunkData.content, chunkData.headingPath, config)
                for ((q, a) in qaList) {
                    db.reviewItemDao().insert(
                        ReviewItem(chunkId = chunkId, question = q, answer = a)
                    )
                }
            } catch (e: Exception) {
                db.reviewItemDao().insert(
                    ReviewItem(
                        chunkId = chunkId,
                        question = fallbackQuestion,
                        answer = fallbackAnswer,
                        llmFailed = true
                    )
                )
            }
        } else {
            db.reviewItemDao().insert(
                ReviewItem(
                    chunkId = chunkId,
                    question = fallbackQuestion,
                    answer = fallbackAnswer,
                    llmFailed = true
                )
            )
        }
    }

    fun reset() {
        _state.value = UploadState.Idle
        _progress.value = UploadProgress()
    }

    sealed class UploadState {
        object Idle : UploadState()
        object Processing : UploadState()
        data class Success(val files: Int, val chunks: Int) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    data class UploadProgress(
        val current: Int = 0,
        val total: Int = 0,
        val currentFile: String = ""
    )
}
