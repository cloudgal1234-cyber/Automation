package com.automation.voicegesture.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Helper for turning recorded gesture strokes into/from the JSON blob stored in Room. */
object GestureSerializer {
    private val gson = Gson()
    private val listType = object : TypeToken<List<GestureStroke>>() {}.type

    fun toJson(strokes: List<GestureStroke>): String = gson.toJson(strokes)

    fun fromJson(json: String): List<GestureStroke> {
        if (json.isBlank()) return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }
}
