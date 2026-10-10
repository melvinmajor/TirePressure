package com.tirepressure.app

import android.app.Application
import com.tirepressure.app.data.backup.BackupManager
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.BikeRepositoryImpl
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.calculator.TirePressureCalculator
import com.tirepressure.app.domain.repository.BikeRepository
import com.tirepressure.app.domain.repository.PreferencesRepository

class TirePressureApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var bikeRepository: BikeRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var backupManager: BackupManager
        private set

    val calculator: TirePressureCalculator by lazy {
        BertoTirePressureCalculator()
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        bikeRepository = BikeRepositoryImpl(database.bikeDao(), this)
        preferencesRepository = PreferencesRepositoryImpl(this)
        backupManager = BackupManager(bikeRepository, preferencesRepository)
    }
}

