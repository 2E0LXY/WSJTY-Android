package uk.co.wsjty.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.wsjty.remote.data.ConnectionState
import uk.co.wsjty.remote.data.Decode
import uk.co.wsjty.remote.data.StationStatus
import uk.co.wsjty.remote.ui.BandOption
import uk.co.wsjty.remote.ui.ftBands
import uk.co.wsjty.remote.ui.theme.WsjtyAccent
import uk.co.wsjty.remote.ui.theme.WsjtyBorder
import uk.co.wsjty.remote.ui.theme.WsjtyGreen
import uk.co.wsjty.remote.ui.theme.WsjtyRed
import uk.co.wsjty.remote.ui.theme.WsjtySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    connectionState: ConnectionState,
    lastError: String?,
    status: StationStatus?,
    decodes: List<Decode>,
    onReply: (Decode) -> Unit,
    onSetBand: (BandOption) -> Unit,
    onHaltTx: () -> Unit,
    onClearDecodes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionDot(connectionState)
                        Spacer(Modifier.width(8.dp))
                        Text("WSJT-Y Remote")
                    }
                },
                actions = {
                    IconButton(onClick = onHaltTx) {
                        Icon(Icons.Filled.Stop, contentDescription = "Halt Tx", tint = WsjtyRed)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (connectionState == ConnectionState.DISCONNECTED && lastError != null) {
                Text(
                    text = lastError,
                    color = WsjtyRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            StatusCard(status)
            BandRow(onSetBand)
            DecodeList(decodes, onReply, onClearDecodes)
        }
    }
}

@Composable
private fun ConnectionDot(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.CONNECTED -> WsjtyGreen
        ConnectionState.CONNECTING -> WsjtyAccent
        ConnectionState.DISCONNECTED -> WsjtyRed
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun StatusCard(status: StationStatus?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WsjtySurface)
            .border(1.dp, WsjtyBorder)
            .padding(12.dp),
    ) {
        if (status == null) {
            Text("Waiting for status from WSJT-Y…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    formatFreqMHz(status.dialFreqHz) + " MHz " + status.mode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (status.transmitting) {
                    Text("TX", color = WsjtyRed, fontWeight = FontWeight.Bold)
                } else if (status.txEnabled) {
                    Text("Enabled", color = WsjtyGreen)
                }
            }
            if (status.dxCall.isNotBlank()) {
                Text(
                    "Working: ${status.dxCall} ${status.dxGrid}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun BandRow(onSetBand: (BandOption) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ftBands) { band ->
            OutlinedButton(onClick = { onSetBand(band) }) {
                Text(band.label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DecodeList(
    decodes: List<Decode>,
    onReply: (Decode) -> Unit,
    onClearDecodes: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${decodes.size} decodes  ·  tap a CQ to reply",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "Clear",
                color = WsjtyAccent,
                modifier = Modifier.clickable { onClearDecodes() },
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(decodes.asReversed()) { decode ->
                DecodeRow(decode, onClick = { if (decode.isCq) onReply(decode) })
            }
        }
    }
}

@Composable
private fun DecodeRow(decode: Decode, onClick: () -> Unit) {
    val bg = when {
        decode.forMe -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
        decode.isCq -> WsjtyAccent.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
            .clickable(enabled = decode.isCq, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(decode.time, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        Text("${decode.snrDb}", fontSize = 11.sp, modifier = Modifier.width(28.dp))
        Text(decode.message, fontSize = 12.sp, fontWeight = if (decode.isCq) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun formatFreqMHz(hz: Long): String {
    val mhz = hz / 1_000_000.0
    return String.format(java.util.Locale.US, "%.3f", mhz)
}
