package uk.co.wsjty.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.co.wsjty.remote.ui.MainViewModel
import uk.co.wsjty.remote.ui.screens.MainScreen
import uk.co.wsjty.remote.ui.screens.PairingScreen
import uk.co.wsjty.remote.ui.theme.WSJTYRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WSJTYRemoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WsjtyApp()
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
            status = status,
            decodes = decodes,
            onReply = viewModel::replyTo,
            onSetBand = viewModel::setBand,
            onHaltTx = viewModel::haltTx,
            onClearDecodes = viewModel::clearDecodes,
            onOpenSettings = { forceShowPairing = true },
        )
    }
}
