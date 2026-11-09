package br.com.minharotina.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import br.com.minharotina.RotinaApp

class MainActivity : ComponentActivity() {

    /** Muda toda vez que o app volta para a frente, para revalidar dia e permissões. */
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        val repo = RotinaApp.repo(this)
        setContent {
            RotinaTheme {
                RotinaRoot(repo = repo, resumeTick = resumeTick.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val repo = RotinaApp.repo(this)
        repo.refreshDay()
        repo.reschedule()
        resumeTick.intValue++
    }
}
