package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.ui.base.activity.BaseMainActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.lang.orUnknownError
import eu.kanade.tachiyomi.util.storage.getUriWithAuthority
import eu.kanade.tachiyomi.util.system.contextCompatDrawable
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.hasSideNavBar
import eu.kanade.tachiyomi.util.system.ignoredSystemInsets
import eu.kanade.tachiyomi.util.system.isBottomTappable
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.system.setThemeByPref
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.util.view.snack
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.hideChapterTitle
import org.nekomanga.domain.manga.isLongStrip
import org.nekomanga.domain.manga.orientationType
import org.nekomanga.domain.manga.readingModeType
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.extensions.collectAsState as preferenceCollectAsState
import org.nekomanga.presentation.screens.reader.GestureNavigationOverlay
import org.nekomanga.presentation.screens.reader.PageNumberIndicator
import org.nekomanga.presentation.screens.reader.ReaderAppBar
import org.nekomanga.presentation.screens.reader.ReaderBottomControls
import org.nekomanga.presentation.screens.reader.ReaderChaptersSheet
import org.nekomanga.presentation.screens.reader.ReaderPageAction
import org.nekomanga.presentation.screens.reader.ReaderPageActionsSheet
import org.nekomanga.presentation.screens.reader.ReaderSettingsSheet
import org.nekomanga.presentation.screens.reader.viewer.ComposePagerViewer
import org.nekomanga.presentation.screens.reader.viewer.ComposeWebtoonViewer
import org.nekomanga.presentation.theme.NekoTheme
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Activity containing the reader of Tachiyomi. This activity is mostly a container of the viewers,
 * to which calls from the view model or UI events are delegated.
 */
class ReaderActivity : BaseMainActivity() {

    val preferences: PreferencesHelper by injectLazy()
    val readerPreferences: ReaderPreferences by injectLazy()
    val securityPreferences: SecurityPreferences by injectLazy()
    val networkPreferences: NetworkPreferences by injectLazy()
    val libraryPreferences: LibraryPreferences by injectLazy()
    val mangaDetailsPreferences: MangaDetailsPreferences by injectLazy()

    val viewModel by viewModels<ReaderViewModel>()

    var overlayNavigation: ViewerNavigation?
        get() = viewModel.state.value.overlayNavigation
        set(value) {
            viewModel.setOverlayNavigation(
                value,
                overlayVisible,
                overlayIsLtr,
                overlayInvertMode,
            )
        }

    var overlayVisible: Boolean
        get() = viewModel.state.value.overlayVisible
        set(value) = viewModel.setOverlayVisibility(value)

    var overlayIsLtr: Boolean
        get() = viewModel.state.value.overlayIsLtr
        set(value) {
            viewModel.setOverlayNavigation(
                overlayNavigation,
                overlayVisible,
                value,
                overlayInvertMode,
            )
        }

    var overlayInvertMode: ViewerNavigation.TappingInvertMode
        get() = viewModel.state.value.overlayInvertMode
        set(value) {
            viewModel.setOverlayNavigation(
                overlayNavigation,
                overlayVisible,
                overlayIsLtr,
                value,
            )
        }

    var chapterTitle: String
        get() = viewModel.state.value.chapterTitle
        set(value) = viewModel.setChapterTitle(value)

    var showShiftDoublePage: Boolean
        get() = viewModel.state.value.showShiftDoublePage
        set(value) = viewModel.setShiftDoublePageState(value, shiftDoublePageIconRes)

    var shiftDoublePageIconRes: Int?
        get() = viewModel.state.value.shiftDoublePageIconRes
        set(value) = viewModel.setShiftDoublePageState(showShiftDoublePage, value)

    var settingsSheetVisible: Boolean
        get() = viewModel.state.value.settingsSheetVisible
        set(value) = viewModel.setSettingsSheetVisibility(value)

    var chaptersSheetVisible: Boolean
        get() = viewModel.state.value.chaptersSheetVisible
        set(value) = viewModel.setChaptersSheetVisibility(value)

    var pageActionsPage: Pair<ReaderPage, ReaderPage?>?
        get() = viewModel.state.value.pageActionsPage
        set(value) = viewModel.setPageActionsPage(value)

    var brightnessOverlayAlpha: Float
        get() = viewModel.state.value.brightnessOverlayAlpha
        set(value) = viewModel.setBrightnessOverlayAlpha(value)

    var colorFilterOverlayColor: Int
        get() = viewModel.state.value.colorFilterOverlayColor
        set(value) = viewModel.setColorFilterOverlay(value, colorFilterOverlayMode)

    var colorFilterOverlayMode: Int
        get() = viewModel.state.value.colorFilterOverlayMode
        set(value) = viewModel.setColorFilterOverlay(colorFilterOverlayColor, value)

    val scope = lifecycleScope

    /** Viewer used to display the pages (pager, webtoon, ...). */
    var viewer by mutableStateOf<BaseViewer?>(null)

    /** Whether the menu is currently visible. */
    var menuVisible = false
        private set

    /** Whether the menu should stay visible. */
    private var menuStickyVisible = false
        set(value) {
            field = value
            viewModel.setMenuStickyVisibility(value)
        }

    private var coroutine: Job? = null

    private var fromUrl = false

    /** Configuration at reader level, like background color or forced orientation. */
    private var config: ReaderConfig? = null

    var sheetManageNavColor = false

    private val wic by lazy { WindowInsetsControllerCompat(window, window.decorView) }
    var lastVis = false

    private var snackbar: Snackbar? = null

    private var intentPageNumber: Int? = null

    var isLoading = false

    private var lastShiftDoubleState: Boolean? = null
    private var indexPageToShift: Int? = null
    private var indexChapterToShift: Long? = null

    private var lastCropRes = 0

    val isSplitScreen: Boolean
        get() = isInMultiWindowMode

    var didTransistionFromChapter = false
    var visibleChapterRange = longArrayOf()
    private var backPressedCallback: OnBackPressedCallback? = null

    private val backCallback = {
        if (chaptersSheetVisible) {
            chaptersSheetVisible = false
        } else if (settingsSheetVisible) {
            settingsSheetVisible = false
        } else if (pageActionsPage != null) {
            pageActionsPage = null
        }
        reEnableBackPressedCallBack()
    }

    var isScrollingThroughPagesOrChapters = false

    val decimalFormat by lazy {
        DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
    }

    companion object {

        const val SHIFT_DOUBLE_PAGES = "shiftingDoublePages"
        const val SHIFTED_PAGE_INDEX = "shiftedPageIndex"
        const val SHIFTED_CHAP_INDEX = "shiftedChapterIndex"

        const val TRANSITION_NAME = "${BuildConfig.APPLICATION_ID}.TRANSITION_NAME"
        const val VISIBLE_CHAPTERS = "${BuildConfig.APPLICATION_ID}.VISIBLE_CHAPTERS"

        fun newIntent(context: Context, manga: Manga, chapter: Chapter): Intent {
            return newIntent(context, manga.id, chapter.id)
        }

        fun newIntent(context: Context, mangaId: Long?, chapterId: Long?): Intent {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("manga", mangaId)
            intent.putExtra("chapter", chapterId)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }
    }

    /** Called when the activity is created. Initializes the view model and configuration. */
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeByPref(preferences)
        super.onCreate(savedInstanceState)

        setContent {
            NekoTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val readerPreferences: ReaderPreferences = remember { Injekt.get() }
                val readerTheme by readerPreferences.readerTheme().preferenceCollectAsState()
                val themeBackground = MaterialTheme.colorScheme.background
                val backgroundColor =
                    remember(readerTheme, themeBackground) {
                        ReaderTheme.fromPreference(readerTheme).color(themeBackground)
                    }
                Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
                    // Native Compose Viewers
                    val currentViewer = viewer
                    val items =
                        state.viewerItems.ifEmpty {
                            when (currentViewer) {
                                is PagerViewer -> currentViewer.items
                                is WebtoonViewer -> currentViewer.items
                                else -> emptyList()
                            }
                        }
                    if (currentViewer is PagerViewer && items.isNotEmpty()) {
                        ComposePagerViewer(
                            viewer = currentViewer,
                            items = items,
                            isRtl = currentViewer is R2LPagerViewer,
                            isVertical = currentViewer is VerticalPagerViewer,
                            manga = viewModel.manga,
                            downloadManager = Injekt.get<DownloadManager>(),
                            onPageSelected = { page, hasExtraPage ->
                                onPageSelected(page, hasExtraPage)
                            },
                            onTransitionSelected = { transition ->
                                onTransitionSelected(transition)
                            },
                            onRetryTransition = { chapter -> requestPreloadChapter(chapter) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (currentViewer is WebtoonViewer && items.isNotEmpty()) {
                        ComposeWebtoonViewer(
                            viewer = currentViewer,
                            items = items,
                            manga = viewModel.manga,
                            downloadManager = Injekt.get<DownloadManager>(),
                            onPageSelected = { page -> onPageSelected(page, false) },
                            onTransitionSelected = { transition ->
                                onTransitionSelected(transition)
                            },
                            onRetryTransition = { chapter -> requestPreloadChapter(chapter) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Color Filter Overlay
                    if (state.colorFilterOverlayColor != 0) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize().drawWithContent {
                                    drawRect(
                                        color =
                                            androidx.compose.ui.graphics.Color(
                                                state.colorFilterOverlayColor
                                            ),
                                        blendMode =
                                            when (state.colorFilterOverlayMode) {
                                                1 -> BlendMode.Multiply
                                                2 -> BlendMode.Screen
                                                3 -> BlendMode.Overlay
                                                4 -> BlendMode.Lighten
                                                5 -> BlendMode.Darken
                                                else -> BlendMode.SrcOver
                                            },
                                    )
                                }
                        )
                    }

                    // Brightness Overlay
                    if (state.brightnessOverlayAlpha > 0f) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Color.Black.copy(
                                            alpha = state.brightnessOverlayAlpha
                                        )
                                    )
                        )
                    }

                    GestureNavigationOverlay(
                        navigation = state.overlayNavigation,
                        isLtr = state.overlayIsLtr,
                        invertMode = state.overlayInvertMode,
                        visible = state.overlayVisible,
                        onDismiss = { overlayVisible = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                    ReaderAppBar(
                        title =
                            state.manga?.userTitle?.takeIf { it.isNotBlank() }
                                ?: state.manga?.title
                                ?: "",
                        subtitle = state.chapterTitle,
                        onBack = { finish() },
                        showShiftDoublePage = state.showShiftDoublePage,
                        shiftDoublePageIconRes = state.shiftDoublePageIconRes,
                        onShiftDoublePage = { shiftDoublePages() },
                        visible = state.menuVisible || state.menuStickyVisible,
                        onMangaClick = {
                            if (fromUrl) {
                                viewModel.manga?.id?.let { id ->
                                    val intent =
                                        MainActivity.openMangaIntent(this@ReaderActivity, id)
                                            .apply {
                                                flags =
                                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            }
                                    startActivity(intent)
                                    finishAfterTransition()
                                }
                            } else {
                                finish()
                            }
                        },
                    )
                    val enabledButtons by
                        readerPreferences.readerBottomButtons().preferenceCollectAsState()
                    val isCommentsVisible = ReaderBottomButton.Comment.isIn(enabledButtons)
                    val isWebViewVisible = ReaderBottomButton.WebView.isIn(enabledButtons)
                    val isChaptersVisible = ReaderBottomButton.ViewChapters.isIn(enabledButtons)
                    val isReadingModeVisible = ReaderBottomButton.ReadingMode.isIn(enabledButtons)
                    val isRotationVisible = ReaderBottomButton.Rotation.isIn(enabledButtons)
                    val isCropBordersVisible =
                        if (viewer is PagerViewer) {
                            ReaderBottomButton.CropBordersPaged.isIn(enabledButtons)
                        } else {
                            ReaderBottomButton.CropBordersWebtoon.isIn(enabledButtons)
                        }
                    val isGrayscaleVisible = ReaderBottomButton.Grayscale.isIn(enabledButtons)
                    val isDoublePageVisible =
                        (viewer is PagerViewer) &&
                            ReaderBottomButton.PageLayout.isIn(enabledButtons)
                    val isShiftPageVisible =
                        ((viewer as? PagerViewer)?.config?.doublePages ?: false) &&
                            canShowSplitAtBottom()
                    val isSettingsVisible = true

                    val cropBordersPref =
                        if (
                            (viewer as? WebtoonViewer)?.hasMargins == true ||
                                (viewer is PagerViewer)
                        ) {
                            readerPreferences.cropBorders()
                        } else {
                            readerPreferences.cropBordersWebtoon()
                        }
                    val cropBorders by cropBordersPref.preferenceCollectAsState()
                    val grayscale by readerPreferences.grayscale().preferenceCollectAsState()

                    val viewerMode =
                        ReadingModeType.fromPreference(state.manga?.readingModeType ?: 0)
                    val readingModeIconRes = viewerMode.iconRes

                    val defaultOrientation by
                        readerPreferences.defaultOrientationType().preferenceCollectAsState()
                    val orientation =
                        OrientationType.fromPreference(
                            state.manga?.orientationType ?: defaultOrientation
                        )
                    val rotationIconRes = orientation.iconRes

                    val pageLayout by readerPreferences.pageLayout().preferenceCollectAsState()
                    val isDoublePage =
                        pageLayout == PageLayout.DOUBLE_PAGES.value ||
                            (pageLayout == PageLayout.AUTOMATIC.value &&
                                (viewer as? PagerViewer)?.config?.doublePages ?: false)
                    val doublePageIconRes =
                        when {
                            isDoublePage -> R.drawable.ic_book_open_variant_24dp
                            (viewer as? PagerViewer)?.config?.splitPages == true ->
                                R.drawable.ic_book_open_split_24dp
                            else -> R.drawable.ic_single_page_24dp
                        }

                    val shiftPageIconRes =
                        shiftDoublePageIconRes ?: R.drawable.ic_page_next_outline_24dp

                    val onReadingModeClick: () -> Unit = {
                        val nextMode =
                            ReadingModeType.getNextReadingMode(state.manga?.readingModeType ?: 0)
                        viewModel.setMangaReadingMode(nextMode.flagValue)
                    }

                    val onRotationClick: () -> Unit = {
                        val currentRot =
                            OrientationType.fromPreference(
                                state.manga?.orientationType ?: defaultOrientation
                            )
                        val allRotations = OrientationType.entries
                        val nextRot =
                            allRotations[(allRotations.indexOf(currentRot) + 1) % allRotations.size]
                        viewModel.setMangaOrientationType(nextRot.flagValue)
                    }

                    val onCropBordersClick: () -> Unit = { cropBordersPref.toggle() }

                    val onGrayscaleClick: () -> Unit = { readerPreferences.grayscale().toggle() }

                    val onDoublePageClick: () -> Unit = {
                        if (readerPreferences.pageLayout().get() == PageLayout.AUTOMATIC.value) {
                            (viewer as? PagerViewer)?.config?.let { config ->
                                config.doublePages = !config.doublePages
                                reloadChapters(config.doublePages, true)
                            }
                        } else {
                            showPageLayoutMenu()
                        }
                    }

                    val onShiftPageClick: () -> Unit = { shiftDoublePages() }

                    ReaderBottomControls(
                        currentPageText = state.currentPageText,
                        totalPagesText = state.totalPagesText,
                        currentPageIndex = state.currentPageIndex,
                        totalPages = state.totalPages,
                        isRtl = viewer is R2LPagerViewer,
                        onPageChange = { index -> moveToPageIndex(index, animated = false) },
                        onSkipPrevious = { loadAdjacentChapter(false) },
                        onSkipNext = { loadAdjacentChapter(true) },
                        visible =
                            state.menuVisible &&
                                !state.chaptersSheetVisible &&
                                !state.settingsSheetVisible,
                        isLoading = state.isLoadingAdjacentChapter,
                        pageNumberVisible = state.pageNumberVisible,
                        isChaptersVisible = isChaptersVisible,
                        isCommentsVisible = isCommentsVisible,
                        isWebViewVisible = isWebViewVisible,
                        isReadingModeVisible = isReadingModeVisible,
                        isRotationVisible = isRotationVisible,
                        isCropBordersVisible = isCropBordersVisible,
                        isGrayscaleVisible = isGrayscaleVisible,
                        isDoublePageVisible = isDoublePageVisible,
                        isShiftPageVisible = isShiftPageVisible,
                        isSettingsVisible = isSettingsVisible,
                        cropBorders = cropBorders,
                        grayscale = grayscale,
                        readingModeIconRes = readingModeIconRes,
                        rotationIconRes = rotationIconRes,
                        doublePageIconRes = doublePageIconRes,
                        shiftPageIconRes = shiftPageIconRes,
                        onChaptersClick = {
                            chaptersSheetVisible = true
                            reEnableBackPressedCallBack()
                        },
                        onCommentsClick = { openWebView(true) },
                        onWebviewClick = { openWebView(false) },
                        onReadingModeClick = onReadingModeClick,
                        onRotationClick = onRotationClick,
                        onCropBordersClick = onCropBordersClick,
                        onGrayscaleClick = onGrayscaleClick,
                        onDoublePageClick = onDoublePageClick,
                        onShiftPageClick = onShiftPageClick,
                        onSettingsClick = {
                            settingsSheetVisible = true
                            reEnableBackPressedCallBack()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    if (state.settingsSheetVisible) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                settingsSheetVisible = false
                                reEnableBackPressedCallBack()
                            },
                            sheetState =
                                rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        ) {
                            val hasCutout =
                                window.decorView.rootWindowInsets?.let { insets ->
                                    if (
                                        android.os.Build.VERSION.SDK_INT >=
                                            android.os.Build.VERSION_CODES.P
                                    ) {
                                        insets.displayCutout?.safeInsetTop != null ||
                                            insets.displayCutout?.safeInsetBottom != null
                                    } else false
                                } ?: false
                            ReaderSettingsSheet(
                                manga = viewModel.manga,
                                hasMargins = (viewer as? WebtoonViewer)?.hasMargins ?: false,
                                hasCutout = hasCutout,
                                onReadingModeChange = { readingMode ->
                                    viewModel.setMangaReadingMode(readingMode.flagValue)
                                },
                                onOrientationChange = { orientation ->
                                    viewModel.setMangaOrientationType(orientation.flagValue)
                                },
                                onOpenReaderSettings = {
                                    val intent =
                                        MainActivity.openReaderSettings(this@ReaderActivity)
                                    startActivity(intent)
                                },
                                onDismiss = {
                                    settingsSheetVisible = false
                                    reEnableBackPressedCallBack()
                                },
                            )
                        }
                    }
                    if (state.chaptersSheetVisible) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                chaptersSheetVisible = false
                                reEnableBackPressedCallBack()
                            },
                            sheetState =
                                rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        ) {
                            // Load chapters when visible
                            LaunchedEffect(state.chaptersSheetVisible) {
                                if (state.chaptersSheetVisible) {
                                    viewModel.getChapters()
                                }
                            }

                            ReaderChaptersSheet(
                                chapters = state.chapters,
                                isChaptersEnabled = isChaptersVisible,
                                isCommentsEnabled = isCommentsVisible,
                                isWebViewEnabled = isWebViewVisible,
                                isReadingModeEnabled = isReadingModeVisible,
                                isRotationEnabled = isRotationVisible,
                                isCropBordersEnabled = isCropBordersVisible,
                                isGrayscaleEnabled = isGrayscaleVisible,
                                isDoublePageEnabled = isDoublePageVisible,
                                isShiftPageEnabled = isShiftPageVisible,
                                cropBorders = cropBorders,
                                grayscale = grayscale,
                                readingModeIconRes = readingModeIconRes,
                                rotationIconRes = rotationIconRes,
                                doublePageIconRes = doublePageIconRes,
                                shiftPageIconRes = shiftPageIconRes,
                                onChapterClick = { item, index ->
                                    if (
                                        item.chapter.id !=
                                            viewModel.getCurrentChapter()?.chapter?.id
                                    ) {
                                        isScrollingThroughPagesOrChapters = true
                                        lifecycleScope.launch {
                                            loadChapter(item.chapter)
                                            chaptersSheetVisible = false
                                            reEnableBackPressedCallBack()
                                        }
                                    } else {
                                        chaptersSheetVisible = false
                                        reEnableBackPressedCallBack()
                                    }
                                },
                                onBookmarkClick = { item ->
                                    viewModel.toggleBookmark(item.chapter)
                                    lifecycleScope.launch { viewModel.getChapters() }
                                },
                                onCommentsClick = {
                                    openWebView(true)
                                    chaptersSheetVisible = false
                                    reEnableBackPressedCallBack()
                                },
                                onWebviewClick = {
                                    openWebView(false)
                                    chaptersSheetVisible = false
                                    reEnableBackPressedCallBack()
                                },
                                onReadingModeClick = onReadingModeClick,
                                onRotationClick = onRotationClick,
                                onCropBordersClick = onCropBordersClick,
                                onGrayscaleClick = onGrayscaleClick,
                                onDoublePageClick = onDoublePageClick,
                                onShiftPageClick = onShiftPageClick,
                                onDisplayOptionsClick = {
                                    settingsSheetVisible = true
                                    chaptersSheetVisible = false
                                    reEnableBackPressedCallBack()
                                },
                                onDismiss = {
                                    chaptersSheetVisible = false
                                    reEnableBackPressedCallBack()
                                },
                            )
                        }
                    }
                    if (state.pageActionsPage != null) {
                        val pages = state.pageActionsPage
                        val page = pages?.first
                        val extraPage = pages?.second
                        if (page != null) {
                            ModalBottomSheet(
                                onDismissRequest = {
                                    pageActionsPage = null
                                    reEnableBackPressedCallBack()
                                },
                                sheetState =
                                    rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            ) {
                                ReaderPageActionsSheet(
                                    hasExtraPage = extraPage != null,
                                    onActionClicked = { action ->
                                        handlePageAction(action, page, extraPage)
                                    },
                                    onDismiss = {
                                        pageActionsPage = null
                                        reEnableBackPressedCallBack()
                                    },
                                )
                            }
                        }
                    }
                    if (
                        state.pageNumberVisible &&
                            state.currentPageText.isNotEmpty() &&
                            state.totalPagesText.isNotEmpty()
                    ) {
                        val pageNumberText =
                            if (resources.isLTR) {
                                "${state.currentPageText}/${state.totalPagesText}"
                            } else {
                                "${state.totalPagesText}/${state.currentPageText}"
                            }
                        PageNumberIndicator(
                            text = pageNumberText,
                            modifier =
                                Modifier.align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = Size.smedium),
                        )
                    }
                    AnimatedVisibility(
                        visible = state.isLoading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f)
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                }
            }
        }

        val a = obtainStyledAttributes(intArrayOf(android.R.attr.windowLightStatusBar))
        val lightStatusBar = a.getBoolean(0, false)
        a.recycle()
        setNotchCutoutMode()

        wic.isAppearanceLightStatusBars = lightStatusBar
        wic.isAppearanceLightNavigationBars = lightStatusBar

        backPressedCallback = onBackPressedDispatcher.addCallback { backCallback() }
        if (viewModel.needsInit()) {
            fromUrl = handleIntentAction(intent)
            if (!fromUrl) {
                val manga = intent.extras!!.getLong("manga", -1)
                val chapter = intent.extras!!.getLong("chapter", -1)
                if (manga == -1L || chapter == -1L) {
                    finish()
                    return
                }
                lifecycleScope.launchNonCancellable {
                    val initResult = viewModel.init(manga, chapter)
                    if (!initResult.getOrDefault(false)) {
                        val exception =
                            initResult.exceptionOrNull() ?: IllegalStateException("Unknown err")
                        withUIContext { setInitialChapterError(exception) }
                    }
                }
            } else {
                viewModel.setIsLoading(true)
            }
        }

        if (savedInstanceState != null) {
            menuVisible = savedInstanceState.getBoolean(::menuVisible.name)
            lastShiftDoubleState =
                savedInstanceState.getBoolean(SHIFT_DOUBLE_PAGES).takeIf {
                    savedInstanceState.containsKey(SHIFT_DOUBLE_PAGES)
                }
            indexPageToShift =
                savedInstanceState.getInt(SHIFTED_PAGE_INDEX, Int.MIN_VALUE).takeIf {
                    it != Int.MIN_VALUE
                }
            indexChapterToShift =
                savedInstanceState.getLong(SHIFTED_CHAP_INDEX, Long.MIN_VALUE).takeIf {
                    it != Long.MIN_VALUE
                }
        }
        config = ReaderConfig()
        initializeMenu()

        securityPreferences
            .incognitoMode()
            .changes()
            .onEach { SecureActivityDelegate.setSecure(this) }
            .launchIn(lifecycleScope)
        reEnableBackPressedCallBack()

        viewModel.state
            .map { it.isLoadingAdjacentChapter }
            .observeAndUpdate(lifecycleScope, ::setProgressDialog)

        viewModel.state
            .map { it.manga }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setManga)
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.viewerChapters }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setChapters)
            .launchIn(lifecycleScope)

        viewModel.eventFlow
            .onEach { event ->
                when (event) {
                    ReaderViewModel.Event.ReloadMangaAndChapters -> {
                        viewModel.manga?.let(::setManga)
                        viewModel.state.value.viewerChapters?.let(::setChapters)
                    }
                    ReaderViewModel.Event.ReloadViewerChapters -> {
                        viewModel.state.value.viewerChapters?.let(::setChapters)
                    }
                    is ReaderViewModel.Event.SetOrientation -> {
                        setOrientation(event.orientation)
                    }
                    is ReaderViewModel.Event.SavedImage -> {
                        onSaveImageResult(event.result)
                    }
                    is ReaderViewModel.Event.ShareImage -> {
                        onShareImageResult(event.file, event.page)
                    }
                    is ReaderViewModel.Event.SetCoverResult -> {
                        onSetAsCoverResult(event.result)
                    }
                    is ReaderViewModel.Event.ShareTrackingError -> {
                        showTrackingError(event.errors)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    /** Called when the activity is destroyed. Cleans up the viewer, configuration and any view. */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.deletePendingChapters()
        coroutine?.cancel()
        viewer?.destroy()
        viewer = null
        config = null
        snackbar?.dismiss()
        snackbar = null
    }

    /**
     * Called when the activity is saving instance state. Current progress is persisted if this
     * activity isn't changing configurations.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(::menuVisible.name, menuVisible)
        (viewer as? PagerViewer)?.let { pViewer ->
            val config = pViewer.config
            outState.putBoolean(SHIFT_DOUBLE_PAGES, config.shiftDoublePage)
            if (config.shiftDoublePage && config.doublePages) {
                pViewer.getShiftedPage()?.let {
                    outState.putInt(SHIFTED_PAGE_INDEX, it.index)
                    outState.putLong(SHIFTED_CHAP_INDEX, it.chapter.chapter.id ?: 0L)
                }
            }
        }
        viewModel.onSaveInstanceState()
        super.onSaveInstanceState(outState)
    }

    /**
     * Called when the options menu of the binding.toolbar is being created. It adds our custom
     * menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val splitItem = menu.findItem(R.id.action_shift_double_page)
        val isDoublePage =
            ((viewer as? PagerViewer)?.config?.doublePages ?: false) && !canShowSplitAtBottom()
        splitItem?.isVisible = isDoublePage
        showShiftDoublePage = isDoublePage
        (viewer as? PagerViewer)?.config?.let { config ->
            val iconRes =
                if ((!config.shiftDoublePage).xor(viewer is R2LPagerViewer))
                    R.drawable.ic_page_previous_outline_24dp
                else R.drawable.ic_page_next_outline_24dp
            shiftDoublePageIconRes = iconRes
            val icon = ContextCompat.getDrawable(this, iconRes)
            splitItem?.icon = icon
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun canShowSplitAtBottom(): Boolean {
        return if (!readerPreferences.readerBottomButtons().isSet()) {
            isTablet()
        } else {
            ReaderBottomButton.ShiftDoublePage.isIn(readerPreferences.readerBottomButtons().get())
        }
    }

    fun setNavigation(navigation: ViewerNavigation, showOnStart: Boolean) {
        overlayNavigation = navigation
        overlayIsLtr = viewer !is R2LPagerViewer
        overlayInvertMode = navigation.invertMode
        if (showOnStart) {
            showNavigationAgain()
        }
    }

    fun showNavigationAgain() {
        val nav = overlayNavigation
        if (
            nav != null &&
                nav !is eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
        ) {
            overlayInvertMode = nav.invertMode
            overlayVisible = true
        }
    }

    /**
     * Called when an item of the options menu was clicked. Used to handle clicks on our menu
     * entries.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shift_double_page -> {
                shiftDoublePages()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun shiftDoublePages() {
        (viewer as? PagerViewer)?.config?.let { config ->
            config.shiftDoublePage = !config.shiftDoublePage
            viewModel.state.value.viewerChapters?.let {
                (viewer as? PagerViewer)?.updateShifting()
                TimberKt.d { "about to shiftDoublePages" }
                (viewer as? PagerViewer)?.setChaptersDoubleShift(it)
                TimberKt.d { "finished shiftDoublePages" }
                invalidateOptionsMenu()
            }
        }
    }

    private fun popToMain() {
        if (fromUrl) {
            val intent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            startActivity(intent)
            finishAfterTransition()
        } else {
            backPressedCallback?.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun reEnableBackPressedCallBack() {
        backPressedCallback?.isEnabled =
            chaptersSheetVisible || settingsSheetVisible || pageActionsPage != null
    }

    override fun finishAfterTransition() {
        if (
            didTransistionFromChapter &&
                visibleChapterRange.isNotEmpty() &&
                MainActivity.chapterIdToExitTo !in visibleChapterRange
        ) {
            finish()
        } else {
            viewModel.onActivityFinish()
            super.finishAfterTransition()
        }
    }

    override fun finish() {
        viewModel.onActivityFinish()
        super.finish()
    }

    /** Dispatches a key event. If the viewer doesn't handle it, call the default implementation. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> {
                if (viewer is R2LPagerViewer) {
                    loadAdjacentChapter(false)
                } else {
                    loadAdjacentChapter(true)
                }
                return true
            }
            KeyEvent.KEYCODE_P -> {
                if (viewer !is R2LPagerViewer) {
                    loadAdjacentChapter(false)
                } else {
                    loadAdjacentChapter(true)
                }
                return true
            }
            KeyEvent.KEYCODE_L -> {
                loadAdjacentChapter(false)
                return true
            }
            KeyEvent.KEYCODE_R -> {
                loadAdjacentChapter(true)
                return true
            }
            KeyEvent.KEYCODE_E -> {
                viewer?.moveToNext()
                return true
            }
            KeyEvent.KEYCODE_Q -> {
                viewer?.moveToPrevious()
                return true
            }
            KeyEvent.KEYCODE_C -> {
                openWebView(isComments = true)
                return true
            }
            else -> return super.onKeyUp(keyCode, event)
        }
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

    /** Initializes the reader menu. It sets up click listeners and the initial visibility. */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeMenu() {
        window.statusBarColor = Color.TRANSPARENT
        // Set initial visibility
        setMenuVisibility(menuVisible, false)
        val peek = 50.dpToPx
        lastVis = window.decorView.rootWindowInsetsCompat?.isVisible(statusBars()) ?: false
        var firstPass = true
        window.decorView.doOnApplyWindowInsetsCompat { _, insets, _ ->
            setNavColor(insets)
            val systemInsets = insets.ignoredSystemInsets
            val vis = insets.isVisible(statusBars())
            val fullscreen = readerPreferences.fullscreen().get() && !isSplitScreen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!firstPass && lastVis != vis && fullscreen) {
                    onVisibilityChange(vis)
                }
                firstPass = false
                lastVis = vis
            }
            wic.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (!fullscreen && sheetManageNavColor) {
                window.navigationBarColor = getResourceColor(R.attr.colorSurface)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.setOnSystemUiVisibilityChangeListener {
                if (readerPreferences.fullscreen().get()) {
                    onVisibilityChange((it and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0)
                }
            }
        }
    }

    private fun loadAdjacentChapter(rightButton: Boolean) {
        if (isLoading) {
            return
        }
        isScrollingThroughPagesOrChapters = true
        lifecycleScope.launch {
            val getNextChapter = (viewer is R2LPagerViewer).xor(rightButton)
            val adjChapter = viewModel.adjacentChapter(getNextChapter)
            if (adjChapter != null) {
                loadChapter(adjChapter)
            } else {
                toast(
                    if (getNextChapter) {
                        R.string.theres_no_next_chapter
                    } else {
                        R.string.theres_no_previous_chapter
                    }
                )
            }
        }
    }

    suspend fun loadChapter(chapter: Chapter) {
        loadChapter(ReaderChapter(chapter))
    }

    private suspend fun loadChapter(chapter: ReaderChapter) {
        val lastPage = viewModel.loadChapter(chapter) ?: return
        isScrollingThroughPagesOrChapters = false
        if (lastPage >= 0) {
            moveToPageIndex(lastPage, false, chapterChange = true)
        }
        refreshChapters()
    }

    fun setNavColor(insets: WindowInsetsCompat) {
        sheetManageNavColor =
            when {
                isSplitScreen -> {
                    window.statusBarColor = getResourceColor(R.attr.colorPrimaryVariant)
                    window.navigationBarColor = getResourceColor(R.attr.colorPrimaryVariant)
                    false
                }
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 -> {
                    // basically if in landscape on a phone
                    // For lollipop, draw opaque nav bar
                    window.navigationBarColor =
                        when {
                            insets.hasSideNavBar() -> Color.BLACK
                            isInNightMode() -> {
                                ColorUtils.setAlphaComponent(
                                    getResourceColor(R.attr.colorPrimaryVariant),
                                    179,
                                )
                            }
                            else -> Color.argb(179, 0, 0, 0)
                        }
                    !insets.hasSideNavBar()
                }
                insets.isBottomTappable() -> {
                    window.navigationBarColor = Color.TRANSPARENT
                    false
                }
                insets.hasSideNavBar() -> {
                    window.navigationBarColor = getResourceColor(R.attr.colorSurface)
                    false
                }
                // if in portrait with 2/3 button mode, translucent nav bar
                else -> {
                    true
                }
            }
    }

    private fun showPageLayoutMenu() {
        with(window.decorView) {
            val config = (viewer as? PagerViewer)?.config
            val selectedId =
                when {
                    config?.doublePages == true -> PageLayout.DOUBLE_PAGES
                    config?.splitPages == true -> PageLayout.SPLIT_PAGES
                    else -> PageLayout.SINGLE_PAGE
                }
            popupMenu(
                items =
                    listOf(PageLayout.SINGLE_PAGE, PageLayout.DOUBLE_PAGES, PageLayout.SPLIT_PAGES)
                        .map { it.value to it.stringRes },
                selectedItemId = selectedId.value,
            ) {
                val newLayout = PageLayout.fromPreference(itemId)

                if (readerPreferences.pageLayout().get() == PageLayout.AUTOMATIC.value) {
                    (viewer as? PagerViewer)?.config?.let { config ->
                        config.doublePages = newLayout == PageLayout.DOUBLE_PAGES
                        if (newLayout == PageLayout.SINGLE_PAGE) {
                            readerPreferences.automaticSplitsPage().set(false)
                        } else if (newLayout == PageLayout.SPLIT_PAGES) {
                            readerPreferences.automaticSplitsPage().set(true)
                        }
                        reloadChapters(config.doublePages, true)
                    }
                } else {
                    readerPreferences.pageLayout().set(newLayout.value)
                }
            }
        }
    }

    fun hideMenu() {
        if (menuVisible && !isScrollingThroughPagesOrChapters) {
            setMenuVisibility(false)
        }
    }

    /**
     * Sets the visibility of the menu according to [visible] and with an optional parameter to
     * [animate] the views.
     */
    private fun setMenuVisibility(visible: Boolean, animate: Boolean = true) {
        val oldVisibility = menuVisible
        menuVisible = visible
        viewModel.setMenuVisibility(visible)

        if (visible) coroutine?.cancel()
        if (visible) {
            snackbar?.dismiss()
            wic.show(systemBars())

            if (sheetManageNavColor) {
                window.navigationBarColor = Color.TRANSPARENT
            }
        } else {
            if (readerPreferences.fullscreen().get()) {
                wic.hide(systemBars())
                wic.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        menuStickyVisible = false
        viewModel.setMenuStickyVisibility(false)
        reEnableBackPressedCallBack()
    }

    /**
     * Called from the view model when a manga is ready. Used to instantiate the appropriate viewer
     * and the binding.toolbar title.
     */
    fun setManga(manga: MangaItem) {
        val prevViewer = viewer
        val noDefault = manga.viewerFlags == -1
        val mangaViewer = viewModel.getMangaReadingMode()
        val newViewer =
            when (mangaViewer) {
                ReadingModeType.LEFT_TO_RIGHT.flagValue -> L2RPagerViewer(this)
                ReadingModeType.VERTICAL.flagValue -> VerticalPagerViewer(this)
                ReadingModeType.WEBTOON.flagValue -> WebtoonViewer(this, !manga.isLongStrip())
                else -> R2LPagerViewer(this)
            }

        if (
            noDefault &&
                viewModel.manga?.readingModeType!! > 0 &&
                viewModel.manga?.readingModeType!! != readerPreferences.defaultReadingMode().get()
        ) {
            snackbar =
                window.decorView.snack(
                    getString(
                        R.string.reading_,
                        getString(
                                when (mangaViewer) {
                                    ReadingModeType.RIGHT_TO_LEFT.flagValue ->
                                        R.string.right_to_left_viewer
                                    ReadingModeType.VERTICAL.flagValue -> R.string.vertical_viewer
                                    ReadingModeType.WEBTOON.flagValue -> R.string.webtoon_style
                                    else -> R.string.left_to_right_viewer
                                }
                            )
                            .lowercase(Locale.getDefault()),
                    ),
                    4000,
                ) {
                    if (viewModel.manga?.isLongStrip() != true) {
                        setAction(R.string.use_default) { viewModel.setMangaReadingMode(0) }
                    }
                }
        }

        setOrientation(viewModel.getMangaOrientationType())

        val isSameViewerType =
            prevViewer != null &&
                prevViewer::class == newViewer::class &&
                (prevViewer !is WebtoonViewer ||
                    (newViewer is WebtoonViewer &&
                        prevViewer.noWebtoonTag == newViewer.noWebtoonTag))

        if (!isSameViewerType) {
            // Destroy previous viewer if there was one
            prevViewer?.destroy()
            viewer = newViewer

            if (newViewer is PagerViewer) {
                if (readerPreferences.pageLayout().get() == PageLayout.AUTOMATIC.value) {
                    setDoublePageMode(newViewer)
                }
                lastShiftDoubleState?.let { newViewer.config.shiftDoublePage = it }
                viewModel.setViewerItems(newViewer.items)
            } else if (newViewer is WebtoonViewer) {
                viewModel.setViewerItems(newViewer.items)
            } else {
                viewModel.setViewerItems(emptyList())
            }
        }

        overlayIsLtr = (viewer ?: newViewer) !is R2LPagerViewer

        supportActionBar?.title = manga.userTitle.ifBlank { manga.title }
        chapterTitle = viewModel.getCurrentChapter()?.chapter?.name ?: ""

        if (viewModel.state.value.viewerChapters == null) {
            viewModel.setIsLoading(true)
        }
        invalidateOptionsMenu()
        startPostponedEnterTransition()
    }

    override fun onPause() {
        viewModel.saveCurrentChapterReadingProgress()
        viewModel.flushReadTimer()
        viewModel.deletePendingChapters()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.restartReadTimer()
    }

    fun updatePagedViewerItems() {
        (viewer as? PagerViewer)?.let { pViewer -> viewModel.setViewerItems(pViewer.items) }
    }

    fun updateWebtoonViewerItems() {
        (viewer as? WebtoonViewer)?.let { wViewer -> viewModel.setViewerItems(wViewer.items) }
    }

    fun reloadChapters(doublePages: Boolean, force: Boolean = false) {
        val pViewer = viewer as? PagerViewer ?: return
        pViewer.updateShifting()
        if (!force && pViewer.config.autoDoublePages) {
            setDoublePageMode(pViewer)
        } else {
            pViewer.config.doublePages = doublePages
            if (pViewer.config.autoDoublePages) {
                pViewer.config.splitPages =
                    readerPreferences.automaticSplitsPage().get() && !pViewer.config.doublePages
            }
        }
        val currentChapter = viewModel.getCurrentChapter()
        if (doublePages) {
            // If we're moving from singe to double, we want the current page to be the first page
            val pageIndex = viewModel.state.value.currentPageIndex
            pViewer.config.shiftDoublePage =
                (pageIndex +
                    (currentChapter?.pages?.take(pageIndex)?.count {
                        it.fullPage == true || it.isolatedPage
                    } ?: 0)) % 2 != 0
        }
        viewModel.state.value.viewerChapters?.let {
            TimberKt.d { "about to reloadChapter call set chaptersDoubleShift" }
            pViewer.setChaptersDoubleShift(it)
            TimberKt.d { "finished reloadChapter call set chaptersDoubleShift" }
            updatePagedViewerItems()
        }
        invalidateOptionsMenu()
    }

    /**
     * Called from the view model whenever a new [viewerChapters] have been set. It delegates the
     * method to the current viewer, but also set the subtitle on the binding.toolbar.
     */
    fun setChapters(viewerChapters: ViewerChapters) {
        viewModel.setIsLoading(false)
        if (indexChapterToShift != null && indexPageToShift != null) {
            viewerChapters.currChapter.pages
                ?.find {
                    it.index == indexPageToShift && it.chapter.chapter.id == indexChapterToShift
                }
                ?.let { (viewer as? PagerViewer)?.updateShifting(it) }
            indexChapterToShift = null
            indexPageToShift = null
        } else if (lastShiftDoubleState != null) {
            val currentChapter = viewerChapters.currChapter
            (viewer as? PagerViewer)?.config?.shiftDoublePage =
                (currentChapter.requestedPage +
                    (currentChapter.pages?.take(currentChapter.requestedPage)?.count {
                        it.fullPage == true || it.isolatedPage
                    } ?: 0)) % 2 != 0
        }
        val currentChapterPageCount = viewerChapters.currChapter.pages?.size ?: 1
        lastShiftDoubleState = null
        viewer?.setChapters(viewerChapters)
        if (viewer is PagerViewer) {
            updatePagedViewerItems()
        } else if (viewer is WebtoonViewer) {
            updateWebtoonViewerItems()
        }
        intentPageNumber?.let { moveToPageIndex(it) }
        intentPageNumber = null
        val subtitleText =
            if (viewModel.manga!!.hideChapterTitle(mangaDetailsPreferences)) {
                val number =
                    decimalFormat.format(
                        viewerChapters.currChapter.chapter.chapter_number.toDouble()
                    )
                getString(R.string.chapter_, number)
            } else {
                viewerChapters.currChapter.chapter.name
            }
        chapterTitle = subtitleText ?: ""
        if (didTransistionFromChapter) {
            MainActivity.chapterIdToExitTo = viewerChapters.currChapter.chapter.id ?: 0L
        }
    }

    /**
     * Called from the view model if the initial load couldn't load the pages of the chapter. In
     * this case the activity is closed and a toast is shown to the user.
     */
    fun setInitialChapterError(error: Throwable) {
        if (error is CancellationException) {
            return
        }
        TimberKt.e(error) { "Error setting initial chapter" }
        finish()
        toast(error.message.orUnknownError(this))
    }

    /**
     * Called from the view model whenever it's loading the next or previous chapter. It shows or
     * dismisses a non-cancellable dialog to prevent user interaction according to the value of
     * [show]. This is only used when the next/previous buttons on the binding.toolbar are clicked;
     * the other cases are handled with chapter transitions on the viewers and chapter preloading.
     */
    fun setProgressDialog(show: Boolean) {

        if (show) {
            isLoading = show
        } else {
            scope.launchIO {
                delay(100)
                isLoading = show
            }
        }
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the page
     * is not found.
     */
    fun moveToPageIndex(index: Int, animated: Boolean = true, chapterChange: Boolean = false) {
        val viewer = viewer ?: return
        val currentChapter = viewModel.getCurrentChapter() ?: return
        val page = currentChapter.pages?.getOrNull(index) ?: return
        viewer.moveToPage(page, animated)
        if (chapterChange) {
            isScrollingThroughPagesOrChapters = false
        }
    }

    fun refreshChapters() {
        lifecycleScope.launch { viewModel.getChapters() }
    }

    /**
     * Called from the viewer whenever a [page] is marked as active. It updates the values of the
     * bottom menu and delegates the change to the view model.
     */
    @SuppressLint("SetTextI18n")
    fun onPageSelected(page: ReaderPage, hasExtraPage: Boolean) {
        (viewer as? PagerViewer)?.hasMoved = true
        viewModel.onPageSelected(page, hasExtraPage)
        val pages = page.chapter.pages ?: return

        val currentPage =
            if (hasExtraPage) {
                val invertDoublePage = (viewer as? PagerViewer)?.config?.invertDoublePages ?: false
                if (!(viewer is R2LPagerViewer).xor(invertDoublePage)) {
                    "${page.number}-${page.number + 1}"
                } else {
                    "${page.number + 1}-${page.number}"
                }
            } else {
                "${page.number}${if (page.firstHalf == false) "*" else ""}"
            }

        val totalPages = pages.size.toString()
        if (viewModel.getCurrentChapter()?.chapter?.id != page.chapter.chapter.id) {
            lifecycleScope.launch { viewModel.getChapters() }
        }
        val progress = page.index + if (hasExtraPage) 1 else 0
        val progressVal = if (progress == pages.lastIndex) progress else page.index
        viewModel.updatePageProgress(
            currentPageText = currentPage,
            totalPagesText = totalPages,
            currentPageIndex = progressVal,
            totalPages = pages.lastIndex,
        )
    }

    /** Called from the viewer whenever a transition is marked as active. */
    fun onTransitionSelected(transition: ChapterTransition) {
        viewModel.updatePageProgress(
            currentPageText = "",
            totalPagesText = "",
            currentPageIndex = 0,
            totalPages = 0,
        )
        val toChapter = transition.to
        if (toChapter != null) {
            requestPreloadChapter(toChapter)
        } else if (transition is ChapterTransition.Next) {
            showMenu()
        }
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: ReaderPage, extraPage: ReaderPage? = null) {
        window.decorView.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
        )
        pageActionsPage = page to extraPage
        reEnableBackPressedCallBack()
    }

    private fun handlePageAction(
        action: ReaderPageAction,
        page: ReaderPage,
        extraPage: ReaderPage?,
    ) {
        when (action) {
            ReaderPageAction.Share -> shareImage(page)
            ReaderPageAction.Save -> saveImage(page)
            ReaderPageAction.SetAsCover -> showSetCoverPrompt(page)
            ReaderPageAction.ShareFirstPage -> shareImage(page)
            ReaderPageAction.SaveFirstPage -> saveImage(page)
            ReaderPageAction.SetFirstPageAsCover -> showSetCoverPrompt(page)
            ReaderPageAction.ShareSecondPage -> extraPage?.let { shareImage(it) }
            ReaderPageAction.SaveSecondPage -> extraPage?.let { saveImage(it) }
            ReaderPageAction.SetSecondPageAsCover -> extraPage?.let { showSetCoverPrompt(it) }
            ReaderPageAction.ShareCombinedPages,
            ReaderPageAction.SaveCombinedPages -> {
                extraPage?.let { secondPage ->
                    (viewer as? PagerViewer)?.let { viewer ->
                        val isLTR = (viewer !is R2LPagerViewer).xor(viewer.config.invertDoublePages)
                        val theme = ReaderTheme.fromPreference(viewer.config.readerTheme)
                        val bg =
                            if (theme.isSmart || theme == ReaderTheme.WHITE) {
                                Color.WHITE
                            } else {
                                Color.BLACK
                            }
                        if (action == ReaderPageAction.ShareCombinedPages) {
                            viewModel.shareImages(page, secondPage, isLTR, bg)
                        } else {
                            viewModel.saveImages(page, secondPage, isLTR, bg)
                        }
                    }
                }
            }
        }
    }

    /**
     * Called from the viewer when the given [chapter] should be preloaded. It should be called when
     * the viewer is reaching the beginning or end of a chapter or the transition page is active.
     */
    fun requestPreloadChapter(chapter: ReaderChapter) {
        lifecycleScope.launch { viewModel.preloadChapter(chapter) }
    }

    /**
     * Called from the viewer to toggle the visibility of the menu. It's implemented on the viewer
     * because each one implements its own touch and key events.
     */
    fun toggleMenu() {
        if (chaptersSheetVisible || settingsSheetVisible || pageActionsPage != null) {
            chaptersSheetVisible = false
            settingsSheetVisible = false
            pageActionsPage = null
            setMenuVisibility(false)
        } else {
            setMenuVisibility(!menuVisible)
        }
    }

    /** Called from the viewer to show the menu. */
    fun showMenu() {
        if (!menuVisible) {
            setMenuVisibility(true)
        }
    }

    /**
     * Called from the page sheet. It delegates the call to the view model to do some IO, which will
     * call [onShareImageResult] with the path the image was saved on when it's ready.
     */
    private fun shareImage(page: ReaderPage) {
        viewModel.shareImage(page)
    }

    private fun showSetCoverPrompt(page: ReaderPage) {
        if (page.status != Page.State.READY) return

        materialAlertDialog()
            .setMessage(R.string.use_image_as_cover)
            .setPositiveButton(android.R.string.ok) { _, _ -> setAsCover(page) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Called from the view model when a page is ready to be shared. It shows Android's default
     * sharing tool.
     */
    fun onShareImageResult(file: UniFile, page: ReaderPage, secondPage: ReaderPage? = null) {
        val manga = viewModel.manga ?: return
        val chapter = page.chapter.chapter

        val decimalFormat =
            DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })

        val pageNumber =
            if (secondPage != null) {
                getString(
                    R.string.pages_,
                    if (resources.isLTR) "${page.number}-${page.number + 1}"
                    else "${page.number + 1}-${page.number}",
                )
            } else {
                getString(R.string.page_, page.number)
            }

        val text =
            "${manga.title}: ${
            getString(
                R.string.chapter_,
                decimalFormat.format(chapter.chapter_number),
            )
        }, $pageNumber, <${MdConstants.baseUrl + manga.url}>"

        val stream = file.uri.getUriWithAuthority(this)
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_STREAM, stream)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                clipData = ClipData.newRawUri(null, stream)
                type = "image/*"
            }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        val chapterUrl = viewModel.getChapterUrl() ?: return
        outContent.webUri = Uri.parse(chapterUrl)
    }

    /**
     * Called from the page sheet. It delegates saving the image of the given [page] on external
     * storage to the viewModel.
     */
    private fun saveImage(page: ReaderPage) {
        viewModel.saveImage(page)
    }

    /**
     * Called from the view model when a page is saved or fails. It shows a message or logs the
     * event depending on the [result].
     */
    private fun onSaveImageResult(result: ReaderViewModel.SaveImageResult) {
        when (result) {
            is ReaderViewModel.SaveImageResult.Success -> {
                toast(R.string.picture_saved)
            }
            is ReaderViewModel.SaveImageResult.Error -> {
                TimberKt.e(result.error) { "on save image result error" }
            }
        }
    }

    private fun openWebView(isComments: Boolean) {
        val currentChapter = viewModel.getCurrentChapter()
        currentChapter ?: return

        if (isComments) {
            if (currentChapter.chapter.isMergedChapter()) {
                toast(R.string.comments_unavailable, duration = Toast.LENGTH_SHORT)
            } else {
                viewModel.setIsLoading(true)
                scope.launchIO {
                    val threadId = viewModel.lookupComment(currentChapter.chapter.uuid())

                    scope.launchUI {
                        viewModel.setIsLoading(false)

                        if (threadId == null) {
                            toast(R.string.comments_unavailable, duration = Toast.LENGTH_SHORT)
                        } else {
                            this@ReaderActivity.openInBrowser(MdConstants.forumUrl + threadId)
                        }
                    }
                }
            }
        } else {
            this@ReaderActivity.openInBrowser(viewModel.getChapterUrl()!!)
        }
    }

    /**
     * Called from the page sheet. It delegates setting the image of the given [page] as the cover
     * to the viewModel.
     */
    private fun setAsCover(page: ReaderPage) {
        viewModel.setAsCover(page)
    }

    /**
     * Called from the view model when a page is set as cover or fails. It shows a different message
     * depending on the [result].
     */
    fun onSetAsCoverResult(result: ReaderViewModel.SetAsCoverResult) {
        toast(
            when (result) {
                Success -> R.string.cover_updated
                AddToLibraryFirst -> R.string.must_be_in_library_to_edit
                Error -> R.string.failed_to_update_cover
            }
        )
    }

    private fun showTrackingError(errors: List<Pair<TrackService, String?>>) {
        if (errors.isEmpty()) return
        snackbar?.dismiss()
        val errorText =
            if (errors.size > 1) {
                getString(
                    R.string.failed_to_update_,
                    errors.joinToString(", ") { getString(it.first.nameRes()) },
                )
            } else {
                val (service, errorMessage) = errors.first()
                buildSpannedString {
                    if (errorMessage != null) {
                        val icon =
                            contextCompatDrawable(service.getLogo())?.mutate()?.apply {
                                val size =
                                    resources.getDimension(
                                        com.google.android.material.R.dimen
                                            .design_snackbar_text_size
                                    )
                                val dRatio = intrinsicWidth / intrinsicHeight.toFloat()
                                setBounds(0, 0, (size * dRatio).roundToInt(), size.roundToInt())
                            } ?: return
                        val alignment =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                DynamicDrawableSpan.ALIGN_CENTER
                            else DynamicDrawableSpan.ALIGN_BASELINE
                        inSpans(ImageSpan(icon, alignment)) { append("image") }
                        append(" - $errorMessage")
                    }
                }
            }
        snackbar = window.decorView.snack(errorText, 5000)
    }

    private fun onVisibilityChange(visible: Boolean) {
        if (chaptersSheetVisible || settingsSheetVisible || pageActionsPage != null) return
        if (visible && !menuStickyVisible && !menuVisible) {
            menuStickyVisible = true
            viewModel.setMenuStickyVisibility(true)
            coroutine = launchUI {
                delay(2000)
                menuStickyVisible = false
                viewModel.setMenuStickyVisibility(false)
                setMenuVisibility(false)
            }
            if (sheetManageNavColor) {
                window.navigationBarColor =
                    ColorUtils.setAlphaComponent(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || isInNightMode()) {
                            getResourceColor(R.attr.colorSurface)
                        } else {
                            Color.BLACK
                        },
                        if (window.decorView.rootWindowInsetsCompat?.hasSideNavBar() == true) {
                            255
                        } else {
                            179
                        },
                    )
            }
        } else if (!visible && (menuStickyVisible || menuVisible)) {
            if (menuStickyVisible && !menuVisible) {
                menuStickyVisible = false
                viewModel.setMenuStickyVisibility(false)
                setMenuVisibility(false)
            }
            coroutine?.cancel()
        }
    }

    /** Sets notch cutout mode to "NEVER", if mobile is in a landscape view */
    private fun setNotchCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val currentOrientation = resources.configuration.orientation

            val params = window.attributes
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            } else {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun setDoublePageMode(viewer: PagerViewer) {
        val currentOrientation = resources.configuration.orientation
        viewer.config.doublePages = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        if (viewer.config.autoDoublePages) {
            viewer.config.splitPages =
                readerPreferences.automaticSplitsPage().get() && !viewer.config.doublePages
        }
    }

    private fun handleIntentAction(intent: Intent): Boolean {
        val pathSegments = intent.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val id = pathSegments[1]
            val secondary = pathSegments.getOrNull(2)
            if (secondary == "comments") {
                openInBrowser(intent.data!!.toString(), true)
                return true
            } else if (!id.isNullOrBlank()) {
                intentPageNumber = secondary?.toIntOrNull()?.minus(1)
                setMenuVisibility(visible = false, animate = true)
                scope.launch(Dispatchers.IO) {
                    try {
                        viewModel.loadChapterURL(id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { setInitialChapterError(e) }
                    }
                }
                return true
            }
        }
        return false
    }

    /** Forces the user preferred [orientation] on the activity. */
    fun setOrientation(orientation: Int) {
        val newOrientation = OrientationType.fromPreference(orientation)
        if (newOrientation.flag != requestedOrientation) {
            requestedOrientation = newOrientation.flag
        }
    }

    /** Class that handles the user preferences of the reader. */
    private inner class ReaderConfig {

        var showNewChapter = false

        /** Initializes the reader subscriptions. */
        init {
            readerPreferences
                .defaultOrientationType()
                .changes()
                .drop(1)
                .onEach {
                    delay(250)
                    setOrientation(viewModel.getMangaOrientationType())
                }
                .launchIn(scope)

            readerPreferences
                .showPageNumber()
                .changes()
                .onEach { setPageNumberVisibility(it) }
                .launchIn(scope)

            readerPreferences
                .displayProfile()
                .changes()
                .onEach { setDisplayProfile(it) }
                .launchIn(scope)

            readerPreferences.fullscreen().changes().onEach { setFullscreen(it) }.launchIn(scope)

            readerPreferences
                .keepScreenOn()
                .changes()
                .onEach { setKeepScreenOn(it) }
                .launchIn(scope)

            readerPreferences
                .customBrightness()
                .changes()
                .onEach { setCustomBrightness(it) }
                .launchIn(scope)

            readerPreferences.colorFilter().changes().onEach { setColorFilter(it) }.launchIn(scope)

            readerPreferences
                .colorFilterMode()
                .changes()
                .onEach { setColorFilter(readerPreferences.colorFilter().get()) }
                .launchIn(scope)

            merge(
                    readerPreferences.grayscale().changes(),
                    readerPreferences.invertedColors().changes(),
                )
                .onEach {
                    setLayerPaint(
                        readerPreferences.grayscale().get(),
                        readerPreferences.invertedColors().get(),
                    )
                }
                .launchIn(lifecycleScope)

            readerPreferences
                .alwaysShowChapterTransition()
                .changes()
                .onEach { showNewChapter = it }
                .launchIn(scope)

            readerPreferences
                .automaticSplitsPage()
                .changes()
                .drop(1)
                .onEach {
                    val isPaused =
                        !this@ReaderActivity.lifecycle.currentState.isAtLeast(
                            Lifecycle.State.RESUMED
                        )
                    if (isPaused) {
                        (viewer as? PagerViewer)?.config?.let { config ->
                            reloadChapters(config.doublePages, true)
                        }
                    }
                }
                .launchIn(scope)
        }

        private fun setPageNumberVisibility(visible: Boolean) {
            viewModel.setPageNumberVisibility(visible)
        }

        /** Sets the display profile to [path]. */
        private fun setDisplayProfile(path: String) {
            // Display profile calibration for native image rendering
        }

        /** Sets the fullscreen reading mode (immersive) according to [enabled]. */
        private fun setFullscreen(enabled: Boolean) {
            WindowCompat.setDecorFitsSystemWindows(window, !enabled || isSplitScreen)
            wic.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.decorView.rootWindowInsetsCompat?.let { setNavColor(it) }
        }

        /** Sets the keep screen on mode according to [enabled]. */
        private fun setKeepScreenOn(enabled: Boolean) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        /** Sets the custom brightness overlay according to [enabled]. */
        private fun setCustomBrightness(enabled: Boolean) {
            if (enabled) {
                readerPreferences
                    .customBrightnessValue()
                    .changes()
                    .sample(100)
                    .onEach { setCustomBrightnessValue(it) }
                    .launchIn(scope)
            } else {
                setCustomBrightnessValue(0)
            }
        }

        private fun setColorFilter(enabled: Boolean) {
            if (enabled) {
                readerPreferences
                    .colorFilterValue()
                    .changes()
                    .sample(100)
                    .onEach { setColorFilterValue(it) }
                    .launchIn(scope)
            } else {
                colorFilterOverlayColor = 0
            }
        }

        private fun getCombinedPaint(grayscale: Boolean, invertedColors: Boolean): Paint {
            return Paint().apply {
                colorFilter =
                    ColorMatrixColorFilter(
                        ColorMatrix().apply {
                            if (grayscale) {
                                setSaturation(0f)
                            }
                            if (invertedColors) {
                                postConcat(
                                    ColorMatrix(
                                        floatArrayOf(
                                            -1f,
                                            0f,
                                            0f,
                                            0f,
                                            255f,
                                            0f,
                                            -1f,
                                            0f,
                                            0f,
                                            255f,
                                            0f,
                                            0f,
                                            -1f,
                                            0f,
                                            255f,
                                            0f,
                                            0f,
                                            0f,
                                            1f,
                                            0f,
                                        )
                                    )
                                )
                            }
                        }
                    )
            }
        }

        private fun setCustomBrightnessValue(value: Int) {
            // Calculate and set reader brightness.
            val readerBrightness =
                when {
                    value > 0 -> {
                        value / 100f
                    }
                    value < 0 -> {
                        0.01f
                    }
                    else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }

            window.attributes = window.attributes.apply { screenBrightness = readerBrightness }

            // Set black overlay visibility.
            if (value < 0) {
                val alpha = (abs(value) * 2.56).toInt()
                brightnessOverlayAlpha = alpha / 255f
            } else {
                brightnessOverlayAlpha = 0f
            }
        }

        /** Sets the color filter [value]. */
        private fun setColorFilterValue(value: Int) {
            colorFilterOverlayColor = value
            colorFilterOverlayMode = readerPreferences.colorFilterMode().get()
        }

        private fun setLayerPaint(grayscale: Boolean, invertedColors: Boolean) {
            // Layer paint is handled natively in Compose
        }
    }
}
