package uk.co.wsjty.remote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.wsjty.remote.data.CallsignFlags
import uk.co.wsjty.remote.data.ConnectionState
import uk.co.wsjty.remote.data.Decode
import uk.co.wsjty.remote.data.StationStatus
import uk.co.wsjty.remote.ui.BandOption
import uk.co.wsjty.remote.ui.ftBands
import uk.co.wsjty.remote.ui.theme.Dseg7
import uk.co.wsjty.remote.ui.theme.WsjtyAccent
import uk.co.wsjty.remote.ui.theme.WsjtyBorder
import uk.co.wsjty.remote.ui.theme.WsjtyFreqBlue
import uk.co.wsjty.remote.ui.theme.WsjtyGreen
import uk.co.wsjty.remote.ui.theme.WsjtyRed
import uk.co.wsjty.remote.ui.theme.WsjtySurface
import uk.co.wsjty.remote.ui.theme.WsjtyYellow

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
    onToggleAutoCq: (Boolean) -> Unit,
    onToggleCqOnly: (Boolean) -> Unit,
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
            var showError by remember { mutableStateOf(false) }
            LaunchedEffect(connectionState) {
                if (connectionState == ConnectionState.DISCONNECTED) {
                    kotlinx.coroutines.delay(5000)
                    showError = true
                } else {
                    showError = false
                }
            }
            if (showError && connectionState == ConnectionState.DISCONNECTED && lastError != null) {
                Text(
                    text = lastError,
                    color = WsjtyRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            StatusRow(
                status = status,
                onToggleAutoCq = onToggleAutoCq,
                onToggleCqOnly = onToggleCqOnly,
            )
            StatusCard(status, onSetBand)
            DecodeList(decodes, onReply, onClearDecodes)
        }
    }
}

@Composable
private fun StatusRow(
    status: StationStatus?,
    onToggleAutoCq: (Boolean) -> Unit,
    onToggleCqOnly: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickToggleChip(
            label = "Auto CQ",
            checked = status?.autoCq ?: false,
            onToggle = onToggleAutoCq,
        )
        QuickToggleChip(
            label = "CQ only",
            checked = status?.cqOnly ?: false,
            onToggle = onToggleCqOnly,
        )
    }
}

// Same on/off convention as the desktop's Band Activity toggle buttons:
// checked = green, unchecked = the standard toolbar blue.
@Composable
private fun QuickToggleChip(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val bg = if (checked) WsjtyGreen else WsjtySurface
    val border = if (checked) WsjtyGreen else WsjtyBorder
    val fg = if (checked) MaterialTheme.colorScheme.onPrimary else WsjtyAccent.copy(alpha = 0.8f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
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
private fun StatusCard(status: StationStatus?, onSetBand: (BandOption) -> Unit) {
    var bandPickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WsjtySurface)
            .border(1.dp, WsjtyBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (status == null) {
                Text("Waiting for status from WSJT-Y…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Tapping the frequency/mode expands the band grid below —
                    // DSEG7 readout is preserved, just made clickable with a
                    // chevron that flips to show open/closed state.
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { bandPickerOpen = !bandPickerOpen }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            formatFreqMHz(status.dialFreqHz),
                            fontFamily = Dseg7,
                            fontSize = 26.sp,
                            color = if (bandPickerOpen) WsjtyAccent else WsjtyFreqBlue,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            " MHz " + status.mode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                        Icon(
                            if (bandPickerOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (bandPickerOpen) "Hide band picker" else "Change band",
                            tint = WsjtyAccent,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(bottom = 2.dp),
                        )
                    }
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
                if (status.txMsg.isNotBlank()) {
                    Text(
                        "TX: ${status.txMsg}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = if (status.transmitting) WsjtyRed else WsjtyYellow,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = bandPickerOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            BandGrid(onSelect = { band ->
                onSetBand(band)
                bandPickerOpen = false
            })
        }
    }
}

@Composable
private fun BandGrid(onSelect: (BandOption) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(bottom = 10.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ftBands) { band ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, WsjtyBorder, RoundedCornerShape(6.dp))
                    .clickable { onSelect(band) }
                    .padding(vertical = 10.dp),
            ) {
                Text(band.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
        Text(
            decode.time,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(52.dp),
        )
        Text(
            "${decode.snrDb}",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(30.dp),
        )
        CallsignFlags.flagForMessage(decode.message)?.let { flag ->
            Text(flag, fontSize = 12.sp)
        }
        Text(
            decode.message,
            fontSize = 12.sp,
            fontWeight = if (decode.isCq) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatFreqMHz(hz: Long): String {
    val mhz = hz / 1_000_000.0
    return String.format(java.util.Locale.US, "%.3f", mhz)
}
