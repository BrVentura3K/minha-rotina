package br.com.minharotina.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import br.com.minharotina.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Atualização direto pelo celular: lê a última Release do GitHub e instala o
 * APK pelo PackageInstaller. A tag da Release é "v<versionCode>".
 */
object Updater {

    private const val REPO = "BrVentura3K/minha-rotina"

    data class Release(val versionCode: Int, val name: String, val notes: String, val apkUrl: String)

    val versaoAtual: String get() = BuildConfig.VERSION_NAME

    /** Última versão publicada, ou null se não deu para consultar. */
    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (conn.responseCode != 200) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val code = json.getString("tag_name").removePrefix("v").toIntOrNull() ?: return@withContext null
            val assets = json.getJSONArray("assets")
            var apk: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) apk = a.getString("browser_download_url")
            }
            Release(
                versionCode = code,
                name = json.optString("name", "Versão nova"),
                notes = json.optString("body", "").lines()
                    .filterNot { it.startsWith("Co-Authored-By") || it.startsWith("Claude-Session") }
                    .joinToString("\n").trim(),
                apkUrl = apk ?: return@withContext null,
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isNewer(release: Release): Boolean = release.versionCode > BuildConfig.VERSION_CODE

    /** O Android exige que o app tenha permissão para instalar outros apps (inclusive ele mesmo). */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Baixa o APK direto para uma sessão do instalador e pede a confirmação ao usuário. */
    suspend fun install(context: Context, release: Release, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
        }
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                session.openWrite("app.apk", 0, if (total > 0) total else -1).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var lido = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        lido += n
                        if (total > 0) onProgress(lido.toFloat() / total)
                    }
                    session.fsync(out)
                }
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pi = PendingIntent.getBroadcast(context, 1, Intent(context, InstallReceiver::class.java), flags)
            session.commit(pi.intentSender)
            session.close()
        } catch (e: Exception) {
            session.abandon()
            throw e
        }
    }
}
