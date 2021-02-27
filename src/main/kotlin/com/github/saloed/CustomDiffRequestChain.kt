package com.github.saloed

import com.github.saloed.diff.TwoWayDiff
import com.intellij.diff.chains.DiffRequestChainBase
import com.intellij.diff.chains.DiffRequestProducer
import com.intellij.diff.chains.DiffRequestProducerException
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile

class CustomDiffRequestChain(val file: VirtualFile) : DiffRequestChainBase() {
    init {
        putUserData(DiffUserDataKeysEx.FORCE_DIFF_TOOL, CustomDiffTool())
    }

    override fun getRequests(): List<DiffRequestProducer> {
        return listOf(object : DiffRequestProducer {
            override fun getName(): String = "Change"
            override fun process(context: UserDataHolder, indicator: ProgressIndicator): DiffRequest {
                val diff = try {
                    TwoWayDiff.loadFromJson(file.inputStream.bufferedReader(file.charset).readText())
                } catch (e: IllegalArgumentException) {
                    val message = MyBundle.message("diffLoadError", file.name)
                    throw DiffRequestProducerException(message, e)
                }
                return CustomDiffRequest(diff)
            }
        })
    }
}
