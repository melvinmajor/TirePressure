package com.tirepressure.app.data.local.dao

import androidx.room.*
import com.tirepressure.app.data.local.entity.BikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {

    @Query("SELECT * FROM bikes ORDER BY isDefault DESC, createdAt ASC")
    fun getAllBikes(): Flow<List<BikeEntity>>

    @Query("SELECT * FROM bikes ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllBikesList(): List<BikeEntity>

    @Query("SELECT * FROM bikes WHERE id = :id")
    suspend fun getBikeById(id: String): BikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bike: BikeEntity)

    @Update
    suspend fun update(bike: BikeEntity)

    @Query("DELETE FROM bikes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bikes")
    suspend fun clearAll()

    @Query("UPDATE bikes SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE bikes SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: String)

    @Transaction
    suspend fun setDefaultBike(id: String) {
        clearAllDefaults()
        setAsDefault(id)
    }

    @Transaction
    suspend fun replaceAll(bikes: List<BikeEntity>) {
        clearAll()
        for (bike in bikes) {
            insert(bike)
        }
    }
}

