package uk.co.wsjty.remote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wsjty_settings")

private val KEY_RELAY_URL = stringPreferencesKey("relay_url")
private val KEY_TOKEN = stringPreferencesKey("token")

data class PairingConfig(val relayUrl: String, val token: String) {
    val isConfigured: Boolean get() = relayUrl.isNotBlank() && token.isNotBlank()
}

class SettingsStore(private val context: Context) {

    val pairingFlow: Flow<PairingConfig> = context.dataStore.data.map { prefs ->
        PairingConfig(
            relayUrl = prefs[KEY_RELAY_URL].orEmpty(),
            token = prefs[KEY_TOKEN].orEmpty(),
        )
    }

    suspend fun currentPairing(): PairingConfig = pairingFlow.first()

    suspend fun savePairing(relayUrl: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RELAY_URL] = relayUrl.trim()
            prefs[KEY_TOKEN] = token.trim()
        }
    }

    suspend fun clearPairing() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_RELAY_URL)
            prefs.remove(KEY_TOKEN)
        }
    }
}
