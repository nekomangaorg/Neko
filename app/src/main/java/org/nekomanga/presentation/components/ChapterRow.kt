package org.nekomanga.presentation.screens

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.NekoColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@Composable
fun ChapterRow(themeColor: ThemeColorState, chapterItem: ChapterItem, shouldHideChapterTitles: Boolean = false) {
    val chapter = chapterItem.chapter
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {

            val titleText = when (shouldHideChapterTitles) {
                true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapter.chapterNumber.toDouble()))
                false -> chapter.name
            }

            Row {
                if (chapter.bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark, contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically),
                        tint = themeColor.buttonColor,
                    )
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val statuses = mutableListOf<String>()

            ChapterUtil.relativeDate(chapter.dateUpload)?.let { statuses.add(it) }

            val showPagesLeft = !chapter.read && chapter.lastPageRead > 0
            val resources = LocalContext.current.resources

            if (showPagesLeft && chapter.pagesLeft > 0) {
                statuses.add(
                    resources.getQuantityString(R.plurals.pages_left, chapter.pagesLeft, chapter.pagesLeft),
                )
            } else if (showPagesLeft) {
                statuses.add(
                    stringResource(id = R.string.page_, chapter.lastPageRead + 1),
                )
            }

            if (chapter.scanlator.isNotBlank()) {
                statuses.add(chapter.scanlator)
            }

            Row {
                if (chapter.language.isNotNullOrEmpty() && chapter.language.equals("en", true).not()) {
                    val drawable = AppCompatResources.getDrawable(LocalContext.current, MdLang.fromIsoCode(chapter.language)?.iconResId!!)
                    Image(
                        painter = rememberDrawablePainter(drawable = drawable),
                        modifier = Modifier
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = "flag",
                    )
                }
                Text(
                    text = statuses.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-.6).sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                statuses.joinToString(" • ")

            }

        }



        DownloadButton(themeColor.buttonColor, chapterItem.downloadState, chapterItem.downloadProgress.toFloat(), Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
fun ChapterRowTester(buttonColor: Color, state: Download.State = Download.State.NOT_DOWNLOADED) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Downloaded", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.DOWNLOADED, null, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Downloading", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.DOWNLOADING, 1f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "NotDownloaded", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.NOT_DOWNLOADED, null, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Queue", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.QUEUE, -1f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Checked", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.CHECKED, null, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Error", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.ERROR, null, Modifier.align(Alignment.CenterVertically))
        }
    }
}

val decimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)

