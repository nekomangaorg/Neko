package org.nekomanga.presentation.screens.reader

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.nekomanga.R
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

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
                    IconButton(onClick = onShiftDoublePage) {
                        Icon(
                            painter = painterResource(id = shiftDoublePageIconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
    onPageChange: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    visible: Boolean,
    isLoading: Boolean,
    onChaptersClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onWebviewClick: () -> Unit,
    onSettingsClick: () -> Unit,
    pageNumberVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var lastValue by remember(currentPageIndex) { mutableStateOf(currentPageIndex) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        val bottomPadding = if (pageNumberVisible) Size.extraLarge + Size.tiny else Size.small
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = Size.smedium,
                        end = Size.smedium,
                        top = Size.small,
                        bottom = bottomPadding,
                    )
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    )
                    .padding(horizontal = Size.smedium, vertical = Size.small - Size.extraTiny)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(Size.huge - Size.small),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Size.large),
                                strokeWidth = Size.extraTiny,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            IconButton(onClick = onSkipPrevious) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_skip_previous_24),
                                    contentDescription = stringResource(R.string.previous_chapter),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    val leftText = if (isRtl) totalPagesText else currentPageText
                    val rightText = if (isRtl) currentPageText else totalPagesText

                    Text(
                        text = leftText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(Size.huge - Size.tiny),
                    )

                    val sliderLayoutDirection =
                        if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides sliderLayoutDirection) {
                        Slider(
                            value = currentPageIndex.toFloat(),
                            steps = totalPages,
                            onValueChange = { value ->
                                val roundedValue = value.roundToInt()
                                if (roundedValue != lastValue) {
                                    lastValue = roundedValue
                                    view.performHapticFeedback(
                                        HapticFeedbackConstants.TEXT_HANDLE_MOVE
                                    )
                                }
                                onPageChange(roundedValue)
                            },
                            valueRange = 0f..maxOf(totalPages.toFloat(), 1f),
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(Size.huge - Size.tiny),
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(Size.huge - Size.small),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Size.large),
                                strokeWidth = Size.extraTiny,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            IconButton(onClick = onSkipNext) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_skip_next_24),
                                    contentDescription = stringResource(R.string.next_chapter),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Size.tiny))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onChaptersClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_format_list_numbered_24dp),
                            contentDescription = stringResource(R.string.view_chapters),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onCommentsClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_view_comments_24p),
                            contentDescription = stringResource(R.string.comments),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onWebviewClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_in_webview_24dp),
                            contentDescription = stringResource(R.string.open_in_webview),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tune_24dp),
                            contentDescription = stringResource(R.string.display_options),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
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
        // Outline text (rendered behind)
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D),
                    drawStyle =
                        Stroke(
                            miter = 10f,
                            width = 4f,
                            join = StrokeJoin.Round,
                        ),
                ),
        )
        // Fill text (rendered in front)
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEBEBEB),
                ),
        )
    }
}
