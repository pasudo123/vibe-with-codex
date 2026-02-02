package com.vibewithcodex.core

class Slugifier {
    fun slugify(input: String): String {
        if (input.isBlank()) return ""

        val lowered = input.lowercase()
        val normalized = lowered
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        return normalized
    }
}
