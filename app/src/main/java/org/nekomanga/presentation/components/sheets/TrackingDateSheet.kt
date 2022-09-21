package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lanars.compose.datetextfield.DateTextField
import com.lanars.compose.datetextfield.Format
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.ReadingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange.EditTrackingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange.RemoveTrackingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import java.text.SimpleDateFormat
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColorState
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackingDateSheet(
    themeColorState: ThemeColorState,
    trackAndService: TrackingConstants.TrackAndService,
    trackingDate: TrackingDate,
    trackSuggestedDates: TrackingSuggestedDates?,
    onDismiss: () -> Unit,
    trackDateChanged: (TrackDateChange) -> Unit,
) {
    var showDateField by remember { mutableStateOf(false) }

    var newDate by remember { mutableStateOf<LocalDate?>(null) }

    val dateTimePattern = (trackingDate.dateFormat as SimpleDateFormat).toPattern()

    val dateTimeFormatter = if (dateTimePattern.isEmpty()) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofPattern(dateTimePattern)
    }

    val currentDateExists = trackingDate.currentDate > 0L

    BaseSheet(themeColor = themeColorState) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
        ) {
            IconButton(onClick = { onDismiss() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(
                    id = when (trackingDate.readingDate) {
                        ReadingDate.Start -> R.string.started_reading_date
                        ReadingDate.Finish -> R.string.finished_reading_date
                    },
                ),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
            )
        }

        Gap(8.dp)
        Divider()

        if (currentDateExists) {
            Gap(8.dp)
            Text(
                text = stringResource(id = R.string.current_date_, trackingDate.dateFormat.format(trackingDate.currentDate)),
                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(8.dp)
            TextButton(onClick = { trackDateChanged(RemoveTrackingDate(trackingDate.readingDate, trackAndService)) }) {
                Text(
                    text = stringResource(id = R.string.remove),
                    style = MaterialTheme.typography.titleMedium.copy(color = themeColorState.buttonColor),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            showDateField = true
        }

        val suggestedDateEpoch = when (trackingDate.readingDate) {
            ReadingDate.Start -> trackSuggestedDates?.startDate
            ReadingDate.Finish -> trackSuggestedDates?.finishedDate
        }

        if (suggestedDateEpoch != null && suggestedDateEpoch != 0L) {
            val suggestedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(suggestedDateEpoch), ZoneId.systemDefault()).toLocalDate()
            val currentDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(trackingDate.currentDate), ZoneId.systemDefault()).toLocalDate()

            if ((suggestedDate.atStartOfDay() != currentDate.atStartOfDay())) {
                Gap(8.dp)
                TextButton(onClick = { trackDateChanged(EditTrackingDate(trackingDate.readingDate, suggestedDate, trackAndService)) }) {
                    Text(
                        text = stringResource(id = R.string.use_suggested_date_of_, suggestedDate.format(dateTimeFormatter)),
                        style = MaterialTheme.typography.titleMedium.copy(color = themeColorState.buttonColor),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (!showDateField && currentDateExists) {
            Gap(8.dp)
            TextButton(onClick = { showDateField = !showDateField }) {
                Text(
                    text = stringResource(id = R.string.edit),
                    style = MaterialTheme.typography.titleMedium.copy(color = themeColorState.buttonColor),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (showDateField) {
            Gap(8.dp)
            val format = when {
                dateTimePattern.startsWith("MM", true) -> Format.MMDDYYYY
                dateTimePattern.startsWith("YY", true) -> Format.YYYYMMDD
                dateTimePattern.startsWith("DD", true) -> Format.DDMMYYYY
                else -> Format.YYYYMMDD
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                Arrangement.SpaceBetween,
            ) {
                DateTextField(
                    onEditingComplete = { currentDate -> newDate = currentDate },
                    format = format,
                    maxDate = LocalDate.now(),
                    contentTextStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(themeColorState.buttonColor),
                    hintTextStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)),
                )
                ElevatedButton(
                    onClick = { trackDateChanged(EditTrackingDate(trackingDate.readingDate, newDate!!, trackAndService)) },
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColorState.buttonColor, contentColor = MaterialTheme.colorScheme.surface),
                    enabled = newDate != null,
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}
