package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.system.SideNavMode

data class LibraryScreenState(
    val firstLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val incognitoMode: Boolean = false,
)

data class LibraryScreenActions(
    /* val mangaClick: (Long) -> Unit,*/
    val search: (String?) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
)
