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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    uiState: ReaderBottomControlsUiState,
    onAction: (ReaderBottomBarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (uiState.sliderState.position) {
            SliderOrientation.VerticalLeft,
            SliderOrientation.VerticalRight -> {
                val isLeft = uiState.sliderState.position == SliderOrientation.VerticalLeft
                AnimatedVisibility(
                    visible = uiState.isVisible,
                    enter =
                        slideInHorizontally(initialOffsetX = { if (isLeft) -it else it }) +
                            fadeIn(),
                    exit =
                        slideOutHorizontally(targetOffsetX = { if (isLeft) -it else it }) +
                            fadeOut(),
                    modifier =
                        Modifier.align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd),
                ) {
                    VerticalFloatingSlider(
                        state = uiState.sliderState,
                        isLoading = uiState.isLoading,
                        onPageChange = { onAction(ReaderBottomBarAction.PageChanged(it)) },
                        onSkipPrevious = { onAction(ReaderBottomBarAction.SkipPrevious) },
                        onSkipNext = { onAction(ReaderBottomBarAction.SkipNext) },
                        modifier =
                            Modifier.padding(
                                start = if (isLeft) Size.smedium else Size.none,
                                end = if (isLeft) Size.none else Size.smedium,
                                top = Size.appBarHeight + Size.large,
                                bottom = Size.huge + Size.large,
                            ),
                    )
                }

                AnimatedVisibility(
                    visible = uiState.isVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    BottomActionSheet(
                        buttons = uiState.buttons,
                        onButtonClick = { onAction(ReaderBottomBarAction.ButtonClicked(it)) },
                    )
                }
            }
            SliderOrientation.Horizontal -> {
                AnimatedVisibility(
                    visible = uiState.isVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HorizontalFloatingSlider(
                            state = uiState.sliderState,
                            isLoading = uiState.isLoading,
                            onPageChange = { onAction(ReaderBottomBarAction.PageChanged(it)) },
                            onSkipPrevious = { onAction(ReaderBottomBarAction.SkipPrevious) },
                            onSkipNext = { onAction(ReaderBottomBarAction.SkipNext) },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        start = Size.smedium,
                                        end = Size.smedium,
                                        bottom = Size.small,
                                    ),
                        )

                        BottomActionSheet(
                            buttons = uiState.buttons,
                            onButtonClick = { onAction(ReaderBottomBarAction.ButtonClicked(it)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalFloatingSlider(
    state: ReaderSliderUiState,
    isLoading: Boolean,
    onPageChange: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    var draggingValue by remember { mutableStateOf<Float?>(null) }
    var lastValue by remember { mutableIntStateOf(state.currentPageIndex) }

    LaunchedEffect(state.currentPageIndex) {
        if (draggingValue == null) {
            lastValue = state.currentPageIndex
        }
    }

    val isPagesVisible = state.currentPageText.isNotEmpty() && state.totalPagesText.isNotEmpty()

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
                    val leftText = if (state.isRtl) state.totalPagesText else state.currentPageText
                    val rightText = if (state.isRtl) state.currentPageText else state.totalPagesText

                    Text(
                        text = leftText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(Size.huge - Size.tiny),
                    )

                    val sliderLayoutDirection =
                        if (state.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides sliderLayoutDirection) {
                        val targetMax = maxOf(state.totalPages.toFloat(), 1f)
                        val sliderState =
                            remember(targetMax) {
                                SliderState(
                                    value =
                                        state.currentPageIndex.toFloat().coerceIn(0f, targetMax),
                                    trackRange = 0f..targetMax,
                                )
                            }

                        LaunchedEffect(state.currentPageIndex, targetMax) {
                            val expectedSliderValue =
                                state.currentPageIndex.toFloat().coerceIn(0f, targetMax)
                            if (
                                draggingValue == null &&
                                    !sliderState.isDragging &&
                                    sliderState.value.roundToInt() !=
                                        expectedSliderValue.roundToInt()
                            ) {
                                sliderState.value = expectedSliderValue
                            }
                        }

                        Slider(
                            state = sliderState,
                            modifier = Modifier.weight(1f).padding(horizontal = Size.small),
                            onValueChange = { value ->
                                val coercedValue = value.coerceIn(0f, targetMax)
                                sliderState.value = coercedValue
                                draggingValue = coercedValue
                                val roundedValue = coercedValue.roundToInt()
                                if (roundedValue != lastValue) {
                                    lastValue = roundedValue
                                    view.performHapticFeedback(
                                        HapticFeedbackConstants.TEXT_HANDLE_MOVE
                                    )
                                    currentOnPageChange(roundedValue)
                                }
                            },
                            onValueChangeFinished = {
                                val finalValue = draggingValue?.roundToInt() ?: lastValue
                                draggingValue = null
                                lastValue = finalValue
                                currentOnPageChange(finalValue)
                            },
                            colors =
                                SliderDefaults.colors(
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor =
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                ),
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
fun VerticalFloatingSlider(
    state: ReaderSliderUiState,
    isLoading: Boolean,
    onPageChange: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    val currentView by rememberUpdatedState(view)

    var draggingValue by remember { mutableStateOf<Float?>(null) }
    var lastValue by remember { mutableIntStateOf(state.currentPageIndex) }

    LaunchedEffect(state.currentPageIndex) {
        if (draggingValue == null) {
            lastValue = state.currentPageIndex
        }
    }

    val isPagesVisible = state.currentPageText.isNotEmpty() && state.totalPagesText.isNotEmpty()

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
                    .height(Size.squareCoverLarge * 3.5f)
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
                            state.currentPageText
                        }

                    Text(
                        text = displayCurrentPage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )

                    val targetMax = maxOf(state.totalPages.toFloat(), 1f)
                    val sliderState =
                        remember(targetMax) {
                            SliderState(
                                value = state.currentPageIndex.toFloat().coerceIn(0f, targetMax),
                                trackRange = 0f..targetMax,
                            )
                        }

                    LaunchedEffect(state.currentPageIndex, targetMax) {
                        val expectedSliderValue =
                            state.currentPageIndex.toFloat().coerceIn(0f, targetMax)
                        if (
                            draggingValue == null &&
                                !sliderState.isDragging &&
                                sliderState.value.roundToInt() != expectedSliderValue.roundToInt()
                        ) {
                            sliderState.value = expectedSliderValue
                        }
                    }

                    VerticalSlider(
                        state = sliderState,
                        modifier = Modifier.weight(1f).padding(vertical = Size.small),
                        onValueChange = { value ->
                            val coercedValue = value.coerceIn(0f, targetMax)
                            sliderState.value = coercedValue
                            draggingValue = coercedValue
                            val roundedValue = coercedValue.roundToInt()
                            if (roundedValue != lastValue) {
                                lastValue = roundedValue
                                currentView.performHapticFeedback(
                                    HapticFeedbackConstants.TEXT_HANDLE_MOVE
                                )
                                currentOnPageChange(roundedValue)
                            }
                        },
                        onValueChangeFinished = {
                            val finalValue = draggingValue?.roundToInt() ?: lastValue
                            draggingValue = null
                            lastValue = finalValue
                            currentOnPageChange(finalValue)
                        },
                        colors =
                            SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor =
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                thumbColor = MaterialTheme.colorScheme.primary,
                            ),
                    )

                    Text(
                        text = state.totalPagesText,
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
fun BottomActionSheet(
    buttons: List<ReaderToolbarButtonUiModel>,
    onButtonClick: (ReaderBottomActionId) -> Unit,
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
                buttons.forEach { button ->
                    key(button.id) {
                        if (button.isVisible) {
                            val enabledTint =
                                when (button.toggleStyle) {
                                    ButtonToggleStyle.PrimaryWhenToggled ->
                                        if (button.isToggled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    ButtonToggleStyle.None -> MaterialTheme.colorScheme.onSurface
                                }
                            when (val icon = button.icon) {
                                is ReaderButtonIcon.Vector -> {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(button.tooltipRes),
                                        icon = icon.imageVector,
                                        enabledTint = enabledTint,
                                        onClick = { onButtonClick(button.id) },
                                    )
                                }
                                is ReaderButtonIcon.Resource -> {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(button.tooltipRes),
                                        painter = painterResource(id = icon.id),
                                        enabledTint = enabledTint,
                                        onClick = { onButtonClick(button.id) },
                                    )
                                }
                            }
                        }
                    }
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
        val sampleButtons = buildReaderBottomBarButtons()
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Size.medium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Size.medium)) {
                // Horizontal normal page state
                ReaderBottomControls(
                    uiState =
                        ReaderBottomControlsUiState(
                            isVisible = true,
                            isLoading = false,
                            sliderState =
                                ReaderSliderUiState(
                                    currentPageText = "1",
                                    totalPagesText = "24",
                                    currentPageIndex = 0,
                                    totalPages = 23,
                                    isRtl = false,
                                    position = SliderOrientation.Horizontal,
                                ),
                            buttons = sampleButtons,
                        ),
                    onAction = {},
                )
                // Vertical right-aligned page state
                ReaderBottomControls(
                    uiState =
                        ReaderBottomControlsUiState(
                            isVisible = true,
                            isLoading = false,
                            sliderState =
                                ReaderSliderUiState(
                                    currentPageText = "1",
                                    totalPagesText = "24",
                                    currentPageIndex = 0,
                                    totalPages = 23,
                                    isRtl = false,
                                    position = SliderOrientation.VerticalRight,
                                ),
                            buttons = sampleButtons,
                        ),
                    onAction = {},
                )
                // Vertical left-aligned page state
                ReaderBottomControls(
                    uiState =
                        ReaderBottomControlsUiState(
                            isVisible = true,
                            isLoading = false,
                            sliderState =
                                ReaderSliderUiState(
                                    currentPageText = "1",
                                    totalPagesText = "24",
                                    currentPageIndex = 0,
                                    totalPages = 23,
                                    isRtl = false,
                                    position = SliderOrientation.VerticalLeft,
                                ),
                            buttons = sampleButtons,
                        ),
                    onAction = {},
                )
                // Vertical horizontal-slider page state
                ReaderBottomControls(
                    uiState =
                        ReaderBottomControlsUiState(
                            isVisible = true,
                            isLoading = false,
                            sliderState =
                                ReaderSliderUiState(
                                    currentPageText = "1",
                                    totalPagesText = "24",
                                    currentPageIndex = 0,
                                    totalPages = 23,
                                    isRtl = false,
                                    position = SliderOrientation.Horizontal,
                                ),
                            buttons = sampleButtons,
                        ),
                    onAction = {},
                )
                // Transition page state
                ReaderBottomControls(
                    uiState =
                        ReaderBottomControlsUiState(
                            isVisible = true,
                            isLoading = false,
                            sliderState =
                                ReaderSliderUiState(
                                    currentPageText = "",
                                    totalPagesText = "",
                                    currentPageIndex = 0,
                                    totalPages = 1,
                                    isRtl = false,
                                    position = SliderOrientation.Horizontal,
                                ),
                            buttons = sampleButtons,
                        ),
                    onAction = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun HorizontalFloatingSliderPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Size.medium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Size.medium)) {
                HorizontalFloatingSlider(
                    state =
                        ReaderSliderUiState(
                            currentPageText = "5",
                            totalPagesText = "30",
                            currentPageIndex = 4,
                            totalPages = 29,
                        ),
                    isLoading = false,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                )
                HorizontalFloatingSlider(
                    state =
                        ReaderSliderUiState(
                            currentPageText = "12",
                            totalPagesText = "45",
                            currentPageIndex = 11,
                            totalPages = 44,
                            isRtl = true,
                        ),
                    isLoading = true,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun VerticalFloatingSliderPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Size.medium)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Size.medium)) {
                VerticalFloatingSlider(
                    state =
                        ReaderSliderUiState(
                            currentPageText = "5",
                            totalPagesText = "30",
                            currentPageIndex = 4,
                            totalPages = 29,
                        ),
                    isLoading = false,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                )
                VerticalFloatingSlider(
                    state =
                        ReaderSliderUiState(
                            currentPageText = "1",
                            totalPagesText = "10",
                            currentPageIndex = 0,
                            totalPages = 9,
                        ),
                    isLoading = true,
                    onPageChange = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun BottomActionSheetPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Size.medium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Size.medium)) {
                BottomActionSheet(
                    buttons = buildReaderBottomBarButtons(),
                    onButtonClick = {},
                )
                BottomActionSheet(
                    buttons =
                        buildReaderBottomBarButtons(
                            cropBorders = true,
                            grayscale = true,
                            isDoublePage = true,
                        ),
                    onButtonClick = {},
                )
            }
        }
    }
}
