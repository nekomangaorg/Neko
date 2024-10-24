package org.nekomanga.presentation.screens.mangadetails

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.HotelClass
import androidx.compose.material.icons.outlined._18UpRating
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun InformationBlock(
    themeColorState: ThemeColorState,
    titleProvider: () -> String,
    authorProvider: () -> String,
    artistProvider: () -> String,
    statsProvider: () -> Stats?,
    langFlagProvider: () -> String?,
    statusProvider: () -> Int,
    isPornographicProvider: () -> Boolean,
    missingChaptersProvider: () -> String?,
    estimatedMissingChapterProvider: () -> String?,
    modifier: Modifier = Modifier,
    isExpandedProvider: () -> Boolean,
    showMergedIconProvider: () -> Boolean,
    titleLongClick: (String) -> Unit = {},
    creatorCopyClick: (String) -> Unit = {},
    creatorSearchClick: (String) -> Unit = {},
) {
    val highAlpha =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.highAlphaLowContrast)
    val mediumAlpha =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    Column(modifier = modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = Size.small)) {
        if (!titleProvider().isNullOrEmpty()) {
            NoRippleText(
                text = titleProvider(),
                maxLines = if (isExpandedProvider()) Integer.MAX_VALUE else 4,
                onLongClick = titleLongClick,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = (-.5).sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color = highAlpha,
            )
        }

        if (authorProvider().isNotEmpty() || artistProvider().isNotEmpty()) {
            val creator =
                when (authorProvider() == artistProvider()) {
                    true -> authorProvider().trim()
                    false -> {
                        listOfNotNull(authorProvider().trim(), artistProvider().trim())
                            .joinToString(Constants.SEPARATOR)
                    }
                }

            Gap(Size.tiny)

            var creatorExpanded by remember { mutableStateOf(false) }
            NoRippleText(
                text = creator,
                onClick = { creatorExpanded = !creatorExpanded },
                maxLines = if (isExpandedProvider()) 5 else 2,
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
            val creators = creator.split(Constants.SEPARATOR).map { it.trim() }
            SimpleDropdownMenu(
                expanded = creatorExpanded,
                onDismiss = { creatorExpanded = false },
                themeColorState = themeColorState,
                dropDownItems =
                    creators
                        .map { individualCreator ->
                            SimpleDropDownItem.Parent(
                                text = UiText.String(individualCreator),
                                children =
                                    listOf(
                                        SimpleDropDownItem.Action(
                                            text = UiText.StringResource(R.string.copy)
                                        ) {
                                            creatorExpanded = false
                                            creatorCopyClick(individualCreator)
                                        },
                                        SimpleDropDownItem.Action(
                                            text = UiText.StringResource(R.string.search)
                                        ) {
                                            creatorExpanded = false
                                            creatorSearchClick(individualCreator)
                                        },
                                    ),
                            )
                        }
                        .toPersistentList(),
            )
        }

        if (statusProvider() != 0) {
            Gap(Size.tiny)
            val statusRes =
                when (statusProvider()) {
                    SManga.ONGOING -> R.string.ongoing
                    SManga.COMPLETED -> R.string.completed
                    SManga.LICENSED -> R.string.licensed
                    SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                    SManga.HIATUS -> R.string.hiatus
                    SManga.CANCELLED -> R.string.cancelled
                    else -> R.string.unknown
                }

            NoRippleText(
                text = stringResource(id = statusRes),
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
        }
        if (statsProvider() != null || langFlagProvider() != null) {
            Gap(Size.tiny)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            if (langFlagProvider() != null) {
                val flag = MdLang.fromIsoCode(langFlagProvider()!!.lowercase(Locale.US))?.iconResId
                if (flag != null) {
                    Image(
                        painter =
                            rememberDrawablePainter(
                                drawable =
                                    AppCompatResources.getDrawable(LocalContext.current, flag)
                            ),
                        modifier = Modifier.height(Size.large).clip(RoundedCornerShape(Size.tiny)),
                        contentDescription = "flag",
                    )
                }
            }

            if (isPornographicProvider()) {
                Gap(Size.small)
                Image(
                    imageVector = Icons.Outlined._18UpRating,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Red),
                )
            }

            statsProvider()?.let { stats ->
                stats.rating?.let { rating ->
                    val formattedRating =
                        ((rating.toDouble() * 100).roundToInt() / 100.0).toString()

                    Gap(Size.tiny)
                    Image(
                        imageVector = Icons.Filled.HotelClass,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(mediumAlpha),
                    )
                    Gap(Size.tiny)
                    NoRippleText(
                        text = formattedRating,
                        style = MaterialTheme.typography.bodyLarge,
                        color = mediumAlpha,
                    )
                }

                stats.follows?.let { unformattedNumberOfUsers ->
                    val numberOfUsers =
                        runCatching {
                                NumberFormat.getNumberInstance(Locale.US)
                                    .format(unformattedNumberOfUsers.toInt())
                            }
                            .getOrDefault(0)
                            .toString()

                    Gap(Size.tiny)
                    Image(
                        imageVector = Icons.Filled.Bookmarks,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(mediumAlpha),
                    )
                    Gap(Size.tiny)
                    NoRippleText(
                        text = numberOfUsers,
                        style = MaterialTheme.typography.bodyLarge,
                        color = mediumAlpha,
                    )
                }

                if (stats.threadId != null) {

                    val numberOfComments =
                        runCatching {
                                NumberFormat.getNumberInstance(Locale.US)
                                    .format(stats.repliesCount?.toInt())
                            }
                            .getOrDefault(0)
                            .toString()

                    Gap(Size.tiny)
                    Image(
                        imageVector = Icons.Filled.Comment,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(mediumAlpha),
                    )
                    Gap(Size.tiny)
                    NoRippleText(
                        text = numberOfComments,
                        style = MaterialTheme.typography.bodyLarge,
                        color = mediumAlpha,
                    )
                }
            }

            if (showMergedIconProvider()) {
                Gap(Size.tiny)
                com.mikepenz.iconics.compose.Image(
                    asset = CommunityMaterial.Icon.cmd_check_decagram,
                    colorFilter = ColorFilter.tint(mediumAlpha),
                )
            }
        }

        var showEstimatedMissingChapters by remember { mutableStateOf(false) }

        missingChaptersProvider()?.let { numberOfMissingChapters ->
            Gap(Size.tiny)
            NoRippleText(
                text = stringResource(id = R.string.missing_chapters, numberOfMissingChapters),
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
                onClick = { showEstimatedMissingChapters = !showEstimatedMissingChapters },
            )
        }

        AnimatedVisibility(visible = showEstimatedMissingChapters) {
            estimatedMissingChapterProvider()?.let { estimates ->
                Column {
                    Gap(Size.tiny)
                    NoRippleText(
                        text = estimates,
                        maxLines = 4,
                        style = MaterialTheme.typography.bodySmall,
                        color = mediumAlpha,
                    )
                }
            }
        }
    }
}
