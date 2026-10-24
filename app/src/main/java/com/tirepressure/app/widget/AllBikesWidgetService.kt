package com.tirepressure.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.tirepressure.app.R
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.MassUnit
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.SurfaceCondition
import com.tirepressure.app.domain.model.WheelSetup
import com.tirepressure.app.ui.theme.getCategoryColorInt
import com.tirepressure.app.ui.theme.getRideStyleColorInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class AllBikesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AllBikesRemoteViewsFactory(applicationContext, intent)
    }
}

class AllBikesRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private val calculator = BertoTirePressureCalculator()
    private var bikes: List<BikeProfile> = emptyList()
    private var riderWeight: Double = 75.0
    private var pressureUnit: PressureUnit = PressureUnit.BAR
    private var massUnit: MassUnit = MassUnit.KG
    private var accentColor: AppAccentColor = AppAccentColor.ELECTRIC_LIME
    private var showWetSurface: Boolean = true
    private var showSnowSurface: Boolean = true

    override fun onCreate() {
        // Will load in onDataSetChanged
    }

    override fun onDataSetChanged() {
        runBlocking {
            val database = AppDatabase.getInstance(context)
            val entities = database.bikeDao().getAllBikesList()
            bikes = entities.map { it.toDomain() }

            val prefs = PreferencesRepositoryImpl(context)
            riderWeight = prefs.riderWeightFlow.first()
            pressureUnit = prefs.pressureUnitFlow.first()
            massUnit = prefs.massUnitFlow.first()
            accentColor = prefs.accentColorFlow.first()
            showWetSurface = prefs.showWetSurfaceFlow.first()
            showSnowSurface = prefs.showSnowSurfaceFlow.first()
        }
    }

    override fun onDestroy() {
        bikes = emptyList()
    }

    override fun getCount(): Int = bikes.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= bikes.size) return null
        val bike = bikes[position]

        val isDark = WidgetPreferencesHelper.isDarkTheme(context, appWidgetId)
        val layoutId = if (isDark) R.layout.widget_all_bikes_item_dark else R.layout.widget_all_bikes_item_light
        val views = RemoteViews(context.packageName, layoutId)

        val (locContext, targetLocale) = WidgetUpdateHelper.getWidgetLocalizedContext(context)

        val accentColorInt = accentColor.getPrimaryColorInt(isDark)
        val categoryColorInt = getCategoryColorInt(bike.category, isDark)

        val displayBikeName = if (bike.isDefault) "★ ${bike.name}" else bike.name
        views.setTextViewText(R.id.item_bike_name, displayBikeName)
        views.setTextViewText(
            R.id.item_bike_badge,
            locContext.getString(bike.category.labelRes).uppercase(targetLocale)
        )
        views.setTextColor(R.id.item_bike_badge, categoryColorInt)

        val bikeWeight = com.tirepressure.app.core.util.UnitConverter.fromKg(bike.bikeWeightKg, massUnit)
        val weightText = String.format(targetLocale, "%.1f %s", bikeWeight, massUnit.symbol)
        val metaText = if (bike.frameSize.isNotBlank()) {
            "• ${bike.frameSize} • $weightText"
        } else {
            "• $weightText"
        }
        views.setTextViewText(R.id.item_bike_weight, metaText)

        val frontAbbr = locContext.getString(R.string.widget_front_abbr)
        val rearAbbr = locContext.getString(R.string.widget_rear_abbr)

        // Setup 1 (primary or first)
        val setup1 = bike.primarySetup
        bindSetupBlock(
            views = views,
            locContext = locContext,
            targetLocale = targetLocale,
            bike = bike,
            setup = setup1,
            frontAbbr = frontAbbr,
            rearAbbr = rearAbbr,
            accentColorInt = accentColorInt,
            isDark = isDark,
            nameViewId = R.id.item_setup1_name,
            styleBadgeViewId = R.id.item_setup1_style_badge,
            frontAbbrViewId = R.id.item_setup1_front_abbr,
            frontValViewId = R.id.item_setup1_front_val,
            rearAbbrViewId = R.id.item_setup1_rear_abbr,
            rearValViewId = R.id.item_setup1_rear_val,
            secondarySurfaceViewId = R.id.item_setup1_wet
        )

        // Setup 2 (if present)
        val otherSetups = bike.setups.filter { it.id != setup1.id }
        if (otherSetups.isNotEmpty()) {
            val setup2 = otherSetups.first()
            views.setViewVisibility(R.id.item_setup2_container, View.VISIBLE)
            bindSetupBlock(
                views = views,
                locContext = locContext,
                targetLocale = targetLocale,
                bike = bike,
                setup = setup2,
                frontAbbr = frontAbbr,
                rearAbbr = rearAbbr,
                accentColorInt = accentColorInt,
                isDark = isDark,
                nameViewId = R.id.item_setup2_name,
                styleBadgeViewId = R.id.item_setup2_style_badge,
                frontAbbrViewId = R.id.item_setup2_front_abbr,
                frontValViewId = R.id.item_setup2_front_val,
                rearAbbrViewId = R.id.item_setup2_rear_abbr,
                rearValViewId = R.id.item_setup2_rear_val,
                secondarySurfaceViewId = R.id.item_setup2_wet
            )
        } else {
            views.setViewVisibility(R.id.item_setup2_container, View.GONE)
        }

        // Fill-in intent to launch app
        val fillInIntent = Intent().apply {
            putExtra("selected_bike_id", bike.id)
        }
        views.setOnClickFillInIntent(R.id.all_bikes_item_root, fillInIntent)

        return views
    }

    private fun bindSetupBlock(
        views: RemoteViews,
        locContext: Context,
        targetLocale: Locale,
        bike: BikeProfile,
        setup: WheelSetup,
        frontAbbr: String,
        rearAbbr: String,
        accentColorInt: Int,
        isDark: Boolean,
        nameViewId: Int,
        styleBadgeViewId: Int,
        frontAbbrViewId: Int,
        frontValViewId: Int,
        rearAbbrViewId: Int,
        rearValViewId: Int,
        secondarySurfaceViewId: Int
    ) {
        val setupStyle = setup.getEffectiveRideStyle(bike.category)
        val styleColorInt = getRideStyleColorInt(setupStyle, bike.category, isDark)
        val input = PressureCalculationInput(
            riderWeightKg = riderWeight,
            bikeWeightKg = bike.bikeWeightKg,
            luggageWeightKg = setup.luggageWeightKg,
            rideStyle = setupStyle,
            frontTire = setup.frontTire,
            rearTire = setup.rearTire,
            frontWheel = setup.frontWheel,
            rearWheel = setup.rearWheel
        )
        val dryRes = calculator.calculate(input.copy(surfaceCondition = SurfaceCondition.DRY))
        val wetRes = calculator.calculate(input.copy(surfaceCondition = SurfaceCondition.WET))
        val snowRes = calculator.calculate(input.copy(surfaceCondition = SurfaceCondition.SNOW))

        views.setTextViewText(nameViewId, setup.name)
        views.setTextViewText(
            styleBadgeViewId,
            locContext.getString(setupStyle.labelRes).uppercase(targetLocale)
        )
        views.setTextColor(styleBadgeViewId, styleColorInt)

        views.setTextViewText(frontAbbrViewId, frontAbbr)
        views.setTextColor(frontAbbrViewId, accentColorInt)
        views.setTextViewText(frontValViewId, SingleBikeWidgetReceiver.formatPressure(dryRes.front, pressureUnit))
        views.setTextColor(frontValViewId, accentColorInt)

        views.setTextViewText(rearAbbrViewId, rearAbbr)
        views.setTextColor(rearAbbrViewId, accentColorInt)
        views.setTextViewText(rearValViewId, SingleBikeWidgetReceiver.formatPressure(dryRes.rear, pressureUnit))
        views.setTextColor(rearValViewId, accentColorInt)

        val secondaryLines = buildList {
            if (showWetSurface) {
                val wetLabel = locContext.getString(R.string.overview_wet)
                val frontWet = SingleBikeWidgetReceiver.formatPressure(wetRes.front, pressureUnit)
                val rearWet = SingleBikeWidgetReceiver.formatPressure(wetRes.rear, pressureUnit)
                add("🌧️ $wetLabel : $frontWet • $rearWet")
            }
            if (showSnowSurface) {
                val snowLabel = locContext.getString(R.string.overview_snow)
                val frontSnow = SingleBikeWidgetReceiver.formatPressure(snowRes.front, pressureUnit)
                val rearSnow = SingleBikeWidgetReceiver.formatPressure(snowRes.rear, pressureUnit)
                add("❄️ $snowLabel : $frontSnow • $rearSnow")
            }
        }

        if (secondaryLines.isNotEmpty()) {
            views.setViewVisibility(secondarySurfaceViewId, View.VISIBLE)
            views.setTextViewText(secondarySurfaceViewId, secondaryLines.joinToString("   |   "))
        } else {
            views.setViewVisibility(secondarySurfaceViewId, View.GONE)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
