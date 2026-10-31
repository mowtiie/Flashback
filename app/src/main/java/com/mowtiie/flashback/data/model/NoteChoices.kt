package com.mowtiie.flashback.data.model

import org.json.JSONArray
import org.json.JSONException

/** The wrong options of a multiple choice note, stored in one column as a JSON array. */
object NoteChoices {
    const val MAX = 3

    fun parse(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { array.optString(it, "").trim() }.filter { it.isNotEmpty() }.take(MAX)
        } catch (_: JSONException) {
            emptyList()
        }
    }

    /** Null when there is nothing to store. */
    fun encode(options: List<String>): String? {
        val cleaned = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX)
        return if (cleaned.isEmpty()) null else JSONArray(cleaned).toString()
    }
}
