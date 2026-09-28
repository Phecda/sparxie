package me.phecda.iperf3

object Iperf3Native {
    init {
        System.loadLibrary("iperf3")
    }

    external fun reset()

    external fun prepareClient(
        serverAddress: String,
        serverPort: Int,
        parallelStreams: Int,
    ): Long

    external fun prepareServer(serverPort: Int): Long

    external fun run(handle: Long, listener: IperfJsonListener): Int

    external fun interrupt(): Boolean

    external fun lastErrorCode(): Int

    external fun lastErrorMessage(code: Int): String

    const val RESULT_FINISHED = 0
    const val RESULT_STOPPED = -1
    const val RESULT_FAILED = -2
}

fun interface IperfJsonListener {
    fun onJson(json: String)
}