package com.tirepressure.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tirepressure.app.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val brand: String = "",
    val model: String = "",
    val year: Int? = null,
    val frameSize: String = "",
    val rideStyle: String,
    val bikeWeightKg: Double,
    val setupsJson: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): BikeProfile {
        val parsedSetups = try {
            if (setupsJson.isNotBlank()) {
                jsonConfig.decodeFromString<List<WheelSetup>>(setupsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        val finalSetups = if (parsedSetups.isEmpty()) {
            listOf(
                WheelSetup(
                    name = "Configuration principale",
                    isDefault = true
                )
            )
        } else {
            parsedSetups
        }

        val parsedCategory = runCatching { BikeCategory.valueOf(rideStyle) }.getOrElse {
            when (runCatching { RideStyle.valueOf(rideStyle) }.getOrNull()) {
                RideStyle.XC_MTB, RideStyle.TRAIL_MTB, RideStyle.ENDURO_MTB, RideStyle.DOWNHILL_MTB, RideStyle.FAT_BIKE -> BikeCategory.MTB
                RideStyle.GRAVEL, RideStyle.CYCLOCROSS -> BikeCategory.GRAVEL
                RideStyle.COMMUTE -> BikeCategory.COMMUTE
                else -> BikeCategory.ROAD
            }
        }

        return BikeProfile(
            id = id,
            name = name,
            brand = brand,
            model = model,
            year = year,
            frameSize = frameSize,
            category = parsedCategory,
            bikeWeightKg = bikeWeightKg,
            setups = finalSetups,
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: BikeProfile): BikeEntity {
            val json = jsonConfig.encodeToString(domain.setups)
            return BikeEntity(
                id = domain.id,
                name = domain.name,
                brand = domain.brand,
                model = domain.model,
                year = domain.year,
                frameSize = domain.frameSize,
                rideStyle = domain.category.name,
                bikeWeightKg = domain.bikeWeightKg,
                setupsJson = json,
                isDefault = domain.isDefault,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}
