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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import br.com.minharotina.data.DayEntry
import br.com.minharotina.data.Repository
import br.com.minharotina.data.RoutineTask
import android.widget.Toast
import br.com.minharotina.update.Updater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Aba(val rotulo: String, val icone: ImageVector) {
    HOJE("Hoje", Icons.Default.CheckCircle),
    ROTINAS("Rotinas", Icons.Default.Refresh),
    HISTORICO("Histórico", Icons.Default.DateRange),
    FRASES("Frases", Icons.Default.Favorite),
    CORES("Cores", Icons.Default.Star),
}

sealed interface Folha {
    data class Nova(val naRotina: Boolean) : Folha
    data class Editar(val task: RoutineTask) : Folha
    data class EditarAvulsa(val date: LocalDate, val entry: DayEntry) : Folha
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

    // Atualização do app: confere ao abrir (no máximo a cada 30 min).
    val scope = rememberCoroutineScope()
    var atualizacao by remember { mutableStateOf<Updater.Release?>(null) }
    var progresso by remember { mutableStateOf<Float?>(null) }
    var ultimaChecagem by remember { mutableLongStateOf(0L) }
    LaunchedEffect(resumeTick) {
        val agora = System.currentTimeMillis()
        if (agora - ultimaChecagem > 30 * 60_000L) {
            ultimaChecagem = agora
            Updater.latest()?.let { r -> atualizacao = r.takeIf { Updater.isNewer(it) } }
        }
    }
    val atualizar: () -> Unit = {
        val r = atualizacao
        if (r != null && progresso == null) {
            if (!Updater.canInstall(context)) {
                Toast.makeText(context, "Ative \"Permitir desta fonte\" e toque em Atualizar de novo.", Toast.LENGTH_LONG).show()
                Updater.openInstallPermission(context)
            } else {
                scope.launch {
                    progresso = 0f
                    try {
                        Updater.install(context, r) { progresso = it }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Não foi possível baixar a atualização. Tente de novo.", Toast.LENGTH_LONG).show()
                    } finally {
                        progresso = null
                    }
                }
            }
        }
    }
    val verificarAgora: () -> Unit = {
        scope.launch {
            ultimaChecagem = System.currentTimeMillis()
            val r = Updater.latest()
            val msg = when {
                r == null -> "Não foi possível verificar agora. Confira a internet."
                Updater.isNewer(r) -> { atualizacao = r; aba = Aba.HOJE; "Tem versão nova! Toque em Atualizar." }
                else -> "Você já está na versão mais recente."
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
                Aba.HOJE -> TodayScreen(
                    data = data, hoje = hoje, repo = repo, resumeTick = resumeTick,
                    onEditar = { e ->
                        val rotina = data.tasks.firstOrNull { it.id == e.taskId }
                        folha = when {
                            e.oneOff -> Folha.EditarAvulsa(hoje, e)
                            rotina != null -> Folha.Editar(rotina)
                            else -> null
                        }
                    },
                ) {
                    atualizacao?.let { CartaoAtualizacao(it, progresso, atualizar) }
                }
                Aba.ROTINAS -> RoutinesScreen(tasks = data.tasks, onEditar = { folha = Folha.Editar(it) })
                Aba.HISTORICO -> HistoryScreen(data = data, hoje = hoje)
                Aba.FRASES -> QuotesScreen(data = data, hoje = hoje, repo = repo, onVerificarAtualizacao = verificarAgora)
                Aba.CORES -> ColorsScreen()
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
    val telaCheia = remember(resumeTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent()
    }
    if (notificacoes && exatos && bateria && telaCheia) return

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
            if (!telaCheia && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                BotaoConfig("Permitir alarme em tela cheia") {
                    abrir(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
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

@Composable
private fun CartaoAtualizacao(release: Updater.Release, progresso: Float?, onAtualizar: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Cores.Verde, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nova versão disponível", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
            if (release.notes.isNotBlank()) {
                Text(release.notes, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 4)
            }
            if (progresso != null) {
                Text("Baixando… ${(progresso * 100).toInt()}%", fontSize = 13.sp, color = Color.White)
                BarraProgresso(progresso, cor = Color.White, trilho = Color.White.copy(alpha = 0.3f))
            } else {
                androidx.compose.material3.Button(
                    onClick = onAtualizar,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Cores.Verde,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Atualizar", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
