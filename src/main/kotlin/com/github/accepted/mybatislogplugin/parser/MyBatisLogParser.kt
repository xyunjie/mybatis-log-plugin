package com.github.accepted.mybatislogplugin.parser

import com.github.accepted.mybatislogplugin.model.LogOrigin
import com.github.accepted.mybatislogplugin.model.MyBatisLogEntry
import java.time.Instant
import java.util.UUID

class MyBatisLogParser {
    private var pendingPreparing: String? = null
    private var pendingSource: String? = null
    private val rawBlockLines = mutableListOf<String>()
    private val recentLines = ArrayDeque<String>()

    fun appendLine(line: String, origin: LogOrigin): MyBatisLogEntry? {
        trackRecentLine(line)

        val preparing = extractPayload(line, PREPARING_MARKER)
        if (preparing != null) {
            pendingPreparing = preparing
            pendingSource = detectSource()
            rawBlockLines.clear()
            rawBlockLines += line
            return null
        }

        val parameters = extractPayload(line, PARAMETERS_MARKER)
        if (parameters != null && pendingPreparing != null) {
            rawBlockLines += line
            val entry = buildEntry(
                preparing = pendingPreparing ?: return null,
                parameters = parameters,
                origin = origin,
                sourceText = pendingSource ?: defaultSource(origin),
                rawBlock = rawBlockLines.joinToString(separator = "\n"),
            )
            resetPending()
            return entry
        }

        if (pendingPreparing != null) {
            rawBlockLines += line
        }
        return null
    }

    fun parse(text: String, origin: LogOrigin): List<MyBatisLogEntry> {
        val parser = MyBatisLogParser()
        return text.lineSequence()
            .mapNotNull { parser.appendLine(it, origin) }
            .toList()
    }

    private fun buildEntry(
        preparing: String,
        parameters: String,
        origin: LogOrigin,
        sourceText: String,
        rawBlock: String,
    ): MyBatisLogEntry? {
        val parsedParameters = parseParameters(parameters) ?: return null
        val placeholderCount = preparing.count { it == '?' }
        if (placeholderCount != parsedParameters.size) {
            return null
        }

        val restoredSql = MyBatisSqlFormatter.format(restoreSql(preparing, parsedParameters))
        return MyBatisLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            origin = origin,
            sourceText = sourceText,
            rawPreparing = preparing,
            rawParameters = parameters,
            restoredSql = restoredSql,
            rawBlock = rawBlock,
        )
    }

    private fun restoreSql(sqlTemplate: String, parameters: List<String>): String {
        val result = StringBuilder()
        var parameterIndex = 0

        for (char in sqlTemplate) {
            if (char == '?' && parameterIndex < parameters.size) {
                result.append(parameters[parameterIndex++])
            } else {
                result.append(char)
            }
        }

        return result.toString()
    }

    private fun parseParameters(parameters: String): List<String>? {
        if (parameters.isBlank()) {
            return emptyList()
        }

        val tokens = splitParameters(parameters) ?: return null
        return tokens.map { token ->
            val trimmed = token.trim()
            if (trimmed.equals("null", ignoreCase = true)) {
                "null"
            } else {
                val match = PARAMETER_PATTERN.matchEntire(trimmed) ?: return null
                val value = match.groupValues[1]
                val type = match.groupValues[2]
                MyBatisParameterFormatter.format(value, type)
            }
        }
    }

    private fun splitParameters(parameters: String): List<String>? {
        val normalized = parameters.trim()
        if (normalized.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0

        normalized.forEachIndexed { index, char ->
            when (char) {
                '(' -> {
                    depth++
                    current.append(char)
                }
                ')' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    current.append(char)
                }
                ',' -> {
                    val nextChar = normalized.getOrNull(index + 1)
                    if (depth == 0 && nextChar?.isWhitespace() == true) {
                        val token = current.toString().trim()
                        if (token.isNotEmpty()) {
                            result += token
                        }
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }

        val lastToken = current.toString().trim()
        if (lastToken.isNotEmpty()) {
            result += lastToken
        }

        return result.takeIf { it.isNotEmpty() }
    }

    private fun extractPayload(line: String, marker: String): String? {
        val index = line.indexOf(marker)
        if (index < 0) {
            return null
        }
        return line.substring(index + marker.length).trim()
    }

    private fun detectSource(): String? {
        return recentLines
            .asReversed()
            .firstOrNull { SOURCE_PATTERN.containsMatchIn(it) }
            ?.trim()
    }

    private fun defaultSource(origin: LogOrigin): String = when (origin) {
        LogOrigin.AUTO -> "Auto Captured"
        LogOrigin.MANUAL -> "Manual Input"
    }

    private fun trackRecentLine(line: String) {
        recentLines += line
        while (recentLines.size > RECENT_LINE_LIMIT) {
            recentLines.removeFirst()
        }
    }

    private fun resetPending() {
        pendingPreparing = null
        pendingSource = null
        rawBlockLines.clear()
    }

    companion object {
        private const val PREPARING_MARKER = "Preparing:"
        private const val PARAMETERS_MARKER = "Parameters:"
        private const val RECENT_LINE_LIMIT = 5
        private val PARAMETER_PATTERN = Regex("^(.*)\\(([^()]*)\\)$")
        private val SOURCE_PATTERN = Regex("([A-Za-z0-9_$.]+Mapper|[A-Za-z0-9_$.]+\\.[A-Za-z0-9_$]+)")
    }
}
