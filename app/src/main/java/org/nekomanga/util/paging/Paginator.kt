package org.nekomanga.util.paging

interface Paginator<Key, Item> {
    suspend fun loadNextItems()

    fun reset()
}
