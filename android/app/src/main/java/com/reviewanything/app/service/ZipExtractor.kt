package com.reviewanything.app.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipFile

object ZipExtractor {

    fun extract(context: Context, uri: Uri): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()

        // 先把内容复制到临时 ZIP 文件（ZipFile 需要文件路径）
        val tempZip = File(context.cacheDir, "upload_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempZip.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 用 ZipFile 解压（比 ZipInputStream 兼容性更好）
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

        tempZip.delete()
        return results
    }
}
