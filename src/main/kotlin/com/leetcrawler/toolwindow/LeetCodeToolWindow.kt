package com.leetcrawler.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.leetcrawler.model.LeetCodeProblem
import com.leetcrawler.service.LeetCodeCrawlerService
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.DefaultTableModel

class LeetCodeToolWindow(private val project: Project) {
    private val crawlerService = LeetCodeCrawlerService.getInstance()
    private var currentProblems = mutableListOf<LeetCodeProblem>()
    private var selectedProblem: LeetCodeProblem? = null
    private var currentPage = 0
    private val pageSize = 20
    private var isLoading = false
    
    // UI Components
    private val searchField = JBTextField()
    private val searchButton = JButton("搜索")
    private val refreshButton = JButton("刷新热门")
    private val loadMoreButton = JButton("加载更多")
    private val languageComboBox = JComboBox<String>()
    private val insertButton = JButton("插入代码模板")
    private val problemTable = JBTable()
    private val problemDetailArea = JBTextArea()
    private val codePreviewArea = JBTextArea()
    private val useCnCheckBox = JBCheckBox("使用中文站点", false) // 默认使用国际站
    private val pageLabel = JLabel("第 1 页")
    
    val content: JComponent by lazy { createContent() }
    
    private fun createContent(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 顶部搜索面板
        val topPanel = createTopPanel()
        panel.add(topPanel, BorderLayout.NORTH)
        
        // 主要内容面板
        val mainPanel = createMainPanel()
        panel.add(mainPanel, BorderLayout.CENTER)
        
        // 底部操作面板
        val bottomPanel = createBottomPanel()
        panel.add(bottomPanel, BorderLayout.SOUTH)
        
        // 初始化语言选择
        setupLanguageSelection()
        
        // 初始化数据
        loadPopularProblems()
        
        return panel
    }
    
    private fun createTopPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        searchField.preferredSize = Dimension(200, 30)
        searchField.toolTipText = "输入题目关键词搜索"
        
        panel.add(JLabel("搜索:"))
        panel.add(searchField)
        panel.add(searchButton)
        panel.add(refreshButton)
        panel.add(loadMoreButton)
        panel.add(useCnCheckBox)
        panel.add(pageLabel)
        
        // 添加事件监听器
        searchButton.addActionListener { performSearch() }
        refreshButton.addActionListener { loadPopularProblems() }
        loadMoreButton.addActionListener { loadMoreProblems() }
        
        loadMoreButton.isEnabled = false
        
        return panel
    }
    
    private fun createMainPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 左侧题目列表
        val leftPanel = createProblemListPanel()
        
        // 右侧详情面板
        val rightPanel = createDetailPanel()
        
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
        splitPane.dividerLocation = 400
        
        panel.add(splitPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createProblemListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.emptyTop(5)
        
        // 设置表格模型
        val tableModel = DefaultTableModel()
        tableModel.addColumn("ID")
        tableModel.addColumn("标题")
        tableModel.addColumn("难度")
        
        problemTable.model = tableModel
        problemTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        
        // 添加选择监听器
        problemTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedRow = problemTable.selectedRow
                if (selectedRow >= 0 && selectedRow < currentProblems.size) {
                    selectedProblem = currentProblems[selectedRow]
                    updateProblemDetail()
                }
            }
        }
        
        val scrollPane = JScrollPane(problemTable)
        panel.add(JLabel("题目列表"), BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createDetailPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建选项卡面板
        val tabbedPane = JTabbedPane()
        
        // 题目详情选项卡
        problemDetailArea.isEditable = false
        problemDetailArea.lineWrap = true
        problemDetailArea.wrapStyleWord = true
        val detailScrollPane = JScrollPane(problemDetailArea)
        tabbedPane.addTab("题目详情", detailScrollPane)
        
        // 代码预览选项卡
        codePreviewArea.isEditable = false
        codePreviewArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        val codeScrollPane = JScrollPane(codePreviewArea)
        tabbedPane.addTab("代码模板", codeScrollPane)
        
        panel.add(tabbedPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createBottomPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        // 语言选择
        setupLanguageComboBox()
        
        // 添加语言切换监听器
        languageComboBox.addActionListener { updateCodePreview() }
        
        panel.add(JLabel("语言:"))
        panel.add(languageComboBox)
        panel.add(insertButton)
        
        // 插入代码按钮事件
        insertButton.addActionListener { insertCodeTemplate() }
        
        return panel
    }
    
    private fun setupLanguageComboBox() {
        // 基础语言列表
        val allLanguages = listOf("Java", "Python", "Python3", "C++", "C", "C#", "JavaScript", "TypeScript", "Go", "Kotlin", "Rust", "Swift")
        
        // 根据IDE类型优先显示对应语言
        val ideLanguages = getPreferredLanguagesForIDE()
        
        // 清空并重新添加
        languageComboBox.removeAllItems()
        
        // 先添加优先语言
        ideLanguages.forEach { lang ->
            if (allLanguages.contains(lang)) {
                languageComboBox.addItem(lang)
            }
        }
        
        // 再添加其他语言
        allLanguages.forEach { lang ->
            if (!ideLanguages.contains(lang)) {
                languageComboBox.addItem(lang)
            }
        }
    }
    
    private fun getPreferredLanguagesForIDE(): List<String> {
        val applicationInfo = com.intellij.openapi.application.ApplicationInfo.getInstance()
        val productName = applicationInfo.fullApplicationName.lowercase()
        
        return when {
            productName.contains("goland") -> listOf("Go")
            productName.contains("pycharm") -> listOf("Python", "Python3")
            productName.contains("webstorm") -> listOf("JavaScript", "TypeScript")
            productName.contains("clion") -> listOf("C++", "C")
            productName.contains("rider") -> listOf("C#")
            productName.contains("rustrover") -> listOf("Rust")
            productName.contains("appcode") -> listOf("Swift")
            else -> listOf("Java", "Kotlin") // IntelliJ IDEA 默认
        }
    }
    
    private fun setupLanguageSelection() {
        // 设置默认语言为IDE优先语言
        val preferredLanguages = getPreferredLanguagesForIDE()
        if (preferredLanguages.isNotEmpty() && languageComboBox.itemCount > 0) {
            for (i in 0 until languageComboBox.itemCount) {
                val item = languageComboBox.getItemAt(i)
                if (preferredLanguages.contains(item)) {
                    languageComboBox.selectedIndex = i
                    break
                }
            }
        }
    }
    
    private fun performSearch() {
        val keyword = searchField.text.trim()
        if (keyword.isEmpty()) {
            Messages.showWarningDialog("请输入搜索关键词", "警告")
            return
        }
        
        currentPage = 0
        currentProblems.clear()
        setLoadingState(true)
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 使用新的分页搜索方法
                val problems = crawlerService.searchProblemsWithPaging(keyword, useCnCheckBox.isSelected, currentPage, pageSize).get()
                
                ApplicationManager.getApplication().invokeLater {
                    updateProblemList(problems, false)
                    setLoadingState(false)
                    loadMoreButton.isEnabled = problems.size >= pageSize
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("搜索失败: ${e.message}", "错误")
                    setLoadingState(false)
                }
            }
        }
    }
    
    private fun loadPopularProblems() {
        currentPage = 0
        currentProblems.clear()
        setLoadingState(true)
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val problems = crawlerService.getPopularProblems(useCnCheckBox.isSelected, currentPage, pageSize).get()
                
                ApplicationManager.getApplication().invokeLater {
                    updateProblemList(problems, false)
                    setLoadingState(false)
                    loadMoreButton.isEnabled = problems.size >= pageSize
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("加载失败: ${e.message}", "错误")
                    setLoadingState(false)
                }
            }
        }
    }
    
    private fun loadMoreProblems() {
        if (isLoading) return
        
        currentPage++
        setLoadingState(true)
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val problems = crawlerService.getPopularProblems(useCnCheckBox.isSelected, currentPage, pageSize).get()
                
                ApplicationManager.getApplication().invokeLater {
                    updateProblemList(problems, true)
                    setLoadingState(false)
                    loadMoreButton.isEnabled = problems.size >= pageSize
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("加载更多失败: ${e.message}", "错误")
                    setLoadingState(false)
                    currentPage-- // 回退页码
                }
            }
        }
    }
    
    private fun setLoadingState(loading: Boolean) {
        isLoading = loading
        searchButton.isEnabled = !loading
        refreshButton.isEnabled = !loading
        loadMoreButton.isEnabled = !loading
        
        if (loading) {
            if (currentPage == 0) {
                searchButton.text = "搜索中..."
                refreshButton.text = "加载中..."
            }
            loadMoreButton.text = "加载中..."
            pageLabel.text = "加载中..."
        } else {
            searchButton.text = "搜索"
            refreshButton.text = "刷新热门"
            loadMoreButton.text = "加载更多"
            pageLabel.text = "第 ${currentPage + 1} 页"
        }
    }
    
    private fun updateProblemList(problems: List<LeetCodeProblem>, append: Boolean) {
        if (!append) {
            currentProblems.clear()
        }
        currentProblems.addAll(problems)
        
        val tableModel = problemTable.model as DefaultTableModel
        if (!append) {
            tableModel.rowCount = 0
        }
        
        problems.forEach { problem ->
            tableModel.addRow(arrayOf(problem.id, problem.title, problem.difficulty))
        }
        
        if (currentProblems.isNotEmpty() && !append) {
            problemTable.setRowSelectionInterval(0, 0)
            selectedProblem = currentProblems[0]
            updateProblemDetail()
        }
    }
    
    private fun updateProblemDetail() {
        selectedProblem?.let { problem ->
            // 如果内容是"点击查看详情"，则需要加载完整详情
            if (problem.content == "点击查看详情") {
                // 显示加载中状态
                problemDetailArea.text = "正在加载题目详情..."
                codePreviewArea.text = "正在加载代码模板..."
                
                // 异步加载详细信息
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val detailedProblem = crawlerService.getProblemDetail(problem.titleSlug, useCnCheckBox.isSelected)
                        
                        ApplicationManager.getApplication().invokeLater {
                            if (detailedProblem != null) {
                                // 更新当前选中的问题
                                selectedProblem = detailedProblem
                                // 也更新列表中的问题
                                val index = currentProblems.indexOfFirst { it.titleSlug == problem.titleSlug }
                                if (index >= 0) {
                                    currentProblems[index] = detailedProblem
                                }
                                // 更新显示
                                displayProblemDetail(detailedProblem)
                            } else {
                                problemDetailArea.text = "加载题目详情失败，请稍后重试"
                                codePreviewArea.text = "无法获取代码模板"
                            }
                        }
                    } catch (e: Exception) {
                        ApplicationManager.getApplication().invokeLater {
                            problemDetailArea.text = "加载题目详情失败: ${e.message}"
                            codePreviewArea.text = "无法获取代码模板"
                        }
                    }
                }
            } else {
                // 直接显示已有详情
                displayProblemDetail(problem)
            }
        }
    }
    
    private fun displayProblemDetail(problem: LeetCodeProblem) {
        // 更新题目详情
        val detailText = buildString {
            append("题目 ${problem.id}: ${problem.title}\n")
            append("难度: ${problem.difficulty}\n")
            append("标签: ${problem.topicTags.joinToString(", ") { it.name }}\n\n")
            
            // 解析HTML内容为纯文本
            val plainContent = problem.content
                .replace("<[^>]+>".toRegex(), "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .trim()
            
            append("题目描述:\n")
            append(plainContent)
            
            if (problem.hints.isNotEmpty()) {
                append("\n\n提示:\n")
                problem.hints.forEachIndexed { index, hint ->
                    append("${index + 1}. $hint\n")
                }
            }
        }
        
        problemDetailArea.text = detailText
        problemDetailArea.caretPosition = 0
        
        // 更新代码预览
        updateCodePreview()
    }
    
    private fun updateCodePreview() {
        selectedProblem?.let { problem ->
            val selectedLang = languageComboBox.selectedItem as String
            val codeSnippet = problem.codeSnippets.find { 
                it.lang.equals(selectedLang, ignoreCase = true) || 
                it.langSlug.equals(selectedLang.lowercase(), ignoreCase = true)
            }
            
            codePreviewArea.text = codeSnippet?.code ?: "该语言暂无代码模板"
            codePreviewArea.caretPosition = 0
        }
    }
    
    private fun insertCodeTemplate() {
        selectedProblem?.let { problem ->
            val selectedLang = languageComboBox.selectedItem as String
            val codeSnippet = problem.codeSnippets.find { 
                it.lang.equals(selectedLang, ignoreCase = true) || 
                it.langSlug.equals(selectedLang.lowercase(), ignoreCase = true)
            }
            
            if (codeSnippet != null) {
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                if (editor != null) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        try {
                            val document = editor.document
                            val caretModel = editor.caretModel
                            val offset = caretModel.offset
                            
                            // 插入题目注释和代码模板
                            val codeToInsert = buildString {
                                append("/*\n")
                                append("题目 ${problem.id}: ${problem.title}\n")
                                append("难度: ${problem.difficulty}\n")
                                append("标签: ${problem.topicTags.joinToString(", ") { it.name }}\n")
                                append("*/\n\n")
                                append(codeSnippet.code)
                            }
                            
                            document.insertString(offset, codeToInsert)
                            caretModel.moveToOffset(offset + codeToInsert.length)
                            
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showInfoMessage("代码模板已插入到编辑器", "成功")
                            }
                        } catch (e: Exception) {
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showErrorDialog("插入代码失败: ${e.message}", "错误")
                            }
                        }
                    }
                } else {
                    Messages.showWarningDialog("请先打开一个文件", "提示")
                }
            } else {
                Messages.showWarningDialog("该语言暂无代码模板", "提示")
            }
        } ?: Messages.showWarningDialog("请先选择一个题目", "提示")
    }
} 