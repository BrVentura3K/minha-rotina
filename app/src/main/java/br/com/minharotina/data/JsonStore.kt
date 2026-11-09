package br.com.minharotina.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Salva tudo num único arquivo JSON no armazenamento interno do app. */
class JsonStore(context: Context) {

    private val file = File(context.filesDir, "rotina.json")
    private val tmp = File(context.filesDir, "rotina.json.tmp")

    fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            Log.e("JsonStore", "Falha ao ler dados", e)
            AppData()
        }
    }

    @Synchronized
    fun save(data: AppData) {
        try {
            tmp.writeText(toJson(data).toString())
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e("JsonStore", "Falha ao salvar dados", e)
        }
    }

    private fun toJson(d: AppData): JSONObject = JSONObject().apply {
        put("version", 1)
        put("tasks", JSONArray().apply {
            d.tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("days", JSONArray(t.days.sorted()))
                    put("hour", t.hour ?: -1)
                    put("minute", t.minute ?: -1)
                    put("createdAt", t.createdAt)
                })
            }
        })
        put("records", JSONArray().apply {
            d.records.values.sortedBy { it.date }.forEach { r ->
                put(JSONObject().apply {
                    put("date", r.date)
                    put("entries", JSONArray().apply {
                        r.entries.forEach { e ->
                            put(JSONObject().apply {
                                put("taskId", e.taskId)
                                put("title", e.title)
                                put("hour", e.hour ?: -1)
                                put("minute", e.minute ?: -1)
                                put("done", e.done)
                                put("note", e.note)
                                put("oneOff", e.oneOff)
                            })
                        }
                    })
                })
            }
        })
        put("lastMaterialized", d.lastMaterialized ?: "")
        put("favoriteQuotes", JSONArray(d.favoriteQuotes.toList()))
        put("customQuotes", JSONArray(d.customQuotes))
    }

    private fun fromJson(o: JSONObject): AppData {
        val tasks = mutableListOf<RoutineTask>()
        o.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                val daysArr = t.optJSONArray("days") ?: JSONArray()
                val days = (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()
                tasks += RoutineTask(
                    id = t.getString("id"),
                    title = t.getString("title"),
                    days = days,
                    hour = t.optInt("hour", -1).takeIf { it >= 0 },
                    minute = t.optInt("minute", -1).takeIf { it >= 0 },
                    createdAt = t.optLong("createdAt", 0L),
                )
            }
        }
        val records = mutableMapOf<String, DayRecord>()
        o.optJSONArray("records")?.let { arr ->
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val entries = mutableListOf<DayEntry>()
                val eArr = r.optJSONArray("entries") ?: JSONArray()
                for (j in 0 until eArr.length()) {
                    val e = eArr.getJSONObject(j)
                    entries += DayEntry(
                        taskId = e.getString("taskId"),
                        title = e.getString("title"),
                        hour = e.optInt("hour", -1).takeIf { it >= 0 },
                        minute = e.optInt("minute", -1).takeIf { it >= 0 },
                        done = e.optBoolean("done", false),
                        note = e.optString("note", ""),
                        oneOff = e.optBoolean("oneOff", false),
                    )
                }
                val date = r.getString("date")
                records[date] = DayRecord(date, entries)
            }
        }
        return AppData(
            tasks = tasks,
            records = records,
            lastMaterialized = o.optString("lastMaterialized", "").ifBlank { null },
            favoriteQuotes = o.optJSONArray("favoriteQuotes").toStringList().toSet(),
            customQuotes = o.optJSONArray("customQuotes").toStringList(),
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
