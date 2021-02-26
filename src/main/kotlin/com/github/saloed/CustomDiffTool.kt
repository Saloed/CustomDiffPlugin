package com.github.saloed

import com.intellij.diff.DiffContext
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest

class CustomDiffTool : FrameDiffTool {
    override fun getName(): String = MyBundle.message("diffToolName")

    override fun canShow(context: DiffContext, request: DiffRequest): Boolean = request is CustomDiffRequest

    override fun createComponent(context: DiffContext, request: DiffRequest) = CustomDiffViewer(context, request)
}
