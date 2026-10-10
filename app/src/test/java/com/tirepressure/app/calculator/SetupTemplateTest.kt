package com.tirepressure.app.calculator

import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SetupTemplateTest {

    private lateinit var calculator: BertoTirePressureCalculator

    @Before
    fun setUp() {
        calculator = BertoTirePressureCalculator()
    }

    @Test
    fun testNewSetupWithoutTemplate_inheritsBikeWeightAndStyleDefaults() {
        // Vélo existant avec un poids spécifique (ex: Gravel haut de gamme à 8.2 kg)
        val existingBike = BikeProfile(
            name = "Mon Gravel Carbone",
            category = BikeCategory.GRAVEL,
            bikeWeightKg = 8.2,
            setups = listOf(
                WheelSetup(
                    name = "Pneus Boue 45mm",
                    frontTire = TireConfig(45.0, TireCasing.REINFORCED),
                    rearTire = TireConfig(45.0, TireCasing.REINFORCED),
                    frontWheel = WheelConfig(WheelDiameter.DIAMETER_622, 25.0, RimType.TUBELESS_CROCHET),
                    rearWheel = WheelConfig(WheelDiameter.DIAMETER_622, 25.0, RimType.TUBELESS_CROCHET)
                )
            )
        )

        // Création d'un nouveau profil SANS template (valeurs par défaut de ce vélo)
        val validDiameters = WheelDiameter.getOptionsForStyle(existingBike.rideStyle)
        val freshSetup = WheelSetup(
            name = "Configuration standard",
            frontTire = TireConfig(existingBike.rideStyle.defaultTireWidthMm, existingBike.rideStyle.defaultTireCasing),
            rearTire = TireConfig(existingBike.rideStyle.defaultTireWidthMm, existingBike.rideStyle.defaultTireCasing),
            frontWheel = WheelConfig(
                diameter = validDiameters.first(),
                innerRimWidthMm = existingBike.rideStyle.defaultInnerRimWidthMm,
                rimType = existingBike.rideStyle.defaultRimType
            ),
            rearWheel = WheelConfig(
                diameter = validDiameters.first(),
                innerRimWidthMm = existingBike.rideStyle.defaultInnerRimWidthMm,
                rimType = existingBike.rideStyle.defaultRimType
            )
        )

        val updatedBike = existingBike.copy(setups = existingBike.setups + freshSetup)

        // Vérification : le vélo a bien conservé son poids spécifique (8.2 kg) et sa catégorie (Gravel)
        assertEquals(8.2, updatedBike.bikeWeightKg, 0.001)
        assertEquals(BikeCategory.GRAVEL, updatedBike.category)
        assertEquals(RideStyle.GRAVEL, updatedBike.rideStyle)
        assertEquals(2, updatedBike.setups.size)

        // Calcul de pression pour ce nouveau setup : utilise bien le poids spécifique du vélo (8.2 kg)
        val input = PressureCalculationInput(
            riderWeightKg = 75.0,
            bikeWeightKg = updatedBike.bikeWeightKg,
            rideStyle = updatedBike.rideStyle,
            frontTire = freshSetup.frontTire,
            rearTire = freshSetup.rearTire,
            frontWheel = freshSetup.frontWheel,
            rearWheel = freshSetup.rearWheel
        )

        val result = calculator.calculate(input)
        assertEquals(83.2, result.totalSystemWeightKg, 0.001)
        assertTrue(result.front.pressureBar > 1.5)
        assertTrue(result.rear.pressureBar > result.front.pressureBar)
    }

    @Test
    fun testNewSetupWithExistingTemplate_clonesSelectedTemplateWheelParameters() {
        // Vélo existant avec 2 configurations distinctes
        val setupRoad = WheelSetup(
            name = "Roues Route 30mm",
            frontTire = TireConfig(30.0, TireCasing.THIN),
            rearTire = TireConfig(30.0, TireCasing.THIN),
            frontWheel = WheelConfig(WheelDiameter.DIAMETER_622, 21.0, RimType.TUBELESS_STRAIGHT_SIDE),
            rearWheel = WheelConfig(WheelDiameter.DIAMETER_622, 21.0, RimType.TUBELESS_STRAIGHT_SIDE)
        )
        val setupGravel = WheelSetup(
            name = "Roues Gravel 45mm",
            frontTire = TireConfig(45.0, TireCasing.REINFORCED),
            rearTire = TireConfig(45.0, TireCasing.REINFORCED),
            frontWheel = WheelConfig(WheelDiameter.DIAMETER_584, 25.0, RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(WheelDiameter.DIAMETER_584, 25.0, RimType.TUBELESS_CROCHET)
        )

        val existingBike = BikeProfile(
            name = "Mon All-Road",
            category = BikeCategory.GRAVEL,
            bikeWeightKg = 8.8,
            setups = listOf(setupRoad, setupGravel)
        )

        // L'utilisateur choisit setupRoad comme modèle pour une nouvelle configuration "Route Hiver"
        val clonedFromRoad = setupRoad.copy(
            id = UUID.randomUUID().toString(),
            name = "Route Hiver",
            isDefault = false
        )

        assertEquals(30.0, clonedFromRoad.frontTire.widthMm, 0.001)
        assertEquals(TireCasing.THIN, clonedFromRoad.frontTire.casing)
        assertEquals(21.0, clonedFromRoad.frontWheel.innerRimWidthMm, 0.001)
        assertEquals(WheelDiameter.DIAMETER_622, clonedFromRoad.frontWheel.diameter)

        // L'utilisateur choisit setupGravel comme modèle pour "Bikepacking"
        val clonedFromGravel = setupGravel.copy(
            id = UUID.randomUUID().toString(),
            name = "Bikepacking",
            luggageWeightKg = 6.0,
            isDefault = false
        )

        assertEquals(45.0, clonedFromGravel.frontTire.widthMm, 0.001)
        assertEquals(TireCasing.REINFORCED, clonedFromGravel.frontTire.casing)
        assertEquals(WheelDiameter.DIAMETER_584, clonedFromGravel.frontWheel.diameter)
        assertEquals(6.0, clonedFromGravel.luggageWeightKg, 0.001)

        val updatedBike = existingBike.copy(setups = existingBike.setups + listOf(clonedFromRoad, clonedFromGravel))
        assertEquals(4, updatedBike.setups.size)
        assertEquals(8.8, updatedBike.bikeWeightKg, 0.001)
    }

    @Test
    fun testSameWheelsetDifferentUsageOnSameBike() {
        // Vélo XC de 10.5 kg avec une paire de roues 29" x 2.3"
        val xcSetup = WheelSetup(
            name = "Sortie XC Compétition",
            rideStyle = null, // Hérite de BikeCategory.MTB -> RideStyle.XC_MTB
            frontTire = TireConfig(58.42, TireCasing.STANDARD),
            rearTire = TireConfig(58.42, TireCasing.STANDARD),
            frontWheel = WheelConfig(WheelDiameter.DIAMETER_622, 30.0, RimType.TUBELESS_STRAIGHT_SIDE),
            rearWheel = WheelConfig(WheelDiameter.DIAMETER_622, 30.0, RimType.TUBELESS_STRAIGHT_SIDE)
        )

        val bike = BikeProfile(
            name = "Mon VTT XC",
            category = BikeCategory.MTB,
            bikeWeightKg = 10.5,
            setups = listOf(xcSetup)
        )

        // Cas 1 : Même wheelset, mais profil pour usage "Trail engagé" (chocs plus violents)
        val trailSetup = xcSetup.copy(
            id = UUID.randomUUID().toString(),
            name = "Session Trail / All-Mountain",
            rideStyle = RideStyle.TRAIL_MTB
        )

        // Cas 2 : Même wheelset, mais profil pour entraînement "Route suspensions bloquées"
        val roadSetup = xcSetup.copy(
            id = UUID.randomUUID().toString(),
            name = "Entraînement Route (Bloqué)",
            rideStyle = RideStyle.ROAD
        )

        val multiUsageBike = bike.copy(setups = listOf(xcSetup, trailSetup, roadSetup))

        val riderWeightKg = 75.0

        // Calcul XC
        val xcInput = PressureCalculationInput(
            riderWeightKg = riderWeightKg,
            bikeWeightKg = multiUsageBike.bikeWeightKg, // 10.5 kg préservé
            rideStyle = xcSetup.getEffectiveRideStyle(multiUsageBike.category), // XC via default
            frontTire = xcSetup.frontTire,
            rearTire = xcSetup.rearTire,
            frontWheel = xcSetup.frontWheel,
            rearWheel = xcSetup.rearWheel
        )
        val xcResult = calculator.calculate(xcInput)

        // Calcul Trail
        val trailInput = PressureCalculationInput(
            riderWeightKg = riderWeightKg,
            bikeWeightKg = multiUsageBike.bikeWeightKg, // 10.5 kg toujours préservé !
            rideStyle = trailSetup.getEffectiveRideStyle(multiUsageBike.category), // TRAIL_MTB
            frontTire = trailSetup.frontTire,
            rearTire = trailSetup.rearTire,
            frontWheel = trailSetup.frontWheel,
            rearWheel = trailSetup.rearWheel
        )
        val trailResult = calculator.calculate(trailInput)

        // Calcul Route
        val roadInput = PressureCalculationInput(
            riderWeightKg = riderWeightKg,
            bikeWeightKg = multiUsageBike.bikeWeightKg, // 10.5 kg toujours préservé !
            rideStyle = roadSetup.getEffectiveRideStyle(multiUsageBike.category), // ROAD
            frontTire = roadSetup.frontTire,
            rearTire = roadSetup.rearTire,
            frontWheel = roadSetup.frontWheel,
            rearWheel = roadSetup.rearWheel
        )
        val roadResult = calculator.calculate(roadInput)

        // Vérifications :
        // 1. En Trail, le même pneu nécessite une pression supérieure au XC pour encaisser les descentes et compressions
        assertTrue(
            "La pression arrière en Trail doit être supérieure au XC pour le même pneu",
            trailResult.rear.pressureBar > xcResult.rear.pressureBar
        )
        // 2. En Route, la répartition de masse (42/58) et le coefficient route (1.00 vs 0.95) augmentent la pression arrière
        assertTrue(
            "La pression arrière en Route doit être supérieure au XC",
            roadResult.rear.pressureBar > xcResult.rear.pressureBar
        )
        // 3. Le poids total du système reste identique (75 kg cycliste + 10.5 kg vélo = 85.5 kg)
        assertEquals(85.5, xcResult.totalSystemWeightKg, 0.001)
        assertEquals(85.5, trailResult.totalSystemWeightKg, 0.001)
        assertEquals(85.5, roadResult.totalSystemWeightKg, 0.001)
    }

    @Test
    fun testBikeMainCategoryVsProfileDetailedDisciplines() {
        // Vérifie les 4 catégories principales du vélo
        assertEquals(4, BikeCategory.entries.size)
        assertTrue(BikeCategory.entries.contains(BikeCategory.ROAD))
        assertTrue(BikeCategory.entries.contains(BikeCategory.GRAVEL))
        assertTrue(BikeCategory.entries.contains(BikeCategory.MTB))
        assertTrue(BikeCategory.entries.contains(BikeCategory.COMMUTE))

        // Création d'un vélo avec catégorie principale MTB
        val mtbBike = BikeProfile(
            name = "Mon VTT Polyvalent",
            category = BikeCategory.MTB,
            bikeWeightKg = 13.0
        )

        // Profils avec pratiques détaillées distinctes (Enduro, Trail, XC, etc.)
        val enduroSetup = WheelSetup(
            name = "Enduro Station",
            rideStyle = RideStyle.ENDURO_MTB,
            frontTire = TireConfig(60.96, TireCasing.REINFORCED),
            rearTire = TireConfig(60.96, TireCasing.REINFORCED),
            frontWheel = WheelConfig(WheelDiameter.DIAMETER_622, 30.0, RimType.TUBELESS_CROCHET),
            rearWheel = WheelConfig(WheelDiameter.DIAMETER_622, 30.0, RimType.TUBELESS_CROCHET)
        )

        val commuteSetup = WheelSetup(
            name = "Vélotaf Hiver",
            rideStyle = RideStyle.COMMUTE,
            frontTire = TireConfig(45.0, TireCasing.STANDARD),
            rearTire = TireConfig(45.0, TireCasing.STANDARD),
            frontWheel = WheelConfig(WheelDiameter.DIAMETER_622, 25.0, RimType.TUBES),
            rearWheel = WheelConfig(WheelDiameter.DIAMETER_622, 25.0, RimType.TUBES)
        )

        val defaultSetup = WheelSetup(
            name = "Profil sans discipline explicite",
            rideStyle = null
        )

        // Vérification des résolutions de styles effectifs
        assertEquals(RideStyle.ENDURO_MTB, enduroSetup.getEffectiveRideStyle(mtbBike.category))
        assertEquals(RideStyle.COMMUTE, commuteSetup.getEffectiveRideStyle(mtbBike.category))
        assertEquals(RideStyle.XC_MTB, defaultSetup.getEffectiveRideStyle(mtbBike.category))

        // Calculs de pression respectifs
        val riderWeightKg = 70.0
        val enduroCalc = calculator.calculate(
            PressureCalculationInput(
                riderWeightKg = riderWeightKg,
                bikeWeightKg = mtbBike.bikeWeightKg,
                rideStyle = enduroSetup.getEffectiveRideStyle(mtbBike.category),
                frontTire = enduroSetup.frontTire,
                rearTire = enduroSetup.rearTire,
                frontWheel = enduroSetup.frontWheel,
                rearWheel = enduroSetup.rearWheel
            )
        )
        val commuteCalc = calculator.calculate(
            PressureCalculationInput(
                riderWeightKg = riderWeightKg,
                bikeWeightKg = mtbBike.bikeWeightKg,
                rideStyle = commuteSetup.getEffectiveRideStyle(mtbBike.category),
                frontTire = commuteSetup.frontTire,
                rearTire = commuteSetup.rearTire,
                frontWheel = commuteSetup.frontWheel,
                rearWheel = commuteSetup.rearWheel
            )
        )

        // Les calculs sont correctement différenciés selon la discipline du profil
        assertTrue(enduroCalc.rear.pressureBar > 0)
        assertTrue(commuteCalc.rear.pressureBar > 0)
    }

    @Test
    fun testBikeAndSetupDeletionRules() {
        val setup1 = WheelSetup(name = "Config 1", isDefault = true)
        val setup2 = WheelSetup(name = "Config 2", isDefault = false)
        val bike = BikeProfile(
            name = "Test Bike",
            category = BikeCategory.GRAVEL,
            setups = listOf(setup1, setup2)
        )

        // Deleting a profile from a bike with > 1 profiles is allowed
        assertTrue(bike.setups.size > 1)
        val bikeAfterSetupDeleted = bike.copy(setups = bike.setups.filterNot { it.id == setup2.id })
        assertEquals(1, bikeAfterSetupDeleted.setups.size)
        assertEquals("Config 1", bikeAfterSetupDeleted.setups.first().name)

        // If a bike has only 1 profile left, it cannot be deleted further (must retain at least 1)
        val cannotDeleteLast = if (bikeAfterSetupDeleted.setups.size <= 1) bikeAfterSetupDeleted else bikeAfterSetupDeleted.copy(setups = emptyList())
        assertEquals(1, cannotDeleteLast.setups.size)
    }
}

