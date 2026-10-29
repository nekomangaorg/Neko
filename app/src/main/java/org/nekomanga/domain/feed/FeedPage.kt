package org.nekomanga.domain.feed

data class FeedPage(
    val hasNextPage: Boolean,
    val items: List<FeedItem>,
)
