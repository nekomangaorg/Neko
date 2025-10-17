package org.nekomanga.presentation.screens.mangadetails

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.HotelClass
import androidx.compose.material.icons.outlined._18UpRating
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdLang
import java.text.NumberFormat
import java.util.Locale
import jp.wasabeef.gap.Gap
import kotlin.math.roundToInt
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.manga.Stats
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NoRippleText
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun InformationBlock(
    themeColorState: ThemeColorState,
    // 1. API Simplified: Pass values directly instead of lambda providers.
    title: String,
    author: String,
    artist: String,
    stats: Stats?,
    langFlag: String?,
    status: Int,
    lastChapter: Pair<Int?, Int?>,
    isPornographic: Boolean,
    missingChapters: String?,
    estimatedMissingChapters: String?,
    isExpanded: Boolean,
    showMergedIcon: Boolean,
    modifier: Modifier = Modifier,
    titleLongClick: (String) -> Unit = {},
    creatorCopyClick: (String) -> Unit = {},
    creatorSearchClick: (String) -> Unit = {},
) {

    val context = LocalContext.current

    val highAlpha =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.highAlphaLowContrast)
    val mediumAlpha =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    val creatorText =
        remember(author, artist) {
            if (author.trim() == artist.trim()) {
                author.trim()
            } else {
                listOfNotNull(
                        author.trim().takeIf { it.isNotBlank() },
                        artist.trim().takeIf { it.isNotBlank() },
                    )
                    .joinToString(Constants.SEPARATOR)
            }
        }

    val statusText =
        remember(status, lastChapter) {
            val statusLine = mutableListOf<String>()
            if (status != 0) {
                val statusRes =
                    when (status) {
                        SManga.ONGOING -> R.string.ongoing
                        SManga.COMPLETED -> R.string.completed
                        SManga.LICENSED -> R.string.licensed
                        SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                        SManga.HIATUS -> R.string.hiatus
                        SManga.CANCELLED -> R.string.cancelled
                        else -> R.string.unknown
                    }
                statusLine.add(context.getString(statusRes))
            }

            val (volume, chapter) = lastChapter
            if (volume != null || chapter != null) {
                val last = if (chapter != null) "Chapter" else "Volume"
                val lastText =
                    "Final $last: " +
                        listOfNotNull(volume?.let { "Vol.$it" }, chapter?.let { "Ch.$it" })
                            .joinToString()
                statusLine.add(lastText)
            }
            statusLine.joinToString(Constants.SEPARATOR)
        }

    Column(modifier = modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = Size.small)) {
        if (title.isNotEmpty()) {
            NoRippleText(
                text = title,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                onLongClick = { titleLongClick(title) },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = highAlpha,
            )
        }

        if (creatorText.isNotEmpty()) {
            var creatorExpanded by remember { mutableStateOf(false) }
            Gap(Size.tiny)
            NoRippleText(
                text = creatorText,
                onClick = { creatorExpanded = !creatorExpanded },
                maxLines = if (isExpanded) 5 else 2,
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
            CreatorDropdown(
                expanded = creatorExpanded,
                onDismiss = { creatorExpanded = false },
                themeColorState = themeColorState,
                creatorText = creatorText,
                onCopy = { creator ->
                    creatorExpanded = false
                    creatorCopyClick(creator)
                },
                onSearch = { creator ->
                    creatorExpanded = false
                    creatorSearchClick(creator)
                },
            )
        }

        if (statusText.isNotEmpty()) {
            Gap(Size.tiny)
            NoRippleText(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
        }

        // Stats are grouped into a FlowRow for responsiveness.
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = Size.tiny),
            horizontalArrangement = Arrangement.spacedBy(Size.small),
            verticalArrangement = Arrangement.spacedBy(Size.tiny),
        ) {
            langFlag?.let { LanguageFlag(langFlag = it) }

            if (isPornographic) {
                Icon(
                    imageVector = Icons.Outlined._18UpRating,
                    contentDescription = "Pornographic content",
                    tint = Color.Red,
                )
            }

            stats?.let {
                val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
                it.rating.toDoubleOrNull()?.let { rating ->
                    val formattedRating = ((rating * 100).roundToInt() / 100.0).toString()
                    StatItem(
                        text = formattedRating,
                        icon = Icons.Filled.HotelClass,
                        color = mediumAlpha,
                    )
                }
                it.follows.toIntOrNull()?.let { follows ->
                    StatItem(
                        text = numberFormat.format(follows),
                        icon = Icons.Filled.Bookmarks,
                        color = mediumAlpha,
                    )
                }
                it.repliesCount.toIntOrNull()?.let { replies ->
                    StatItem(
                        text = numberFormat.format(replies),
                        icon = Icons.AutoMirrored.Filled.Comment,
                        color = mediumAlpha,
                    )
                }
            }

            if (showMergedIcon) {
                StatItem(text = "", color = mediumAlpha) {
                    com.mikepenz.iconics.compose.Image(
                        asset = CommunityMaterial.Icon.cmd_check_decagram,
                        colorFilter = ColorFilter.tint(mediumAlpha),
                    )
                }
            }
        }

        if (!missingChapters.isNullOrBlank()) {
            var showEstimatedMissingChapters by remember { mutableStateOf(false) }
            Gap(Size.tiny)
            NoRippleText(
                text = stringResource(id = R.string.missing_chapters, missingChapters),
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
                onClick = { showEstimatedMissingChapters = !showEstimatedMissingChapters },
            )
            AnimatedVisibility(
                visible = showEstimatedMissingChapters && !estimatedMissingChapters.isNullOrBlank()
            ) {
                NoRippleText(
                    modifier = Modifier.padding(top = Size.tiny),
                    text = estimatedMissingChapters!!,
                    maxLines = 4,
                    style = MaterialTheme.typography.bodySmall,
                    color = mediumAlpha,
                )
            }
        }
    }
}

// 3. Reusable Components: `StatItem` reduces code duplication significantly.
@Composable
private fun StatItem(text: String, color: Color, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        if (text.isNotBlank()) {
            Gap(Size.tiny)
            NoRippleText(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

// Overloaded version for standard Material Icons
@Composable
private fun StatItem(text: String, icon: ImageVector, color: Color) {
    StatItem(text = text, color = color) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
    }
}

@Composable
private fun LanguageFlag(langFlag: String) {
    val context = LocalContext.current
    val flag = remember(langFlag) { MdLang.fromIsoCode(langFlag.lowercase(Locale.US))?.iconResId }
    if (flag != null) {
        Image(
            painter =
                rememberDrawablePainter(drawable = AppCompatResources.getDrawable(context, flag)),
            modifier = Modifier.height(Size.large).clip(RoundedCornerShape(Size.tiny)),
            contentDescription = "Language flag",
        )
    }
}

@Composable
private fun CreatorDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    themeColorState: ThemeColorState,
    creatorText: String,
    onCopy: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    val creators =
        remember(creatorText) { creatorText.split(Constants.SEPARATOR).map { it.trim() } }
    SimpleDropdownMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        themeColorState = themeColorState,
        dropDownItems =
            creators
                .map { individualCreator ->
                    SimpleDropDownItem.Parent(
                        text = UiText.String(individualCreator),
                        children =
                            listOf(
                                SimpleDropDownItem.Action(
                                    text = UiText.StringResource(R.string.copy),
                                    onClick = { onCopy(individualCreator) },
                                ),
                                SimpleDropDownItem.Action(
                                    text = UiText.StringResource(R.string.search),
                                    onClick = { onSearch(individualCreator) },
                                ),
                            ),
                    )
                }
                .toPersistentList(),
    )
}
