package br.com.minharotina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.AppData
import br.com.minharotina.data.DayEntry
import br.com.minharotina.data.Repository
import br.com.minharotina.data.formatTime
import br.com.minharotina.quotes.Quotes
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun TodayScreen(
    data: AppData,
    hoje: LocalDate,
    repo: Repository,
    resumeTick: Int,
    onEditar: (DayEntry) -> Unit,
    avisoAtualizacao: @Composable () -> Unit = {},
) {
    val registro = data.record(hoje)
    val entradas = registro?.entries.orEmpty().sortedBy { it.sortKey }
    val feitas = entradas.count { it.done }
    val total = entradas.size
    var expandida by rememberSaveable { mutableStateOf<String?>(null) }

    // Relógio da contagem regressiva: só anda enquanto há temporizador rodando.
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val algumRodando = entradas.any { (it.timerEndsAt ?: 0L) > agora }
    LaunchedEffect(algumRodando, resumeTick) {
        agora = System.currentTimeMillis()
        while (algumRodando) {
            delay(1_000)
            agora = System.currentTimeMillis()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(hoje.porExtenso(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Cores.Suave)
                    Text("Hoje", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta)
                }
                if (total > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$feitas/$total", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Verde)
                        Text("concluídas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Cores.Suave)
                    }
                }
            }
        }

        item { avisoAtualizacao() }

        item { AvisoLembretes(resumeTick) }

        item {
            val manchete = Quotes.headline(hoje, feitas, total, data.customQuotes)
            CartaoFrase(manchete.label, manchete.text, if (total == 0) 0f else feitas.toFloat() / total)
        }

        if (entradas.isEmpty()) {
            item {
                Text(
                    "Nenhuma tarefa para hoje. Toque no + para adicionar uma.",
                    fontSize = 15.sp, color = Cores.Suave,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        items(entradas, key = { it.taskId }) { e ->
            ItemTarefa(
                e = e,
                expandida = expandida == e.taskId,
                onExpandir = { expandida = if (expandida == e.taskId) null else e.taskId },
                onToggle = { repo.toggleDone(hoje, e.taskId) },
                onNota = { repo.setNote(hoje, e.taskId, it) },
                onEditar = { onEditar(e) },
                agora = agora,
                onIniciarTimer = { repo.startTimer(hoje, e.taskId) },
                onPararTimer = { repo.stopTimer(hoje, e.taskId) },
            )
        }
    }
}

@Composable
private fun CartaoFrase(rotulo: String, texto: String, progresso: Float) {
    Surface(shape = RoundedCornerShape(20.dp), color = Cores.Terracota, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                rotulo.uppercase(PT_BR), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, color = Color.White.copy(alpha = 0.9f),
            )
            Text(texto, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp, color = Color.White)
            BarraProgresso(progresso, cor = Color.White, trilho = Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ItemTarefa(
    e: DayEntry,
    expandida: Boolean,
    onExpandir: () -> Unit,
    onToggle: () -> Unit,
    onNota: (String) -> Unit,
    onEditar: () -> Unit,
    agora: Long,
    onIniciarTimer: () -> Unit,
    onPararTimer: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = if (expandida) BorderStroke(2.dp, Cores.Verde) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .clickable(onClickLabel = "Abrir nota", onClick = onExpandir)
                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                CaixaCheck(
                    feito = e.done,
                    rotulo = (if (e.done) "Desmarcar " else "Marcar ") + e.title,
                    onToggle = onToggle,
                )
                Spacer(Modifier.size(6.dp))
                Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        e.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (e.done) Cores.Suave else Cores.Tinta,
                        textDecoration = if (e.done) TextDecoration.LineThrough else null,
                    )
                    val hora = formatTime(e.hour, e.minute)
                    if (e.oneOff || hora != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (e.oneOff) Etiqueta("Só hoje")
                            if (hora != null) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Cores.Suave, modifier = Modifier.size(14.dp))
                                Text(hora, fontSize = 13.sp, color = Cores.Suave)
                            }
                        }
                    }
                    val minutos = e.timerMinutes
                    if (minutos != null && !e.done) {
                        BotaoTemporizador(
                            minutos = minutos,
                            restanteMs = e.timerEndsAt?.let { it - agora }?.takeIf { it > 0 },
                            onIniciar = onIniciarTimer,
                            onParar = onPararTimer,
                        )
                    }
                    if (!expandida && e.note.isNotBlank()) {
                        Text(
                            "Nota: ${e.note}", fontSize = 13.sp, color = Cores.Suave,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (expandida) {
                Column(Modifier.padding(start = 50.dp, top = 6.dp)) {
                    OutlinedTextField(
                        value = e.note,
                        onValueChange = onNota,
                        label = { Text("Nota de hoje") },
                        placeholder = { Text("Como foi? O que aconteceu?") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    )
                    TextButton(onClick = onEditar) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Cores.Verde, modifier = Modifier.size(18.dp))
                        Text("  Editar tarefa", color = Cores.Verde, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotaoTemporizador(minutos: Int, restanteMs: Long?, onIniciar: () -> Unit, onParar: () -> Unit) {
    if (restanteMs == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Cores.Verde)
                .clickable(onClickLabel = "Iniciar temporizador de $minutos minutos", onClick = onIniciar)
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(" Iniciar $minutos min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    } else {
        val total = restanteMs / 1000
        val texto = if (total >= 3600) {
            "%d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
        } else {
            "%02d:%02d".format(total / 60, total % 60)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                texto, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Verde,
                modifier = Modifier.semantics { contentDescription = "Faltam $texto" },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Cores.Borda, RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Parar temporizador", onClick = onParar)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Cores.Suave, modifier = Modifier.size(16.dp))
                Text(" Parar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cores.Suave)
            }
        }
    }
}
