package eu.kanade.tachiyomi.util.view

import com.afollestad.date.DatePicker
import com.afollestad.materialdialogs.MaterialDialog
import java.util.Calendar

/*
 * Copyright (C) 2020 The Neko Manga Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

fun MaterialDialog.setDate(date: Long) {
    val datePicker = findViewById<DatePicker>(com.afollestad.materialdialogs.datetime.R.id.datetimeDatePicker) ?: return
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    datePicker.setDate(calendar)
}