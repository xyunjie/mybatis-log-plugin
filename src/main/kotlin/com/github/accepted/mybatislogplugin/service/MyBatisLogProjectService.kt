package com.github.accepted.mybatislogplugin.service

import com.github.accepted.mybatislogplugin.model.LogOrigin
import com.github.accepted.mybatislogplugin.model.MyBatisLogEntry
import com.github.accepted.mybatislogplugin.parser.MyBatisLogParser
import com.intellij.openapi.components.Service
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class MyBatisLogProjectService {
    private val parser = MyBatisLogParser()
    private val entries = CopyOnWriteArrayList<MyBatisLogEntry>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun appendEntry(entry: MyBatisLogEntry) {
        entries.add(0, entry)
        notifyChanged()
    }

    fun appendLine(line: String, origin: LogOrigin) {
        parser.appendLine(line, origin)?.let(::appendEntry)
    }

    fun parseManual(text: String): List<MyBatisLogEntry> {
        val parsedEntries = parser.parse(text, LogOrigin.MANUAL)
        if (parsedEntries.isEmpty()) {
            return emptyList()
        }
        entries.addAll(0, parsedEntries.asReversed())
        notifyChanged()
        return parsedEntries
    }

    fun getEntries(origin: LogOrigin? = null, keyword: String = ""): List<MyBatisLogEntry> {
        val originFiltered = origin?.let { targetOrigin ->
            entries.filter { it.origin == targetOrigin }
        } ?: entries.toList()

        if (keyword.isBlank()) {
            return originFiltered
        }

        val normalized = keyword.trim().lowercase()
        return originFiltered.filter {
            it.restoredSql.lowercase().contains(normalized) ||
                it.sourceText.lowercase().contains(normalized) ||
                it.rawPreparing.lowercase().contains(normalized) ||
                it.rawParameters.lowercase().contains(normalized)
        }
    }

    fun clear(origin: LogOrigin? = null) {
        if (origin == null) {
            entries.clear()
        } else {
            entries.removeIf { it.origin == origin }
        }
        notifyChanged()
    }

    fun addChangeListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners -= listener
    }

    private fun notifyChanged() {
        listeners.forEach { it.invoke() }
    }
}
