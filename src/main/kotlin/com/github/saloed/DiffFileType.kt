package com.github.saloed

import com.intellij.json.JsonFileType

object DiffFileType : JsonFileType() {
    const val FILE_EXTENSION = "jdiff"

    override fun getName(): String = "JSON Diff"

    override fun getDescription(): String = "Diff in JSON format"

    override fun getDefaultExtension(): String = FILE_EXTENSION
}
