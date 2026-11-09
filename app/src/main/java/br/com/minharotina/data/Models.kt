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
    val hour: Int?,
    val minute: Int?,
    val createdAt: Long,
)

/** Uma tarefa dentro de um dia específico (foto do que era a tarefa naquele dia). */
data class DayEntry(
    val taskId: String,
    val title: String,
    val hour: Int?,
    val minute: Int?,
    val done: Boolean,
    val note: String,
    val oneOff: Boolean,
) {
    val sortKey: Int get() = if (hour == null) 24 * 60 + 1 else hour * 60 + (minute ?: 0)
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

fun formatTime(hour: Int?, minute: Int?): String? =
    if (hour == null) null else "%02d:%02d".format(hour, minute ?: 0)

val DAY_SHORT = listOf("S", "T", "Q", "Q", "S", "S", "D")
val DAY_FULL = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
