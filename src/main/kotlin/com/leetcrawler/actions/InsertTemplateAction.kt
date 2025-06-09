package com.leetcrawler.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

class InsertTemplateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 打开工具窗口
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LeetCode Crawler")
        if (toolWindow != null) {
            toolWindow.show()
            Messages.showInfoMessage("请在工具窗口中选择题目并点击插入按钮", "提示")
        } else {
            Messages.showErrorDialog("无法找到 LeetCode Crawler 工具窗口", "错误")
        }
    }
} 