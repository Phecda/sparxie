package me.phecda.sparxie

import android.app.Application
import com.tencent.mmkv.MMKV
import me.phecda.sparxie.storage.ClientSettingsStore
import me.phecda.sparxie.storage.ServerSettingsStore

class SparxieApplication : Application() {
    lateinit var clientSettingsStore: ClientSettingsStore
        private set
    lateinit var serverSettingsStore: ServerSettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)

        val settings = MMKV.mmkvWithID("sparxie.settings")
        clientSettingsStore = ClientSettingsStore(settings)
        serverSettingsStore = ServerSettingsStore(settings)
    }
}
