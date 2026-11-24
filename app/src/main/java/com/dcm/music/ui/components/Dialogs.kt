package com.dcm.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dcm.music.data.PlaylistUi
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon
import com.dcm.music.ui.theme.DcmSurface

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DcmSurface,
        title = { Text(title, color = Color.White) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Nombre", color = DcmGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DcmNeon,
                    unfocusedBorderColor = DcmGray.copy(alpha = 0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = DcmNeon
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) {
                Text(confirmText, color = DcmNeon)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DcmGray) }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DcmSurface,
        title = { Text(title, color = Color.White) },
        text = { Text(message, color = DcmGray) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = DcmNeon) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DcmGray) }
        }
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistUi>,
    onPick: (PlaylistUi) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DcmSurface,
        title = { Text("Añadir a una playlist", color = Color.White) },
        text = {
            if (playlists.isEmpty()) {
                Text("Aún no tienes playlists. Crea la primera.", color = DcmGray)
            } else {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    playlists.forEach { pl ->
                        Text(
                            "${pl.name}  (${pl.songs.size})",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(pl) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNew) { Text("Nueva playlist", color = DcmNeon) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DcmGray) }
        }
    )
}
