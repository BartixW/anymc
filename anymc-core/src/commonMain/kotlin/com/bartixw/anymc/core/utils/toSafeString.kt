/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.utils

import java.text.Normalizer

fun toSafeString(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)

    val cleaned = normalized
        .replace('ł', 'l')
        .replace('Ł', 'L')
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    return cleaned.replace(Regex("[^a-zA-Z0-9_]+"), "_")
}