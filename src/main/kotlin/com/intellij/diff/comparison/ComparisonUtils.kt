package com.intellij.diff.comparison

import com.intellij.diff.comparison.iterables.FairDiffIterable
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.openapi.progress.ProgressIndicator
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object ComparisonUtils {

    @Suppress("UNCHECKED_CAST")
    fun getLineContents(
        start: Int,
        end: Int,
        text: CharSequence,
        lineOffsets: LineOffsets
    ): List<CharSequence> {
        val lines = createGetLineContentsHandle().invoke(start, end, text, lineOffsets)
        return lines as List<CharSequence>
    }

    @Suppress("UNCHECKED_CAST")
    fun createInnerFragments(
        lineFragments: List<LineFragment>,
        text1: CharSequence,
        text2: CharSequence,
        policy: ComparisonPolicy,
        fragmentsPolicy: InnerFragmentsPolicy,
        indicator: ProgressIndicator
    ): List<LineFragment> {
        val result = createCreateInnerFragmentsHandle()
            .invoke(lineFragments, text1, text2, policy, fragmentsPolicy, indicator)
        return result as List<LineFragment>
    }

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
        return correctChangesSecondStep(leftLines, rightLines, optimized)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLines(text: List<CharSequence>, policy: ComparisonPolicy): List<ByLine.Line> {
        val lines = createGetLinesHandle().invoke(text, policy)
        return lines as List<ByLine.Line>
    }

    private fun optimizeLineChunks(
        lines1: List<ByLine.Line>,
        lines2: List<ByLine.Line>,
        iterable: FairDiffIterable,
        indicator: ProgressIndicator
    ): FairDiffIterable {
        val fairDiffIterable = createOptimizeLineChunksHandle().invoke(lines1, lines2, iterable, indicator)
        return fairDiffIterable as FairDiffIterable
    }

    private fun correctChangesSecondStep(
        lines1: List<ByLine.Line>,
        lines2: List<ByLine.Line>,
        changes: FairDiffIterable
    ): FairDiffIterable {
        val fairDiffIterable = createCorrectChangesSecondStepHandle().invoke(lines1, lines2, changes)
        return fairDiffIterable as FairDiffIterable
    }

    private fun createOptimizeLineChunksHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ByLine::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            FairDiffIterable::class.java,
            java.util.List::class.java,
            java.util.List::class.java,
            FairDiffIterable::class.java,
            ProgressIndicator::class.java
        )
        return lookup.findStatic(ByLine::class.java, "optimizeLineChunks", methodType)
    }

    private fun createCorrectChangesSecondStepHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ByLine::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            FairDiffIterable::class.java,
            java.util.List::class.java,
            java.util.List::class.java,
            FairDiffIterable::class.java
        )
        return lookup.findStatic(ByLine::class.java, "correctChangesSecondStep", methodType)
    }

    private fun createGetLineContentsHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ComparisonManagerImpl::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            java.util.List::class.java,
            Int::class.java,
            Int::class.java,
            java.lang.CharSequence::class.java,
            LineOffsets::class.java
        )
        return lookup.findStatic(ComparisonManagerImpl::class.java, "getLineContents", methodType)
    }


    private fun createCreateInnerFragmentsHandle(): MethodHandle {
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
        return lookup.findStatic(ComparisonManagerImpl::class.java, "createInnerFragments", methodType)
    }

    private fun createGetLinesHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ByLine::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            java.util.List::class.java,
            java.util.List::class.java,
            ComparisonPolicy::class.java
        )
        return lookup.findStatic(ByLine::class.java, "getLines", methodType)
    }
}
