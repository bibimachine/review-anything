package com.reviewanything.app.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ZipExtractor {

    fun extract(context: Context, uri: Uri): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val tempDir = File(context.cacheDir, "extracted_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".md", ignoreCase = true)) {
                        val outFile = File(tempDir, entry.name.replace("/", "_"))
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        val content = outFile.readText()
                        results.add(entry.name to content)
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return results
    }
}
