package org.nekomanga.presentation.screens.reader

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import org.nekomanga.R
import org.nekomanga.constants.Constants

@Immutable
data class ReaderChaptersSheetUiState(
    val chapters: List<ReaderChapterRowUiModel> = emptyList(),
    val quickActions: List<ReaderQuickActionUiModel> = emptyList(),
    val currentChapterIndex: Int = -1,
    val isLoading: Boolean = false,
    val loadingChapterId: Long? = null,
)

@Immutable
data class ReaderChapterRowUiModel(
    val id: Long,
    val formattedTitle: String,
    val formattedSubtitle: String?,
    val isCurrent: Boolean,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val textColor: Color? = null,
    val bookmarkColor: Color? = null,
    val language: String? = null,
)

enum class ReaderQuickActionId {
    Chapters,
    Comments,
    WebView,
    ReadingMode,
    Rotation,
    CropBorders,
    Grayscale,
    DoublePage,
    ShiftPage,
    DisplayOptions,
}

@Immutable
data class ReaderQuickActionUiModel(
    val id: ReaderQuickActionId,
    val icon: ReaderButtonIcon,
    @param:StringRes val tooltipRes: Int,
    val toggleStyle: ButtonToggleStyle = ButtonToggleStyle.None,
    val isToggled: Boolean = false,
    val isEnabled: Boolean = true,
) {
    constructor(
        id: ReaderQuickActionId,
        @DrawableRes iconRes: Int,
        @StringRes tooltipRes: Int,
        toggleStyle: ButtonToggleStyle = ButtonToggleStyle.None,
        isToggled: Boolean = false,
        isEnabled: Boolean = true,
    ) : this(
        id = id,
        icon = ReaderButtonIcon.Resource(iconRes),
        tooltipRes = tooltipRes,
        toggleStyle = toggleStyle,
        isToggled = isToggled,
        isEnabled = isEnabled,
    )

    constructor(
        id: ReaderQuickActionId,
        iconVector: ImageVector,
        @StringRes tooltipRes: Int,
        toggleStyle: ButtonToggleStyle = ButtonToggleStyle.None,
        isToggled: Boolean = false,
        isEnabled: Boolean = true,
    ) : this(
        id = id,
        icon = ReaderButtonIcon.Vector(iconVector),
        tooltipRes = tooltipRes,
        toggleStyle = toggleStyle,
        isToggled = isToggled,
        isEnabled = isEnabled,
    )

    val iconRes: Int
        get() = (icon as? ReaderButtonIcon.Resource)?.id ?: 0

    val iconVector: ImageVector?
        get() = (icon as? ReaderButtonIcon.Vector)?.imageVector
}

sealed interface ReaderChaptersAction {
    data class SelectChapter(val chapterId: Long) : ReaderChaptersAction

    data class ToggleBookmark(val chapterId: Long) : ReaderChaptersAction

    data class QuickActionClick(val actionId: ReaderQuickActionId) : ReaderChaptersAction

    data object Dismiss : ReaderChaptersAction
}

fun buildReaderChaptersQuickActions(
    isChaptersVisible: Boolean = true,
    isCommentsVisible: Boolean = true,
    isWebViewVisible: Boolean = true,
    isReadingModeVisible: Boolean = false,
    readingModeIconRes: Int = R.drawable.ic_reader_default_24dp,
    isRotationVisible: Boolean = false,
    rotationIconRes: Int = R.drawable.ic_screen_rotation_24dp,
    isCropBordersVisible: Boolean = false,
    cropBorders: Boolean = false,
    isGrayscaleVisible: Boolean = false,
    grayscale: Boolean = false,
    isDoublePageVisible: Boolean = false,
    doublePageIconRes: Int = R.drawable.ic_book_open_variant_24dp,
    isShiftPageVisible: Boolean = false,
    shiftPageIconRes: Int = R.drawable.ic_page_next_outline_24dp,
    isDisplayOptionsVisible: Boolean = true,
): List<ReaderQuickActionUiModel> {
    val actions = mutableListOf<ReaderQuickActionUiModel>()
    if (isChaptersVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Chapters,
                iconRes = R.drawable.ic_format_list_numbered_24dp,
                tooltipRes = R.string.chapters,
            )
        )
    }
    if (isCommentsVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Comments,
                iconRes = R.drawable.ic_view_comments_24p,
                tooltipRes = R.string.comments,
            )
        )
    }
    if (isWebViewVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.WebView,
                iconRes = R.drawable.ic_open_in_webview_24dp,
                tooltipRes = R.string.open_in_webview,
            )
        )
    }
    if (isReadingModeVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.ReadingMode,
                iconRes = readingModeIconRes,
                tooltipRes = R.string.reading_mode,
            )
        )
    }
    if (isRotationVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Rotation,
                iconRes = rotationIconRes,
                tooltipRes = R.string.rotation,
            )
        )
    }
    if (isCropBordersVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.CropBorders,
                iconVector = if (cropBorders) Icons.Default.CropFree else Icons.Default.Crop,
                tooltipRes = R.string.crop_borders,
                toggleStyle = ButtonToggleStyle.PrimaryWhenToggled,
                isToggled = cropBorders,
            )
        )
    }
    if (isGrayscaleVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Grayscale,
                iconRes = R.drawable.ic_palette,
                tooltipRes = R.string.grayscale_toggle,
                toggleStyle = ButtonToggleStyle.PrimaryWhenToggled,
                isToggled = grayscale,
            )
        )
    }
    if (isDoublePageVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.DoublePage,
                iconRes = doublePageIconRes,
                tooltipRes = R.string.double_pages,
            )
        )
    }
    if (isShiftPageVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.ShiftPage,
                iconRes = shiftPageIconRes,
                tooltipRes = R.string.shift_one_page_over,
            )
        )
    }
    if (isDisplayOptionsVisible) {
        actions.add(
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.DisplayOptions,
                iconRes = R.drawable.ic_tune_24dp,
                tooltipRes = R.string.display_options,
            )
        )
    }
    return actions
}

private val chapterDecimalFormat = ThreadLocal.withInitial {
    DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
}

fun ReaderChapterItem.toReaderChapterRowUiModel(
    context: Context,
    hideChapterTitles: Boolean = false,
    textColor: Color? = null,
    bookmarkColor: Color? = null,
): ReaderChapterRowUiModel {
    val titleText =
        if (hideChapterTitles) {
            val number = chapterDecimalFormat.get().format(this.chapter_number.toDouble())
            context.getString(R.string.chapter_, number)
        } else {
            this.name
        }

    val statuses = mutableListOf<String>()
    ChapterUtil.relativeDate(this.chapter)?.let { statuses.add(it) }
    this.scanlator?.takeIf { it.isNotBlank() }?.let { statuses.add(it) }
    val subtitleText = statuses.joinToString(Constants.SEPARATOR).ifBlank { null }

    val rawLanguage = this.chapter.language
    val language =
        if (
            !rawLanguage.isNullOrBlank() &&
                !rawLanguage.equals("english", true) &&
                !rawLanguage.equals("en", true)
        ) {
            rawLanguage
        } else {
            null
        }

    return ReaderChapterRowUiModel(
        id = this.chapter.id ?: 0L,
        formattedTitle = titleText,
        formattedSubtitle = subtitleText,
        isCurrent = this.isCurrent,
        isRead = this.chapter.read,
        isBookmarked = this.chapter.bookmark,
        textColor = textColor,
        bookmarkColor = bookmarkColor,
        language = language,
    )
}
