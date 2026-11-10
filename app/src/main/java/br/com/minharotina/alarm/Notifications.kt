package br.com.minharotina.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.minharotina.R
import br.com.minharotina.ui.AlarmActivity
import br.com.minharotina.ui.MainActivity

object Notifications {

    /** Canal antigo: usado só para a versão silenciosa depois que o alarme para de tocar. */
    private const val CHANNEL_SILENCIOSO = "lembretes"

    /** Canal do alarme: som de despertador, vibração e tela cheia. */
    private const val CHANNEL_ALARME = "alarmes_tarefas"

    /** Tempo máximo tocando sem resposta. */
    const val TEMPO_TOCANDO_MS = 2 * 60_000L

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENCIOSO, "Lembretes (silenciosos)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Lembrete que fica na tela depois que o alarme para de tocar"
            },
        )

        val som: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARME, "Alarmes das tarefas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Toca como despertador no horário de cada tarefa"
                setSound(
                    som,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    fun notificationId(taskId: String) = taskId.hashCode()

    fun isShowing(context: Context, taskId: String): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.id == notificationId(taskId) }

    /**
     * Mostra o lembrete. Com [tocar] = true, toca o som de alarme em repetição até a
     * pessoa responder e abre a tela cheia se o celular estiver bloqueado.
     */
    fun showReminder(context: Context, taskId: String, title: String, quote: String, tocar: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, DoneActionReceiver::class.java).apply {
                data = Uri.parse("rotina://concluir/$taskId")
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val adiar = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, SnoozeActionReceiver::class.java).apply {
                data = Uri.parse("rotina://adiar/$taskId")
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                putExtra(ReminderScheduler.EXTRA_TITLE, title)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, if (tocar) CHANNEL_ALARME else CHANNEL_SILENCIOSO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Está na hora! $quote")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Está na hora!\n$quote"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (tocar) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Feito", done)
            .addAction(0, "Adiar 10 min", adiar)

        if (tocar) {
            val telaCheia = PendingIntent.getActivity(
                context, notificationId(taskId),
                Intent(context, AlarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                    putExtra(ReminderScheduler.EXTRA_TITLE, title)
                    putExtra(ReminderScheduler.EXTRA_QUOTE, quote)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setFullScreenIntent(telaCheia, true)
        } else {
            builder.setSilent(true)
        }

        val notification = builder.build()
        if (tocar) notification.flags = notification.flags or Notification.FLAG_INSISTENT

        try {
            NotificationManagerCompat.from(context).notify(notificationId(taskId), notification)
        } catch (e: SecurityException) {
            // Permissão de notificação revogada no meio do caminho.
        }
    }

    fun dismiss(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
    }
}
