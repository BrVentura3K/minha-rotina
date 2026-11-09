package br.com.minharotina.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.minharotina.R
import br.com.minharotina.ui.MainActivity

object Notifications {

    private const val CHANNEL_ID = "lembretes"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Lembretes das tarefas", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisa no horário de cada tarefa"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notificationId(taskId: String) = taskId.hashCode()

    fun showReminder(context: Context, taskId: String, title: String, quote: String) {
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Está na hora! $quote")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Está na hora!\n$quote"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Marcar como feita", done)
            .build()

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
