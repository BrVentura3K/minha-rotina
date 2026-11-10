package br.com.minharotina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorsScreen() {
    val context = LocalContext.current
    val atual = Cores.paleta
    val linhas = PALETAS.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Cores", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta)
                Text("Escolha o visual do app. Muda na hora.", fontSize = 14.sp, color = Cores.Suave)
            }
        }

        item { Previa() }

        items(linhas) { linha ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEach { p ->
                    CartaoPaleta(
                        paleta = p,
                        selecionada = p.id == atual.id,
                        onClick = { Cores.escolher(context, p) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Mini amostra de como fica a tela Hoje com a paleta atual. */
@Composable
private fun Previa() {
    Surface(shape = RoundedCornerShape(20.dp), color = Cores.Fundo, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .border(1.dp, Cores.Borda, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = Cores.Terracota, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FRASE DO DIA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                    Text("Bom dia, detetive! A delegacia conta com você hoje.", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    BarraProgresso(0.6f, cor = Color.White, trilho = Color.White.copy(alpha = 0.3f))
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(Cores.Verde),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("  Tarefa concluída", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cores.Suave)
                    Spacer(Modifier.weight(1f))
                    Etiqueta("Só hoje")
                }
            }
        }
    }
}

@Composable
private fun CartaoPaleta(paleta: Paleta, selecionada: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .background(Color.White)
            .border(if (selecionada) 3.dp else 1.dp, if (selecionada) paleta.principal else Cores.Borda, forma)
            .selectable(selected = selecionada, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(paleta.principal),
            contentAlignment = Alignment.Center,
        ) {
            if (selecionada) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(paleta.secundaria, paleta.secundariaMedia, paleta.principalClaro).forEach { c ->
                Box(Modifier.size(18.dp).clip(CircleShape).background(c).border(1.dp, Cores.Borda, CircleShape))
            }
        }
        Text(
            paleta.nome, fontSize = 14.sp,
            fontWeight = if (selecionada) FontWeight.ExtraBold else FontWeight.Bold,
            color = Cores.Tinta,
        )
    }
}
