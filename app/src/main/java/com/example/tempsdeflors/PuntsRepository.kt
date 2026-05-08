package com.example.tempsdeflors

class PuntsRepository(private val apuntsDao: PuntsDao) {
    fun getAllApunts() = apuntsDao.getAllPunts()
    fun insertApunts(apunts:PuntsEntity) = apuntsDao.insertPunts(apunts)
    fun deleteApunts(apunts: PuntsEntity) = apuntsDao.delete(apunts)
    fun getApuntsById(id: Int) = apuntsDao.getPuntsById(id)
    fun deleteApuntsByNumero(numero: String) = apuntsDao.deleteApuntsByNumero(numero)
    fun updateEstrelles(numero: String, estrelles: Int) = apuntsDao.updateEstrelles(numero, estrelles)
    fun getEstrellesByNumero(numero: String) = apuntsDao.getEstrellesByNumero(numero)
}