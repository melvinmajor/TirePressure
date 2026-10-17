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
import com.tirepressure.app.domain.model.RideStyle
import com.tirepressure.app.domain.model.SurfaceCondition
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
    private var accentColor: AppAccentColor = AppAccentColor.ELECTRIC_LIME

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
            accentColor = prefs.accentColorFlow.first()
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

        views.setTextViewText(R.id.item_bike_name, bike.name)
        views.setTextViewText(R.id.item_bike_badge, locContext.getString(bike.category.labelRes).uppercase(targetLocale))
        views.setTextColor(R.id.item_bike_badge, accentColorInt)
        views.setTextViewText(
            R.id.item_bike_weight,
            String.format(targetLocale, "• %.1f kg", bike.bikeWeightKg)
        )

        val frontAbbr = locContext.getString(R.string.widget_front_abbr)
        val rearAbbr = locContext.getString(R.string.widget_rear_abbr)

        // Setup 1 (primary or first)
        val setup1 = bike.primarySetup
        val setup1Style = setup1.getEffectiveRideStyle(bike.category)
        val input1 = PressureCalculationInput(
            riderWeightKg = riderWeight,
            bikeWeightKg = bike.bikeWeightKg,
            luggageWeightKg = setup1.luggageWeightKg,
            rideStyle = setup1Style,
            frontTire = setup1.frontTire,
            rearTire = setup1.rearTire,
            frontWheel = setup1.frontWheel,
            rearWheel = setup1.rearWheel
        )
        val dry1 = calculator.calculate(input1.copy(surfaceCondition = SurfaceCondition.DRY))
        val wet1 = calculator.calculate(input1.copy(surfaceCondition = SurfaceCondition.WET))

        views.setTextViewText(R.id.item_setup1_name, setup1.name)
        views.setTextViewText(
            R.id.item_setup1_pressures,
            "$frontAbbr ${SingleBikeWidgetReceiver.formatPressure(dry1.front, pressureUnit)}   $rearAbbr ${SingleBikeWidgetReceiver.formatPressure(dry1.rear, pressureUnit)}"
        )
        views.setTextColor(R.id.item_setup1_pressures, accentColorInt)
        views.setTextViewText(
            R.id.item_setup1_wet,
            "🌧️ ${SingleBikeWidgetReceiver.formatPressure(wet1.front, pressureUnit)} / ${SingleBikeWidgetReceiver.formatPressure(wet1.rear, pressureUnit)}"
        )

        // Setup 2 (if present)
        val otherSetups = bike.setups.filter { it.id != setup1.id }
        if (otherSetups.isNotEmpty()) {
            val setup2 = otherSetups.first()
            val setup2Style = setup2.getEffectiveRideStyle(bike.category)
            val input2 = PressureCalculationInput(
                riderWeightKg = riderWeight,
                bikeWeightKg = bike.bikeWeightKg,
                luggageWeightKg = setup2.luggageWeightKg,
                rideStyle = setup2Style,
                frontTire = setup2.frontTire,
                rearTire = setup2.rearTire,
                frontWheel = setup2.frontWheel,
                rearWheel = setup2.rearWheel
            )
            val dry2 = calculator.calculate(input2.copy(surfaceCondition = SurfaceCondition.DRY))
            val wet2 = calculator.calculate(input2.copy(surfaceCondition = SurfaceCondition.WET))

            views.setViewVisibility(R.id.item_setup2_container, View.VISIBLE)
            views.setTextViewText(R.id.item_setup2_name, setup2.name)
            views.setTextViewText(
                R.id.item_setup2_pressures,
                "$frontAbbr ${SingleBikeWidgetReceiver.formatPressure(dry2.front, pressureUnit)}   $rearAbbr ${SingleBikeWidgetReceiver.formatPressure(dry2.rear, pressureUnit)}"
            )
            views.setTextColor(R.id.item_setup2_pressures, accentColorInt)
            views.setTextViewText(
                R.id.item_setup2_wet,
                "🌧️ ${SingleBikeWidgetReceiver.formatPressure(wet2.front, pressureUnit)} / ${SingleBikeWidgetReceiver.formatPressure(wet2.rear, pressureUnit)}"
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

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun getDisciplineLabel(context: Context, style: RideStyle): String {
        val (locContext, targetLocale) = WidgetUpdateHelper.getWidgetLocalizedContext(context)
        return locContext.getString(style.labelRes).uppercase(targetLocale)
    }
}
