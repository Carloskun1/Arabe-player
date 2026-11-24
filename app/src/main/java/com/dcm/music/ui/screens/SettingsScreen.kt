package com.dcm.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun SettingsScreen(songCount: Int, loading: Boolean, onRescan: () -> Unit, onClearRecents: () -> Unit) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ajustes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Text("BIBLIOTECA", color = DcmNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !loading, onClick = onRescan)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    if (loading) "Escaneando…" else "Volver a escanear música",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text("$songCount canciones encontradas en el dispositivo", color = DcmGray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClearRecents)
                .padding(16.dp)
        ) {
            Column {
                Text("Borrar historial de reproducción", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Vacía la lista «Recientes»", color = DcmGray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("ACERCA DE", color = DcmNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text("DCM MUSIC", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Versión 1.1.0", color = DcmGray, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text("Desarrollado por DCM.", color = Color.White)
            Text("Tu música, tu universo.", color = DcmNeon)
        }
    }
}
