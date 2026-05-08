package com.example.tempsdeflors

data class Punts(
    val lat: Double,
    val lon: Double,
    val titol: String,
    val descripcio: String,
    val snippet: String,
    val ruta: String, //ruta1,ruta2,ruta3,accessible
    val numero: String,
    val data: String,
    var visitat: String,
    val foto: String,
    val estrelles: Int
)
