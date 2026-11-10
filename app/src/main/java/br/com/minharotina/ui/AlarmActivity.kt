package br.com.minharotina.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.RotinaApp
import br.com.minharotina.alarm.Notifications
import br.com.minharotina.alarm.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Tela cheia do alarme, mostrada por cima da tela de bloqueio. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID)
        if (taskId == null) {
            finish()
            return
        }
        val titulo = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Tarefa"
        val frase = intent.getStringExtra(ReminderScheduler.EXTRA_QUOTE) ?: ""
        val hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        setContent {
            RotinaTheme {
                TelaAlarme(
                    hora = hora,
                    titulo = titulo,
                    frase = frase,
                    onFeito = {
                        val repo = RotinaApp.repo(this)
                        repo.markDone(LocalDate.now(), taskId)
                        repo.persistNow()
                        Notifications.dismiss(this, taskId)
                        finish()
                    },
                    onAdiar = {
                        Notifications.dismiss(this, taskId)
                        ReminderScheduler.snooze(this, taskId, titulo, 10)
                        finish()
                    },
                    onFechar = {
                        Notifications.dismiss(this, taskId)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun TelaAlarme(
    hora: String,
    titulo: String,
    frase: String,
    onFeito: () -> Unit,
    onAdiar: () -> Unit,
    onFechar: () -> Unit,
) {
    Surface(color = Cores.Terracota, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(hora, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Está na hora de", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(4.dp))
            Text(
                titulo, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                textAlign = TextAlign.Center, lineHeight = 36.sp,
            )
            if (frase.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    frase, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center, lineHeight = 22.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onFeito,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Cores.Terracota),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                ) {
                    Text("Feito", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = onAdiar,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Adiar 10 min", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                TextButton(onClick = onFechar, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
