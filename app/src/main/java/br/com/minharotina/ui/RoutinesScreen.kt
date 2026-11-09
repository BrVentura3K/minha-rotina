package br.com.minharotina.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.RoutineTask
import br.com.minharotina.data.formatTime

@Composable
fun RoutinesScreen(tasks: List<RoutineTask>, onEditar: (RoutineTask) -> Unit) {
    val ordenadas = tasks.sortedWith(compareBy<RoutineTask>({ it.hour ?: 25 }, { it.minute ?: 0 }, { it.title }))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Minhas rotinas", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta)
                Text("Tarefas que se repetem. Toque para editar.", fontSize = 14.sp, color = Cores.Suave)
            }
        }

        if (ordenadas.isEmpty()) {
            item {
                Text(
                    "Você ainda não tem rotinas. Toque no + e escolha \"Na rotina\" para criar a primeira.",
                    fontSize = 15.sp, color = Cores.Suave, modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        items(ordenadas, key = { it.id }) { t ->
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .clickable(onClickLabel = "Editar ${t.title}") { onEditar(t) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            t.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Cores.Tinta,
                            modifier = Modifier.weight(1f),
                        )
                        val hora = formatTime(t.hour, t.minute)
                        if (hora != null) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Cores.Suave, modifier = Modifier.size(14.dp))
                            Text(" $hora", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cores.Suave)
                        } else {
                            Text("Sem alarme", fontSize = 13.sp, color = Cores.Suave)
                        }
                    }
                    DiasMarcados(t.days)
                }
            }
        }
    }
}
