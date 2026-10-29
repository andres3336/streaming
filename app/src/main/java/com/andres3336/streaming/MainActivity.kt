package com.andres3336.streaming

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B1220)),
                ) {
                    HelloScreen()
                }
            }
        }
    }
}

@Composable
private fun HelloScreen() {
    var presses by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Streaming",
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hola TV · build ${BuildConfig.VERSION_NAME}",
            color = Color(0xFF94A3B8),
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = { presses++ }) {
            Text(
                text = if (presses == 0) "Probar el mando" else "Funciona: $presses",
                fontSize = 22.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Usa el D-pad y pulsa OK",
            color = Color(0xFF64748B),
            fontSize = 16.sp,
        )
    }
}
