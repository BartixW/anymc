/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.utils

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

object DateTimeFormatterUtil {
    val currentLocale: Locale? = Locale.getDefault()

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        .withLocale(currentLocale)

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(currentLocale)

    fun formatDateTime(temporalAccessor: TemporalAccessor): String {
        return dateTimeFormatter.format(temporalAccessor)
    }

    fun formatDate(temporalAccessor: TemporalAccessor): String {
        return dateFormatter.format(temporalAccessor)
    }
}