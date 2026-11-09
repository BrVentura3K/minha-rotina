package br.com.minharotina.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.Repository
import br.com.minharotina.data.formatTime
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(folha: Folha, repo: Repository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val editando = (folha as? Folha.Editar)?.task

    var titulo by rememberSaveable { mutableStateOf(editando?.title ?: "") }
    var naRotina by rememberSaveable { mutableStateOf(editando != null || (folha as? Folha.Nova)?.naRotina == true) }
    var dias by remember { mutableStateOf(editando?.days ?: setOf(LocalDate.now().dayOfWeek.value)) }
    var lembrete by rememberSaveable { mutableStateOf(editando?.hour != null) }
    var hora by rememberSaveable { mutableIntStateOf(editando?.hour ?: 8) }
    var minuto by rememberSaveable { mutableIntStateOf(editando?.minute ?: 0) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val podeSalvar = titulo.isNotBlank() && (!naRotina || dias.isNotEmpty())

    fun escolherHorario() {
        TimePickerDialog(context, { _, h, m ->
            hora = h
            minuto = m
            lembrete = true
        }, hora, minuto, true).show()
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
                if (editando != null) "Editar tarefa" else "Nova tarefa",
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta,
            )

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Tarefa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (editando == null) {
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
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Cores.Verde)
                    Column(Modifier.weight(1f)) {
                        Text("Lembrete", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Cores.Tinta)
                        if (lembrete) {
                            Text(
                                formatTime(hora, minuto) ?: "",
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Verde,
                                modifier = Modifier.clickable(onClickLabel = "Mudar horário") { escolherHorario() },
                            )
                        } else {
                            Text("Sem alarme", fontSize = 13.sp, color = Cores.Suave)
                        }
                    }
                    Switch(
                        checked = lembrete,
                        onCheckedChange = { ligado -> if (ligado) escolherHorario() else lembrete = false },
                        colors = SwitchDefaults.colors(checkedTrackColor = Cores.Verde),
                    )
                }
            }

            Button(
                onClick = {
                    val h = if (lembrete) hora else null
                    val m = if (lembrete) minuto else null
                    when {
                        editando != null -> repo.updateRoutine(
                            editando.copy(title = titulo.trim(), days = dias, hour = h, minute = m),
                        )
                        naRotina -> repo.addRoutine(titulo, dias, h, m)
                        else -> repo.addOneOff(titulo, h, m)
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
