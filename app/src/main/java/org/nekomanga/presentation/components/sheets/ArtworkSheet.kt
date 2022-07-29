package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    saveClick: (String) -> Unit,
    setClick: (String) -> Unit,
    shareClick: (String) -> Unit,
    resetClick: () -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        if (alternativeArtwork.isEmpty()) {
            BaseSheet(themeColor = themeColorState) {
                Text(
                    text = "Please swipe refresh to pull latest artwork", textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        } else {

            var currentImage by remember { mutableStateOf(alternativeArtwork.first { it.active }) }

            val screenHeight = LocalConfiguration.current.screenHeightDp
            val thumbnailHeight = screenHeight * .12f
            val imageHeight = screenHeight * .7f
            val gradientHeight = thumbnailHeight / 2

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(imageHeight.dp)
                        .padding(horizontal = 8.dp)
                        .clip(
                            RoundedCornerShape(Shapes.coverRadius),
                        ),
                )

                if (currentImage.description.isNotBlank()) {
                    Text(text = currentImage.description, modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                    Gap(height = 8.dp)
                }

                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)) {

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(alternativeArtwork) { artwork ->
                            Box {
                                Thumbnail(
                                    artwork = artwork, thumbnailHeight = thumbnailHeight.dp,
                                ) {
                                    currentImage = artwork
                                }
                                if (artwork.active) {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(imageVector = Icons.Filled.Star, modifier = Modifier.padding(4.dp), contentDescription = null, tint = themeColorState.buttonColor)
                                    }
                                }
                                if (artwork.volume.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .width(thumbnailHeight.dp)
                                            .height(gradientHeight.dp)
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
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        Arrangement.spacedBy(4.dp),
                    ) {
                        FilledIconButton(
                            onClick = { saveClick(currentImage.url) },
                            modifier = Modifier.weight(1f),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                        ) {
                            Text(text = stringResource(id = R.string.save), color = MaterialTheme.colorScheme.surface)
                        }
                        if (inLibrary) {
                            FilledIconButton(
                                onClick = { setClick(currentImage.url) },
                                modifier = Modifier.weight(1f),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                            ) {
                                Text(text = stringResource(id = R.string.set), color = MaterialTheme.colorScheme.surface)
                            }
                            FilledIconButton(
                                onClick = resetClick,
                                modifier = Modifier.weight(1f),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                            ) {
                                Text(text = stringResource(id = R.string.reset), color = MaterialTheme.colorScheme.surface)
                            }
                        }
                        FilledIconButton(
                            onClick = { shareClick(currentImage.url) },
                            modifier = Modifier.weight(1f),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                        ) {
                            Text(text = stringResource(id = R.string.share), color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(artwork: Artwork, thumbnailHeight: Dp, modifier: Modifier = Modifier, thumbnailClicked: () -> Unit) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(artwork)
            .placeholder(Pastel.getColorLight())
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(thumbnailHeight)
            .clip(
                RoundedCornerShape(Shapes.coverRadius),
            )
            .clickable {
                thumbnailClicked()
            },
    )
}

