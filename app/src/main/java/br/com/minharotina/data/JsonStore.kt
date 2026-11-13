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
                    put("alarms", JSONArray(t.alarms))
                    put("order", t.order)
                    put("createdAt", t.createdAt)
                    put("timerMinutes", t.timerMinutes ?: -1)
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
                                put("alarms", JSONArray(e.alarms))
                                put("order", e.order)
                                put("done", e.done)
                                put("note", e.note)
                                put("oneOff", e.oneOff)
                                put("timerMinutes", e.timerMinutes ?: -1)
                                put("timerEndsAt", e.timerEndsAt ?: 0L)
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
                    alarms = lerAlarmes(t),
                    order = t.optInt("order", -1),
                    createdAt = t.optLong("createdAt", 0L),
                    timerMinutes = t.optInt("timerMinutes", -1).takeIf { it > 0 },
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
                        alarms = lerAlarmes(e),
                        order = e.optInt("order", -1),
                        done = e.optBoolean("done", false),
                        note = e.optString("note", ""),
                        oneOff = e.optBoolean("oneOff", false),
                        timerMinutes = e.optInt("timerMinutes", -1).takeIf { it > 0 },
                        timerEndsAt = e.optLong("timerEndsAt", 0L).takeIf { it > 0 },
                    )
                }
                val date = r.getString("date")
                // Dados antigos não tinham posição: numera pela ordem do horário.
                val ordenadas = if (entries.any { it.order < 0 }) {
                    entries.sortedBy { it.sortKey }.mapIndexed { i, e -> e.copy(order = i) }
                } else entries
                records[date] = DayRecord(date, ordenadas)
            }
        }
        val tarefas = if (tasks.any { it.order < 0 }) {
            tasks.sortedWith(compareBy({ it.sortKey }, { it.title })).mapIndexed { i, t -> t.copy(order = i) }
        } else tasks
        return AppData(
            tasks = tarefas,
            records = records,
            lastMaterialized = o.optString("lastMaterialized", "").ifBlank { null },
            favoriteQuotes = o.optJSONArray("favoriteQuotes").toStringList().toSet(),
            customQuotes = o.optJSONArray("customQuotes").toStringList(),
        )
    }

    /** Lê a lista de alarmes; versões antigas guardavam um só, em "hour" e "minute". */
    private fun lerAlarmes(o: JSONObject): List<Int> {
        o.optJSONArray("alarms")?.let { arr ->
            return (0 until arr.length()).map { arr.getInt(it) }.filter { it in 0 until 24 * 60 }.distinct().sorted()
        }
        val h = o.optInt("hour", -1)
        return if (h >= 0) listOf(h * 60 + o.optInt("minute", 0).coerceAtLeast(0)) else emptyList()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
