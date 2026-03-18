import re

with open('./app/src/main/java/eu/kanade/tachiyomi/data/database/resolvers/LibraryMangaGetResolver.kt', 'r') as f:
    content = f.read()

search_fast_path = """            while (startIndex < length) {
                var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
                if (endIndex == -1) {
                    endIndex = length
                }

                val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)
                if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) {
                    val countStr =
                        substring(
                            countSeparatorIndex + Constants.RAW_CHAPTER_COUNT_SEPARATOR.length,
                            endIndex,
                        )
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

search_filter_merged = """        while (startIndex < length) {
            var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
            if (endIndex == -1) {
                endIndex = length
            }

            val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)
            val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)

            val infoEndIndex =
                if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) countSeparatorIndex
                else endIndex

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

with open('./app/src/main/java/eu/kanade/tachiyomi/data/database/resolvers/LibraryMangaGetResolver.kt', 'w') as f:
    f.write(content)
