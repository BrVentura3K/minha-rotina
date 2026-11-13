package br.com.minharotina.data

import java.time.LocalDate

/**
 * Tarefa da rotina. [days] usa a numeração de java.time.DayOfWeek:
 * 1 = segunda ... 7 = domingo.
 */
data class RoutineTask(
    val id: String,
    val title: String,
    val days: Set<Int>,
    /** Horários dos alarmes, em minutos desde a meia-noite, em ordem. */
    val alarms: List<Int>,
    val createdAt: Long,
    /** Duração do temporizador em minutos, ou null se a tarefa não tem temporizador. */
    val timerMinutes: Int? = null,
    /** Posição na fila (0 = primeira). */
    val order: Int = 0,
)

/** Uma tarefa dentro de um dia específico (foto do que era a tarefa naquele dia). */
data class DayEntry(
    val taskId: String,
    val title: String,
    val alarms: List<Int>,
    val done: Boolean,
    val note: String,
    val oneOff: Boolean,
    val timerMinutes: Int? = null,
    /** Quando o temporizador em andamento termina (epoch ms), ou null se parado. */
    val timerEndsAt: Long? = null,
    /** Posição na fila do dia (0 = primeira). */
    val order: Int = 0,
) {
    /** Para organizar por horário: o primeiro alarme; sem alarme vai para o fim. */
    val sortKey: Int get() = alarms.minOrNull() ?: (24 * 60 + 1)
}

data class DayRecord(
    val date: String,
    val entries: List<DayEntry>,
) {
    val total: Int get() = entries.size
    val doneCount: Int get() = entries.count { it.done }

    /** Percentual concluído, ou null quando o dia não tinha tarefas. */
    val percent: Int? get() = if (total == 0) null else doneCount * 100 / total
}

data class AppData(
    val tasks: List<RoutineTask> = emptyList(),
    val records: Map<String, DayRecord> = emptyMap(),
    val lastMaterialized: String? = null,
    val favoriteQuotes: Set<String> = emptySet(),
    val customQuotes: List<String> = emptyList(),
) {
    fun record(date: LocalDate): DayRecord? = records[date.toString()]
}

const val MAX_ALARMES = 10

fun formatMinutes(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)

/** "08:00 · 12:30", ou null se não houver alarme. */
fun List<Int>.formatAlarms(): String? = if (isEmpty()) null else sorted().joinToString(" · ") { formatMinutes(it) }

val RoutineTask.sortKey: Int get() = alarms.minOrNull() ?: (24 * 60 + 1)

val DAY_SHORT = listOf("S", "T", "Q", "Q", "S", "S", "D")
val DAY_FULL = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
