
package com.example.aviatorfloatinganalyzer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RoundRecord(
    val multiplier: Double,
    val timestamp: Long
)

object MultiplierDatabase {

    private const val PREFS = "aviator_history"
    private const val KEY_HISTORY = "rounds"
    private const val MAX_RECORDS = 5000
    private const val DUPLICATE_WINDOW = 10000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun addRound(context: Context, multiplier: Double): Boolean {
        if (!multiplier.isFinite() || multiplier < 1.0) {
            return false
        }

        val history = getHistory(context)
        val now = System.currentTimeMillis()
        val last = history.lastOrNull()

        if (last != null &&
            last.multiplier == multiplier &&
            now - last.timestamp < DUPLICATE_WINDOW
        ) {
            return false
        }

        val updated = (history + RoundRecord(multiplier, now))
            .takeLast(MAX_RECORDS)

        saveHistory(context, updated)
        return true
    }

    fun getHistory(context: Context): List<RoundRecord> {
        val raw = prefs(context).getString(KEY_HISTORY, "[]") ?: "[]"

        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<RoundRecord>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result.add(
                    RoundRecord(
                        item.getDouble("multiplier"),
                        item.getLong("timestamp")
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(
        context: Context,
        history: List<RoundRecord>
    ) {
        val array = JSONArray()

        history.forEach {
            array.put(JSONObject().apply {
                put("multiplier", it.multiplier)
                put("timestamp", it.timestamp)
            })
        }

        prefs(context).edit()
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }

    fun getCount(context: Context): Int =
        getHistory(context).size

    fun clearHistory(context: Context) {
        prefs(context).edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}
