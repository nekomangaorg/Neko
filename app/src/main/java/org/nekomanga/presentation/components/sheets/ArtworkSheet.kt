package org.nekomanga.presentation.components.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes
import kotlin.math.roundToInt

@Composable
fun ArtworkSheet(themeColors: ThemeColors, artworkLinks: List<String>) {
    CompositionLocalProvider(LocalRippleTheme provides themeColors.rippleTheme, LocalTextSelectionColors provides themeColors.textSelectionColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val images = remember {
                mutableStateListOf(
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/dfcaab7a-2c3c-4ea5-8641-abffd2a95b5f.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/512496fb-6e57-483f-9380-aa6027d4f157.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/d9497f0d-3bd7-42d9-832c-696ff39a6a28.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/bb38cabc-769b-4b6c-b7c1-dc3a933cd3c9.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/e393ec1a-320d-4ef7-92de-ca84b0d20309.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/17acc2b0-2cab-46f2-954d-91b1174db67e.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/5a2e4c1d-696e-4983-ad9c-67eba37c0daa.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/1b266184-eb7a-4801-9ad6-8f53ac8acb47.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/67cc4435-16cc-4d32-8163-a82a681b826e.jpg",
                    "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/8b4b0dec-d3f9-4471-be67-46ea386164a1.jpg",

                    )
            }
            var currentImage by rememberSaveable { mutableStateOf(images[0]) }

            val screenHeight = LocalConfiguration.current.screenHeightDp
            val thumbnailHeight = screenHeight * .15f

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier.weight(1f)) {
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
                    items(images) { url ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
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
                                    currentImage = url
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
                    FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                        Text(text = "Save", color = MaterialTheme.colorScheme.surface)
                    }
                    Gap(8.dp)
                    FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                        Text(text = "Set", color = MaterialTheme.colorScheme.surface)
                    }
                    Gap(8.dp)
                    FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                        Text(text = "Share", color = MaterialTheme.colorScheme.surface)
                    }
                    Gap(8.dp)
                }
            }
        }
    }
}

@Composable
fun Card(
    url: String,
    advance: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var offsetX = remember(url) { Animatable(0f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(Shapes.coverRadius))
            .background(color = Color.White)
            .clickable {
                coroutineScope.launch {
                    offsetX.animateTo(
                        targetValue = 3000F,
                    )
                }
                coroutineScope.launch {
                    delay(400)
                    advance()
                }
            },
    ) {
        Image(
            painter = rememberAsyncImagePainter(url),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(Shapes.coverRadius)),
        )
    }
}

