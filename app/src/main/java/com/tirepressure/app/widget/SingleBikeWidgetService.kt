package com.tirepressure.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.tirepressure.app.R
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.SurfaceCondition
import com.tirepressure.app.domain.model.WheelSetup
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

        views.setTextViewText(R.id.setup_item_name, setup.name)
        views.setViewVisibility(R.id.setup_item_style_badge, View.VISIBLE)
        views.setTextViewText(
            R.id.setup_item_style_badge,
            locContext.getString(effectiveStyle.labelRes).uppercase(targetLocale)
        )
        views.setViewVisibility(R.id.setup_item_warning, if (dryRes.hasHooklessWarning) View.VISIBLE else View.GONE)

        views.setTextViewText(R.id.setup_front_abbr, locContext.getString(R.string.widget_front_abbr))
        views.setTextViewText(R.id.setup_rear_abbr, locContext.getString(R.string.widget_rear_abbr))
        views.setTextViewText(R.id.setup_front_dry_val, SingleBikeWidgetReceiver.formatPressure(dryRes.front, pressureUnit))
        views.setTextViewText(R.id.setup_rear_dry_val, SingleBikeWidgetReceiver.formatPressure(dryRes.rear, pressureUnit))

        if (setup.showWet) {
            val wetLabel = locContext.getString(R.string.overview_wet)
            val frontWet = SingleBikeWidgetReceiver.formatPressure(wetRes.front, pressureUnit)
            val rearWet = SingleBikeWidgetReceiver.formatPressure(wetRes.rear, pressureUnit)
            views.setViewVisibility(R.id.setup_wet_info, View.VISIBLE)
            views.setTextViewText(R.id.setup_wet_info, "🌧️ $wetLabel : $frontWet • $rearWet")
        } else {
            views.setViewVisibility(R.id.setup_wet_info, View.GONE)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
