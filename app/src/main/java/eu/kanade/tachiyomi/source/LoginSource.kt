package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.Logout

interface LoginSource {

    fun isLogged(): Boolean

    suspend fun login(username: String, password: String): Boolean

    suspend fun logout(): Logout
}
