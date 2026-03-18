import re

with open('./app/src/main/java/eu/kanade/tachiyomi/data/database/resolvers/LibraryMangaGetResolver.kt', 'r') as f:
    content = f.read()

# Add ChapterGroup and parseChapterGroup helper
helper_code = """    private data class ChapterGroup(val scanlator: String, val uploader: String, val count: Int)

    private fun String.parseChapterGroup(startIndex: Int, endIndex: Int): ChapterGroup {
        val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)
        val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)

        val scanlator: String
        val uploader: String
        val count: Int

        val infoEndIndex =
            if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) countSeparatorIndex
            else endIndex

        if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) {
            val countStr =
                substring(
                    countSeparatorIndex + Constants.RAW_CHAPTER_COUNT_SEPARATOR.length,
                    endIndex,
                )
            count = countStr.toIntOrNull() ?: 1
        } else {
            count = 1
        }

        if (typeSeparatorIndex != -1 && typeSeparatorIndex < infoEndIndex) {
            scanlator = substring(startIndex, typeSeparatorIndex)
            uploader =
                substring(
                    typeSeparatorIndex + Constants.RAW_SCANLATOR_TYPE_SEPARATOR.length,
                    infoEndIndex,
                )
        } else {
            scanlator = substring(startIndex, infoEndIndex)
            uploader = ""
        }
        return ChapterGroup(scanlator, uploader, count)
    }

    private fun String.filterMerged(): Boolean {"""

content = content.replace("    private fun String.filterMerged(): Boolean {", helper_code)

# Replace filterMerged logic
search_filter_merged = """        while (startIndex < length) {
            var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
            if (endIndex == -1) {
                endIndex = length
            }

            val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)
            val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)

            val infoEndIndex = if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) countSeparatorIndex else endIndex

            val scanlator =
                if (typeSeparatorIndex != -1 && typeSeparatorIndex < infoEndIndex) {
                    substring(startIndex, typeSeparatorIndex)
                } else {
                    substring(startIndex, infoEndIndex)
                }

            if (MergeType.containsMergeSourceName(scanlator)) {
                return true
            }

            startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
        }"""

replace_filter_merged = """        while (startIndex < length) {
            var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
            if (endIndex == -1) {
                endIndex = length
            }

            val group = parseChapterGroup(startIndex, endIndex)

            if (MergeType.containsMergeSourceName(group.scanlator)) {
                return true
            }

            startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
        }"""

content = content.replace(search_filter_merged, replace_filter_merged)

# Replace fast path logic
search_fast_path = """            while (startIndex < length) {
                var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
                if (endIndex == -1) {
                    endIndex = length
                }

                val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)
                if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) {
                    val countStr = substring(countSeparatorIndex + Constants.RAW_CHAPTER_COUNT_SEPARATOR.length, endIndex)
                    totalCount += countStr.toIntOrNull() ?: 1
                } else {
                    totalCount++
                }

                startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
            }"""

replace_fast_path = """            while (startIndex < length) {
                var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
                if (endIndex == -1) {
                    endIndex = length
                }

                val group = parseChapterGroup(startIndex, endIndex)
                totalCount += group.count

                startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
            }"""
content = content.replace(search_fast_path, replace_fast_path)

# Replace slow path logic
search_slow_path = """            val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)
            val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)

            val scanlator: String
            val uploader: String
            val currentGroupCount: Int

            val infoEndIndex = if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) countSeparatorIndex else endIndex

            if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) {
                val countStr = substring(countSeparatorIndex + Constants.RAW_CHAPTER_COUNT_SEPARATOR.length, endIndex)
                currentGroupCount = countStr.toIntOrNull() ?: 1
            } else {
                currentGroupCount = 1
            }

            if (typeSeparatorIndex != -1 && typeSeparatorIndex < infoEndIndex) {
                scanlator = substring(startIndex, typeSeparatorIndex)
                uploader =
                    substring(
                        typeSeparatorIndex + Constants.RAW_SCANLATOR_TYPE_SEPARATOR.length,
                        infoEndIndex,
                    )
            } else {
                scanlator = substring(startIndex, infoEndIndex)
                uploader = ""
            }"""

replace_slow_path = """            val group = parseChapterGroup(startIndex, endIndex)
            val scanlator = group.scanlator
            val uploader = group.uploader
            val currentGroupCount = group.count"""

content = content.replace(search_slow_path, replace_slow_path)

with open('./app/src/main/java/eu/kanade/tachiyomi/data/database/resolvers/LibraryMangaGetResolver.kt', 'w') as f:
    f.write(content)
