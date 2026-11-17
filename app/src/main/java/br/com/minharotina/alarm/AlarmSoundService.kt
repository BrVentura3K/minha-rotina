package br.com.minharotina.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Toca o alarme como um despertador: o próprio app toca o som (no volume de alarme)
 * e vibra, sem depender do som do canal de notificação, que a Samsung ou o
 * "Não perturbe" podem silenciar.
 */
class AlarmSoundService : Service() {

    private data class Alarme(val taskId: String, val title: String, val quote: String, val chamada: String)

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var atual: Alarme? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instancia = this
    }

    override fun onDestroy() {
        pararSomEVibracao()
        if (instancia === this) instancia = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(ReminderScheduler.EXTRA_TASK_ID)
        if (taskId == null) {
            if (atual == null) stopSelf()
            return START_NOT_STICKY
        }
        val novo = Alarme(
            taskId = taskId,
            title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Tarefa",
            quote = intent.getStringExtra(ReminderScheduler.EXTRA_QUOTE) ?: "",
            chamada = intent.getStringExtra(Notifications.EXTRA_CHAMADA) ?: "Está na hora!",
        )
        val anterior = atual

        val notificacao = Notifications.buildReminder(
            this, novo.taskId, novo.title, novo.quote, Notifications.Modo.TOCANDO_PELO_APP, novo.chamada,
        )
        try {
            ServiceCompat.startForeground(
                this, Notifications.notificationId(novo.taskId), notificacao,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Não deu para tocar pelo app; usando o som da notificação", e)
            tocarPelaNotificacao(this, novo.taskId, novo.title, novo.quote, novo.chamada)
            if (anterior == null) stopSelf()
            return START_NOT_STICKY
        }

        // Se outro alarme já estava tocando, ele fica na tela, mas em silêncio.
        if (anterior != null && anterior.taskId != novo.taskId) {
            Notifications.showReminder(this, anterior.taskId, anterior.title, anterior.quote, tocar = false, chamada = anterior.chamada)
        }
        atual = novo

        pararSomEVibracao()
        tocarSom()
        vibrar()
        handler.postDelayed({ encerrar(deixarAviso = true) }, Notifications.TEMPO_TOCANDO_MS)
        return START_NOT_STICKY
    }

    private fun tocarSom() {
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidatos = listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri: Uri in candidatos) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(atributos)
                    setDataSource(this@AlarmSoundService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Som indisponível: $uri", e)
                player?.release()
                player = null
            }
        }
    }

    private fun vibrar() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        vibrator = v
        val padrao = VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800, 1200), 0)
        try {
            v.vibrate(
                padrao,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao vibrar", e)
        }
    }

    private fun pararSomEVibracao() {
        handler.removeCallbacksAndMessages(null)
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    /**
     * Para de tocar. Com [deixarAviso], a notificação continua na tela em silêncio
     * (tempo máximo esgotado); sem ele, some (ela respondeu).
     */
    private fun encerrar(deixarAviso: Boolean) {
        val alarme = atual
        atual = null
        pararSomEVibracao()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (deixarAviso && alarme != null) {
            Notifications.showReminder(this, alarme.taskId, alarme.title, alarme.quote, tocar = false, chamada = alarme.chamada)
        }
        stopSelf()
    }

    companion object {
        private const val TAG = "AlarmSoundService"

        @Volatile
        private var instancia: AlarmSoundService? = null

        /** Começa a tocar o alarme da tarefa. */
        fun start(context: Context, taskId: String, title: String, quote: String, chamada: String) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                putExtra(ReminderScheduler.EXTRA_TITLE, title)
                putExtra(ReminderScheduler.EXTRA_QUOTE, quote)
                putExtra(Notifications.EXTRA_CHAMADA, chamada)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Serviço do alarme não pôde iniciar; usando o som da notificação", e)
                tocarPelaNotificacao(context, taskId, title, quote, chamada)
            }
        }

        /** Para o alarme da tarefa, se for ele que está tocando. */
        fun stop(taskId: String) {
            val s = instancia ?: return
            s.handler.post {
                if (s.atual?.taskId == taskId) s.encerrar(deixarAviso = false)
            }
        }

        /** Plano B: o som do canal de notificação, como nas versões anteriores. */
        private fun tocarPelaNotificacao(context: Context, taskId: String, title: String, quote: String, chamada: String) {
            Notifications.showReminder(context, taskId, title, quote, tocar = true, chamada = chamada)
            ReminderScheduler.scheduleSilence(context, taskId, title, quote, Notifications.TEMPO_TOCANDO_MS)
        }
    }
}
