package com.intellij.diff.comparison

import com.intellij.diff.comparison.iterables.FairDiffIterable
import com.intellij.openapi.progress.ProgressIndicator
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object ByLineAccess {

    fun createOptimizeLineChunksHandle(): MethodHandle {
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

    fun createCorrectChangesSecondStepHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ByLine::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            FairDiffIterable::class.java,
            java.util.List::class.java,
            java.util.List::class.java,
            FairDiffIterable::class.java
        )
        return lookup.findStatic(ByLine::class.java, "correctChangesSecondStep", methodType)
    }

    fun createGetLinesHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ByLine::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            java.util.List::class.java,
            java.util.List::class.java,
            ComparisonPolicy::class.java
        )
        return lookup.findStatic(ByLine::class.java, "getLines", methodType)
    }

    fun createExpandRangesHandle(): MethodHandle {
        val lookup = MethodHandles.privateLookupIn(ByLine::class.java, MethodHandles.lookup())
        val methodType = MethodType.methodType(
            FairDiffIterable::class.java,
            java.util.List::class.java,
            java.util.List::class.java,
            FairDiffIterable::class.java
        )
        return lookup.findStatic(ByLine::class.java, "expandRanges", methodType)
    }
}
