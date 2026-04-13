package eu.kanade.tachiyomi.util.system

import com.pushtorefresh.storio.sqlite.operations.delete.PreparedDeleteByQuery
import com.pushtorefresh.storio.sqlite.operations.delete.PreparedDeleteCollectionOfObjects
import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetListOfObjects
import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetObject
import com.pushtorefresh.storio.sqlite.operations.put.PreparedPutCollectionOfObjects
import com.pushtorefresh.storio.sqlite.operations.put.PreparedPutObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

suspend fun <T> PreparedGetListOfObjects<T>.executeOnIO(): List<T> =
    withContext(Dispatchers.IO) { executeAsBlocking() }

suspend fun <T> PreparedGetObject<T>.executeOnIO(): T? =
    withContext(Dispatchers.IO) { executeAsBlocking() }

suspend fun <T> PreparedPutObject<T>.executeOnIO() =
    withContext(Dispatchers.IO) { executeAsBlocking() }

suspend fun PreparedDeleteByQuery.executeOnIO() =
    withContext(Dispatchers.IO) { executeAsBlocking() }

suspend fun <T> PreparedPutCollectionOfObjects<T>.executeOnIO() =
    withContext(Dispatchers.IO) { executeAsBlocking() }

suspend fun <T> PreparedDeleteCollectionOfObjects<T>.executeOnIO() =
    withContext(Dispatchers.IO) { executeAsBlocking() }

fun <T> PreparedGetObject<T>.asFlow(): Flow<T> = callbackFlow {
    val subscription =
        this@asFlow.asRxObservable().subscribe({ trySend(it) }, { close(it) }, { close() })
    awaitClose { subscription.unsubscribe() }
}

fun <T> PreparedGetListOfObjects<T>.asFlow(): Flow<List<T>> = callbackFlow {
    val subscription =
        this@asFlow.asRxObservable().subscribe({ trySend(it) }, { close(it) }, { close() })
    awaitClose { subscription.unsubscribe() }
}
