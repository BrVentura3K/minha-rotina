package br.com.minharotina.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.minharotina.data.AppData
import br.com.minharotina.data.Repository
import br.com.minharotina.quotes.Quotes
import br.com.minharotina.update.Updater
import java.time.LocalDate

@Composable
fun QuotesScreen(data: AppData, hoje: LocalDate, repo: Repository, onVerificarAtualizacao: () -> Unit) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var nova by rememberSaveable { mutableStateOf("") }
    val fraseHoje = Quotes.ofDay(hoje, data.customQuotes)

    // distinct(): a lista usa o texto como chave, e chaves repetidas derrubam o app.
    val lista = when (aba) {
        0 -> data.customQuotes + Quotes.all
        1 -> (data.customQuotes + Quotes.all).filter { it in data.favoriteQuotes }
        else -> data.customQuotes
    }.distinct()

    fun adicionar() {
        repo.addCustomQuote(nova)
        nova = ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Frases", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Cores.Tinta)
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Cores.Verde, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "FRASE DE HOJE", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp, color = Color.White.copy(alpha = 0.9f),
                    )
                    Text(fraseHoje, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Muda à meia-noite", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
                        val fav = fraseHoje in data.favoriteQuotes
                        IconButton(onClick = { repo.toggleFavorite(fraseHoje) }) {
                            Icon(
                                if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (fav) "Remover dos favoritos" else "Favoritar frase de hoje",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }

        item {
            Segmentado(
                opcoes = listOf("Todas", "Favoritas", "Minhas"),
                selecionado = aba,
                onSelect = { aba = it },
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (aba == 2) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nova,
                        onValueChange = { nova = it },
                        label = { Text("Escreva uma frase sua") },
                        placeholder = { Text("Ex.: hoje eu dou conta") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { adicionar() }),
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconButton(
                        onClick = { adicionar() },
                        enabled = nova.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Cores.Terracota),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar frase", tint = Color.White)
                    }
                }
            }
        }

        if (lista.isEmpty()) {
            item {
                Text(
                    when (aba) {
                        1 -> "Toque no coração de uma frase para guardar aqui."
                        else -> "Suas frases aparecem aqui e entram no sorteio da frase do dia."
                    },
                    fontSize = 14.sp, color = Cores.Suave, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        items(lista, key = { it }) { frase ->
            val fav = frase in data.favoriteQuotes
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(frase, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, color = Cores.Tinta, modifier = Modifier.weight(1f))
                    if (aba == 2) {
                        IconButton(onClick = { repo.removeCustomQuote(frase) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Apagar frase", tint = Cores.Suave)
                        }
                    }
                    IconButton(onClick = { repo.toggleFavorite(frase) }) {
                        Icon(
                            if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (fav) "Remover dos favoritos" else "Favoritar",
                            tint = if (fav) Cores.Terracota else Cores.Suave,
                        )
                    }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Versão ${Updater.versaoAtual}", fontSize = 12.sp, color = Cores.Suave)
                androidx.compose.material3.TextButton(onClick = onVerificarAtualizacao) {
                    Text("Verificar atualização", color = Cores.Verde, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
