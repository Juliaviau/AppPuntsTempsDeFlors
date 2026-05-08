package com.example.tempsdeflors

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State

object PuntRepository {

    private lateinit var database: AppDatabase
    private val puntsVisitats = mutableListOf<PuntsEntity>()

    private val _quantitatDePunts = mutableStateOf(0)
    val quantitatDePunts: State<Int> get() = _quantitatDePunts

    fun init(context: Context) {
        database = AppDatabase.getInstance(context) ?: throw IllegalStateException("No DB")
        carregarPuntsVisitats()
    }


    fun teFoto(numero: String): Boolean {
        return synchronized(puntsVisitats) {
            puntsVisitats.any { it.numero == numero && it.fotoUri?.isNotEmpty() ?: false }
        }
    }


    fun getPuntByNumero(numero: String): PuntsEntity? {
        return synchronized(puntsVisitats) {
            puntsVisitats.find { it.numero == numero }
        }
    }

    fun updateFotoUri(numero: String, uri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            database.puntsDao()?.updateFotoUri(numero, uri)
            Log.i("PuntRepository", "foto actualitzada per $numero a $uri")
        }
    }

    fun getFotoUriByNumero(numero: String): String? {
        return synchronized(puntsVisitats) {
            puntsVisitats.find { it.numero == numero }?.fotoUri
        }
    }

    fun getDataByNumero(numero: String): String? {
        return synchronized(puntsVisitats) {
            puntsVisitats.find { it.numero == numero }?.data
        }
    }

    private fun carregarPuntsVisitats() {
        CoroutineScope(Dispatchers.IO).launch {
            val punts = database.puntsDao()?.getAllPunts() ?: mutableListOf()
            synchronized(puntsVisitats) {
                puntsVisitats.clear()
                puntsVisitats.addAll(punts.filter { it.visitat == "si" })
                _quantitatDePunts.value = puntsVisitats.size
            }
        }
    }

    fun existeixPuntByNumero(numero: String): Boolean {
        return synchronized(puntsVisitats) {
            puntsVisitats.any { it.numero == numero }
        }
    }

    suspend fun marcaPuntComVisitat(ruta: String, numero: String, visitat: String) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val punt = PuntsEntity(
                ruta = ruta,
                data = timestamp.toString(),
                numero = numero,
                visitat = visitat
            )
            database.puntsDao()?.insertPunts(punt.copy(visitat = "si"))
            synchronized(puntsVisitats) {
                if (puntsVisitats.none { it.numero == punt.numero }) {
                    puntsVisitats.add(punt.copy(visitat = "si"))
                    carregarPuntsVisitats()
                }
            }
        }
    }

    fun eliminarPunt(punt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            database.puntsDao()?.deleteApuntsByNumero(punt)
            synchronized(puntsVisitats) {
                puntsVisitats.remove(getPuntByNumero(punt))
                carregarPuntsVisitats()
            }
        }
    }

    fun getPuntsVisitats(): List<PuntsEntity> {
        return synchronized(puntsVisitats) {
            puntsVisitats.toList()
        }
    }
    fun updateEstrelles(numero: String, estrelles: Int) {
        /*CoroutineScope(Dispatchers.IO).launch {
            database.puntsDao()?.updateEstrelles(numero, estrelles)
            Log.i("ACtuanota", numero + " " + estrelles);
        }//puntsVisitats.updateEstrelles(num, estrelles)*/




        CoroutineScope(Dispatchers.IO).launch {
            // 1. Actualitzem la Base de Dades (Room)
            database.puntsDao()?.updateEstrelles(numero, estrelles)

            // 2. Actualitzem la llista en memòria perquè el "get" sigui instantani
            synchronized(puntsVisitats) {
                val puntEnMemoria = puntsVisitats.find { it.numero == numero }
                puntEnMemoria?.let {
                    it.estrelles = estrelles // Ara la llista ja té la nota nova
                    Log.i("PuntRepository", "Memòria actualitzada: Punt $numero ara té $estrelles estrelles")
                }
            }
        }

    }

    fun getEstrellesByNumero(numero: String): Int? {
        return synchronized(puntsVisitats) {
            val nota = puntsVisitats.find { it.numero == numero }?.estrelles ?: 0
            Log.i("GETNOTA", "Llegint nota de memòria per al punt $numero: $nota")
            nota // Aquesta és l'última línia, és el que retorna el synchronized
        }
    }
}
