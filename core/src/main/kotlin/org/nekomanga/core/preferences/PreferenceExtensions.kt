package org.nekomanga.core.preferences

import tachiyomi.core.preference.Preference

fun Preference<Boolean>.toggle() = set(!get())
