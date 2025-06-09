package com.leetcrawler.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class LeetCodeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val leetCodeToolWindow = LeetCodeToolWindow(project)
        val content = ContentFactory.getInstance().createContent(leetCodeToolWindow.content, "", false)
        toolWindow.contentManager.addContent(content)
    }
} 