package br.com.minharotina.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import br.com.minharotina.data.AppData
import br.com.minharotina.data.RoutineTask
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mantém agendado o próximo alarme de cada tarefa.
 * Cada tarefa tem um único PendingIntent (identificado pela URI), então
 * agendar de novo substitui o anterior.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "br.com.minharotina.REMINDER"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TITLE = "title"
    const val EXTRA_QUOTE = "quote"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun rescheduleAll(context: Context, data: AppData) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayRecord = data.record(today)

        for (task in data.tasks) {
            val next = nextOccurrence(task, now, todayRecord?.entries?.firstOrNull { it.taskId == task.id }?.done == true)
            if (next == null) cancel(context, task.id) else schedule(context, task.id, task.title, next)
        }

        todayRecord?.entries?.filter { it.oneOff }?.forEach { e ->
            val at = e.hour?.let { today.atTime(it, e.minute ?: 0) }
            if (at != null && !e.done && at.isAfter(now)) {
                schedule(context, e.taskId, e.title, at)
            } else {
                cancel(context, e.taskId)
            }
        }
    }

    private fun nextOccurrence(task: RoutineTask, now: LocalDateTime, doneToday: Boolean): LocalDateTime? {
        val hour = task.hour ?: return null
        if (task.days.isEmpty()) return null
        val today: LocalDate = now.toLocalDate()
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            if (date.dayOfWeek.value !in task.days) continue
            val at = date.atTime(hour, task.minute ?: 0)
            if (offset == 0 && (doneToday || !at.isAfter(now))) continue
            return at
        }
        return null
    }

    private fun intentFor(context: Context, taskId: String, title: String? = null) =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("rotina://tarefa/$taskId")
            putExtra(EXTRA_TASK_ID, taskId)
            if (title != null) putExtra(EXTRA_TITLE, title)
        }

    private fun schedule(context: Context, taskId: String, title: String, at: LocalDateTime) {
        val pi = PendingIntent.getBroadcast(
            context, 0, intentFor(context, taskId, title),
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

    fun cancel(context: Context, taskId: String) {
        val pi = PendingIntent.getBroadcast(
            context, 0, intentFor(context, taskId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }
}
