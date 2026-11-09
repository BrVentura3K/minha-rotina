package br.com.minharotina.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Cores {
    val Terracota = Color(0xFFB8431F)
    val TerracotaClaro = Color(0xFFFBE7DF)
    val TerracotaEscuro = Color(0xFF8F3215)
    val Verde = Color(0xFF1F6F66)
    val VerdeMedio = Color(0xFF8CC0B7)
    val Salmao = Color(0xFFF2C3B2)
    val Fundo = Color(0xFFF6F4EF)
    val Tinta = Color(0xFF1F1D1A)
    val Suave = Color(0xFF6B655C)
    val Borda = Color(0xFFDDD7CC)
    val Trilho = Color(0xFFE9E4DA)
    val Vazio = Color(0xFFEFEBE3)
    val CheckVazio = Color(0xFFA39C91)
}

private val esquema = lightColorScheme(
    primary = Cores.Terracota,
    onPrimary = Color.White,
    primaryContainer = Cores.TerracotaClaro,
    onPrimaryContainer = Cores.TerracotaEscuro,
    secondary = Cores.Verde,
    onSecondary = Color.White,
    secondaryContainer = Cores.VerdeMedio,
    onSecondaryContainer = Cores.Tinta,
    background = Cores.Fundo,
    onBackground = Cores.Tinta,
    surface = Color.White,
    onSurface = Cores.Tinta,
    surfaceVariant = Cores.Vazio,
    onSurfaceVariant = Cores.Suave,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    outline = Cores.Borda,
    outlineVariant = Cores.Trilho,
)

@Composable
fun RotinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, content = content)
}
