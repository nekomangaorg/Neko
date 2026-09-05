package org.nekomanga.presentation.screens.reader

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.nekomanga.R
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.components.icons.SkipNext
import org.nekomanga.presentation.components.icons.SkipPrevious
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showShiftDoublePage: Boolean,
    shiftDoublePageIconRes: Int?,
    onShiftDoublePage: () -> Unit,
    visible: Boolean,
    onMangaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier,
    ) {
        TitleTopAppBar(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            onColor = MaterialTheme.colorScheme.onSurface,
            title = title,
            subtitle = subtitle,
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationIconClicked = onBack,
            onTitleClick = onMangaClick,
            incognitoMode = false,
            actions = {
                if (showShiftDoublePage && shiftDoublePageIconRes != null) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.shift_one_page_over),
                        painter = painterResource(id = shiftDoublePageIconRes),
                        onClick = onShiftDoublePage,
                    )
                }
            },
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
        )
    }
}

@Composable
fun ReaderBottomControls(
    currentPageText: String,
    totalPagesText: String,
    currentPageIndex: Int,
    totalPages: Int,
    isRtl: Boolean,
    isVertical: Boolean = false,
    onPageChange: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    visible: Boolean,
    isLoading: Boolean,
    pageNumberVisible: Boolean = false,
    isChaptersVisible: Boolean = true,
    isCommentsVisible: Boolean = true,
    isWebViewVisible: Boolean = true,
    isReadingModeVisible: Boolean = false,
    isRotationVisible: Boolean = false,
    isCropBordersVisible: Boolean = false,
    isGrayscaleVisible: Boolean = false,
    isDoublePageVisible: Boolean = false,
    isShiftPageVisible: Boolean = false,
    isSettingsVisible: Boolean = true,
    cropBorders: Boolean = false,
    grayscale: Boolean = false,
    readingModeIconRes: Int = R.drawable.ic_reader_default_24dp,
    rotationIconRes: Int = R.drawable.ic_screen_rotation_24dp,
    doublePageIconRes: Int = R.drawable.ic_book_open_variant_24dp,
    shiftPageIconRes: Int = R.drawable.ic_page_next_outline_24dp,
    onChaptersClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onWebviewClick: () -> Unit = {},
    onReadingModeClick: () -> Unit = {},
    onRotationClick: () -> Unit = {},
    onCropBordersClick: () -> Unit = {},
    onGrayscaleClick: () -> Unit = {},
    onDoublePageClick: () -> Unit = {},
    onShiftPageClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (isVertical) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                VerticalFloatingSlider(
                    currentPageText = currentPageText,
                    totalPagesText = totalPagesText,
                    currentPageIndex = currentPageIndex,
                    totalPages = totalPages,
                    onPageChange = onPageChange,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    isLoading = isLoading,
                    modifier =
                        Modifier.padding(
                            end = Size.smedium,
                            top = Size.appBarHeight + Size.large,
                            bottom = Size.huge + Size.large,
                        ),
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomActionSheet(
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
                    onChaptersClick = onChaptersClick,
                    onCommentsClick = onCommentsClick,
                    onWebviewClick = onWebviewClick,
                    onReadingModeClick = onReadingModeClick,
                    onRotationClick = onRotationClick,
                    onCropBordersClick = onCropBordersClick,
                    onGrayscaleClick = onGrayscaleClick,
                    onDoublePageClick = onDoublePageClick,
                    onShiftPageClick = onShiftPageClick,
                    onSettingsClick = onSettingsClick,
                )
            }
        } else {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HorizontalFloatingSlider(
                        currentPageText = currentPageText,
                        totalPagesText = totalPagesText,
                        currentPageIndex = currentPageIndex,
                        totalPages = totalPages,
                        isRtl = isRtl,
                        onPageChange = onPageChange,
                        onSkipPrevious = onSkipPrevious,
                        onSkipNext = onSkipNext,
                        isLoading = isLoading,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(
                                    start = Size.smedium,
                                    end = Size.smedium,
                                    bottom = Size.small,
                                ),
                    )

                    BottomActionSheet(
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
                        onChaptersClick = onChaptersClick,
                        onCommentsClick = onCommentsClick,
                        onWebviewClick = onWebviewClick,
                        onReadingModeClick = onReadingModeClick,
                        onRotationClick = onRotationClick,
                        onCropBordersClick = onCropBordersClick,
                        onGrayscaleClick = onGrayscaleClick,
                        onDoublePageClick = onDoublePageClick,
                        onShiftPageClick = onShiftPageClick,
                        onSettingsClick = onSettingsClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalFloatingSlider(
    currentPageText: String,
    totalPagesText: String,
    currentPageIndex: Int,
    totalPages: Int,
    isRtl: Boolean,
    onPageChange: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var draggingValue by remember { mutableStateOf<Float?>(null) }
    var lastValue by remember(currentPageIndex) { mutableIntStateOf(currentPageIndex) }

    val isPagesVisible = currentPageText.isNotEmpty() && totalPagesText.isNotEmpty()

    Row(
        horizontalArrangement = Arrangement.spacedBy(Size.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(Size.extraHuge)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Size.large),
                    strokeWidth = Size.extraTiny,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                ToolTipButton(
                    toolTipLabel = stringResource(R.string.previous_chapter),
                    icon = SkipPrevious,
                    iconModifier = Modifier.size(Size.largePlus),
                    enabledTint = MaterialTheme.colorScheme.primary,
                    onClick = onSkipPrevious,
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.weight(1f)
                    .height(Size.extraHuge)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    )
                    .padding(horizontal = Size.smedium),
        ) {
            if (isPagesVisible) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val leftText = if (isRtl) totalPagesText else currentPageText
                    val rightText = if (isRtl) currentPageText else totalPagesText

                    Text(
                        text = leftText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(Size.huge - Size.tiny),
                    )

                    val sliderLayoutDirection =
                        if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides sliderLayoutDirection) {
                        val targetMax = maxOf(totalPages.toFloat(), 1f)
                        val displayValue =
                            (draggingValue ?: currentPageIndex.toFloat()).coerceIn(
                                0f,
                                targetMax,
                            )
                        Slider(
                            value = displayValue,
                            onValueChange = { value ->
                                draggingValue = value
                                val roundedValue = value.roundToInt()
                                if (roundedValue != lastValue) {
                                    lastValue = roundedValue
                                    view.performHapticFeedback(
                                        HapticFeedbackConstants.TEXT_HANDLE_MOVE
                                    )
                                    onPageChange(roundedValue)
                                }
                            },
                            onValueChangeFinished = {
                                val finalValue = lastValue
                                draggingValue = null
                                onPageChange(finalValue)
                            },
                            valueRange = 0f..targetMax,
                            colors =
                                SliderDefaults.colors(
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor =
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                ),
                            modifier = Modifier.weight(1f).padding(horizontal = Size.small),
                        )
                    }

                    Text(
                        text = rightText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(Size.huge - Size.tiny),
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(Size.extraHuge)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Size.large),
                    strokeWidth = Size.extraTiny,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                ToolTipButton(
                    toolTipLabel = stringResource(R.string.next_chapter),
                    icon = SkipNext,
                    iconModifier = Modifier.size(Size.largePlus),
                    enabledTint = MaterialTheme.colorScheme.primary,
                    onClick = onSkipNext,
                )
            }
        }
    }
}

@Composable
private fun VerticalFloatingSlider(
    currentPageText: String,
    totalPagesText: String,
    currentPageIndex: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var draggingValue by remember { mutableStateOf<Float?>(null) }
    var lastValue by remember(currentPageIndex) { mutableIntStateOf(currentPageIndex) }

    val isPagesVisible = currentPageText.isNotEmpty() && totalPagesText.isNotEmpty()

    Column(
        verticalArrangement = Arrangement.spacedBy(Size.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(Size.extraHuge)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Size.large),
                    strokeWidth = Size.extraTiny,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                ToolTipButton(
                    toolTipLabel = stringResource(R.string.previous_chapter),
                    modifier = Modifier.rotate(90f),
                    iconModifier = Modifier.size(Size.largePlus),
                    icon = SkipPrevious,
                    enabledTint = MaterialTheme.colorScheme.primary,
                    onClick = onSkipPrevious,
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.width(Size.extraHuge)
                    .height(Size.squareCoverLarge * 2.5f)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    )
                    .padding(vertical = Size.smedium, horizontal = Size.tiny),
        ) {
            if (isPagesVisible) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    val displayCurrentPage =
                        if (draggingValue != null) {
                            (draggingValue!!.roundToInt() + 1).toString()
                        } else {
                            currentPageText
                        }

                    Text(
                        text = displayCurrentPage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )

                    val targetMax = maxOf(totalPages.toFloat(), 1f)
                    val sliderState =
                        remember(targetMax) {
                            SliderState(
                                value = (targetMax - currentPageIndex).coerceIn(0f, targetMax),
                                valueRange = 0f..targetMax,
                                onValueChangeFinished = {
                                    val finalValue = lastValue
                                    draggingValue = null
                                    onPageChange(finalValue)
                                },
                            )
                        }

                    LaunchedEffect(currentPageIndex, targetMax) {
                        val expectedSliderValue =
                            (targetMax - currentPageIndex).coerceIn(0f, targetMax)
                        if (
                            draggingValue == null &&
                                sliderState.value.roundToInt() != expectedSliderValue.roundToInt()
                        ) {
                            sliderState.value = expectedSliderValue
                        }
                    }

                    LaunchedEffect(sliderState.value, targetMax) {
                        val rawPage = (targetMax - sliderState.value).coerceIn(0f, targetMax)
                        val roundedPage = rawPage.roundToInt()
                        if (roundedPage != lastValue) {
                            draggingValue = rawPage
                            lastValue = roundedPage
                            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                            onPageChange(roundedPage)
                        }
                    }

                    VerticalSlider(
                        state = sliderState,
                        colors =
                            SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor =
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                thumbColor = MaterialTheme.colorScheme.primary,
                            ),
                        modifier = Modifier.weight(1f).padding(vertical = Size.small),
                    )

                    Text(
                        text = totalPagesText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(Size.extraHuge)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Size.large),
                    strokeWidth = Size.extraTiny,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                ToolTipButton(
                    modifier = Modifier.rotate(90f),
                    toolTipLabel = stringResource(R.string.next_chapter),
                    iconModifier = Modifier.size(Size.largePlus),
                    icon = SkipNext,
                    enabledTint = MaterialTheme.colorScheme.primary,
                    onClick = onSkipNext,
                )
            }
        }
    }
}

@Composable
private fun BottomActionSheet(
    isChaptersVisible: Boolean,
    isCommentsVisible: Boolean,
    isWebViewVisible: Boolean,
    isReadingModeVisible: Boolean,
    isRotationVisible: Boolean,
    isCropBordersVisible: Boolean,
    isGrayscaleVisible: Boolean,
    isDoublePageVisible: Boolean,
    isShiftPageVisible: Boolean,
    isSettingsVisible: Boolean,
    cropBorders: Boolean,
    grayscale: Boolean,
    readingModeIconRes: Int,
    rotationIconRes: Int,
    doublePageIconRes: Int,
    shiftPageIconRes: Int,
    onChaptersClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onWebviewClick: () -> Unit,
    onReadingModeClick: () -> Unit,
    onRotationClick: () -> Unit,
    onCropBordersClick: () -> Unit,
    onGrayscaleClick: () -> Unit,
    onDoublePageClick: () -> Unit,
    onShiftPageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape =
                        RoundedCornerShape(
                            topStart = Size.largePlus,
                            topEnd = Size.largePlus,
                        ),
                )
                .navigationBarsPadding()
                .padding(horizontal = Size.smedium, vertical = Size.smedium)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.widthIn(min = maxWidth).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isChaptersVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.view_chapters),
                        painter = painterResource(id = R.drawable.ic_format_list_numbered_24dp),
                        onClick = onChaptersClick,
                    )
                }
                if (isCommentsVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.comments),
                        painter = painterResource(id = R.drawable.ic_view_comments_24p),
                        onClick = onCommentsClick,
                    )
                }
                if (isWebViewVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.open_in_webview),
                        painter = painterResource(id = R.drawable.ic_open_in_webview_24dp),
                        onClick = onWebviewClick,
                    )
                }
                if (isReadingModeVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.reading_mode),
                        painter = painterResource(id = readingModeIconRes),
                        onClick = onReadingModeClick,
                    )
                }
                if (isRotationVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.rotation),
                        painter = painterResource(id = rotationIconRes),
                        onClick = onRotationClick,
                    )
                }
                if (isCropBordersVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.crop_borders),
                        icon = if (cropBorders) Icons.Default.CropFree else Icons.Default.Crop,
                        enabledTint =
                            if (cropBorders) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        onClick = onCropBordersClick,
                    )
                }
                if (isGrayscaleVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.grayscale_toggle),
                        painter = painterResource(id = R.drawable.ic_palette),
                        enabledTint =
                            if (grayscale) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        onClick = onGrayscaleClick,
                    )
                }
                if (isDoublePageVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.double_pages),
                        painter = painterResource(id = doublePageIconRes),
                        onClick = onDoublePageClick,
                    )
                }
                if (isShiftPageVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.shift_one_page_over),
                        painter = painterResource(id = shiftPageIconRes),
                        onClick = onShiftPageClick,
                    )
                }
                if (isSettingsVisible) {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.display_options),
                        painter = painterResource(id = R.drawable.ic_tune_24dp),
                        onClick = onSettingsClick,
                    )
                }
            }
        }
    }
}

@Composable
fun PageNumberIndicator(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(Color.Transparent).padding(Size.tiny),
    ) {
        // Outer outline text (black outline rendered behind)
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    drawStyle =
                        Stroke(
                            miter = 10f,
                            width = 6f,
                            join = StrokeJoin.Round,
                        ),
                ),
        )
        // Middle outline text (primary color outline)
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    drawStyle =
                        Stroke(
                            miter = 10f,
                            width = 4f,
                            join = StrokeJoin.Round,
                        ),
                ),
        )
        // Fill text (onPrimary color number rendered in front)
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                ),
        )
    }
}

@Preview
@Composable
private fun PageNumberIndicatorPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Size.medium)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Size.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PageNumberIndicator(text = "1/24")
                PageNumberIndicator(text = "12/45")
                PageNumberIndicator(text = "128/350")
            }
        }
    }
}

@Preview
@Composable
private fun ReaderBottomControlsPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Size.medium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Size.medium)) {
                // Horizontal normal page state
                ReaderBottomControls(
                    currentPageText = "1",
                    totalPagesText = "24",
                    currentPageIndex = 0,
                    totalPages = 23,
                    isRtl = false,
                    isVertical = false,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                    visible = true,
                    isLoading = false,
                    pageNumberVisible = true,
                )
                // Vertical normal page state
                ReaderBottomControls(
                    currentPageText = "1",
                    totalPagesText = "24",
                    currentPageIndex = 0,
                    totalPages = 23,
                    isRtl = false,
                    isVertical = true,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                    visible = true,
                    isLoading = false,
                    pageNumberVisible = true,
                )
                // Transition page state
                ReaderBottomControls(
                    currentPageText = "",
                    totalPagesText = "",
                    currentPageIndex = 0,
                    totalPages = 1,
                    isRtl = false,
                    isVertical = false,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                    visible = true,
                    isLoading = false,
                    pageNumberVisible = true,
                )
            }
        }
    }
}
