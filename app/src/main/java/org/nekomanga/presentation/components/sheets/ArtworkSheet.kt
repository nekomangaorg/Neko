package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun ArtworkSheet(
    themeColorState: ThemeColorState,
    alternativeArtwork: List<Artwork>,
    inLibrary: Boolean,
    saveClick: (Artwork) -> Unit,
    setClick: (Artwork) -> Unit,
    shareClick: (Artwork) -> Unit,
    resetClick: () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        var currentImage by
            remember(alternativeArtwork) {
                mutableStateOf(
                    alternativeArtwork.firstOrNull { it.active } ?: alternativeArtwork.firstOrNull()
                )
            }

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val (imageHeight, thumbnailSize, gradientHeight) =
            remember(screenHeight) {
                val thumbSize = screenHeight * 0.12f
                Triple(screenHeight * 0.7f, thumbSize, thumbSize / 2f)
            }

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
            if (alternativeArtwork.isEmpty() || currentImage == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_artwork_found),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val context = LocalContext.current

                Text(
                    text = currentImage!!.description,
                    modifier = Modifier.padding(horizontal = Size.small),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                )

                val mainImageRequest =
                    remember(currentImage) {
                        ImageRequest.Builder(context).data(currentImage).build()
                    }
                Box(
                    modifier =
                        Modifier.padding(horizontal = Size.small)
                            .heightIn(imageHeight / 2, imageHeight)
                ) {
                    AsyncImage(
                        model = mainImageRequest,
                        contentDescription = stringResource(R.string.artwork),
                        contentScale = ContentScale.Fit,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Gap(Size.tiny)
                    ActionButtons(
                        inLibrary = inLibrary,
                        themeColorState = themeColorState,
                        onSave = { currentImage?.let(saveClick) },
                        onSet = { currentImage?.let(setClick) },
                        onReset = resetClick,
                        onShare = { currentImage?.let(shareClick) },
                    )
                    if (alternativeArtwork.size > 1) {
                        Gap(Size.small)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Size.tiny)) {
                            items(alternativeArtwork, key = { it.url }) { artwork ->
                                ArtworkThumbnail(
                                    artwork = artwork,
                                    themeColorState = themeColorState,
                                    thumbnailSize = thumbnailSize,
                                    gradientHeight = gradientHeight,
                                    onClick = { currentImage = artwork },
                                )
                            }
                        }
                    }
                    Gap(Size.small)
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    inLibrary: Boolean,
    themeColorState: ThemeColorState,
    onSave: () -> Unit,
    onSet: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.tiny),
        horizontalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        ArtworkButton(
            text = stringResource(id = R.string.save),
            color = themeColorState.primaryColor,
            modifier = Modifier.weight(1f),
            onClick = onSave,
        )
        if (inLibrary) {
            ArtworkButton(
                text = stringResource(id = R.string.set),
                color = themeColorState.primaryColor,
                modifier = Modifier.weight(1f),
                onClick = onSet,
            )
            ArtworkButton(
                text = stringResource(id = R.string.reset),
                color = themeColorState.primaryColor,
                modifier = Modifier.weight(1f),
                onClick = onReset,
            )
        }
        ArtworkButton(
            text = stringResource(id = R.string.share),
            color = themeColorState.primaryColor,
            modifier = Modifier.weight(1f),
            onClick = onShare,
        )
    }
}

@Composable
private fun ArtworkButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = color,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Text(text = text)
    }
}

@Composable
private fun ArtworkThumbnail(
    artwork: Artwork,
    themeColorState: ThemeColorState,
    thumbnailSize: Dp,
    gradientHeight: Dp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val request = remember(artwork) { ImageRequest.Builder(context).data(artwork).build() }
    Box(
        modifier =
            Modifier.size(thumbnailSize)
                .clip(RoundedCornerShape(Shapes.coverRadius))
                .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = request,
            placeholder = ColorPainter(colorResource(Pastel.getColorLight())),
            contentDescription = stringResource(id = R.string.artwork),
            contentScale = ContentScale.Crop,
            modifier = Modifier.aspectRatio(1f / 1f),
        )
        if (artwork.active) {
            ActiveIndicator(themeColorState = themeColorState)
        }
        if (artwork.volume.isNotBlank()) {
            VolumeLabel(
                volume = artwork.volume,
                thumbnailSize = thumbnailSize,
                gradientHeight = gradientHeight,
            )
        }
    }
}

@Composable
private fun BoxScope.VolumeLabel(volume: String, thumbnailSize: Dp, gradientHeight: Dp) {
    Box(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .width(thumbnailSize)
                .height(gradientHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.onSurface)
                    )
                )
    )
    Text(
        text = volume,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Size.tiny),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun ActiveIndicator(themeColorState: ThemeColorState) {
    Box(
        modifier =
            Modifier.padding(Size.tiny)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            modifier = Modifier.padding(Size.tiny),
            contentDescription = null,
            tint = themeColorState.primaryColor,
        )
    }
}
