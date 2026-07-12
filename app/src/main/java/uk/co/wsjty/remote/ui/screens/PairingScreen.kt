package uk.co.wsjty.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PairingScreen(
    initialRelayUrl: String,
    initialToken: String,
    onConnect: (relayUrl: String, token: String) -> Unit,
) {
    var relayUrl by remember { mutableStateOf(initialRelayUrl) }
    var token by remember { mutableStateOf(initialToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Pair with WSJT-Y",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Text(
            "Relay: get the wss:// URL + password from WSJT-Y's Tools -> Configure " +
                "Remote Control, or wsjty-relay's -add-station output.\n" +
                "Direct (same LAN or port-forwarded): use ws://<PC's IP>:<port> " +
                "and the same password — WSJT-Y shows both when you enable it.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = relayUrl,
            onValueChange = { relayUrl = it },
            label = { Text("Address (wss://relay/... or ws://192.168.x.x:port)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        )

        Button(
            onClick = { onConnect(relayUrl, token) },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            enabled = relayUrl.isNotBlank() && token.isNotBlank(),
        ) {
            Text("Connect")
        }
    }
}
