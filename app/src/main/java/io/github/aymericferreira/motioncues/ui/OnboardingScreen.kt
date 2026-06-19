package io.github.aymericferreira.motioncues.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aymericferreira.motioncues.R

/**
 * First-run flow: explains the feature and the system-UI limitation, then walks the two permission
 * gates (overlay + notifications) before letting the user continue.
 */
@Composable
fun OnboardingScreen(
    hasOverlay: Boolean,
    hasNotif: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotif: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboard_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboard_intro), style = MaterialTheme.typography.bodyLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.onboard_limitation_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.onboard_limitation_body), style = MaterialTheme.typography.bodyMedium)
            }
        }

        PermissionCard(
            title = stringResource(R.string.onboard_perm_overlay_title),
            body = stringResource(R.string.onboard_perm_overlay_body),
            granted = hasOverlay,
            grantedText = stringResource(R.string.onboard_perm_overlay_granted),
            buttonText = stringResource(R.string.onboard_perm_overlay_button),
            onRequest = onRequestOverlay,
        )

        PermissionCard(
            title = stringResource(R.string.onboard_perm_notif_title),
            body = stringResource(R.string.onboard_perm_notif_body),
            granted = hasNotif,
            grantedText = stringResource(R.string.onboard_perm_notif_granted),
            buttonText = stringResource(R.string.onboard_perm_notif_button),
            onRequest = onRequestNotif,
        )

        Button(
            onClick = onContinue,
            enabled = hasOverlay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboard_continue))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    grantedText: String,
    buttonText: String,
    onRequest: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (granted) {
                Text(grantedText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(onClick = onRequest) { Text(buttonText) }
            }
        }
    }
}
