package uk.co.wsjty.remote.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

/**
 * Owns the single WebSocket connection to wsjty-relay. Reconnects
 * automatically with a fixed backoff while enabled. UI/ViewModel code
 * observes [connectionState], [latestStatus], and [decodes]; incoming
 * qso_logged/decodes_cleared events come through [events] since they're
 * one-shot rather than "current state".
 */
class RelayConnection(private val scope: CoroutineScope) {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var pairing: PairingConfig? = null
    private var enabled = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latestStatus = MutableStateFlow<StationStatus?>(null)
    val latestStatus: StateFlow<StationStatus?> = _latestStatus.asStateFlow()

    private val _decodes = MutableStateFlow<List<Decode>>(emptyList())
    val decodes: StateFlow<List<Decode>> = _decodes.asStateFlow()

    private val _events = MutableSharedFlow<RelayEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<RelayEvent> = _events.asSharedFlow()

    private companion object {
        const val RECONNECT_DELAY_MS = 4000L
        const val MAX_DECODES_KEPT = 500
    }

    fun connect(config: PairingConfig) {
        pairing = config
        enabled = true
        if (!config.isConfigured) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        openSocket()
    }

    fun disconnect() {
        enabled = false
        reconnectJob?.cancel()
        socket?.close(1000, "user disconnected")
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun clearDecodes() {
        _decodes.value = emptyList()
    }

    fun sendReply(call: String, grid: String, audioFreqHz: Int) =
        send(buildReplyMessage(call, grid, audioFreqHz))

    fun sendHaltTx() = send(buildHaltTxMessage())

    fun sendSetBandByName(bandName: String) = send(buildSetBandByNameMessage(bandName))

    fun sendSetBandByFreq(freqHz: Long) = send(buildSetBandByFreqMessage(freqHz))

    private fun send(text: String) {
        socket?.send(text)
    }

    private fun openSocket() {
        val cfg = pairing ?: return
        if (!enabled) return

        val url = buildString {
            append(cfg.relayUrl.trimEnd('/'))
            append("/ws?token=")
            append(cfg.token)
        }

        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncoming(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!enabled) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (enabled) openSocket()
        }
    }

    private fun handleIncoming(text: String) {
        when (val event = parseRelayEvent(text) ?: return) {
            is RelayEvent.DecodeEvent -> {
                _decodes.value = (_decodes.value + event.decode).takeLast(MAX_DECODES_KEPT)
                scope.launch { _events.emit(event) }
            }
            is RelayEvent.StatusEvent -> {
                _latestStatus.value = event.status
                scope.launch { _events.emit(event) }
            }
            RelayEvent.DecodesCleared -> {
                _decodes.value = emptyList()
                scope.launch { _events.emit(event) }
            }
            is RelayEvent.QsoLoggedEvent -> {
                scope.launch { _events.emit(event) }
            }
        }
    }
}
