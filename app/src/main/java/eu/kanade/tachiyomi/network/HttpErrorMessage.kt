package eu.kanade.tachiyomi.network

/** The error shown for a page image request that fails with an HTTP status. */
fun httpErrorMessage(code: Int, host: String): String = "HTTP $code from $host"
