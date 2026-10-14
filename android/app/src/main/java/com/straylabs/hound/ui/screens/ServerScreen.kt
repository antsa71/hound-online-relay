package com.straylabs.hound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.straylabs.hound.util.TransferHistory

/** Automatic server status screen: no manual port, folder, auth, toggle, or QR controls. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ServerScreen(
    serverRunning: Boolean,
    selectedPath: String?,
    serverPort: Int,
    serverIp: String?,
    pairingToken: String = "",
    authEnabled: Boolean = false,
    authUsername: String = "",
    authPassword: String = "",
    onStartServer: (Int) -> Unit,
    onStopServer: () -> Unit,
    onSelectFolder: () -> Unit,
    onPathChanged: (String?) -> Unit,
    onPortChanged: (Int) -> Unit,
    onAuthChanged: (enabled: Boolean, username: String, password: String) -> Unit = { _, _, _ -> },
    transferHistory: TransferHistory? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (serverRunning) "Server automatique actif" else "Server automatique",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Dossier partagé: /storage/emulated/0",
            style = MaterialTheme.typography.bodyMedium
        )
        if (pairingToken.isNotBlank()) {
            Text(
                text = "Token Client: $pairingToken",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Adresse: ${if (serverIp != null) "http://$serverIp:$serverPort" else "recherche..."}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (serverRunning) "Prêt — les vrais fichiers du dossier sont disponibles."
            else "Démarrage automatique en cours...",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
