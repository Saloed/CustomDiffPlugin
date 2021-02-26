package com.github.saloed

import com.github.saloed.diff.DiffType
import com.github.saloed.diff.TwoWayDiff
import com.intellij.diff.DiffContext
import com.intellij.diff.comparison.ComparisonManagerImpl
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.comparison.InnerFragmentsPolicy
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.fragments.LineFragmentImpl
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.simple.SimpleDiffChange
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.tools.util.text.TextDiffProviderBase
import com.intellij.openapi.progress.ProgressIndicator
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class CustomDiffViewer(context: DiffContext, request: DiffRequest) : SimpleDiffViewer(context, request) {
    override fun computeDifferences(indicator: ProgressIndicator): Runnable {
        indicator.checkCanceled()
        val request = this.request as CustomDiffRequest
        val leftOffsets = LineOffsetsUtil.create(request.leftContent.document)
        val rightOffsets = LineOffsetsUtil.create(request.rightContent.document)
        val fragments = buildLineFragments(request.diff, leftOffsets, rightOffsets)
        val innerFragments = computeInnerFragmentIfEnabled(fragments, request, indicator)
        if (innerFragments.isEmpty()) return apply(null, true)
        val changes = innerFragments.mapIndexed { i, fragment -> SimpleDiffChange(i, fragment) }
        return apply(changes, false)
    }

    private fun computeInnerFragmentIfEnabled(
        fragments: List<LineFragment>,
        request: CustomDiffRequest,
        indicator: ProgressIndicator
    ): List<LineFragment> {
        val highlightPolicy = (myTextDiffProvider as TextDiffProviderBase).highlightPolicy
        val ignorePolicy = (myTextDiffProvider as TextDiffProviderBase).ignorePolicy
        if (highlightPolicy.fragmentsPolicy == InnerFragmentsPolicy.NONE) return fragments
        return createInnerFragments(
            fragments,
            request.leftContent.document.text,
            request.rightContent.document.text,
            ignorePolicy.comparisonPolicy,
            highlightPolicy.fragmentsPolicy,
            indicator
        )
    }

    private fun buildLineFragments(
        twoWayDiff: TwoWayDiff,
        leftOffsets: LineOffsets,
        rightOffsets: LineOffsets
    ): List<LineFragment> {
        // todo: support inner fragments
        val result = mutableListOf<LineFragment>()
        for (diff in twoWayDiff.diff) {
            if (diff.type == DiffType.EQUAL) continue
            with(diff) {
                result += LineFragmentImpl(
                    left.startLine,
                    left.endLine,
                    right.startLine,
                    right.endLine,
                    leftOffsets.getLineStart(left.startLine) + left.startLineOffset,
                    leftOffsets.getLineStart(left.endLine) + left.endLineOffset,
                    rightOffsets.getLineStart(right.startLine) + right.startLineOffset,
                    rightOffsets.getLineStart(right.endLine) + right.endLineOffset
                )
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun createInnerFragments(
        lineFragments: List<LineFragment>,
        text1: CharSequence,
        text2: CharSequence,
        policy: ComparisonPolicy,
        fragmentsPolicy: InnerFragmentsPolicy,
        indicator: ProgressIndicator
    ): List<LineFragment> {
        val lookup = MethodHandles.privateLookupIn(ComparisonManagerImpl::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            java.util.List::class.java,
            java.util.List::class.java,
            java.lang.CharSequence::class.java,
            java.lang.CharSequence::class.java,
            ComparisonPolicy::class.java,
            InnerFragmentsPolicy::class.java,
            ProgressIndicator::class.java
        )
        val handle = lookup.findStatic(ComparisonManagerImpl::class.java, "createInnerFragments", methodType)
        val result = handle.invoke(lineFragments, text1, text2, policy, fragmentsPolicy, indicator)
        return result as List<LineFragment>
    }

}
