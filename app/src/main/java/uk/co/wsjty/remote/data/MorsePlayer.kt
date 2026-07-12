package uk.co.wsjty.remote.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin
import kotlin.math.PI

/**
 * Plays a short string as CW (Morse code) — used as an audible alert when
 * the operator's own callsign is heard being called, since a distinctive
 * "CQ" in Morse is more meaningful at a glance/listen than a generic
 * notification beep for anyone actually running the mode this app is for.
 *
 * Self-contained: synthesises a sine-wave tone directly via AudioTrack
 * rather than needing a bundled sound asset, so timing/pitch/speed are
 * all just numbers here rather than requiring re-recording an asset to
 * change.
 */
object MorsePlayer {

    private const val TONE_HZ = 700.0        // standard CW sidetone pitch
    private const val SAMPLE_RATE = 44100
    private const val WPM = 20                // words per minute (PARIS standard)
    private val DIT_MS = (1200.0 / WPM).toInt()  // standard Morse timing formula

    private val morseTable = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---",
        '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....",
        '7' to "--...", '8' to "---..", '9' to "----.",
    )

    /** Plays [text] as Morse code. Fire-and-forget — safe to call from the
     *  main thread; the actual playback runs on its own thread. */
    fun play(text: String) {
        Thread {
            try {
                playBlocking(text)
            } catch (_: Exception) {
                // Best-effort alert -- a failure here should never crash or
                // otherwise disrupt the rest of the app.
            }
        }.start()
    }

    private fun playBlocking(text: String) {
        val ditSamples = msToSamples(DIT_MS)
        val dahSamples = msToSamples(DIT_MS * 3)
        val elementGapSamples = msToSamples(DIT_MS)
        val letterGapSamples = msToSamples(DIT_MS * 3)

        val pcm = ArrayList<Short>(SAMPLE_RATE * 2)
        text.uppercase().forEachIndexed { index, ch ->
            if (ch == ' ') {
                appendSilence(pcm, msToSamples(DIT_MS * 7))
                return@forEachIndexed
            }
            val pattern = morseTable[ch] ?: return@forEachIndexed
            pattern.forEachIndexed { i, symbol ->
                appendTone(pcm, if (symbol == '.') ditSamples else dahSamples)
                if (i != pattern.length - 1) appendSilence(pcm, elementGapSamples)
            }
            if (index != text.length - 1) appendSilence(pcm, letterGapSamples)
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(pcm.toShortArray(), 0, pcm.size)
        audioTrack.play()

        val totalMs = (pcm.size * 1000L) / SAMPLE_RATE
        Thread.sleep(totalMs + 100)
        audioTrack.stop()
        audioTrack.release()
    }

    private fun msToSamples(ms: Int): Int = (SAMPLE_RATE * ms) / 1000

    private fun appendTone(out: ArrayList<Short>, samples: Int) {
        // Short fade in/out to avoid audible clicking at element boundaries.
        val fade = minOf(samples / 8, 200)
        for (i in 0 until samples) {
            var amplitude = 1.0
            if (i < fade) amplitude = i.toDouble() / fade
            else if (i > samples - fade) amplitude = (samples - i).toDouble() / fade
            val sample = (amplitude * Short.MAX_VALUE * 0.6 *
                sin(2.0 * PI * TONE_HZ * i / SAMPLE_RATE)).toInt().toShort()
            out.add(sample)
        }
    }

    private fun appendSilence(out: ArrayList<Short>, samples: Int) {
        repeat(samples) { out.add(0) }
    }
}
