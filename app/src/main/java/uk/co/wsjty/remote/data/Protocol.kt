package uk.co.wsjty.remote.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Wire protocol shared with wsjty-relay / Network::RemoteBridge in the
 * wsjt-zii desktop app. See wsjty-relay's README for the authoritative
 * message shapes — this file mirrors them by hand rather than depending
 * on a shared schema, since the two live in different repos/languages.
 *
 * Unknown "type" values are ignored, same as the desktop side, so the
 * protocol can grow fields without breaking older clients.
 */

val relayJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/** One decoded FT8/etc message, as broadcast by the desktop. */
data class Decode(
    val time: String,
    val snrDb: Int,
    val dt: Double,
    val audioFreqHz: Int,
    val mode: String,
    val message: String,
    val isCq: Boolean,
    val forMe: Boolean,
)

/** Current station status, as broadcast by the desktop. */
data class StationStatus(
    val dialFreqHz: Long,
    val mode: String,
    val dxCall: String,
    val dxGrid: String,
    val txEnabled: Boolean,
    val transmitting: Boolean,
)

/** A QSO the desktop just logged. */
data class QsoLogged(
    val call: String,
    val grid: String,
    val dialFreqHz: Long,
    val mode: String,
    val reportSent: String,
    val reportRcvd: String,
)

/** Parses one incoming relay text message into a typed event, or null if
 *  the type is unrecognised (forward-compatibility — just ignore it). */
sealed class RelayEvent {
    data class DecodeEvent(val decode: Decode) : RelayEvent()
    data class StatusEvent(val status: StationStatus) : RelayEvent()
    data class QsoLoggedEvent(val qso: QsoLogged) : RelayEvent()
    object DecodesCleared : RelayEvent()
}

fun parseRelayEvent(text: String): RelayEvent? {
    val obj = runCatching { relayJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
    return when (obj["type"]?.jsonPrimitive?.contentOrNull) {
        "decode" -> RelayEvent.DecodeEvent(
            Decode(
                time = obj["time"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                snrDb = obj["snr"]?.jsonPrimitive?.intOrNull ?: 0,
                dt = obj["dt"]?.jsonPrimitive?.floatOrNull?.toDouble() ?: 0.0,
                audioFreqHz = obj["audio_freq_hz"]?.jsonPrimitive?.intOrNull ?: 0,
                mode = obj["mode"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                message = obj["message"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                isCq = obj["cq"]?.jsonPrimitive?.booleanOrNull ?: false,
                forMe = obj["for_me"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        )
        "status" -> RelayEvent.StatusEvent(
            StationStatus(
                dialFreqHz = obj["dial_freq_hz"]?.jsonPrimitive?.longOrNull ?: 0L,
                mode = obj["mode"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                dxCall = obj["dx_call"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                dxGrid = obj["dx_grid"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                txEnabled = obj["tx_enabled"]?.jsonPrimitive?.booleanOrNull ?: false,
                transmitting = obj["transmitting"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        )
        "qso_logged" -> RelayEvent.QsoLoggedEvent(
            QsoLogged(
                call = obj["call"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                grid = obj["grid"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                dialFreqHz = obj["dial_freq_hz"]?.jsonPrimitive?.longOrNull ?: 0L,
                mode = obj["mode"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                reportSent = obj["report_sent"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                reportRcvd = obj["report_rcvd"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        )
        "decodes_cleared" -> RelayEvent.DecodesCleared
        else -> null
    }
}

/** Outgoing commands (app -> relay -> desktop). */
fun buildReplyMessage(call: String, grid: String, audioFreqHz: Int): String =
    """{"type":"reply","call":"$call","grid":"$grid","audio_freq_hz":$audioFreqHz}"""

fun buildHaltTxMessage(): String = """{"type":"halt_tx"}"""

fun buildSetBandByNameMessage(bandName: String): String =
    """{"type":"set_band","band":"$bandName"}"""

fun buildSetBandByFreqMessage(freqHz: Long): String =
    """{"type":"set_band","freq_hz":$freqHz}"""
