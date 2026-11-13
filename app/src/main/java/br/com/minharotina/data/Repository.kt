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
        tasks.filter { date.dayOfWeek.value in it.days }
            .sortedBy { it.order }
            .mapIndexed { i, t -> t.toEntry(i) }

    private fun RoutineTask.toEntry(order: Int) = DayEntry(
        taskId = id, title = title, alarms = alarms,
        done = false, note = "", oneOff = false, timerMinutes = timerMinutes, order = order,
    )

    private fun DayRecord.nextOrder(): Int = (entries.maxOfOrNull { it.order } ?: -1) + 1

    private fun AppData.withRecord(date: LocalDate, change: (DayRecord) -> DayRecord): AppData {
        val key = date.toString()
        val rec = records[key] ?: DayRecord(key, emptyList())
        return copy(records = records + (key to change(rec)))
    }

    private fun DayRecord.mapEntry(taskId: String, change: (DayEntry) -> DayEntry) =
        copy(entries = entries.map { if (it.taskId == taskId) change(it) else it })

    /** Ticar também para o temporizador, se estiver rodando. */
    fun toggleDone(date: LocalDate, taskId: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(done = !it.done, timerEndsAt = null) } }
    }

    fun markDone(date: LocalDate, taskId: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(done = true, timerEndsAt = null) } }
    }

    fun startTimer(date: LocalDate, taskId: String) = mutate { d ->
        d.withRecord(date) { r ->
            r.mapEntry(taskId) { e ->
                val min = e.timerMinutes ?: return@mapEntry e
                e.copy(timerEndsAt = System.currentTimeMillis() + min * 60_000L)
            }
        }
    }

    fun stopTimer(date: LocalDate, taskId: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(timerEndsAt = null) } }
    }

    fun setNote(date: LocalDate, taskId: String, note: String) = mutate { d ->
        d.withRecord(date) { r -> r.mapEntry(taskId) { it.copy(note = note) } }
    }

    fun addOneOff(title: String, alarms: List<Int>, timerMinutes: Int?, date: LocalDate = LocalDate.now()) {
        refreshDay()
        addOneOffInternal(title, alarms, timerMinutes, date)
    }

    private fun addOneOffInternal(title: String, alarms: List<Int>, timerMinutes: Int?, date: LocalDate) = mutate { d ->
        d.withRecord(date) { r ->
            val entry = DayEntry(
                taskId = "avulsa-" + UUID.randomUUID().toString(),
                title = title.trim(), alarms = alarms.distinct().sorted(),
                done = false, note = "", oneOff = true, timerMinutes = timerMinutes, order = r.nextOrder(),
            )
            r.copy(entries = r.entries + entry)
        }
    }

    /** Edita uma tarefa "Só hoje". */
    fun updateOneOff(date: LocalDate, taskId: String, title: String, alarms: List<Int>, timerMinutes: Int?) = mutate { d ->
        d.withRecord(date) { r ->
            r.mapEntry(taskId) {
                it.copy(
                    title = title.trim(), alarms = alarms.distinct().sorted(), timerMinutes = timerMinutes,
                    timerEndsAt = if (timerMinutes == null) null else it.timerEndsAt,
                )
            }
        }
    }

    fun removeOneOff(date: LocalDate, taskId: String) {
        ReminderScheduler.cancel(context, taskId)
        ReminderScheduler.cancelTimer(context, taskId)
        mutate { d -> d.withRecord(date) { r -> r.copy(entries = r.entries.filterNot { it.taskId == taskId }) } }
    }

    fun addRoutine(title: String, days: Set<Int>, alarms: List<Int>, timerMinutes: Int?) {
        refreshDay()
        addRoutineInternal(title, days, alarms, timerMinutes)
    }

    private fun addRoutineInternal(title: String, days: Set<Int>, alarms: List<Int>, timerMinutes: Int?) = mutate { d ->
        val today = LocalDate.now()
        val task = RoutineTask(
            id = UUID.randomUUID().toString(),
            title = title.trim(), days = days, alarms = alarms.distinct().sorted(),
            createdAt = System.currentTimeMillis(), timerMinutes = timerMinutes,
            order = (d.tasks.maxOfOrNull { it.order } ?: -1) + 1,
        )
        val withTask = d.copy(tasks = d.tasks + task)
        if (today.dayOfWeek.value in days) {
            withTask.withRecord(today) { r -> r.copy(entries = r.entries + task.toEntry(r.nextOrder())) }
        } else withTask
    }

    /** Altera a rotina. Só o dia de hoje é ajustado; o histórico fica como estava. */
    fun updateRoutine(updated: RoutineTask) {
        refreshDay()
        updateRoutineInternal(updated)
    }

    private fun updateRoutineInternal(updated: RoutineTask) = mutate { d ->
        val today = LocalDate.now()
        val limpa = updated.copy(alarms = updated.alarms.distinct().sorted())
        val withTask = d.copy(tasks = d.tasks.map { if (it.id == limpa.id) limpa else it })
        withTask.withRecord(today) { r ->
            val existing = r.entries.firstOrNull { it.taskId == updated.id }
            val scheduledToday = today.dayOfWeek.value in updated.days
            when {
                existing != null && scheduledToday -> r.mapEntry(updated.id) {
                    it.copy(
                        title = updated.title, alarms = updated.alarms,
                        timerMinutes = updated.timerMinutes,
                        timerEndsAt = if (updated.timerMinutes == null) null else it.timerEndsAt,
                    )
                }
                existing != null && !existing.done -> r.copy(entries = r.entries - existing)
                existing == null && scheduledToday -> r.copy(entries = r.entries + updated.toEntry(r.nextOrder()))
                else -> r
            }
        }
    }

    /** Remove da rotina. Dias passados continuam no histórico. */
    fun deleteRoutine(taskId: String) {
        ReminderScheduler.cancel(context, taskId)
        ReminderScheduler.cancelTimer(context, taskId)
        refreshDay()
        mutate { d ->
            val today = LocalDate.now()
            d.copy(tasks = d.tasks.filterNot { it.id == taskId })
                .withRecord(today) { r -> r.copy(entries = r.entries.filterNot { it.taskId == taskId && !it.done }) }
        }
    }

    // ---------- Ordem da fila ----------

    /** Sobe (delta = -1) ou desce (delta = +1) uma tarefa na fila do dia. */
    fun moveEntry(date: LocalDate, taskId: String, delta: Int) = mutate { d ->
        val lista = d.record(date)?.entries.orEmpty().sortedBy { it.order }.toMutableList()
        val i = lista.indexOfFirst { it.taskId == taskId }
        val j = i + delta
        if (i < 0 || j !in lista.indices) return@mutate d
        java.util.Collections.swap(lista, i, j)
        aplicarOrdemDoDia(d, date, lista)
    }

    /** Coloca as tarefas do dia em ordem cronológica (pelo primeiro alarme). */
    fun sortDayByTime(date: LocalDate) = mutate { d ->
        val lista = d.record(date)?.entries.orEmpty()
            .sortedWith(compareBy<DayEntry>({ it.sortKey }, { it.order }))
        aplicarOrdemDoDia(d, date, lista)
    }

    /**
     * Grava a nova ordem do dia e leva a mudança para a rotina: as tarefas de rotina
     * do dia trocam de lugar entre si na fila geral, para os próximos dias seguirem igual.
     */
    private fun aplicarOrdemDoDia(d: AppData, date: LocalDate, lista: List<DayEntry>): AppData {
        val entradas = lista.mapIndexed { i, e -> e.copy(order = i) }
        val idsDoDia = entradas.filter { !it.oneOff }.map { it.taskId }
        val globais = d.tasks.sortedBy { it.order }.toMutableList()
        val vagas = globais.indices.filter { globais[it].id in idsDoDia }
        val naNovaOrdem = idsDoDia.mapNotNull { id -> globais.firstOrNull { it.id == id } }
        vagas.forEachIndexed { k, vaga -> if (k < naNovaOrdem.size) globais[vaga] = naNovaOrdem[k] }
        val tasks = globais.mapIndexed { i, t -> t.copy(order = i) }
        return d.copy(tasks = tasks).withRecord(date) { it.copy(entries = entradas) }
    }

    /** Sobe ou desce uma tarefa na fila geral da rotina (aba Rotinas). */
    fun moveRoutine(taskId: String, delta: Int) = mutate { d ->
        val globais = d.tasks.sortedBy { it.order }.toMutableList()
        val i = globais.indexOfFirst { it.id == taskId }
        val j = i + delta
        if (i < 0 || j !in globais.indices) return@mutate d
        java.util.Collections.swap(globais, i, j)
        aplicarOrdemDaRotina(d, globais)
    }

    /** Coloca a rotina inteira em ordem cronológica. */
    fun sortRoutinesByTime() = mutate { d ->
        aplicarOrdemDaRotina(d, d.tasks.sortedWith(compareBy<RoutineTask>({ it.sortKey }, { it.order })))
    }

    /** Grava a ordem geral e reflete no dia de hoje (tarefas "Só hoje" ficam onde estão). */
    private fun aplicarOrdemDaRotina(d: AppData, novaOrdem: List<RoutineTask>): AppData {
        val tasks = novaOrdem.mapIndexed { i, t -> t.copy(order = i) }
        val posicao = tasks.associate { it.id to it.order }
        val hoje = LocalDate.now()
        val atual = d.record(hoje)?.entries.orEmpty().sortedBy { it.order }.toMutableList()
        val vagas = atual.indices.filter { !atual[it].oneOff && atual[it].taskId in posicao }
        val rotinas = vagas.map { atual[it] }.sortedBy { posicao[it.taskId] }
        vagas.forEachIndexed { k, vaga -> atual[vaga] = rotinas[k] }
        val entradas = atual.mapIndexed { i, e -> e.copy(order = i) }
        val comTarefas = d.copy(tasks = tasks)
        return if (d.record(hoje) == null) comTarefas else comTarefas.withRecord(hoje) { it.copy(entries = entradas) }
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
