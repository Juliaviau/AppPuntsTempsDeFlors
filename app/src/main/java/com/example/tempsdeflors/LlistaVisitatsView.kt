package com.example.tempsdeflors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.* // Pel remember i el mutableStateOf
import androidx.compose.foundation.layout.fillMaxSize // Pel fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.ui.Modifier // Aquest és obligatori per usar fillMaxWidth()
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlistaVisitatsView(navController: NavController, puntsTotals: List<Punts>) {
    val context = LocalContext.current
    // Estat per a l'ordre: "estrelles" o "data"
    var ordrePerEstrelles by remember { mutableStateOf(true) }

    // Obtenim només els punts que estan a la base de dades (visitats)
    val puntsVisitats = remember(puntsTotals) {
        puntsTotals.filter { PuntRepository.existeixPuntByNumero(it.numero) }
    }.sortedWith(compareByDescending {
        if (ordrePerEstrelles) PuntRepository.getEstrellesByNumero(it.numero)
        else PuntRepository.getDataByNumero(it.numero)?.toLong() ?: 0L
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Els meus visitats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Tornar")
                    }
                },
                actions = {
                    // Botó per canviar l'ordre
                    TextButton(onClick = { ordrePerEstrelles = !ordrePerEstrelles }) {
                        Text(if (ordrePerEstrelles) "Per Data" else "Per Estrelles", color = colorResource(R.color.colorpred))
                    }
                }
            )
        }
    ) { padding ->
        if (puntsVisitats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Encara no has visitat cap lloc", color = Color.Gray)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                items(puntsVisitats) { punt ->
                    CardVisitadaItem(punt)
                }
            }
        }
    }
}

@Composable
fun CardVisitadaItem(punt: Punts) {
    val estrelles = PuntRepository.getEstrellesByNumero(punt.numero)
    val data = PuntRepository.getDataByNumero(punt.numero)
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundModeFosc())
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(punt.titol, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = lletraModeFosc())
                Text("Ruta ${punt.ruta}", fontSize = 14.sp, color = Color.Gray)
                Text(simpleDateFormat.format(
                    PuntRepository.getDataByNumero(punt.numero)?.toLong() ?:
                    System.currentTimeMillis()
                ) ?: "", fontSize = 14.sp, color = Color.Gray)
                Text(punt.snippet, fontSize = 14.sp, color = Color.Gray)
                // Mostrem estrelles
                Row(Modifier.padding(top = 4.dp)) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < estrelles!!) Icons.Default.Star else Icons.Default.Clear,
                            contentDescription = null,
                            tint = if (index < estrelles) Color(0xFFFFD700) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // Imatge (si en té)
            val uriStr = PuntRepository.getFotoUriByNumero(punt.numero)
            if (!uriStr.isNullOrEmpty()) {
                AsyncImage(
                    model = uriStr,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}