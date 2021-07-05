package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.view.setDate
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.util.Calendar

class SetTrackReadingDatesDialog<T> : DialogController
        where T : Controller {

    private val item: TrackItem

    private val dateToUpdate: ReadingDate
    private val suggestedDate: Long?

    private val preferences: PreferencesHelper by injectLazy()
    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    private lateinit var listener: Listener

    constructor(target: T, listener: Listener, dateToUpdate: ReadingDate, item: TrackItem, suggestedDate: Long?) : super(
        bundleOf(KEY_ITEM_TRACK to item.track)
    ) {
        targetController = target
        this.listener = listener
        this.item = item
        this.dateToUpdate = dateToUpdate
        this.suggestedDate = suggestedDate
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val track = bundle.getSerializable(KEY_ITEM_TRACK) as Track
        val service = Injekt.get<TrackManager>().getService(track.sync_id)!!
        item = TrackItem(track, service)
        dateToUpdate = ReadingDate.Start
        suggestedDate = null
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(
                when (dateToUpdate) {
                    ReadingDate.Start -> R.string.started_reading_date
                    ReadingDate.Finish -> R.string.finished_reading_date
                }
            )
            .datePicker(currentDate = getCurrentDate()) { _, date ->
                listener.setReadingDate(item, dateToUpdate, date.timeInMillis)
            }
            .neutralButton(R.string.remove) {
                listener.setReadingDate(item, dateToUpdate, -1L)
            }.apply {
                getSuggestedDate()?.let {
                    message(
                        text = it,
                        applySettings = {
                            messageTextView.setOnClickListener {
                                this@apply.setDate(suggestedDate ?: 0L)
                            }
                        }
                    )
                }
            }
    }

    private fun getSuggestedDate(): String? {
        item.track ?: return null
        val date = when (dateToUpdate) {
            ReadingDate.Start -> item.track.started_reading_date
            ReadingDate.Finish -> item.track.finished_reading_date
        }
        if (date != 0L) {
            if (suggestedDate != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = date
                val suggestedCalendar = Calendar.getInstance()
                suggestedCalendar.timeInMillis = suggestedDate
                return if (date > suggestedDate &&
                    (
                        suggestedCalendar.year != calendar.year ||
                            suggestedCalendar.month != calendar.month ||
                            suggestedCalendar.dayOfMonth != calendar.dayOfMonth
                        )
                ) {
                    activity?.getString(
                        R.string.suggested_date_,
                        dateFormat.format(suggestedDate)
                    )
                } else {
                    null
                }
            }
        }
        suggestedDate?.let {
            return activity?.getString(
                R.string.suggested_date_,
                dateFormat.format(suggestedDate)
            )
        }
        return null
    }

    private fun getCurrentDate(): Calendar {
        // Today if no date is set, otherwise the already set date
        return Calendar.getInstance().apply {
            suggestedDate?.let {
                timeInMillis = it
            }
            item.track?.let {
                val date = when (dateToUpdate) {
                    ReadingDate.Start -> it.started_reading_date
                    ReadingDate.Finish -> it.finished_reading_date
                }
                if (date != 0L) {
                    timeInMillis = date
                }
            }
        }
    }

    interface Listener {
        fun setReadingDate(item: TrackItem, type: ReadingDate, date: Long)
    }

    enum class ReadingDate {
        Start,
        Finish
    }

    companion object {
        private const val KEY_ITEM_TRACK = "SetTrackReadingDatesDialog.item.track"
    }
}
