package com.github.saloed

import com.github.saloed.diff.DiffFile
import com.github.saloed.diff.TwoWayDiff
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.contents.DocumentContentImpl
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.openapi.editor.impl.DocumentImpl

class CustomDiffRequest(val diff: TwoWayDiff) : ContentDiffRequest() {
    val leftContent by lazy { diff.left.documentContent() }
    val rightContent by lazy { diff.right.documentContent() }
    override fun getTitle(): String = MyBundle.message("diffWindowTitle", diff.left.title, diff.right.title)
    override fun getContents() = listOf(leftContent, rightContent)
    override fun getContentTitles() = listOf(diff.left.title, diff.right.title)
    private fun DiffFile.documentContent(): DocumentContent {
        val text = contentLines.joinToString("\n")
        val document = DocumentImpl(text)
        return DocumentContentImpl(document)
    }
}
