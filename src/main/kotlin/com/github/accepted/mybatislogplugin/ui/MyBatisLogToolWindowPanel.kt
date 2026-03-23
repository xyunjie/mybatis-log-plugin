package com.github.accepted.mybatislogplugin.ui

import com.github.accepted.mybatislogplugin.MyBatisLogBundle
import com.github.accepted.mybatislogplugin.model.LogOrigin
import com.github.accepted.mybatislogplugin.model.MyBatisLogEntry
import com.github.accepted.mybatislogplugin.service.MyBatisLogProjectService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MyBatisLogToolWindowPanel(project: Project) : JPanel(BorderLayout()), Disposable {
    private val service = project.getService(MyBatisLogProjectService::class.java)

    private val autoListModel = DefaultListModel<MyBatisLogEntry>()
    private val autoEntryList = JBList(autoListModel)
    private val autoSearchField = JBTextField()
    private val parsedSqlArea = createReadOnlyArea()
    private val rawPreparingArea = createReadOnlyArea()
    private val rawParametersArea = createReadOnlyArea()
    private var autoKeyword: String = ""

    private val manualInputArea = JBTextArea()
    private val manualParsedArea = createReadOnlyArea()

    private val changeListener: () -> Unit = {
        ApplicationManager.getApplication().invokeLater { refreshAutoEntries() }
    }

    init {
        service.addChangeListener(changeListener)
        layoutComponents()
        bindEvents()
        refreshAutoEntries()
        updateManualParsedState()
    }

    override fun dispose() {
        service.removeChangeListener(changeListener)
    }

    private fun layoutComponents() {
        border = JBUI.Borders.empty(8)

        val tabs = JTabbedPane().apply {
            addTab(MyBatisLogBundle.message("ui.tab.auto"), createAutoTab())
            addTab(MyBatisLogBundle.message("ui.tab.manual"), createManualTab())
        }
        add(tabs, BorderLayout.CENTER)
    }

    private fun createAutoTab(): JPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
        add(createAutoToolbar(), BorderLayout.NORTH)
        add(createAutoContent(), BorderLayout.CENTER)
    }

    private fun createAutoToolbar(): JPanel {
        autoSearchField.emptyText.text = MyBatisLogBundle.message("ui.auto.toolbar.search.placeholder")

        val clearButton = JButton(MyBatisLogBundle.message("ui.auto.button.clear"), AllIcons.Actions.GC)
        val copyButton = JButton(MyBatisLogBundle.message("ui.auto.button.copySql"), AllIcons.Actions.Copy)
        val actionPanel = JPanel().apply {
            isOpaque = false
            add(copyButton)
            add(clearButton)
        }

        clearButton.addActionListener {
            service.clear(LogOrigin.AUTO)
            clearAutoDetails()
        }

        copyButton.addActionListener {
            val selected = autoEntryList.selectedValue ?: return@addActionListener
            CopyPasteManager.getInstance().setContents(StringSelection(selected.restoredSql))
        }

        return JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(JBLabel(MyBatisLogBundle.message("ui.auto.toolbar.title")), BorderLayout.WEST)
            add(autoSearchField, BorderLayout.CENTER)
            add(actionPanel, BorderLayout.EAST)
        }
    }

    private fun createAutoContent(): JSplitPane {
        autoEntryList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        autoEntryList.cellRenderer = MyBatisLogEntryCellRenderer()
        autoEntryList.emptyText.text = MyBatisLogBundle.message("ui.auto.list.empty")

        val listPanel = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            add(JBLabel(MyBatisLogBundle.message("ui.auto.list.title")), BorderLayout.NORTH)
            add(JBScrollPane(autoEntryList), BorderLayout.CENTER)
        }

        val rawContentPanel = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            labeledPane(MyBatisLogBundle.message("ui.auto.details.parameterizedSql.title"), rawPreparingArea),
            labeledPane(MyBatisLogBundle.message("ui.auto.details.parameters.title"), rawParametersArea),
        ).apply {
            resizeWeight = 0.5
            border = JBUI.Borders.emptyTop(4)
        }

        val detailTabs = JTabbedPane().apply {
            addTab(
                MyBatisLogBundle.message("ui.auto.details.parsedSql.tab"),
                labeledPane(MyBatisLogBundle.message("ui.auto.details.parsedSql.title"), parsedSqlArea),
            )
            addTab(MyBatisLogBundle.message("ui.auto.details.rawContent.tab"), rawContentPanel)
        }

        val detailPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            add(JBLabel(MyBatisLogBundle.message("ui.auto.details.title")), BorderLayout.NORTH)
            add(detailTabs, BorderLayout.CENTER)
        }

        return JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            listPanel,
            detailPanel,
        ).apply {
            resizeWeight = 0.38
            dividerSize = JBUI.scale(6)
        }
    }

    private fun createManualTab(): JSplitPane {
        manualInputArea.lineWrap = true
        manualInputArea.wrapStyleWord = true
        manualInputArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        manualInputArea.border = BorderFactory.createLineBorder(JBColor.border())

        val parseButton = JButton(MyBatisLogBundle.message("ui.manual.button.parse"), AllIcons.Actions.Execute)
        val copyButton = JButton(MyBatisLogBundle.message("ui.manual.button.copyResult"), AllIcons.Actions.Copy)
        val clearButton = JButton(MyBatisLogBundle.message("ui.manual.button.clearInput"), AllIcons.Actions.GC)

        val inputPanel = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            add(
                JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                    isOpaque = false
                    add(JBLabel(MyBatisLogBundle.message("ui.manual.input.title")), BorderLayout.WEST)
                    add(JPanel().apply {
                        isOpaque = false
                        add(parseButton)
                        add(copyButton)
                        add(clearButton)
                    }, BorderLayout.EAST)
                },
                BorderLayout.NORTH,
            )
            add(JBScrollPane(manualInputArea), BorderLayout.CENTER)
        }

        val parsedPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            add(JBLabel(MyBatisLogBundle.message("ui.manual.result.title")), BorderLayout.NORTH)
            add(JBScrollPane(manualParsedArea), BorderLayout.CENTER)
        }

        parseButton.addActionListener {
            val parsedEntries = service.parseManual(manualInputArea.text)
            if (parsedEntries.isEmpty()) {
                manualParsedArea.text = ""
                updateManualParsedState()
                Messages.showInfoMessage(
                    this,
                    MyBatisLogBundle.message("ui.dialog.noValidEntries"),
                    MyBatisLogBundle.message("ui.dialog.title"),
                )
            } else {
                manualParsedArea.text = parsedEntries.joinToString(separator = "\n\n") { it.restoredSql }
                updateManualParsedState()
            }
        }

        copyButton.addActionListener {
            if (manualParsedArea.text.isBlank()) {
                return@addActionListener
            }
            CopyPasteManager.getInstance().setContents(StringSelection(manualParsedArea.text))
        }

        clearButton.addActionListener {
            manualInputArea.text = ""
            manualParsedArea.text = ""
            updateManualParsedState()
        }

        return JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            inputPanel,
            parsedPanel,
        ).apply {
            resizeWeight = 0.5
            dividerSize = JBUI.scale(6)
        }
    }

    private fun bindEvents() {
        autoSearchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateKeyword()
            override fun removeUpdate(e: DocumentEvent) = updateKeyword()
            override fun changedUpdate(e: DocumentEvent) = updateKeyword()

            private fun updateKeyword() {
                autoKeyword = autoSearchField.text.orEmpty()
                refreshAutoEntries()
            }
        })

        autoEntryList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                showAutoEntry(autoEntryList.selectedValue)
            }
        }
    }

    private fun refreshAutoEntries() {
        val items = service.getEntries(origin = LogOrigin.AUTO, keyword = autoKeyword)
        val selectedId = autoEntryList.selectedValue?.id
        autoListModel.clear()
        items.forEach(autoListModel::addElement)

        val selectedIndex = items.indexOfFirst { it.id == selectedId }
        when {
            selectedIndex >= 0 -> autoEntryList.selectedIndex = selectedIndex
            autoListModel.size > 0 -> autoEntryList.selectedIndex = 0
            else -> clearAutoDetails()
        }
    }

    private fun showAutoEntry(entry: MyBatisLogEntry?) {
        if (entry == null) {
            clearAutoDetails()
            return
        }
        parsedSqlArea.text = entry.restoredSql
        rawPreparingArea.text = entry.rawPreparing
        rawParametersArea.text = entry.rawParameters
        updateAutoDetailStates()
    }

    private fun clearAutoDetails() {
        parsedSqlArea.text = ""
        rawPreparingArea.text = ""
        rawParametersArea.text = ""
        updateAutoDetailStates()
    }

    private fun labeledPane(title: String, area: JBTextArea): JPanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
        border = JBUI.Borders.empty(4)
        add(JBLabel(title), BorderLayout.NORTH)
        add(JBScrollPane(area), BorderLayout.CENTER)
    }

    private fun updateAutoDetailStates() {
        parsedSqlArea.emptyText.text = MyBatisLogBundle.message("ui.auto.details.parsedSql.empty")
        rawPreparingArea.emptyText.text = MyBatisLogBundle.message("ui.auto.details.parameterizedSql.empty")
        rawParametersArea.emptyText.text = MyBatisLogBundle.message("ui.auto.details.parameters.empty")
    }

    private fun updateManualParsedState() {
        manualParsedArea.emptyText.text = MyBatisLogBundle.message("ui.manual.result.empty")
    }

    private fun createReadOnlyArea(): JBTextArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        border = BorderFactory.createLineBorder(JBColor.border())
        margin = JBUI.insets(8)
    }
}
