package com.intellij.diff.comparison

import com.intellij.diff.comparison.iterables.FairDiffIterable
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.openapi.progress.ProgressIndicator

object ComparisonUtils {

    @Suppress("UNCHECKED_CAST")
    fun getLineContents(
        start: Int,
        end: Int,
        text: CharSequence,
        lineOffsets: LineOffsets
    ) = ComparisonManagerAccess.getLineContentsHandle()
        .invoke(start, end, text, lineOffsets) as List<CharSequence>

    @Suppress("UNCHECKED_CAST")
    fun createInnerFragments(
        lineFragments: List<LineFragment>,
        text1: CharSequence,
        text2: CharSequence,
        policy: ComparisonPolicy,
        fragmentsPolicy: InnerFragmentsPolicy,
        indicator: ProgressIndicator
    ) = ComparisonManagerAccess.createInnerFragmentsHandle()
        .invoke(lineFragments, text1, text2, policy, fragmentsPolicy, indicator) as List<LineFragment>

    fun correctAndOptimizeChanges(
        left: List<CharSequence>,
        right: List<CharSequence>,
        changes: FairDiffIterable,
        policy: ComparisonPolicy,
        indicator: ProgressIndicator
    ): FairDiffIterable {
        val leftLines = getLines(left, policy)
        val rightLines = getLines(right, policy)
        val optimized = optimizeLineChunks(leftLines, rightLines, changes, indicator)
        return if (policy == ComparisonPolicy.IGNORE_WHITESPACES) {
            expandRanges(leftLines, rightLines, optimized)
        } else {
            correctChangesSecondStep(leftLines, rightLines, optimized)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLines(
        text: List<CharSequence>,
        policy: ComparisonPolicy
    ) = ByLineAccess.createGetLinesHandle()
        .invoke(text, policy) as List<ByLine.Line>

    private fun optimizeLineChunks(
        lines1: List<ByLine.Line>,
        lines2: List<ByLine.Line>,
        iterable: FairDiffIterable,
        indicator: ProgressIndicator
    ) = ByLineAccess.createOptimizeLineChunksHandle()
        .invoke(lines1, lines2, iterable, indicator) as FairDiffIterable

    private fun correctChangesSecondStep(
        lines1: List<ByLine.Line>,
        lines2: List<ByLine.Line>,
        changes: FairDiffIterable
    ) = ByLineAccess.createCorrectChangesSecondStepHandle()
        .invoke(lines1, lines2, changes) as FairDiffIterable

    private fun expandRanges(
        lines1: List<ByLine.Line>,
        lines2: List<ByLine.Line>,
        iterable: FairDiffIterable
    ) = ByLineAccess.createExpandRangesHandle()
        .invoke(lines1, lines2, iterable) as FairDiffIterable
}
