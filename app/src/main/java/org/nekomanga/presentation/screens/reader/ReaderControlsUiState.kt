package org.nekomanga.presentation.screens.reader

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.ui.reader.settings.ReaderSliderPosition
import org.nekomanga.R

@Immutable
data class ReaderBottomControlsUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val sliderState: ReaderSliderUiState = ReaderSliderUiState(),
    val buttons: List<ReaderToolbarButtonUiModel> = emptyList(),
)

@Immutable
data class ReaderSliderUiState(
    val currentPageText: String = "",
    val totalPagesText: String = "",
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val isRtl: Boolean = false,
    val position: SliderOrientation = SliderOrientation.Horizontal,
)

enum class SliderOrientation {
    Horizontal,
    VerticalLeft,
    VerticalRight;

    companion object {
        fun from(isVertical: Boolean, sliderPosition: ReaderSliderPosition): SliderOrientation {
            return if (!isVertical || sliderPosition == ReaderSliderPosition.HORIZONTAL) {
                Horizontal
            } else if (sliderPosition == ReaderSliderPosition.LEFT) {
                VerticalLeft
            } else {
                VerticalRight
            }
        }
    }
}

@Immutable
sealed interface ReaderButtonIcon {
    @Immutable data class Resource(@param:DrawableRes val id: Int) : ReaderButtonIcon

    @Immutable data class Vector(val imageVector: ImageVector) : ReaderButtonIcon
}

enum class ButtonToggleStyle {
    None,
    PrimaryWhenToggled,
}

@Immutable
data class ReaderToolbarButtonUiModel(
    val id: ReaderBottomActionId,
    val icon: ReaderButtonIcon,
    @param:StringRes val tooltipRes: Int,
    val toggleStyle: ButtonToggleStyle = ButtonToggleStyle.None,
    val isToggled: Boolean = false,
    val isVisible: Boolean = true,
) {
    constructor(
        id: ReaderBottomActionId,
        @DrawableRes iconRes: Int,
        @StringRes tooltipRes: Int,
        toggleStyle: ButtonToggleStyle = ButtonToggleStyle.None,
        isToggled: Boolean = false,
        isVisible: Boolean = true,
    ) : this(
        id = id,
        icon = ReaderButtonIcon.Resource(iconRes),
        tooltipRes = tooltipRes,
        toggleStyle = toggleStyle,
        isToggled = isToggled,
        isVisible = isVisible,
    )

    constructor(
        id: ReaderBottomActionId,
        iconVector: ImageVector,
        @StringRes tooltipRes: Int,
        toggleStyle: ButtonToggleStyle = ButtonToggleStyle.None,
        isToggled: Boolean = false,
        isVisible: Boolean = true,
    ) : this(
        id = id,
        icon = ReaderButtonIcon.Vector(iconVector),
        tooltipRes = tooltipRes,
        toggleStyle = toggleStyle,
        isToggled = isToggled,
        isVisible = isVisible,
    )

    val iconRes: Int
        get() = (icon as? ReaderButtonIcon.Resource)?.id ?: 0

    val iconVector: ImageVector?
        get() = (icon as? ReaderButtonIcon.Vector)?.imageVector
}

enum class ReaderBottomActionId {
    Chapters,
    Comments,
    WebView,
    ReadingMode,
    Rotation,
    CropBorders,
    Grayscale,
    DoublePage,
    ShiftPage,
    Settings,
}

sealed interface ReaderBottomBarAction {
    data class PageChanged(val pageIndex: Int) : ReaderBottomBarAction

    data object SkipPrevious : ReaderBottomBarAction

    data object SkipNext : ReaderBottomBarAction

    data class ButtonClicked(val actionId: ReaderBottomActionId) : ReaderBottomBarAction
}

fun buildReaderBottomBarButtons(
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
    isDoublePage: Boolean = false,
    doublePageIconRes: Int = R.drawable.ic_book_open_variant_24dp,
    isShiftPageVisible: Boolean = false,
    shiftPageIconRes: Int = R.drawable.ic_page_next_outline_24dp,
    isSettingsVisible: Boolean = true,
): List<ReaderToolbarButtonUiModel> {
    return listOf(
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.Chapters,
            icon = ReaderButtonIcon.Resource(R.drawable.ic_format_list_numbered_24dp),
            tooltipRes = R.string.view_chapters,
            isVisible = isChaptersVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.Comments,
            icon = ReaderButtonIcon.Resource(R.drawable.ic_view_comments_24p),
            tooltipRes = R.string.comments,
            isVisible = isCommentsVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.WebView,
            icon = ReaderButtonIcon.Resource(R.drawable.ic_open_in_webview_24dp),
            tooltipRes = R.string.open_in_webview,
            isVisible = isWebViewVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.ReadingMode,
            icon = ReaderButtonIcon.Resource(readingModeIconRes),
            tooltipRes = R.string.reading_mode,
            isVisible = isReadingModeVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.Rotation,
            icon = ReaderButtonIcon.Resource(rotationIconRes),
            tooltipRes = R.string.rotation,
            isVisible = isRotationVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.CropBorders,
            icon =
                ReaderButtonIcon.Vector(
                    if (cropBorders) Icons.Default.CropFree else Icons.Default.Crop
                ),
            tooltipRes = R.string.crop_borders,
            toggleStyle = ButtonToggleStyle.PrimaryWhenToggled,
            isToggled = cropBorders,
            isVisible = isCropBordersVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.Grayscale,
            icon = ReaderButtonIcon.Resource(R.drawable.ic_palette),
            tooltipRes = R.string.grayscale_toggle,
            toggleStyle = ButtonToggleStyle.PrimaryWhenToggled,
            isToggled = grayscale,
            isVisible = isGrayscaleVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.DoublePage,
            icon = ReaderButtonIcon.Resource(doublePageIconRes),
            tooltipRes = R.string.double_pages,
            isToggled = isDoublePage,
            isVisible = isDoublePageVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.ShiftPage,
            icon = ReaderButtonIcon.Resource(shiftPageIconRes),
            tooltipRes = R.string.shift_one_page_over,
            isVisible = isShiftPageVisible,
        ),
        ReaderToolbarButtonUiModel(
            id = ReaderBottomActionId.Settings,
            icon = ReaderButtonIcon.Resource(R.drawable.ic_tune_24dp),
            tooltipRes = R.string.display_options,
            isVisible = isSettingsVisible,
        ),
    )
}
