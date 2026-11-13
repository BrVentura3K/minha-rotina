package br.com.minharotina.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.Repository
import br.com.minharotina.data.MAX_ALARMES
import br.com.minharotina.data.formatMinutes
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(folha: Folha, repo: Repository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val editando = (folha as? Folha.Editar)?.task
    val avulsa = folha as? Folha.EditarAvulsa
    val emEdicao = editando != null || avulsa != null

    var titulo by rememberSaveable { mutableStateOf(editando?.title ?: avulsa?.entry?.title ?: "") }
    var naRotina by rememberSaveable { mutableStateOf(editando != null || (folha as? Folha.Nova)?.naRotina == true) }
    var dias by remember { mutableStateOf(editando?.days ?: setOf(LocalDate.now().dayOfWeek.value)) }
    var alarmes by remember { mutableStateOf(editando?.alarms ?: avulsa?.entry?.alarms ?: emptyList()) }
    val timerInicial = editando?.timerMinutes ?: avulsa?.entry?.timerMinutes
    var temporizador by rememberSaveable { mutableStateOf(timerInicial != null) }
    var minutosTimer by rememberSaveable { mutableStateOf((timerInicial ?: 25).toString()) }
    val minutosValidos = minutosTimer.toIntOrNull()?.takeIf { it in 1..600 }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val podeSalvar = titulo.isNotBlank() && (!naRotina || dias.isNotEmpty()) &&
        (!temporizador || minutosValidos != null)

    /** Abre o relógio. [trocar] = horário que está sendo alterado, ou null para adicionar um novo. */
    fun escolherHorario(trocar: Int?) {
        val inicial = trocar ?: alarmes.maxOrNull()?.let { (it + 60).coerceAtMost(23 * 60 + 59) } ?: (8 * 60)
        TimePickerDialog(context, { _, h, m ->
            val novo = h * 60 + m
            alarmes = ((alarmes - (trocar ?: -1)) + novo).distinct().sorted()
        }, inicial / 60, inicial % 60, true).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cores.Fundo,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                if (emEdicao) "Editar tarefa" else "Nova tarefa",
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta,
            )

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Tarefa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!emEdicao) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rotulo("Adicionar")
                    Segmentado(
                        opcoes = listOf("Só hoje", "Na rotina"),
                        selecionado = if (naRotina) 1 else 0,
                        onSelect = { naRotina = it == 1 },
                    )
                }
            }

            if (naRotina) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rotulo("Em quais dias?")
                    SeletorDias(dias = dias, onChange = { dias = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Atalho("Seg a sex") { dias = (1..5).toSet() }
                        Atalho("Todos os dias") { dias = (1..7).toSet() }
                        Atalho("Fim de semana") { dias = setOf(6, 7) }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Cores.Verde)
                        Column(Modifier.weight(1f)) {
                            Text("Alarmes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Cores.Tinta)
                            Text(
                                when (alarmes.size) {
                                    0 -> "Sem alarme"
                                    1 -> "1 alarme. Toque no horário para mudar."
                                    else -> "${alarmes.size} alarmes. Toque no horário para mudar."
                                },
                                fontSize = 13.sp, color = Cores.Suave,
                            )
                        }
                    }
                    alarmes.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Cores.Fundo),
                        ) {
                            Text(
                                formatMinutes(m),
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Verde,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClickLabel = "Mudar horário ${formatMinutes(m)}") { escolherHorario(m) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                            IconButton(onClick = { alarmes = alarmes - m }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover alarme das ${formatMinutes(m)}", tint = Cores.Suave)
                            }
                        }
                    }
                    if (alarmes.size < MAX_ALARMES) {
                        OutlinedButton(onClick = { escolherHorario(null) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Cores.Verde, modifier = Modifier.size(18.dp))
                            Text(
                                if (alarmes.isEmpty()) "  Adicionar alarme" else "  Adicionar outro alarme",
                                color = Cores.Verde, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Cores.Verde)
                        Column(Modifier.weight(1f)) {
                            Text("Temporizador", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Cores.Tinta)
                            Text(
                                if (temporizador) "Toca o alarme quando o tempo acabar" else "Sem temporizador",
                                fontSize = 13.sp, color = Cores.Suave,
                            )
                        }
                        Switch(
                            checked = temporizador,
                            onCheckedChange = { temporizador = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Cores.Verde),
                        )
                    }
                    if (temporizador) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(5, 10, 15, 20, 25, 30, 45, 60).forEach { m ->
                                val ativo = minutosValidos == m
                                Text(
                                    "$m min",
                                    fontSize = 13.sp,
                                    fontWeight = if (ativo) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (ativo) Color.White else Cores.Suave,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (ativo) Cores.Verde else Color.Transparent)
                                        .border(1.dp, if (ativo) Cores.Verde else Cores.Borda, RoundedCornerShape(18.dp))
                                        .clickable(role = Role.Button) { minutosTimer = m.toString() }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = minutosTimer,
                            onValueChange = { v -> minutosTimer = v.filter { it.isDigit() }.take(3) },
                            label = { Text("Minutos") },
                            supportingText = {
                                if (minutosValidos == null) Text("Escolha de 1 a 600 minutos")
                            },
                            isError = minutosValidos == null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val t = if (temporizador) minutosValidos else null
                    when {
                        editando != null -> repo.updateRoutine(
                            editando.copy(title = titulo.trim(), days = dias, alarms = alarmes, timerMinutes = t),
                        )
                        avulsa != null -> repo.updateOneOff(avulsa.date, avulsa.entry.taskId, titulo, alarmes, t)
                        naRotina -> repo.addRoutine(titulo, dias, alarmes, t)
                        else -> repo.addOneOff(titulo, alarmes, t)
                    }
                    onDismiss()
                },
                enabled = podeSalvar,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cores.Terracota),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text("Salvar tarefa", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }

            if (editando != null) {
                TextButton(onClick = { confirmarExclusao = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Excluir da rotina", color = Cores.TerracotaEscuro, fontWeight = FontWeight.Bold)
                }
            }
            if (avulsa != null) {
                TextButton(
                    onClick = {
                        repo.removeOneOff(avulsa.date, avulsa.entry.taskId)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Remover tarefa de hoje", color = Cores.TerracotaEscuro, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }

    if (confirmarExclusao && editando != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir \"${editando.title}\"?") },
            text = { Text("Ela sai da rotina dos próximos dias. O que já está no histórico continua lá.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.deleteRoutine(editando.id)
                    confirmarExclusao = false
                    onDismiss()
                }) { Text("Excluir", color = Cores.TerracotaEscuro, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun Rotulo(texto: String) {
    Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cores.Suave)
}

@Composable
private fun Atalho(texto: String, onClick: () -> Unit) {
    Text(
        texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Cores.Suave,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Cores.Borda, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}
