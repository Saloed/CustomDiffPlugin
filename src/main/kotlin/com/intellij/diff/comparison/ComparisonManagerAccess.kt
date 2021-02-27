package com.intellij.diff.comparison

import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.openapi.progress.ProgressIndicator
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object ComparisonManagerAccess {

    fun getLineContentsHandle(): MethodHandle {
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

    fun createInnerFragmentsHandle(): MethodHandle {
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
}
