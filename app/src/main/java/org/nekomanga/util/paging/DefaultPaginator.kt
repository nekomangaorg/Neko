package org.nekomanga.util.paging

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import org.nekomanga.domain.network.ResultError

class DefaultPaginator<Key, Item>(
    private val initialKey: Key,
    private inline val onLoadUpdated: (Boolean) -> Unit,
    private inline val onRequest:
        suspend (nextKey: Key) -> Result<Pair<Boolean, List<Item>>, ResultError>,
    private inline val getNextKey: suspend (List<Item>) -> Key,
    private inline val onError: suspend (ResultError?) -> Unit,
    private inline val onSuccess:
        suspend (hasNextPage: Boolean, items: List<Item>, newKey: Key) -> Unit,
) : Paginator<Key, Item> {

    private var currentKey: Key = initialKey
    private var isMakingRequest = false

    override suspend fun loadNextItems() {
        if (isMakingRequest) return

        isMakingRequest = true
        onLoadUpdated(true)
        val result = onRequest(currentKey)
        isMakingRequest = false

        result
            .onSuccess { (hasNextPage, items) ->
                currentKey = getNextKey(items)
                onSuccess(hasNextPage, items, currentKey)
                onLoadUpdated(false)
            }
            .onFailure {
                onError(it)
                onLoadUpdated(false)
            }
    }

    override fun reset() {
        currentKey = initialKey
    }
}
