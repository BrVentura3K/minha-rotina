package br.com.minharotina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.DAY_FULL
import br.com.minharotina.data.DAY_SHORT
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

fun LocalDate.porExtenso(): String =
    format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR))
        .replaceFirstChar { it.titlecase(PT_BR) }

/** Caixinha de check com área de toque de 44dp. */
@Composable
fun CaixaCheck(feito: Boolean, rotulo: String, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .toggleable(value = feito, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = rotulo },
        contentAlignment = Alignment.Center,
    ) {
        if (feito) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Cores.Verde),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Color.White)
                    .border(2.dp, Cores.CheckVazio, RoundedCornerShape(9.dp)),
            )
        }
    }
}

/** Botões lado a lado em que só um fica selecionado. */
@Composable
fun Segmentado(opcoes: List<String>, selecionado: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Cores.Trilho)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        opcoes.forEachIndexed { i, texto ->
            val ativo = i == selecionado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (ativo) Modifier.shadow(1.dp, RoundedCornerShape(11.dp)) else Modifier)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (ativo) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    texto,
                    fontSize = 15.sp,
                    fontWeight = if (ativo) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (ativo) Cores.Tinta else Cores.Suave,
                )
            }
        }
    }
}

/** Seletor dos 7 dias (segunda a domingo). */
@Composable
fun SeletorDias(dias: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (d in 1..7) {
            val ativo = d in dias
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ativo) Cores.Verde else Color.White)
                    .then(if (ativo) Modifier else Modifier.border(1.dp, Cores.Borda, RoundedCornerShape(12.dp)))
                    .toggleable(value = ativo, role = Role.Checkbox) {
                        onChange(if (ativo) dias - d else dias + d)
                    }
                    .semantics { contentDescription = DAY_FULL[d - 1] },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    DAY_SHORT[d - 1],
                    fontSize = 14.sp,
                    fontWeight = if (ativo) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (ativo) Color.White else Cores.Suave,
                )
            }
        }
    }
}

/** Fileira só de leitura com os dias marcados. */
@Composable
fun DiasMarcados(dias: Set<Int>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (d in 1..7) {
            val ativo = d in dias
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (ativo) Cores.Verde else Cores.Vazio),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    DAY_SHORT[d - 1],
                    fontSize = 12.sp,
                    fontWeight = if (ativo) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (ativo) Color.White else Cores.Suave,
                )
            }
        }
    }
}

@Composable
fun BarraProgresso(fracao: Float, cor: Color, trilho: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trilho),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fracao.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(cor),
        )
    }
}

@Composable
fun Etiqueta(texto: String) {
    Text(
        texto,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Cores.TerracotaEscuro,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Cores.TerracotaClaro)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
