package br.com.minharotina.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Um tema de cores do app. Texto branco precisa ter bom contraste sobre [principal] e [secundaria]. */
data class Paleta(
    val id: String,
    val nome: String,
    val principal: Color,
    val principalClaro: Color,
    val principalEscuro: Color,
    val secundaria: Color,
    val secundariaMedia: Color,
    val baixa: Color,
    val fundo: Color,
)

val PALETAS = listOf(
    Paleta("terracota", "Terracota", Color(0xFFB8431F), Color(0xFFFBE7DF), Color(0xFF8F3215), Color(0xFF1F6F66), Color(0xFF8CC0B7), Color(0xFFF2C3B2), Color(0xFFF6F4EF)),
    Paleta("rosa", "Rosa", Color(0xFFC2185B), Color(0xFFFCE4EC), Color(0xFF880E4F), Color(0xFF7B1FA2), Color(0xFFCE93D8), Color(0xFFF8BBD0), Color(0xFFFFF5F8)),
    Paleta("lilas", "Lilás", Color(0xFF6D4BB5), Color(0xFFEDE7F6), Color(0xFF4527A0), Color(0xFF00796B), Color(0xFF80CBC4), Color(0xFFD1C4E9), Color(0xFFF7F5FB)),
    Paleta("vinho", "Vinho", Color(0xFF880E4F), Color(0xFFF8E1EA), Color(0xFF560027), Color(0xFF455A64), Color(0xFFB0BEC5), Color(0xFFF3C1D5), Color(0xFFFAF5F7)),
    Paleta("cereja", "Cereja", Color(0xFFB71C1C), Color(0xFFFFEBEE), Color(0xFF7F0000), Color(0xFF00695C), Color(0xFF80CBC4), Color(0xFFFFCDD2), Color(0xFFFBF5F5)),
    Paleta("por-do-sol", "Pôr do sol", Color(0xFFC24E00), Color(0xFFFFF0E0), Color(0xFF8A3300), Color(0xFF00796B), Color(0xFF80CBC4), Color(0xFFFFD6A8), Color(0xFFFFF8F1)),
    Paleta("dourado", "Dourado", Color(0xFF8F5E10), Color(0xFFFBF0D6), Color(0xFF654108), Color(0xFF2E7D32), Color(0xFFA5D6A7), Color(0xFFFFE0A3), Color(0xFFFBF8F0)),
    Paleta("floresta", "Floresta", Color(0xFF2E7D32), Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF1565C0), Color(0xFF90CAF9), Color(0xFFC8E6C9), Color(0xFFF4F7F2)),
    Paleta("oceano", "Oceano", Color(0xFF1E5AA8), Color(0xFFE3EDF9), Color(0xFF0D3B75), Color(0xFF00796B), Color(0xFF80CBC4), Color(0xFFBBD3F0), Color(0xFFF3F6FA)),
    Paleta("turquesa", "Turquesa", Color(0xFF00727C), Color(0xFFE0F5F7), Color(0xFF004D55), Color(0xFFC2185B), Color(0xFFF48FB1), Color(0xFFB2EBF2), Color(0xFFF2FAFB)),
    Paleta("grafite", "Grafite", Color(0xFF37474F), Color(0xFFECEFF1), Color(0xFF263238), Color(0xFFC2185B), Color(0xFFF48FB1), Color(0xFFCFD8DC), Color(0xFFF5F6F7)),
    Paleta("noite", "Noite estrelada", Color(0xFF283593), Color(0xFFE8EAF6), Color(0xFF1A237E), Color(0xFF8E24AA), Color(0xFFCE93D8), Color(0xFFC5CAE9), Color(0xFFF5F6FB)),
)

/**
 * Cores usadas pelas telas. As de destaque vêm da paleta escolhida (estado observável,
 * então trocar a paleta redesenha o app na hora); as neutras são fixas.
 */
object Cores {
    var paleta by mutableStateOf(PALETAS.first())

    val Terracota: Color get() = paleta.principal
    val TerracotaClaro: Color get() = paleta.principalClaro
    val TerracotaEscuro: Color get() = paleta.principalEscuro
    val Verde: Color get() = paleta.secundaria
    val VerdeMedio: Color get() = paleta.secundariaMedia
    val Salmao: Color get() = paleta.baixa
    val Fundo: Color get() = paleta.fundo

    val Tinta = Color(0xFF1F1D1A)
    val Suave = Color(0xFF6B655C)
    val Borda = Color(0xFFDDD7CC)
    val Trilho = Color(0xFFE9E4DA)
    val Vazio = Color(0xFFEFEBE3)
    val CheckVazio = Color(0xFFA39C91)

    private const val PREFS = "aparencia"
    private const val CHAVE = "paleta"

    fun carregar(context: Context) {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHAVE, null)
        paleta = PALETAS.firstOrNull { it.id == id } ?: PALETAS.first()
    }

    fun escolher(context: Context, nova: Paleta) {
        paleta = nova
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CHAVE, nova.id).apply()
    }
}

@Composable
fun RotinaTheme(content: @Composable () -> Unit) {
    val esquema = lightColorScheme(
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
    MaterialTheme(colorScheme = esquema, content = content)
}
