package br.com.minharotina.data

import android.content.Context
import br.com.minharotina.alarm.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class Repository(private val context: Context) {

    private val store = JsonStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    init {
        scope.launch {
            for (ignored in saveRequests) {
                val snapshot = _data.value
                store.save(snapshot)
                ReminderScheduler.rescheduleAll(context, snapshot)
            }
        }
        refreshDay()
    }

    private fun mutate(block: (AppData) -> AppData) {
        _data.update(block)
        saveRequests.trySend(Unit)
    }

    /** Agenda os lembretes de novo sem alterar dados (mudança de permissão). */
    fun reschedule() {
        saveRequests.trySend(Unit)
    }

    /**
     * Salva e reagenda na hora, sem esperar a fila. Usado pelos receivers,
     * porque o sistema pode encerrar o processo logo depois que eles terminam.
     */
    fun persistNow() {
        val snapshot = _data.value
        store.save(snapshot)
        ReminderScheduler.rescheduleAll(context, snapshot)
    }

    /**
     * Garante que existe um registro para cada dia desde a última abertura até hoje.
     * Dias em que o app não foi aberto entram com todas as tarefas como não feitas.
     */
    fun refreshDay(today: LocalDate = LocalDate.now()) {
        val current = _data.value
        if (current.lastMaterialized == today.toString() && current.record(today) != null) return
        mutate { d ->
            val last = d.lastMaterialized?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            var day = if (last == null || last.isAfter(today)) today else last.plusDays(1)
            // Evita laços enormes se o app ficou meses sem abrir.
            if (day.isBefore(today.minusDays(60))) day = today.minusDays(60)
            val records = d.records.toMutableMap()
            while (!day.isAfter(today)) {
                val key = day.toString()
                if (records[key] == null) {
                    records[key] = DayRecord(key, scheduledEntries(d.tasks, day))
                }
                day = day.plusDays(1)
            }
            d.copy(records = records, lastMaterialized = today.toString())
        }
    }

    private fun scheduledEntries(tasks: List<RoutineTask>, date: LocalDate): List<DayEntry> =
        tasks.filter { date.dayOfWeek.value in it.days }.map { it.toEntry() }

    private fun RoutineTask.toEntry() = DayEntry(
        taskId = id, title = title, hour = hour, minute = minute,
        done = false, note = "", oneOff = false,
    )

    private fun AppData.withRecord(date: LocalDate, change: (DayRecord) -> DayRecord): AppData {
        val key = date.toString()
        val rec = records[key] ?: DayRecord(key, emptyList())
        return copy(records = records + (key to change(rec)))
    }

    private fun DayRecord.mapEntry(taskId: String, change: (DayEntry) -> DayEntry) =
        copy(entries = entries.map { if (it.taskId == taskId) change(it) else it })

    fun toggleDone(date: LocalDate, taskId: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(done = !it.done) } }
    }

    fun markDone(date: LocalDate, taskId: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(done = true) } }
    }

    fun setNote(date: LocalDate, taskId: String, note: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(note = note) } }
    }

    fun addOneOff(title: String, hour: Int?, minute: Int?, date: LocalDate = LocalDate.now()) {
        refreshDay()
        addOneOffInternal(title, hour, minute, date)
    }

    private fun addOneOffInternal(title: String, hour: Int?, minute: Int?, date: LocalDate) = mutate { d ->
        val entry = DayEntry(
            taskId = "avulsa-" + UUID.randomUUID().toString(),
            title = title.trim(), hour = hour, minute = minute,
            done = false, note = "", oneOff = true,
        )
        d.withRecord(date) { r -> r.copy(entries = r.entries + entry) }
    }

    fun removeOneOff(date: LocalDate, taskId: String) {
        ReminderScheduler.cancel(context, taskId)
        mutate { d -> d.withRecord(date) { r -> r.copy(entries = r.entries.filterNot { it.taskId == taskId }) } }
    }

    fun addRoutine(title: String, days: Set<Int>, hour: Int?, minute: Int?) {
        refreshDay()
        addRoutineInternal(title, days, hour, minute)
    }

    private fun addRoutineInternal(title: String, days: Set<Int>, hour: Int?, minute: Int?) = mutate { d ->
        val today = LocalDate.now()
        val task = RoutineTask(
            id = UUID.randomUUID().toString(),
            title = title.trim(), days = days, hour = hour, minute = minute,
            createdAt = System.currentTimeMillis(),
        )
        val withTask = d.copy(tasks = d.tasks + task)
        if (today.dayOfWeek.value in days) {
            withTask.withRecord(today) { r -> r.copy(entries = r.entries + task.toEntry()) }
        } else withTask
    }

    /** Altera a rotina. Só o dia de hoje é ajustado; o histórico fica como estava. */
    fun updateRoutine(updated: RoutineTask) {
        refreshDay()
        updateRoutineInternal(updated)
    }

    private fun updateRoutineInternal(updated: RoutineTask) = mutate { d ->
        val today = LocalDate.now()
        val withTask = d.copy(tasks = d.tasks.map { if (it.id == updated.id) updated else it })
        withTask.withRecord(today) { r ->
            val existing = r.entries.firstOrNull { it.taskId == updated.id }
            val scheduledToday = today.dayOfWeek.value in updated.days
            when {
                existing != null && scheduledToday -> r.mapEntry(updated.id) {
                    it.copy(title = updated.title, hour = updated.hour, minute = updated.minute)
                }
                existing != null && !existing.done -> r.copy(entries = r.entries - existing)
                existing == null && scheduledToday -> r.copy(entries = r.entries + updated.toEntry())
                else -> r
            }
        }
    }

    /** Remove da rotina. Dias passados continuam no histórico. */
    fun deleteRoutine(taskId: String) {
        ReminderScheduler.cancel(context, taskId)
        refreshDay()
        mutate { d ->
            val today = LocalDate.now()
            d.copy(tasks = d.tasks.filterNot { it.id == taskId })
                .withRecord(today) { r -> r.copy(entries = r.entries.filterNot { it.taskId == taskId && !it.done }) }
        }
    }

    fun toggleFavorite(quote: String) = mutate { d ->
        d.copy(favoriteQuotes = if (quote in d.favoriteQuotes) d.favoriteQuotes - quote else d.favoriteQuotes + quote)
    }

    fun addCustomQuote(quote: String) {
        val q = quote.trim()
        if (q.isEmpty()) return
        mutate { d -> if (q in d.customQuotes) d else d.copy(customQuotes = d.customQuotes + q) }
    }

    fun removeCustomQuote(quote: String) = mutate { d ->
        d.copy(customQuotes = d.customQuotes - quote, favoriteQuotes = d.favoriteQuotes - quote)
    }
}
