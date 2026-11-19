package br.com.minharotina.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import br.com.minharotina.ui.AlarmActivity
import kotlin.math.roundToInt
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
    private var volumeOriginal: Int? = null
    private var foco: AudioFocusRequest? = null
    private var overlay: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instancia = this
    }

    override fun onDestroy() {
        pararSomEVibracao()
        devolverVolume()
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
        garantirVolume()
        pedirFoco()
        tocarSom()
        vibrar()
        abrirTelaCheia(novo)
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

    /**
     * O som sai no volume de ALARME, que o modo silencioso e o vibrar não cortam.
     * Se esse volume estiver baixo ou no zero, sobe enquanto toca e depois volta.
     */
    private fun garantirVolume() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val minimo = (max * 0.7f).roundToInt().coerceAtLeast(1)
        val agora = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        if (agora < minimo) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, minimo, 0)
                if (volumeOriginal == null) volumeOriginal = agora
            } catch (e: SecurityException) {
                Log.w(TAG, "Não foi possível subir o volume do alarme", e)
            }
        }
    }

    private fun devolverVolume() {
        val original = volumeOriginal ?: return
        volumeOriginal = null
        try {
            getSystemService(AudioManager::class.java)?.setStreamVolume(AudioManager.STREAM_ALARM, original, 0)
        } catch (_: SecurityException) {
        }
    }

    /** Pausa música ou vídeo enquanto o alarme toca. */
    private fun pedirFoco() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val pedido = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            .build()
        audio.requestAudioFocus(pedido)
        foco = pedido
    }

    /**
     * Abre a tela cheia do alarme também com o celular em uso. Com a tela bloqueada,
     * a notificação já abre sozinha. Desbloqueado, o Android só deixa abrir se o app
     * tiver a permissão "Aparecer por cima de outros apps"; uma janelinha de 1 ponto
     * fica visível por um instante para o Android liberar a abertura.
     */
    private fun abrirTelaCheia(alarme: Alarme) {
        if (!Settings.canDrawOverlays(this)) return
        try {
            val wm = getSystemService(WindowManager::class.java)
            val v = View(this)
            val params = WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
            wm.addView(v, params)
            overlay = v
        } catch (e: Exception) {
            Log.w(TAG, "Sem janela por cima", e)
        }
        try {
            startActivity(
                Intent(this, AlarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    putExtra(ReminderScheduler.EXTRA_TASK_ID, alarme.taskId)
                    putExtra(ReminderScheduler.EXTRA_TITLE, alarme.title)
                    putExtra(ReminderScheduler.EXTRA_QUOTE, alarme.quote)
                    putExtra(Notifications.EXTRA_CHAMADA, alarme.chamada)
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "Não deu para abrir a tela cheia", e)
        }
        handler.postDelayed({ removerOverlay() }, 3_000)
    }

    private fun removerOverlay() {
        val v = overlay ?: return
        overlay = null
        try {
            getSystemService(WindowManager::class.java).removeView(v)
        } catch (_: Exception) {
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
        removerOverlay()
        foco?.let { getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(it) }
        foco = null
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
        devolverVolume()
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
