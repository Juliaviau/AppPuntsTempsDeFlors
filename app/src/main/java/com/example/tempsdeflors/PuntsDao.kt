package com.example.tempsdeflors


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PuntsDao {
    @Query("SELECT * FROM punts ORDER BY numero DESC")
    fun getAllPunts(): MutableList<PuntsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPunts(punts: PuntsEntity)

    @Query("SELECT * FROM punts WHERE numero = :id")
    fun getPuntsById(id: Int): PuntsEntity?

    @Delete
    fun delete(somni: PuntsEntity)

    @Query("DELETE FROM punts WHERE numero = :numero")
    fun deleteApuntsByNumero(numero: String)

    @Query("SELECT EXISTS(SELECT 1 FROM punts WHERE numero = :numero)")
    suspend fun existeixPuntByNumero(numero: String): Boolean

    @Query("UPDATE punts SET fotoUri = :uri WHERE numero = :numero")
    fun updateFotoUri(numero: String, uri: String)

    @Query("SELECT fotoUri FROM punts WHERE numero = :numero")
    fun getFotoUriByNumero(numero: String): String?

    @Query("UPDATE punts SET estrelles = :notes WHERE numero = :numero")
    fun updateEstrelles(numero: String, notes: Int)

    @Query("SELECT estrelles FROM punts WHERE numero = :numero")
    fun getEstrellesByNumero(numero: String): Int?

}