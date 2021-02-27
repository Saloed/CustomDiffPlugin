package com.github.saloed

import com.github.saloed.diff.DiffRange
import com.github.saloed.diff.DiffType
import com.intellij.diff.DiffContext
import com.intellij.diff.comparison.ComparisonManagerImpl
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.comparison.ComparisonUtils
import com.intellij.diff.comparison.InnerFragmentsPolicy
import com.intellij.diff.comparison.iterables.DiffIterableUtil
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.simple.SimpleDiffChange
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.tools.util.text.TextDiffProviderBase
import com.intellij.diff.util.Range
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.diff.Diff
import com.github.saloed.diff.Diff as MyDiff

class CustomDiffViewer(context: DiffContext, request: DiffRequest) : SimpleDiffViewer(context, request) {
    override fun computeDifferences(indicator: ProgressIndicator): Runnable {
        indicator.checkCanceled()
        val request = this.request as CustomDiffRequest
        val myChanges = mergeDiffChain(request.diff.diff)
        val changes = translateChanges(myChanges)
        val fragments = buildFragments(changes, request.leftContent.document, request.rightContent.document, indicator)
        if (fragments.isEmpty()) return apply(null, true)
        val diffChanges = fragments.mapIndexed { i, fragment -> SimpleDiffChange(i, fragment) }
        return apply(diffChanges, false)
    }

    private fun computeInnerFragmentIfEnabled(
        fragments: List<LineFragment>,
        leftDocument: Document,
        rightDocument: Document,
        indicator: ProgressIndicator
    ): List<LineFragment> {
        if (fragmentsPolicy == InnerFragmentsPolicy.NONE) return fragments
        return ComparisonUtils.createInnerFragments(
            fragments, leftDocument.text, rightDocument.text,
            comparisonPolicy, fragmentsPolicy, indicator
        )
    }

    private fun buildFragments(
        changes: Diff.Change,
        leftDocument: Document,
        rightDocument: Document,
        indicator: ProgressIndicator
    ): List<LineFragment> {
        val leftOffset = LineOffsetsUtil.create(leftDocument)
        val rightOffset = LineOffsetsUtil.create(rightDocument)
        val changeChain = DiffIterableUtil.create(changes, leftOffset.lineCount, rightOffset.lineCount)
        val fairChangeChain = DiffIterableUtil.fair(changeChain)

        val range = Range(0, leftOffset.lineCount, 0, rightOffset.lineCount)
        val leftLineContent = ComparisonUtils.getLineContents(range.start1, range.end1, leftDocument.text, leftOffset)
        val rightLineContent = ComparisonUtils.getLineContents(
            range.start2, range.end2, rightDocument.text, rightOffset
        )

        val correctedChanges = ComparisonUtils.correctAndOptimizeChanges(
            leftLineContent, rightLineContent, fairChangeChain,
            comparisonPolicy, indicator
        )
        val fragments = ComparisonManagerImpl.convertIntoLineFragments(range, leftOffset, rightOffset, correctedChanges)
        return computeInnerFragmentIfEnabled(fragments, leftDocument, rightDocument, indicator)
    }

    private val comparisonPolicy: ComparisonPolicy
        get() = (myTextDiffProvider as TextDiffProviderBase).ignorePolicy.comparisonPolicy

    private val fragmentsPolicy: InnerFragmentsPolicy
        get() = (myTextDiffProvider as TextDiffProviderBase).highlightPolicy.fragmentsPolicy


    data class MyChange(val isEqual: Boolean, val left: Int, val right: Int) {
        fun add(change: MyChange) = MyChange(isEqual, left + change.left, right + change.right)
    }

    private fun translateChanges(changes: List<MyChange>): Diff.Change {
        val builder = Diff.ChangeBuilder(0)
        for (change in changes) {
            if (change.isEqual) {
                builder.addEqual(change.left)
            } else {
                builder.addChange(change.left, change.right)
            }
        }
        return builder.firstChange
    }

    private fun mergeDiffChain(diffChain: List<MyDiff>): List<MyChange> {
        if (diffChain.isEmpty()) return emptyList()
        val changes = mutableListOf<MyChange>()
        var lastDiff = diffChain.first()
        var lastChange = lastDiff.makeChange()
        for (diff in diffChain.drop(1)) {
            check(diff.isRelativeTo(lastDiff)) { "Gap in diff chain" }
            val change = diff.makeChange()
            if (change.isEqual == lastChange.isEqual) {
                lastChange = lastChange.add(change)
                lastDiff = diff
                continue
            }
            changes.add(lastChange)
            lastChange = change
            lastDiff = diff
        }
        changes.add(lastChange)
        return changes
    }

    private fun DiffRange.lineDelta() = endLine - startLine
    private fun MyDiff.isEqual() = type == DiffType.EQUAL && checkEqualDelta()
    private fun MyDiff.makeChange() = MyChange(isEqual(), left.lineDelta(), right.lineDelta())
    private fun MyDiff.isRelativeTo(other: MyDiff) =
        left.startLine == other.left.endLine && right.startLine == other.right.endLine

    private fun MyDiff.checkEqualDelta(): Boolean {
        check(left.lineDelta() == right.lineDelta()) { "Equal has non equal deltas" }
        return true
    }
}

