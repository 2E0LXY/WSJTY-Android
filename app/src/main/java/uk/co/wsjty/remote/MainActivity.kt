package uk.co.wsjty.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.co.wsjty.remote.ui.MainViewModel
import uk.co.wsjty.remote.ui.screens.MainScreen
import uk.co.wsjty.remote.ui.screens.PairingScreen
import uk.co.wsjty.remote.ui.theme.WSJTYRemoteTheme

// Same navy-to-green diagonal gradient applied to the desktop app's main
// window background, sampled from the supplied artwork: #001D3F (top
// left) through #06413B to #0E5A40 (bottom right).
val WsjtyAppBackground = Brush.linearGradient(
    colors = listOf(Color(0xFF001D3F), Color(0xFF06413B), Color(0xFF0E5A40)),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WSJTYRemoteTheme {
                // Root cause of "some text is unreadable/dark": nothing in
                // this hierarchy (no Surface, no Scaffold at this level)
                // provides a content colour, so any Text() without an
                // explicit colour falls through to Compose's own global
                // default (black) rather than this app's theme. Every
                // existing Text() has since been given an explicit colour,
                // but this provider is a safety net for anything missed,
                // or added later, rather than relying on remembering to
                // set colour on every single Text() by hand.
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(WsjtyAppBackground),
                    ) {
                        WsjtyApp()
                    }
                }
            }
        }
    }
}

@Composable
private fun WsjtyApp(viewModel: MainViewModel = viewModel()) {
    val pairing by viewModel.pairing.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val status by viewModel.latestStatus.collectAsState()
    val decodes by viewModel.decodes.collectAsState()
    val qsoLog by viewModel.qsoLog.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    // Force the pairing screen open manually (e.g. to re-pair) even if
    // already configured, via the Settings action on MainScreen.
    var forceShowPairing by remember { mutableStateOf(false) }

    if (!pairing.isConfigured || forceShowPairing) {
        PairingScreen(
            initialRelayUrl = pairing.relayUrl,
            initialToken = pairing.token,
            onConnect = { url, token ->
                viewModel.savePairingAndConnect(url, token)
                forceShowPairing = false
            },
        )
    } else {
        MainScreen(
            connectionState = connectionState,
            lastError = lastError,
            status = status,
            decodes = decodes,
            qsoLog = qsoLog,
            onReply = viewModel::replyTo,
            onSetBand = viewModel::setBand,
            onHaltTx = viewModel::haltTx,
            onClearDecodes = viewModel::clearDecodes,
            onOpenSettings = { forceShowPairing = true },
            onToggleAutoCq = viewModel::toggleAutoCq,
            onToggleCqOnly = viewModel::toggleCqOnly,
        )
    }
}
