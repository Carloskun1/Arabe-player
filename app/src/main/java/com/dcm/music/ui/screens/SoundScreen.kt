package com.dcm.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.R
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun SoundScreen() {
    Column(Modifier.padding(24.dp)) {
        Text("Sonido", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Icon(painterResource(R.drawable.ic_equalizer), null, tint = DcmNeon, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Ecualizador y efectos de audio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Disponible en la Fase 3: ecualizador de 5 bandas, Bass Boost, virtualizador y presets, " +
                "usando las clases de audio reales de Android (sin controles ficticios).",
            color = DcmGray
        )
    }
}
