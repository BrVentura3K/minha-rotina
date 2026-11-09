package br.com.minharotina

import android.app.Application
import android.content.Context
import br.com.minharotina.alarm.Notifications
import br.com.minharotina.data.Repository

class RotinaApp : Application() {

    val repository: Repository by lazy { Repository(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
    }

    companion object {
        fun repo(context: Context): Repository =
            (context.applicationContext as RotinaApp).repository
    }
}
