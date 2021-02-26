package com.github.saloed

import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileTypes.FileTypeRegistry

class CustomDiffAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
        val request = CustomDiffRequestChain(file)
        DiffManager.getInstance().showDiff(project, request, DiffDialogHints.DEFAULT)
    }

    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null && file != null && FileTypeRegistry.getInstance().isFileOfType(file, DiffFileType)
        e.presentation.isEnabledAndVisible = enabled
    }
}
