package com.tirepressure.app.data.repository

import com.tirepressure.app.data.local.dao.BikeDao
import com.tirepressure.app.data.local.entity.BikeEntity
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.repository.BikeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context
import com.tirepressure.app.widget.WidgetUpdateHelper
import java.util.UUID

class BikeRepositoryImpl(
    private val bikeDao: BikeDao,
    private val context: Context? = null
) : BikeRepository {

    override fun getAllBikes(): Flow<List<BikeProfile>> {
        return bikeDao.getAllBikes().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllBikesList(): List<BikeProfile> {
        return bikeDao.getAllBikesList().map { it.toDomain() }
    }

    override suspend fun getBikeById(id: String): BikeProfile? {
        return bikeDao.getBikeById(id)?.toDomain()
    }

    override suspend fun insertBike(bike: BikeProfile) {
        val entity = BikeEntity.fromDomain(bike)
        bikeDao.insert(entity)
        if (bike.isDefault) {
            bikeDao.setDefaultBike(bike.id)
        }
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }

    override suspend fun updateBike(bike: BikeProfile) {
        val entity = BikeEntity.fromDomain(bike.copy(updatedAt = System.currentTimeMillis()))
        bikeDao.update(entity)
        if (bike.isDefault) {
            bikeDao.setDefaultBike(bike.id)
        }
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }

    override suspend fun deleteBike(id: String) {
        bikeDao.deleteById(id)
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }

    override suspend fun setDefaultBike(id: String) {
        bikeDao.setDefaultBike(id)
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }

    override suspend fun duplicateBike(id: String): BikeProfile? {
        val original = getBikeById(id) ?: return null
        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (copie)",
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        insertBike(duplicate)
        return duplicate
    }

    override suspend fun replaceAll(bikes: List<BikeProfile>) {
        val entities = bikes.map { BikeEntity.fromDomain(it) }
        bikeDao.replaceAll(entities)
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }

    override suspend fun clearAll() {
        bikeDao.clearAll()
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }
}

