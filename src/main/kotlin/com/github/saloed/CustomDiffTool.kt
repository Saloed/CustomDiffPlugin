package com.github.saloed

import com.github.saloed.diff.DiffMode
import com.intellij.diff.DiffContext
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest

class CustomDiffTool : FrameDiffTool {
    override fun getName(): String = MyBundle.message("diffToolName")

    override fun canShow(context: DiffContext, request: DiffRequest): Boolean =
        request is CustomDiffRequest && request.diff.mode == DiffMode.LINE

    override fun createComponent(context: DiffContext, request: DiffRequest) = CustomDiffViewer(context, request)
}
