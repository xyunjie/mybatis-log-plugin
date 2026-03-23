package com.github.accepted.mybatislogplugin.ui

import com.github.accepted.mybatislogplugin.MyBatisLogBundle
import com.github.accepted.mybatislogplugin.model.MyBatisLogEntry
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class MyBatisLogEntryCellRenderer : SimpleListCellRenderer<MyBatisLogEntry>() {
    override fun customize(
        list: JList<out MyBatisLogEntry>,
        value: MyBatisLogEntry?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        text = value?.sourceText ?: ""
    }

    override fun getListCellRendererComponent(
        list: JList<out MyBatisLogEntry>,
        value: MyBatisLogEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val panel = JPanel(BorderLayout()).apply {
            background = if (isSelected) list.selectionBackground else list.background
            foreground = if (isSelected) list.selectionForeground else list.foreground
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(8),
            )
        }

        val titleLabel = JLabel(value?.sourceText ?: MyBatisLogBundle.message("ui.list.unknownSource")).apply {
            font = font.deriveFont(Font.BOLD)
            foreground = panel.foreground
        }

        val timeLabel = JLabel(value?.timestamp?.let { DateFormatUtil.formatDateTime(it.toEpochMilli()) } ?: "").apply {
            foreground = if (isSelected) panel.foreground else JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, font.size2D - 1)
        }

        val summaryLabel = JLabel(buildSummary(value)).apply {
            foreground = panel.foreground
            font = font.deriveFont(Font.PLAIN)
        }

        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(titleLabel, BorderLayout.WEST)
            add(timeLabel, BorderLayout.EAST)
        }

        val content = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(header)
            add(summaryLabel)
        }

        panel.add(content, BorderLayout.CENTER)
        return panel
    }

    private fun buildSummary(value: MyBatisLogEntry?): String {
        val summary = value?.restoredSql?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (summary.isBlank()) {
            return MyBatisLogBundle.message("ui.list.noParsedSql")
        }
        return if (summary.length > 120) summary.take(117) + "..." else summary
    }
}
