package com.github.linwancen.plugin.compare.action

import com.github.linwancen.plugin.compare.ui.CompareJarBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import io.github.linwancen.compare.Main
import java.io.File

object UnJarAndDeleteSame : AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.text = CompareJarBundle.message("delete.same")
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        ApplicationManager.getApplication().runWriteAction {
            object : Task.Backgroundable(project, "Delete same") {
                override fun run(indicator: ProgressIndicator) {
                    val fileList = files.map { File(it.path) }.toList()
                    Main.diff(fileList)
                }
            }.queue()
        }
    }
}