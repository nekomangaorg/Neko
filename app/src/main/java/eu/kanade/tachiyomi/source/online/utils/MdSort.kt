package eu.kanade.tachiyomi.source.online.utils

enum class MdSort(val displayName: String, val key: String) {
    latest("Latest Uploaded chapter (Any language)", MdConstants.Sort.latest),
    relevance("Relevance", MdConstants.Sort.relevance),
    followCount("Number of follows", MdConstants.Sort.followCount),
    createdAt("Created at", MdConstants.Sort.createdAt),
    updatedAt("Manga info updated", MdConstants.Sort.updatedAt),
    title("Title", MdConstants.Sort.title),
    rating("Rating", MdConstants.Sort.rating),
}
