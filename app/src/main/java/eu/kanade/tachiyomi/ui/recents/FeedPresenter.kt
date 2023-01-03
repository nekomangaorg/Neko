package eu.kanade.tachiyomi.ui.recents

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedPresenter(
    val preferences: PreferencesHelper = Injekt.get(),
    private val feedRepository: FeedRepository = Injekt.get(),

    ) : BaseCoroutinePresenter<FeedController>() {

    private val _feedScreenState = MutableStateFlow(
        FeedScreenState(
            outlineCovers = preferences.outlineOnCovers().get(),
            incognitoMode = preferences.incognitoMode().get(),
            groupChaptersUpdates = preferences.groupChaptersUpdates().get(),
        ),
    )
    val feedScreenState: StateFlow<FeedScreenState> = _feedScreenState.asStateFlow()

    private val paginator = DefaultPaginator(
        initialKey = _feedScreenState.value.offset,
        onLoadUpdated = { },
        onRequest = {
            feedRepository.getPage(_feedScreenState.value.offset, _feedScreenState.value.feedScreenType)
        },
        getNextKey = {
            _feedScreenState.value.offset + ENDLESS_LIMIT
        },
        onError = {
            //TODO
        },
        onSuccess = { hasNextPage, items, newKey ->
            _feedScreenState.update { state ->
                state.copy(
                    allFeedChapters = (state.allFeedChapters + items).toImmutableList(),
                    offset = newKey,
                    hasMoreResults = hasNextPage,
                )
            }

        },
    )

    override fun onCreate() {
        super.onCreate()

        if (_feedScreenState.value.allFeedChapters.size == 0) {
            loadNextPage()
        }

        presenterScope.launch {
            _feedScreenState.update {
                it.copy(sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()))
            }
        }

        presenterScope.launch {
            preferences.incognitoMode().asFlow().collectLatest {
                _feedScreenState.update { state ->
                    state.copy(incognitoMode = it)
                }
            }
        }
        presenterScope.launch {
            preferences.groupChaptersUpdates().asFlow().collectLatest {
                _feedScreenState.update { state ->
                    state.copy(groupChaptersUpdates = it)
                }
            }
        }
    }

    fun loadNextPage() {
        presenterScope.launchIO {
            paginator.loadNextItems()
        }
    }

    fun toggleGroupChaptersUpdates() {
        presenterScope.launch {
            preferences.groupChaptersUpdates().set(!preferences.groupChaptersUpdates().get())
        }
    }

    fun toggleIncognitoMode() {
        presenterScope.launch {
            preferences.incognitoMode().set(!preferences.incognitoMode().get())
        }
    }

    companion object {
        const val ENDLESS_LIMIT = 50
    }
}
