package com.example.addictionreductionapp.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for types that SQLite cannot store natively.
 *
 * Room's SQLite layer only supports: Int, Long, Float, Double, String, ByteArray.
 * Any richer type must be serialised/deserialised with a TypeConverter pair.
 *
 * Registered globally on [AppDatabase] via @TypeConverters(Converters::class).
 *
 * NOTE: Gson is used here for simplicity.  If you later add the Kotlin
 * serialisation plugin you can switch to kotlinx.serialization with zero
 * changes to callers — just replace these two methods.
 *
 * Current conversions:
 *  - List<String>  ↔  JSON string  (e.g. blocked app package list)
 *  - List<Int>     ↔  JSON string  (e.g. scheduled-hour lists)
 */
class Converters {

    private val gson = Gson()

    // ── List<String> ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // ── List<Int> ─────────────────────────────────────────────────────────────

    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return gson.toJson(list ?: emptyList<Int>())
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
