package br.com.minharotina.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.minharotina.alarm.ReminderScheduler
import br.com.minharotina.data.Repository
import br.com.minharotina.data.RoutineTask
import kotlinx.coroutines.delay
import java.time.LocalDate

enum class Aba(val rotulo: String, val icone: ImageVector) {
    HOJE("Hoje", Icons.Default.CheckCircle),
    ROTINAS("Rotinas", Icons.Default.Refresh),
    HISTORICO("Histórico", Icons.Default.DateRange),
    FRASES("Frases", Icons.Default.Favorite),
}

sealed interface Folha {
    data class Nova(val naRotina: Boolean) : Folha
    data class Editar(val task: RoutineTask) : Folha
}

@Composable
fun RotinaRoot(repo: Repository, resumeTick: Int) {
    val data by repo.data.collectAsState()
    var aba by rememberSaveable { mutableStateOf(Aba.HOJE) }
    var folha by remember { mutableStateOf<Folha?>(null) }
    var hoje by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(resumeTick) { hoje = LocalDate.now() }
    // Vira o dia sozinho se o app ficar aberto depois da meia-noite.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            val agora = LocalDate.now()
            if (agora != hoje) {
                repo.refreshDay(agora)
                hoje = agora
            }
        }
    }

    val context = LocalContext.current
    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = Cores.Fundo,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Aba.entries.forEach { a ->
                    NavigationBarItem(
                        selected = aba == a,
                        onClick = { aba = a },
                        icon = { Icon(a.icone, contentDescription = null) },
                        label = { Text(a.rotulo, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cores.Terracota,
                            selectedTextColor = Cores.Terracota,
                            indicatorColor = Cores.TerracotaClaro,
                            unselectedIconColor = Cores.Suave,
                            unselectedTextColor = Cores.Suave,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (aba == Aba.HOJE || aba == Aba.ROTINAS) {
                FloatingActionButton(
                    onClick = { folha = Folha.Nova(naRotina = aba == Aba.ROTINAS) },
                    containerColor = Cores.Terracota,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar tarefa")
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (aba) {
                Aba.HOJE -> TodayScreen(data = data, hoje = hoje, repo = repo, resumeTick = resumeTick)
                Aba.ROTINAS -> RoutinesScreen(tasks = data.tasks, onEditar = { folha = Folha.Editar(it) })
                Aba.HISTORICO -> HistoryScreen(data = data, hoje = hoje)
                Aba.FRASES -> QuotesScreen(data = data, hoje = hoje, repo = repo)
            }
        }
    }

    folha?.let { f ->
        TaskSheet(folha = f, repo = repo, onDismiss = { folha = null })
    }
}

/** Mostra o que falta liberar para os lembretes tocarem no horário certo. */
@Composable
fun AvisoLembretes(resumeTick: Int) {
    val context = LocalContext.current
    val notificacoes = remember(resumeTick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exatos = remember(resumeTick) { ReminderScheduler.canScheduleExact(context) }
    val bateria = remember(resumeTick) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    if (notificacoes && exatos && bateria) return

    Surface(shape = RoundedCornerShape(16.dp), color = Cores.TerracotaClaro, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Configure os lembretes", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Cores.TerracotaEscuro)
            Text(
                "Sem isso os alarmes podem atrasar ou não tocar.",
                fontSize = 13.sp, color = Cores.Tinta,
            )
            if (!notificacoes) {
                BotaoConfig("Permitir notificações") {
                    abrir(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            if (!exatos && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BotaoConfig("Permitir alarmes no horário exato") {
                    abrir(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
            if (!bateria) {
                BotaoConfig("Liberar da economia de bateria") {
                    abrir(context, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
                }
            }
        }
    }
}

@Composable
private fun BotaoConfig(texto: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(texto, color = Cores.TerracotaEscuro, fontWeight = FontWeight.Bold)
    }
}

private fun abrir(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }
}
