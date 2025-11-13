package eu.kanade.tachiyomi.source.online.merged.kagane

/**
 * Normalizes a manga/series title string, extracting volume, chapter, and the full normalized
 * title.
 *
 * @param input The raw title string.
 * @return A Triple where:
 * - `first` is the prefixed volume string? (e.g., "Vol.1", "Vol.3", null).
 * - `second` is the prefixed chapter string? (e.g., "Ch.1", "Ch.16", "Ch.0", "Ch.4.1", null).
 * - `third` is the full normalized string plus extra text (e.g., "Vol.1 Ch.1 - 1r0n") OR just the
 *   normalized string (e.g., "Vol.1 Ch.1") OR just the extra text (e.g., "Thank you!").
 */
fun normalizeTitle(input: String): Triple<String?, String?, String> {
    val extraParts = mutableListOf<String>()
    var main = input

    // --- 1. Pre-process: Extract bracketed and colon-ed text ---
    // This text is almost always "extra".
    val bracketRegex = Regex("""[\[\(](.*?)[\]\)]""")
    main =
        bracketRegex.replace(main) {
            extraParts.add(it.groupValues[1].trim())
            "" // Remove from main string
        }

    val colonRegex = Regex(""":\s*(.*)""")
    main =
        colonRegex.replace(main) {
            extraParts.add(it.groupValues[1].trim())
            "" // Remove from main string
        }

    var remaining = main.trim()
    var volNum: String? = null // Will store just the number
    var chNum: String? = null // Will store just the number (or "0", "1.1")

    // --- 2. Find Volume ---
    // Keywords: Volume, Vol, Season
    val volRegex = Regex("""(?i)\b(Volume|Vol|Season)[\s.]*(\d+|[IVXLCDM]+)\b""")
    // Loop to find all matches, replacing them. The *last* one found will be used.
    // e.g., "Volume 1 Season 2" -> volNum will be "2"
    var volMatch = volRegex.find(remaining)
    while (volMatch != null) {
        volNum = parseNumber(volMatch.groupValues[2])
        remaining = remaining.replaceRange(volMatch.range, "")
        volMatch = volRegex.find(remaining) // Find the next match in the modified string
    }

    // --- 3. Find Chapter (in order of priority) ---

    // 3a. Prologue/Promo -> Ch.0
    val prologueRegex = Regex("""(?i)\b(Prologue|Promo)\b""")
    val prologueMatch = prologueRegex.find(remaining)
    if (prologueMatch != null) {
        chNum = "0"
        remaining = remaining.replaceRange(prologueMatch.range, "")
    }

    // 3b. Chapter/Episode/Ch -> Ch.#
    // Updated regex to capture decimals like "4.1"
    val chRegex = Regex("""(?i)\b(Chapter|Episode|Ch)[\s.]*(\d+(\.\d+)?|[IVXLCDM]+)\b""")
    val chMatch = chRegex.find(remaining)
    if (chNum == null && chMatch != null) { // Only if prologue didn't match
        chNum = parseNumber(chMatch.groupValues[2])
        remaining = remaining.replaceRange(chMatch.range, "")
    }

    // 3c. Dash format "A-B" -> Ch.A.B
    val dashRegex = Regex("""\b(\d+)[-\s]+(\d+)\b""")
    val dashMatch = dashRegex.find(remaining)
    if (chNum == null && dashMatch != null) {
        val g1 = parseNumber(dashMatch.groupValues[1])
        val g2 = parseNumber(dashMatch.groupValues[2]) // "01" -> "1"
        if (g1 != null && g2 != null) {
            chNum = "$g1.$g2"
            remaining = remaining.replaceRange(dashMatch.range, "")
        }
    }

    // 3d. Just a number at the start -> Ch.#
    val justNumRegex = Regex("""^\s*(\d+)[\s.]*""")
    val justNumMatch = justNumRegex.find(remaining)
    if (chNum == null && justNumMatch != null) {
        chNum = parseNumber(justNumMatch.groupValues[1])
        remaining = remaining.replaceRange(justNumMatch.range, "")
    }

    // --- 4. Final Cleanup ---
    // Any "remaining" text is also extra.
    // Clean up leading junk characters like ' - '
    val cleanedRemaining = remaining.replace(Regex("""^[\s.,-]+|[\s.,-]+$"""), "").trim()
    if (cleanedRemaining.isNotEmpty()) {
        extraParts.add(cleanedRemaining)
    }

    val finalExtra = extraParts.filter { it.isNotEmpty() }.joinToString(" ").trim()

    // --- 5. Build Prefixed Strings ---
    val finalVolStr = volNum?.let { "Vol.$it" }
    val finalChStr = chNum?.let { "Ch.$it" }

    val normalized = listOfNotNull(finalVolStr, finalChStr).joinToString(" ")

    // Handle edge case where nothing was found
    if (normalized.isEmpty() && finalExtra.isEmpty()) {
        // Return original as extra if it's unparsable
        return Triple(null, null, input)
    }

    // --- 6. Build Third Part of Triple ---
    // Combine normalized string and extra text
    val finalTitle =
        if (normalized.isNotEmpty() && finalExtra.isNotEmpty()) {
            "$normalized - $finalExtra"
        } else if (normalized.isNotEmpty()) {
            normalized
        } else {
            finalExtra
        }

    return Triple(finalVolStr, finalChStr, finalTitle)
}

/**
 * Parses a string that could be an integer or a Roman numeral.
 *
 * @return The number as a string, or null if unparsable.
 */
private fun parseNumber(numStr: String): String? {
    val trimmed = numStr.trim().uppercase()
    if (trimmed.isEmpty()) return null

    // Try Int first (handles "01" -> 1)
    trimmed.toIntOrNull()?.let {
        return it.toString()
    }

    // Try Roman (only if it *only* contains Roman chars)
    if (trimmed.all { it in "IVXLCDM" }) {
        val intVal = romanToInt(trimmed)
        if (intVal > 0) return intVal.toString()
    }

    // NEW: Check if it's a decimal number string
    if (trimmed.contains('.') && trimmed.matches(Regex("""^\d+\.\d+$"""))) {
        val asDouble = trimmed.toDoubleOrNull()
        if (asDouble != null) {
            if (asDouble == asDouble.toInt().toDouble()) {
                return asDouble.toInt().toString() // "4.0" -> "4"
            }
            return asDouble.toString() // "4.1" -> "4.1"
        }
        return trimmed.lowercase() // fallback
    }

    // Not a number we can parse
    return null
}

/** Converts a Roman numeral string to an integer. */
private fun romanToInt(s: String): Int {
    val romanMap =
        mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    var result = 0
    var prevValue = 0
    for (i in s.uppercase().reversed()) {
        val value = romanMap[i] ?: return 0 // Invalid char
        if (value < prevValue) {
            result -= value
        } else {
            result += value
        }
        prevValue = value
    }
    return result
}

// --- Main function to run tests ---
/*
fun main() {
    val testCases = listOf(
        "Chapter 1 - Volume 1",
        "Chapter 1",
        "Ch. 1 Vings I",
        "Ch. 28",
        "Episode 1",
        "1-01",
        "Thank you!",
        "Chapter 1 - Volume 1 (1r0n)",
        "Vol.3 Chapter XVI: Blood Is Thicker Than Water",
        "Vol.3 Chapter II: Wild test",
        "Prologue 9 - Volume 3 (danke-Empire)",
        "18. So Cute",
        "Chapter 4.1 - Volume 1 (1r0n)" // New test case
    )

    println("--- Running Title Normalization Tests ---")
    testCases.forEach {
        val (volume, chapter, normalizedTitle) = normalizeTitle(it)
        println("Input:    \"$it\"")
        println("Volume:     \"$volume\"")
        println("Chapter:    \"$chapter\"")
        println("Title:      \"$normalizedTitle\"")
        println("-".repeat(30))
    }
}*/
