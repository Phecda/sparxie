package me.phecda.sparxie.storage

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClientSettings(
    val addressInput: String = "",
    val portInput: String = "5201",
    val parallelStreamsInput: String = "1",
)

class ClientSettingsStore(private val kv: MMKV) {
    private val mutableState = MutableStateFlow(
        ClientSettings(
            addressInput = kv.decodeString(KEY_ADDRESS, "") ?: "",
            portInput = kv.decodeString(KEY_PORT, "5201") ?: "5201",
            parallelStreamsInput = kv.decodeString(KEY_PARALLEL_STREAMS, "1") ?: "1",
        ),
    )
    val state = mutableState.asStateFlow()

    @Synchronized
    fun setAddress(value: String) {
        check(kv.encode(KEY_ADDRESS, value)) { "Unable to save client address." }
        mutableState.value = mutableState.value.copy(addressInput = value)
    }

    @Synchronized
    fun setPort(value: String) {
        check(kv.encode(KEY_PORT, value)) { "Unable to save client port." }
        mutableState.value = mutableState.value.copy(portInput = value)
    }

    @Synchronized
    fun setParallelStreams(value: String) {
        check(kv.encode(KEY_PARALLEL_STREAMS, value)) { "Unable to save parallel streams." }
        mutableState.value = mutableState.value.copy(parallelStreamsInput = value)
    }

    private companion object {
        const val KEY_ADDRESS = "client.address"
        const val KEY_PORT = "client.port"
        const val KEY_PARALLEL_STREAMS = "client.parallel_streams"
    }
}

data class ServerSettings(val portInput: String = "5201")

class ServerSettingsStore(private val kv: MMKV) {
    private val mutableState = MutableStateFlow(
        ServerSettings(portInput = kv.decodeString(KEY_PORT, "5201") ?: "5201"),
    )
    val state = mutableState.asStateFlow()

    @Synchronized
    fun setPort(value: String) {
        check(kv.encode(KEY_PORT, value)) { "Unable to save server port." }
        mutableState.value = ServerSettings(portInput = value)
    }

    private companion object {
        const val KEY_PORT = "server.port"
    }
}
