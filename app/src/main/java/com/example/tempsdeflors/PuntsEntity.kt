package com.example.tempsdeflors

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "punts")
data class PuntsEntity(
    @PrimaryKey val numero: String,
    val ruta: String, //ruta1,ruta2,ruta3,accessible
    val data: String,
    var visitat: String,
    var fotoUri: String? = null,
    var estrelles: Int = 0
)