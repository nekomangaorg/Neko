package eu.kanade.tachiyomi.source.online.utils

enum class MdSort(val displayName: String, val key: String) {
    latest("Latest uploaded (Any language)", MdConstants.Sort.latest),
    relevance("Relevance", MdConstants.Sort.relevance),
    followCount("Number of follows", MdConstants.Sort.followCount),
    createdAt("Recently added", MdConstants.Sort.createdAt),
    updatedAt("Information updated", MdConstants.Sort.updatedAt),
    title("Title", MdConstants.Sort.title),
    rating("Rating", MdConstants.Sort.rating),
    year("Year", MdConstants.Sort.year)
}
