package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Shapes

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
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        if (alternativeArtwork.isEmpty()) {
            BaseSheet(themeColor = themeColorState) {
                Text(
                    text = "Please swipe refresh to pull latest artwork",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        } else {
            var currentImage by remember { mutableStateOf(alternativeArtwork.first { it.active }) }

            val screenHeight = LocalConfiguration.current.screenHeightDp
            val thumbnailSize = (screenHeight * .12f).dp
            val imageHeight = screenHeight * .7f
            val gradientHeight = (thumbnailSize / 2f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.95f)
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentImage)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(imageHeight.dp)
                        .padding(horizontal = 8.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    Arrangement.spacedBy(4.dp),
                ) {
                    ArtworkButton(text = stringResource(id = R.string.save), color = themeColorState.buttonColor, modifier = Modifier.weight(1f)) {
                        saveClick(currentImage)
                    }
                    if (inLibrary) {
                        ArtworkButton(text = stringResource(id = R.string.set), color = themeColorState.buttonColor, modifier = Modifier.weight(1f)) {
                            setClick(currentImage)
                        }
                        ArtworkButton(text = stringResource(id = R.string.reset), color = themeColorState.buttonColor, modifier = Modifier.weight(1f)) {
                            resetClick()
                        }
                    }
                    ArtworkButton(text = stringResource(id = R.string.share), color = themeColorState.buttonColor, modifier = Modifier.weight(1f)) {
                        shareClick(currentImage)
                    }
                }
                if (currentImage.description.isNotBlank()) {
                    Text(text = currentImage.description, modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                }
                Gap(height = 8.dp)
                if (alternativeArtwork.size > 1) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(alternativeArtwork) { artwork ->
                                Box {
                                    Thumbnail(
                                        artwork = artwork,
                                        thumbnailSize = thumbnailSize,
                                    ) {
                                        currentImage = artwork
                                    }
                                    if (artwork.active) {
                                        ActiveIndicator(themeColorState)
                                    }
                                    if (artwork.volume.isNotBlank()) {
                                        VolumeSection(thumbnailSize, gradientHeight, artwork)
                                    }
                                }
                            }
                        }
                    }
                    Gap(8.dp)
                }
            }
        }
    }
}

@Composable
private fun ArtworkButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(35),
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = color),
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.surface)
    }
}

/**
 * Thumbnail for the artwork sheet
 */
@Composable
private fun Thumbnail(artwork: Artwork, thumbnailSize: Dp, thumbnailClicked: () -> Unit) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(artwork)
            .placeholder(Pastel.getColorLight())
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(thumbnailSize)
            .clip(
                RoundedCornerShape(Shapes.coverRadius),
            )
            .clickable {
                thumbnailClicked()
            },
    )
}

@Composable
private fun BoxScope.VolumeSection(thumbnailSize: Dp, gradientHeight: Dp, artwork: Artwork) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .width(thumbnailSize)
            .height(gradientHeight)
            .clip(RoundedCornerShape(bottomStart = Shapes.coverRadius, bottomEnd = Shapes.coverRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.onSurface),
                ),
            ),
    )
    Text(
        text = artwork.volume,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 4.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun ActiveIndicator(themeColorState: ThemeColorState) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Icon(imageVector = Icons.Filled.Star, modifier = Modifier.padding(4.dp), contentDescription = null, tint = themeColorState.buttonColor)
    }
}
