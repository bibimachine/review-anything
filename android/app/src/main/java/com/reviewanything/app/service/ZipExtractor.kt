package com.reviewanything.app.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ZipExtractor {

    fun extract(context: Context, uri: Uri): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()

        // 先把内容复制到临时 ZIP 文件（ZipFile 需要文件路径）
        val tempZip = File(context.cacheDir, "upload_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempZip.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("无法读取文件")

        // 策略1: ZipFile（兼容性最好，支持大多数 ZIP）
        var zipFileSuccess = false
        try {
            ZipFile(tempZip).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory && entry.name.endsWith(".md", ignoreCase = true)) {
                        zip.getInputStream(entry).use { stream ->
                            val content = stream.bufferedReader().readText()
                            results.add(entry.name to content)
                        }
                    }
                }
            }
            zipFileSuccess = results.isNotEmpty()
        } catch (e: Exception) {
            // ZipFile 失败，尝试 ZipInputStream
        }

        // 策略2: ZipInputStream（fallback，某些 ZipFile 不支持的格式可用）
        if (!zipFileSuccess) {
            results.clear()
            try {
                ZipInputStream(tempZip.inputStream().buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".md", ignoreCase = true)) {
                            val content = zis.bufferedReader().readText()
                            results.add(entry.name to content)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } catch (e: Exception) {
                // 不是有效的 ZIP，继续 fallback
            }
        }

        // 策略3: 直接读取单个文件（用户可能选了单个 .md 文件而不是 ZIP）
        if (results.isEmpty()) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    val name = uri.lastPathSegment ?: "note.md"
                    if (name.endsWith(".md", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true)) {
                        results.add(name to content)
                    }
                }
            } catch (e: Exception) {
                // 忽略
            }
        }

        tempZip.delete()

        if (results.isEmpty()) {
            throw IllegalArgumentException("未找到 Markdown 文件，请确认上传的是包含 .md 文件的 ZIP 或单个 .md 文件")
        }

        return results
    }
}
