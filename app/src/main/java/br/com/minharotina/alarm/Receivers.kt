package br.com.minharotina.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.minharotina.RotinaApp
import br.com.minharotina.quotes.Quotes
import java.time.LocalDate

/** Dispara no horário da tarefa. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val repo = RotinaApp.repo(context)
        val today = LocalDate.now()
        repo.refreshDay(today)

        val ehTemporizador = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) == ReminderScheduler.KIND_TIMER
        val antes = repo.data.value.record(today)?.entries?.firstOrNull { it.taskId == taskId }
        // O temporizador terminou: deixa de estar "em andamento".
        if (ehTemporizador) repo.stopTimer(today, taskId)

        val data = repo.data.value
        val entry = data.record(today)?.entries?.firstOrNull { it.taskId == taskId }
        // Só avisa se a tarefa é de hoje e ainda não foi feita.
        if (entry != null && !entry.done && (!ehTemporizador || antes?.timerEndsAt != null)) {
            val title = entry.title.ifBlank { intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Tarefa" }
            val quote = if (ehTemporizador) {
                "O temporizador de ${entry.timerMinutes ?: 0} min terminou."
            } else {
                Quotes.ofDay(today, data.customQuotes)
            }
            Notifications.showReminder(
                context, taskId, title, quote, tocar = true,
                chamada = if (ehTemporizador) "Tempo esgotado!" else "Está na hora!",
            )
            // Para de tocar sozinho se ninguém responder.
            ReminderScheduler.scheduleSilence(context, taskId, title, quote, Notifications.TEMPO_TOCANDO_MS)
        }
        // Agenda a próxima ocorrência.
        repo.persistNow()
    }
}

/** Botão "Marcar como feita" da notificação. */
class DoneActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val repo = RotinaApp.repo(context)
        repo.markDone(LocalDate.now(), taskId)
        repo.persistNow()
        Notifications.dismiss(context, taskId)
    }
}

/** Reagenda tudo depois de reiniciar o celular, atualizar o app ou mudar o relógio. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = RotinaApp.repo(context)
        repo.refreshDay()
        repo.persistNow()
    }
}

/** Botão "Adiar 10 min": para o alarme e toca de novo daqui a 10 minutos. */
class SnoozeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Tarefa"
        Notifications.dismiss(context, taskId)
        ReminderScheduler.snooze(context, taskId, title, 10)
    }
}

/** Depois do tempo máximo tocando, troca o alarme por uma notificação silenciosa. */
class SilenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        if (!Notifications.isShowing(context, taskId)) return
        Notifications.showReminder(
            context, taskId,
            intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Tarefa",
            intent.getStringExtra(ReminderScheduler.EXTRA_QUOTE) ?: "",
            tocar = false,
        )
    }
}
