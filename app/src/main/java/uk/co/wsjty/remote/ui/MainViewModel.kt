package uk.co.wsjty.remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.wsjty.remote.data.Decode
import uk.co.wsjty.remote.data.MorsePlayer
import uk.co.wsjty.remote.data.PairingConfig
import uk.co.wsjty.remote.data.RelayConnection
import uk.co.wsjty.remote.data.SettingsStore

const val FT8_CALLING_FREQ_HZ_20M = 14_074_000L

data class BandOption(val label: String, val freqHz: Long)

val ftBands = listOf(
    BandOption("160m", 1_840_000),
    BandOption("80m", 3_573_000),
    BandOption("40m", 7_074_000),
    BandOption("30m", 10_136_000),
    BandOption("20m", 14_074_000),
    BandOption("17m", 18_100_000),
    BandOption("15m", 21_074_000),
    BandOption("12m", 24_915_000),
    BandOption("10m", 28_074_000),
    BandOption("6m", 50_313_000),
)

// Matches WSJT-Y's Mode menu (on_actionXXX_triggered handlers) exactly —
// these strings go straight into the set_mode wire message and the
// desktop dispatches on them verbatim.
val wsjtyModes = listOf("FT8", "FT4", "MSK144", "Q65", "JT65", "JT9", "JT4", "WSPR", "FST4", "FST4W")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsStore(application)
    private val relay = RelayConnection(viewModelScope)

    val connectionState = relay.connectionState
    val latestStatus = relay.latestStatus
    val decodes: StateFlow<List<Decode>> = relay.decodes
    val qsoLog: StateFlow<List<uk.co.wsjty.remote.data.QsoLogged>> = relay.qsoLog
    val lastError = relay.lastError

    val pairing: StateFlow<PairingConfig> = settings.pairingFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PairingConfig("", ""))

    init {
        viewModelScope.launch {
            val current = settings.currentPairing()
            if (current.isConfigured) relay.connect(current)
        }

        // Fire a Morse "CQ" alert the first time a new for-me decode
        // appears. Track by count rather than diffing full decode objects
        // -- decodes are only ever appended (see RelayConnection), so
        // anything beyond the previously-seen size is new. The very first
        // collected value is whatever was already cached when this
        // ViewModel started (StateFlow always replays current state to a
        // new collector) -- skip alerting on that batch, only on genuine
        // new arrivals afterwards.
        viewModelScope.launch {
            var lastSeenCount = -1
            decodes.collect { list ->
                if (lastSeenCount >= 0 && list.size > lastSeenCount) {
                    list.subList(lastSeenCount, list.size)
                        .filter { it.forMe }
                        .forEach { MorsePlayer.play("CQ") }
                }
                lastSeenCount = list.size
            }
        }
    }

    fun savePairingAndConnect(relayUrl: String, token: String) {
        viewModelScope.launch {
            settings.savePairing(relayUrl, token)
            relay.connect(PairingConfig(relayUrl.trim(), token.trim()))
        }
    }

    fun forgetPairing() {
        viewModelScope.launch {
            relay.disconnect()
            settings.clearPairing()
        }
    }

    fun replyTo(decode: Decode) {
        val call = extractCallsign(decode.message) ?: return
        val grid = extractGrid(decode.message)
        relay.sendReply(call, grid, decode.audioFreqHz)
    }

    fun haltTx() = relay.sendHaltTx()

    fun setBand(band: BandOption) = relay.sendSetBandByFreq(band.freqHz)

    fun setMode(mode: String) = relay.sendSetMode(mode)

    fun toggleAutoCq(on: Boolean) = relay.sendSetAutoCq(on)

    fun toggleCqOnly(on: Boolean) = relay.sendSetCqOnly(on)

    fun clearDecodes() = relay.clearDecodes()
}

/** "CQ F5BRF IN95" / "CQ DX F5BRF" / "F5BRF 2E0LXY IO93" -> best-guess callsign. */
private fun extractCallsign(message: String): String? {
    val words = message.trim().split(Regex("\\s+"))
    if (words.isEmpty()) return null
    return when {
        words.size >= 2 && words[0] == "CQ" && words[1] !in setOf("DX") -> words[1]
        words.size >= 3 && words[0] == "CQ" -> words[2]
        else -> words.getOrNull(0)
    }
}

/** Last token in the message if it looks like a 4-6 char Maidenhead grid. */
private fun extractGrid(message: String): String {
    val gridRe = Regex("^[A-Ra-r]{2}[0-9]{2}([A-Xa-x]{2})?$")
    val words = message.trim().split(Regex("\\s+"))
    return words.lastOrNull { gridRe.matches(it) }.orEmpty()
}
