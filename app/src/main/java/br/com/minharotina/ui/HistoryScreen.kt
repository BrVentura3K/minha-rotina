package br.com.minharotina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.AppData
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun HistoryScreen(data: AppData, hoje: LocalDate) {
    var mes by remember { mutableStateOf(YearMonth.from(hoje)) }
    var selecionado by remember { mutableStateOf(hoje) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Histórico", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val seq = sequencia(data, hoje)
            Estatistica(if (seq == 1) "1 dia" else "$seq dias", "sequência completa", Cores.Verde, Modifier.weight(1f))
            Estatistica(mediaDoMes(data, mes, hoje)?.let { "$it%" } ?: "—", "média do mês", Cores.Tinta, Modifier.weight(1f))
            Estatistica(maisEsquecida(data, mes, hoje) ?: "—", "mais esquecida", Cores.TerracotaEscuro, Modifier.weight(1f), pequeno = true)
        }

        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { mes = mes.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior", tint = Cores.Suave)
                    }
                    Text(
                        mes.month.getDisplayName(TextStyle.FULL_STANDALONE, PT_BR).replaceFirstChar { it.titlecase(PT_BR) } + " ${mes.year}",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = { mes = mes.plusMonths(1) }, enabled = mes.isBefore(YearMonth.from(hoje))) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês", tint = Cores.Suave)
                    }
                }
                Calendario(data, mes, hoje, selecionado) { selecionado = it }
                Legenda()
            }
        }

        DetalheDia(data, selecionado)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Estatistica(valor: String, rotulo: String, cor: Color, modifier: Modifier, pequeno: Boolean = false) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                valor, fontSize = if (pequeno) 15.sp else 22.sp, fontWeight = FontWeight.ExtraBold,
                color = cor, maxLines = 2, lineHeight = if (pequeno) 18.sp else 26.sp,
            )
            Text(rotulo, fontSize = 12.sp, color = Cores.Suave)
        }
    }
}

@Composable
private fun Calendario(
    data: AppData,
    mes: YearMonth,
    hoje: LocalDate,
    selecionado: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val semana = listOf("D", "S", "T", "Q", "Q", "S", "S")
    val inicio = mes.atDay(1).dayOfWeek.value % 7 // domingo = 0
    val celulas: List<LocalDate?> = List(inicio) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
    val linhas = celulas.chunked(7)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        semana.forEach {
            Text(
                it, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cores.Suave,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
    linhas.forEach { linha ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 7) {
                val dia = linha.getOrNull(i)
                if (dia == null) {
                    Spacer(Modifier.weight(1f).height(36.dp))
                } else {
                    CelulaDia(
                        dia = dia,
                        percent = data.record(dia)?.percent,
                        futuro = dia.isAfter(hoje),
                        ehHoje = dia == hoje,
                        selecionado = dia == selecionado,
                        onClick = { onSelect(dia) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CelulaDia(
    dia: LocalDate,
    percent: Int?,
    futuro: Boolean,
    ehHoje: Boolean,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val (fundo, texto) = when {
        futuro -> Cores.Fundo to Cores.CheckVazio
        percent == null -> Cores.Vazio to Cores.Suave
        percent == 100 -> Cores.Verde to Color.White
        percent >= 70 -> Cores.VerdeMedio to Cores.Tinta
        else -> Cores.Salmao to Cores.Tinta
    }
    val forma = RoundedCornerShape(9.dp)
    val borda = when {
        selecionado -> Modifier.border(2.dp, Cores.Tinta, forma)
        ehHoje -> Modifier.border(2.dp, Cores.Terracota, forma)
        else -> Modifier
    }
    val descricao = if (percent == null) "Dia ${dia.dayOfMonth}" else "Dia ${dia.dayOfMonth}, $percent% concluído"
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(forma)
            .background(fundo)
            .then(borda)
            .clickable(enabled = !futuro, onClick = onClick)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${dia.dayOfMonth}", fontSize = 13.sp,
            fontWeight = if (ehHoje || selecionado) FontWeight.ExtraBold else FontWeight.Bold,
            color = texto,
        )
    }
}

@Composable
private fun Legenda() {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        ItemLegenda(Cores.Verde, "100%")
        ItemLegenda(Cores.VerdeMedio, "70–99%")
        ItemLegenda(Cores.Salmao, "abaixo de 70%")
    }
}

@Composable
private fun ItemLegenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(cor))
        Text(" $texto", fontSize = 11.sp, color = Cores.Suave)
    }
}

@Composable
private fun DetalheDia(data: AppData, dia: LocalDate) {
    val registro = data.record(dia)
    val entradas = registro?.entries.orEmpty().sortedBy { it.sortKey }
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dia.porExtenso(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta, modifier = Modifier.weight(1f))
                if (registro != null && registro.total > 0) {
                    Text(
                        "${registro.doneCount} de ${registro.total}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (registro.percent == 100) Cores.Verde else Cores.TerracotaEscuro,
                    )
                }
            }
            if (entradas.isEmpty()) {
                Text("Nenhuma tarefa registrada neste dia.", fontSize = 14.sp, color = Cores.Suave)
            }
            entradas.forEach { e ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (e.done) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (e.done) "Feita" else "Não feita",
                        tint = if (e.done) Cores.Verde else Cores.Terracota,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            e.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (e.done) Cores.Tinta else Cores.Suave,
                        )
                        if (e.note.isNotBlank()) {
                            Text("“${e.note}”", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Cores.Suave)
                        }
                    }
                }
            }
        }
    }
}

/** Dias seguidos com 100% (dias sem tarefas não quebram a sequência). */
private fun sequencia(data: AppData, hoje: LocalDate): Int {
    var dia = hoje
    if (data.record(hoje)?.percent != 100) dia = hoje.minusDays(1)
    var n = 0
    repeat(400) {
        val r = data.record(dia) ?: return n
        when {
            r.total == 0 -> Unit
            r.percent == 100 -> n++
            else -> return n
        }
        dia = dia.minusDays(1)
    }
    return n
}

private fun mediaDoMes(data: AppData, mes: YearMonth, hoje: LocalDate): Int? {
    val percents = (1..mes.lengthOfMonth())
        .map { mes.atDay(it) }
        .filter { !it.isAfter(hoje) }
        .mapNotNull { data.record(it)?.percent }
    return if (percents.isEmpty()) null else percents.sum() / percents.size
}

private fun maisEsquecida(data: AppData, mes: YearMonth, hoje: LocalDate): String? {
    val contagem = (1..mes.lengthOfMonth())
        .map { mes.atDay(it) }
        .filter { it.isBefore(hoje) }
        .flatMap { data.record(it)?.entries.orEmpty() }
        .filter { !it.done }
        .groupingBy { it.title }
        .eachCount()
    return contagem.maxByOrNull { it.value }?.key
}
