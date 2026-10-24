package com.tirepressure.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.tirepressure.app.R
import com.tirepressure.app.core.model.AppAccentColor
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.SurfaceCondition
import com.tirepressure.app.domain.model.WheelSetup
import com.tirepressure.app.ui.theme.getRideStyleColorInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SingleBikeWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SingleBikeSetupsRemoteViewsFactory(applicationContext, intent)
    }
}

private class SingleBikeSetupsRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private val calculator = BertoTirePressureCalculator()
    private var selectedBike: BikeProfile? = null
    private var setups: List<WheelSetup> = emptyList()
    private var riderWeight: Double = 75.0
    private var pressureUnit: PressureUnit = PressureUnit.BAR
    private var accentColor: AppAccentColor = AppAccentColor.ELECTRIC_LIME
    private var showWetSurface: Boolean = true
    private var showSnowSurface: Boolean = true

    override fun onCreate() {
        // Loaded in onDataSetChanged
    }

    override fun onDataSetChanged() {
        runBlocking {
            val database = AppDatabase.getInstance(context)
            val entities = database.bikeDao().getAllBikesList()
            val bikes = entities.map { it.toDomain() }

            val storedBikeId = WidgetPreferencesHelper.getSingleBikeWidgetBikeId(context, appWidgetId)
            selectedBike = bikes.find { it.id == storedBikeId }
                ?: bikes.find { it.isDefault }
                ?: bikes.firstOrNull()
            setups = selectedBike?.setups ?: emptyList()

            val prefs = PreferencesRepositoryImpl(context)
            riderWeight = prefs.riderWeightFlow.first()
            pressureUnit = prefs.pressureUnitFlow.first()
            accentColor = prefs.accentColorFlow.first()
            showWetSurface = prefs.showWetSurfaceFlow.first()
            showSnowSurface = prefs.showSnowSurfaceFlow.first()
        }
    }

    override fun onDestroy() {
        selectedBike = null
        setups = emptyList()
    }

    override fun getCount(): Int = setups.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in setups.indices) return null
        val bike = selectedBike ?: return null
        val setup = setups[position]

        val isDark = WidgetPreferencesHelper.isDarkTheme(context, appWidgetId)
        val layoutId = if (isDark) R.layout.widget_single_bike_setup_item_dark else R.layout.widget_single_bike_setup_item_light
        val views = RemoteViews(context.packageName, layoutId)

        val (locContext, targetLocale) = WidgetUpdateHelper.getWidgetLocalizedContext(context)
        val effectiveStyle = setup.getEffectiveRideStyle(bike.category)

        val baseInput = PressureCalculationInput(
            riderWeightKg = riderWeight,
            bikeWeightKg = bike.bikeWeightKg,
            luggageWeightKg = setup.luggageWeightKg,
            rideStyle = effectiveStyle,
            frontTire = setup.frontTire,
            rearTire = setup.rearTire,
            frontWheel = setup.frontWheel,
            rearWheel = setup.rearWheel
        )

        val dryRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.DRY))
        val wetRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.WET))
        val snowRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.SNOW))

        val accentColorInt = accentColor.getPrimaryColorInt(isDark)
        val styleColorInt = getRideStyleColorInt(effectiveStyle, bike.category, isDark)

        views.setTextViewText(R.id.setup_item_name, setup.name)
        views.setViewVisibility(R.id.setup_item_style_badge, View.VISIBLE)
        views.setTextViewText(
            R.id.setup_item_style_badge,
            locContext.getString(effectiveStyle.labelRes).uppercase(targetLocale)
        )
        views.setTextColor(R.id.setup_item_style_badge, styleColorInt)
        views.setViewVisibility(R.id.setup_item_warning, if (dryRes.hasHooklessWarning) View.VISIBLE else View.GONE)

        views.setTextViewText(R.id.setup_front_abbr, locContext.getString(R.string.widget_front_abbr))
        views.setTextColor(R.id.setup_front_abbr, accentColorInt)
        views.setTextViewText(R.id.setup_rear_abbr, locContext.getString(R.string.widget_rear_abbr))
        views.setTextColor(R.id.setup_rear_abbr, accentColorInt)

        views.setTextViewText(R.id.setup_front_dry_val, SingleBikeWidgetReceiver.formatPressure(dryRes.front, pressureUnit))
        views.setTextColor(R.id.setup_front_dry_val, accentColorInt)
        views.setTextViewText(R.id.setup_rear_dry_val, SingleBikeWidgetReceiver.formatPressure(dryRes.rear, pressureUnit))
        views.setTextColor(R.id.setup_rear_dry_val, accentColorInt)

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
            views.setViewVisibility(R.id.setup_wet_info, View.VISIBLE)
            views.setTextViewText(R.id.setup_wet_info, secondaryLines.joinToString("   |   "))
        } else {
            views.setViewVisibility(R.id.setup_wet_info, View.GONE)
        }

        val fillInIntent = Intent().apply {
            putExtra("selected_bike_id", bike.id)
        }
        views.setOnClickFillInIntent(R.id.setup_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
