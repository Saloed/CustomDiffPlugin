package com.github.saloed.z3.trace

import com.github.saloed.CustomDiffRequestChain
import com.github.saloed.MyBundle
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.util.DiffUtil
import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile

class Z3TraceDiffAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        if (files.size != 2 || files.any { !it.isValid }) return
        if (files.map { getType(it) }.any { it != Type.FILE }) return
        val requestChain = Z3TraceDiffRequestChain(files[0], files[1])
        DiffManager.getInstance().showDiff(e.project, requestChain, DiffDialogHints.DEFAULT)
    }

    override fun update(e: AnActionEvent) {
        val canShow = isAvailable(e)
        e.presentation.isEnabled = canShow
        if (ActionPlaces.isPopupPlace(e.place)) {
            e.presentation.isVisible = canShow
        }
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (files == null || files.size != 2) return
        e.presentation.text = MyBundle.message("action.compare.z3.traces")
    }

    private fun isAvailable(e: AnActionEvent): Boolean {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false
        if (files.size != 2) return false
        if (files.any { DiffUtil.isFileWithoutContent(it) }) return false
        if (files.map { getType(it) }.any { it != Type.FILE }) return false
        return true
    }

    private fun getType(file: VirtualFile?) = when {
        file == null -> Type.FILE
        file.fileType is ArchiveFileType -> Type.ARCHIVE
        file.isDirectory -> Type.DIRECTORY
        else -> Type.FILE
    }

    private enum class Type {
        FILE, DIRECTORY, ARCHIVE
    }
}