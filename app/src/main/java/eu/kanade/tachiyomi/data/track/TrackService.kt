package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import rx.Completable
import rx.Observable
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Int) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    abstract val name: String

    @DrawableRes
    abstract fun getLogo(): Int

    abstract fun getLogoColor(): Int

    open fun getStatusList(): List<Int> = throw Exception("Not used")

    open fun getStatus(status: Int): String = throw Exception("Not used")

    open fun getScoreList(): List<String> = throw Exception("Not used")

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    open fun displayScore(track: Track): String = throw Exception("Not used")

    open fun add(track: Track): Observable<Track> = throw Exception("Not used")

    open fun update(track: Track): Observable<Track> = throw Exception("Not used")

    open fun bind(track: Track): Observable<Track> = throw Exception("Not used")

    open fun search(query: String): Observable<List<TrackSearch>> = throw Exception("Not used")

    open fun refresh(track: Track): Observable<Track> = throw Exception("Not used")

    open fun login(username: String, password: String): Completable = throw Exception("Not used")

    open fun isTracker() = true

    open fun isMdList() = false

    open fun isExternalLink() = false

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open val isLogged: Boolean
        get() = getUsername().isNotEmpty() &&
                getPassword().isNotEmpty()

    fun getUsername() = preferences.trackUsername(this)!!

    fun getPassword() = preferences.trackPassword(this)!!

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }
}
