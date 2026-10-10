package com.tirepressure.app.domain.repository

import com.tirepressure.app.domain.model.BikeProfile
import kotlinx.coroutines.flow.Flow

interface BikeRepository {
    fun getAllBikes(): Flow<List<BikeProfile>>
    suspend fun getAllBikesList(): List<BikeProfile>
    suspend fun getBikeById(id: String): BikeProfile?
    suspend fun insertBike(bike: BikeProfile)
    suspend fun updateBike(bike: BikeProfile)
    suspend fun deleteBike(id: String)
    suspend fun setDefaultBike(id: String)
    suspend fun duplicateBike(id: String): BikeProfile?
    suspend fun replaceAll(bikes: List<BikeProfile>)
    suspend fun clearAll()
}

