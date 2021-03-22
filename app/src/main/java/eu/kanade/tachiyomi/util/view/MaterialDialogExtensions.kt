package eu.kanade.tachiyomi.util.view

import com.afollestad.date.DatePicker
import com.afollestad.materialdialogs.MaterialDialog
import java.util.Calendar

fun MaterialDialog.setDate(date: Long) {
    val datePicker = findViewById<DatePicker>(com.afollestad.materialdialogs.datetime.R.id.datetimeDatePicker) ?: return
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    datePicker.setDate(calendar)
}
