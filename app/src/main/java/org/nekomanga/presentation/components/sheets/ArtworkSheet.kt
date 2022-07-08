package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {

            var currentImage by remember { mutableStateOf(alternativeArtwork[0]) }

            val screenHeight = LocalConfiguration.current.screenHeightDp
            val thumbnailHeight = screenHeight * .15f

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(Shapes.coverRadius),
                            ),
                    )
                }
                Gap(4.dp)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(alternativeArtwork) { artwork ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artwork)
                                .placeholder(Pastel.getColorLight())
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(thumbnailHeight.dp)
                                .aspectRatio(3f / 4f)
                                .clip(
                                    RoundedCornerShape(Shapes.coverRadius),
                                )
                                .clickable {
                                    currentImage = artwork
                                },
                        )
                    }
                }

                Gap(4.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Gap(8.dp)
                    FilledIconButton(
                        onClick = { saveClick(currentImage.url) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                    ) {
                        Text(text = stringResource(id = R.string.save), color = MaterialTheme.colorScheme.surface)
                    }
                    if (inLibrary) {
                        Gap(8.dp)
                        FilledIconButton(
                            onClick = { setClick(currentImage.url) },
                            modifier = Modifier.weight(1f),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                        ) {
                            Text(text = stringResource(id = R.string.set), color = MaterialTheme.colorScheme.surface)
                        }
                        Gap(8.dp)
                        FilledIconButton(
                            onClick = resetClick,
                            modifier = Modifier.weight(1f),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                        ) {
                            Text(text = stringResource(id = R.string.reset), color = MaterialTheme.colorScheme.surface)
                        }
                    }
                    Gap(8.dp)
                    FilledIconButton(
                        onClick = { shareClick(currentImage.url) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColorState.buttonColor),
                    ) {
                        Text(text = stringResource(id = R.string.share), color = MaterialTheme.colorScheme.surface)
                    }
                    Gap(8.dp)
                }
            }
        }
    }
}

