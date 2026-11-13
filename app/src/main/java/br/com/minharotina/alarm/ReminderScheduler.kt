package br.com.minharotina.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import br.com.minharotina.data.AppData
import br.com.minharotina.data.MAX_ALARMES
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mantém agendado o próximo disparo de cada alarme de cada tarefa.
 * Cada alarme tem seu PendingIntent (identificado pela URI), então
 * agendar de novo substitui o anterior.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "br.com.minharotina.REMINDER"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TITLE = "title"
    const val EXTRA_QUOTE = "quote"
    const val EXTRA_KIND = "kind"
    const val KIND_TIMER = "timer"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun rescheduleAll(context: Context, data: AppData) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayRecord = data.record(today)

        // Cada tarefa tem até MAX_ALARMES "vagas" de alarme; vagas sem horário são canceladas.
        for (task in data.tasks) {
            val feitaHoje = todayRecord?.entries?.firstOrNull { it.taskId == task.id }?.done == true
            for (vaga in 0 until MAX_ALARMES) {
                val minutos = task.alarms.getOrNull(vaga)
                val next = minutos?.let { nextOccurrence(task.days, it, now, feitaHoje) }
                if (next == null) cancelSlot(context, task.id, vaga) else schedule(context, task.id, vaga, task.title, next)
            }
            cancelSlot(context, task.id, null) // alarme no formato antigo (um só por tarefa)
        }

        // Temporizadores em andamento (também sobrevivem a reiniciar o celular).
        val agoraMs = System.currentTimeMillis()
        todayRecord?.entries?.forEach { e ->
            val fim = e.timerEndsAt
            if (fim != null && fim > agoraMs && !e.done) scheduleTimer(context, e.taskId, e.title, fim)
            else cancelTimer(context, e.taskId)
        }

        todayRecord?.entries?.filter { it.oneOff }?.forEach { e ->
            for (vaga in 0 until MAX_ALARMES) {
                val at = e.alarms.getOrNull(vaga)?.let { today.atTime(it / 60, it % 60) }
                if (at != null && !e.done && at.isAfter(now)) {
                    schedule(context, e.taskId, vaga, e.title, at)
                } else {
                    cancelSlot(context, e.taskId, vaga)
                }
            }
            cancelSlot(context, e.taskId, null)
        }
    }

    private fun nextOccurrence(days: Set<Int>, minutos: Int, now: LocalDateTime, doneToday: Boolean): LocalDateTime? {
        if (days.isEmpty()) return null
        val today: LocalDate = now.toLocalDate()
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            if (date.dayOfWeek.value !in days) continue
            val at = date.atTime(minutos / 60, minutos % 60)
            if (offset == 0 && (doneToday || !at.isAfter(now))) continue
            return at
        }
        return null
    }

    /** [vaga] = qual alarme da tarefa; null é o formato antigo, de alarme único. */
    private fun intentFor(context: Context, taskId: String, vaga: Int?, title: String? = null) =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("rotina://tarefa/$taskId" + (vaga?.let { "/$it" } ?: ""))
            putExtra(EXTRA_TASK_ID, taskId)
            if (title != null) putExtra(EXTRA_TITLE, title)
        }

    private fun schedule(context: Context, taskId: String, vaga: Int, title: String, at: LocalDateTime) {
        val pi = PendingIntent.getBroadcast(
            context, 0, intentFor(context, taskId, vaga, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(context, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi)
    }

    private fun setAlarm(context: Context, triggerAtMillis: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "Sem permissão de alarme exato", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun timerIntent(context: Context, taskId: String, title: String? = null) =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("rotina://timer/$taskId")
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_KIND, KIND_TIMER)
            if (title != null) putExtra(EXTRA_TITLE, title)
        }

    private fun scheduleTimer(context: Context, taskId: String, title: String, endsAtMillis: Long) {
        val pi = PendingIntent.getBroadcast(
            context, 0, timerIntent(context, taskId, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(context, endsAtMillis, pi)
    }

    fun cancelTimer(context: Context, taskId: String) {
        val pi = PendingIntent.getBroadcast(
            context, 0, timerIntent(context, taskId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }

    /** "Adiar": toca de novo daqui a [minutos], sem mexer no alarme normal da tarefa. */
    fun snooze(context: Context, taskId: String, title: String, minutos: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("rotina://adiada/$taskId")
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
        }
        val pi = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(context, System.currentTimeMillis() + minutos * 60_000L, pi)
    }

    /** Depois de alguns minutos tocando sem resposta, o alarme vira notificação silenciosa. */
    fun scheduleSilence(context: Context, taskId: String, title: String, quote: String, depoisMs: Long) {
        val intent = Intent(context, SilenceReceiver::class.java).apply {
            data = Uri.parse("rotina://silenciar/$taskId")
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_QUOTE, quote)
        }
        val pi = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + depoisMs, pi)
    }

    /** Cancela todos os alarmes da tarefa. */
    fun cancel(context: Context, taskId: String) {
        for (vaga in 0 until MAX_ALARMES) cancelSlot(context, taskId, vaga)
        cancelSlot(context, taskId, null)
    }

    private fun cancelSlot(context: Context, taskId: String, vaga: Int?) {
        val pi = PendingIntent.getBroadcast(
            context, 0, intentFor(context, taskId, vaga),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }
}
