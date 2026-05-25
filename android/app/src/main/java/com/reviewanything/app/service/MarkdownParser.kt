package com.reviewanything.app.service

import java.security.MessageDigest

data class ParsedChunk(
    val content: String,
    val headingPath: String
)

object MarkdownParser {

    fun parse(content: String): List<ParsedChunk> {
        val lines = content.lines()
        val chunks = mutableListOf<ParsedChunk>()
        val headingStack = mutableListOf<String>()
        val currentContent = StringBuilder()

        fun flushChunk() {
            if (currentContent.isNotBlank()) {
                val text = currentContent.toString().trim()
                if (text.length >= 20) {
                    chunks.add(ParsedChunk(text, headingStack.joinToString("/")))
                }
            }
            currentContent.clear()
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                flushChunk()
                val level = trimmed.takeWhile { it == '#' }.length
                val title = trimmed.drop(level).trim()
                while (headingStack.size >= level) {
                    headingStack.removeAt(headingStack.size - 1)
                }
                headingStack.add(title)
            } else {
                currentContent.appendLine(line)
            }
        }
        flushChunk()

        return chunks.ifEmpty {
            listOf(ParsedChunk(content.trim(), ""))
        }
    }

    fun computeHash(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
