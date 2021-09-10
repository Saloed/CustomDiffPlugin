package com.github.saloed.z3.trace

import com.github.saloed.CustomDiffRequest
import com.github.saloed.CustomDiffTool
import com.github.saloed.diff.*
import com.intellij.diff.chains.DiffRequestChainBase
import com.intellij.diff.chains.DiffRequestProducer
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile
import name.fraser.neil.plaintext.DiffMatchPatch
import name.fraser.neil.plaintext.diff_match_patch.Diff
import name.fraser.neil.plaintext.diff_match_patch.Operation

class Z3TraceDiffRequestChain(val left: VirtualFile, val right: VirtualFile) : DiffRequestChainBase() {
    init {
        putUserData(DiffUserDataKeysEx.FORCE_DIFF_TOOL, CustomDiffTool())
    }

    override fun getRequests(): List<DiffRequestProducer> {
        return listOf(object : DiffRequestProducer {
            override fun getName(): String = "Change"
            override fun process(context: UserDataHolder, indicator: ProgressIndicator): DiffRequest {
                left.refresh(false, false)
                right.refresh(false, false)
                val diff = buildDiff(left, right)
                return CustomDiffRequest(diff)
            }
        })
    }

    companion object {
        private fun VirtualFile.readLines() = inputStream.bufferedReader(charset).readLines()

        private fun buildDiff(left: VirtualFile, right: VirtualFile): TwoWayDiff {

            val lhsSections = makeSections(left.readLines().let { collapseCallAnalyzer(it) })
            val rhsSections = makeSections(right.readLines().let { collapseCallAnalyzer(it) })

            if (lhsSections.size == rhsSections.size && lhsSections.size == 1) {
                val leftSection = lhsSections.first()
                val rightSection = rhsSections.first()
                val leftFile = DiffFile(left.name, leftSection.content)
                val rightFile = DiffFile(right.name, rightSection.content)
                val leftRange = DiffRange(0, 0, leftFile.contentLines.size, 0)
                val rightRange = DiffRange(0, 0, rightFile.contentLines.size, 0)
                val leftDiff = Diff(DiffType.DELETE, leftRange, DiffRange(0, 0, 0, 0))
                val emptyRangeAfterLeft =
                    DiffRange(leftRange.endLine, leftRange.endLineOffset, leftRange.endLine, leftRange.endLineOffset)
                val rightDiff = Diff(DiffType.INSERT, emptyRangeAfterLeft, rightRange)
                return TwoWayDiff(leftFile, rightFile, listOf(leftDiff, rightDiff), DiffMode.LINE)
            }

            val leftHeaders = lhsSections.joinToString("\n") { it.header }
            val rightHeaders = rhsSections.joinToString("\n") { it.header }
            val linesDiff = DiffMatchPatch().diffLineLevel(leftHeaders, rightHeaders)
            val headerDiff = makeHeaderDiff(linesDiff)

            val sectionGroups = groupSections(headerDiff, lhsSections, rhsSections)
            check(sectionGroups.filter { it.left != null }.count() == lhsSections.size) { "Left groups mismatch" }
            check(sectionGroups.filter { it.right != null }.count() == rhsSections.size) { "Right groups mismatch" }
            val diffs = sectionGroups.flatMap { it.asDiff() }
            return twoWayDiffFromDMPDiff(diffs, left.name, right.name, DiffMode.LINE)
        }

        private data class HeaderDiff(val left: String?, val right: String?)


        data class FileSection(val header: String, val content: List<String>) {
            fun contentString() = content.joinToString(separator = "") { "$it\n" }
            fun toDiff(operation: Operation) = listOf(Diff(operation, contentString()))
        }

        private data class SectionGroup(val left: FileSection?, val right: FileSection?) {
            fun asDiff(): List<Diff> = when {
                left != null && right != null -> contentDiff(left, right)
                left == null && right != null -> right.toDiff(Operation.INSERT)
                left != null && right == null -> left.toDiff(Operation.DELETE)
                else -> emptyList()
            }
        }

        private fun contentDiff(left: FileSection, right: FileSection) =
            DiffMatchPatch().diffLineLevel(left.contentString(), right.contentString())

        private val headerLineRegex = Regex("^-------- \\[.+\\] .* ---------$")

        private fun String.isHeaderLine() = headerLineRegex.matches(this)

        private fun Diff.lines() = text.lines().dropLastWhile { it.isEmpty() }

        private fun makeHeaderDiff(diff: List<Diff>): List<HeaderDiff> {
            val result = mutableListOf<HeaderDiff>()
            for (diffItem in diff) {
                val headers = diffItem.lines()
                result += headers.map {
                    when (diffItem.operation) {
                        Operation.EQUAL -> HeaderDiff(it, it)
                        Operation.DELETE -> HeaderDiff(it, null)
                        Operation.INSERT -> HeaderDiff(null, it)
                    }
                }
            }
            return result
        }

        private fun groupSections(
            diff: List<HeaderDiff>,
            leftSections: List<FileSection>,
            rightSections: List<FileSection>
        ): List<SectionGroup> {
            val leftIter = leftSections.iterator()
            val rightIter = rightSections.iterator()
            return diff.map { item ->
                val left = item.left?.let { header ->
                    leftIter.next()
                        .also { check(it.header == header) { "Left header mismatch: $header | ${it.header}" } }
                }
                val right = item.right?.let { header ->
                    rightIter.next()
                        .also { check(it.header == header) { "Right header mismatch: $header | ${it.header}" } }
                }
                SectionGroup(left, right)
            }
        }

        private fun makeSections(lines: List<String>): List<FileSection> {
            val sections = mutableListOf<FileSection>()
            var currentHeader = ""
            val currentSectionContent = mutableListOf<String>()
            var firstSection = true
            for (line in lines) {
                val isHeader = line.isHeaderLine()
                if (isHeader && !firstSection) {
                    sections += FileSection(currentHeader, currentSectionContent.toList())
                    currentHeader = line
                    currentSectionContent.clear()
                }
                if (isHeader && firstSection) {
                    firstSection = false
                    currentHeader = line
                    currentSectionContent.clear()
                }
                currentSectionContent += line
            }
            if (currentSectionContent.isNotEmpty()) {
                sections += FileSection(currentHeader, currentSectionContent.toList())
            }
            return sections
        }

        private val analyzerStart = Regex("^CALL ANALYZER: (\\d+)$")
        private val analyzerEnd = Regex("^RETURN ANALYZER: (\\d+)$")
        private val noCall = -1

        private fun collapseCallAnalyzer(lines: List<String>): List<String> {
            val newLines = mutableListOf<String>()
            var skip = false
            var callId = noCall
            for (line in lines) {
                val startPattern = analyzerStart.find(line)
                val endPattern = analyzerEnd.find(line)
                if (startPattern != null) {
                    if (skip || callId != noCall) error("Nested call")
                    callId = startPattern.groupValues[1].toInt()
                    skip = true
                    continue
                }
                if (endPattern != null) {
                    if (!skip || callId == noCall) error("Return from unknown call")
                    val endCall = endPattern.groupValues[1].toInt()
                    if (endCall != callId) error("Return call mismatch")
                    callId = noCall
                    skip = false
                    continue
                }
                if (skip) continue
                newLines += line
            }
            return newLines
        }
    }
}