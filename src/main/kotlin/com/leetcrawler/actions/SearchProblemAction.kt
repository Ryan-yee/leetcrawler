package com.leetcrawler.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

class SearchProblemAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val keyword = Messages.showInputDialog(
            project,
            "请输入要搜索的题目关键词:",
            "搜索 LeetCode 题目",
            Messages.getQuestionIcon()
        )
        
        if (!keyword.isNullOrBlank()) {
            // 打开工具窗口
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LeetCode Crawler")
            toolWindow?.show()
            
            // 可以在这里触发搜索逻辑
            Messages.showInfoMessage("正在搜索关键词: $keyword", "搜索中")
        }
    }
} 