package org.nekomanga.usecases.chapters

class ParseChapterNameUseCase {
    operator fun invoke(chapterName: String, pageNumber: String): String {
        val builder = StringBuilder()
        var title = ""
        var vol = ""
        val list = chapterName.split(Regex(" "), 3)

        list.forEach {
            if (it.startsWith("vol.", true)) {
                vol = " Vol." + it.substringAfter(".").padStart(4, '0')
            } else if (it.startsWith("ch.", true)) {
                builder.append(" Ch.")
                builder.append(it.substringAfter(".").padStart(4, '0'))
            } else {
                title = " $it"
            }
        }

        if (vol.isNotBlank()) {
            builder.append(vol)
        }
        builder.append(" Pg.")
        builder.append(pageNumber.padStart(4, '0'))

        if (title.isNotEmpty()) {
            builder.append(title.take(200))
        }

        return builder.toString().trim()
    }
}
