import re

with open('./app/src/main/java/eu/kanade/tachiyomi/data/database/resolvers/LibraryMangaGetResolver.kt', 'r') as f:
    content = f.read()

search_slow_path = """            val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)
            val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)

            val scanlator: String
            val uploader: String
            val currentGroupCount: Int

            val infoEndIndex =
                if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) countSeparatorIndex
                else endIndex

            if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) {
                val countStr =
                    substring(
                        countSeparatorIndex + Constants.RAW_CHAPTER_COUNT_SEPARATOR.length,
                        endIndex,
                    )
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
