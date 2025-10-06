package org.nekomanga.presentation.screens.feed.history

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import org.nekomanga.R
import org.nekomanga.constants.Constants

@Composable
fun LastReadLine(
    lastRead: Long,
    hasPagesLeft: Boolean,
    pagesLeft: Int,
    style: TextStyle,
    lastReadPreviousChapter: String = "",
    textColor: Color,
) {
    val statuses = mutableListOf<String>()

    if (lastRead != 0L) {
        statuses.add(stringResource(R.string.read_, lastRead.timeSpanFromNow))
    }
    if (hasPagesLeft) {
        statuses.add(pluralStringResource(R.plurals.pages_left, pagesLeft, pagesLeft))
    }
    if (lastReadPreviousChapter.isNotEmpty()) {
        statuses.add(stringResource(R.string.last_read_, lastReadPreviousChapter))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = statuses.joinToString(Constants.SEPARATOR),
            style = style.copy(color = textColor, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
