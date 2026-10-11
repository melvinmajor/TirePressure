package com.tirepressure.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.tirepressure.app.MainActivity
import com.tirepressure.app.R
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.calculator.BertoTirePressureCalculator
import com.tirepressure.app.domain.model.BikeProfile
import com.tirepressure.app.domain.model.PressureCalculationInput
import com.tirepressure.app.domain.model.RideStyle
import com.tirepressure.app.domain.model.SurfaceCondition
import com.tirepressure.app.domain.model.WheelPressureResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class SingleBikeWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            WidgetPreferencesHelper.deleteWidgetPrefs(context, appWidgetId)
        }
    }

    companion object {
        private val calculator = BertoTirePressureCalculator()

        suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val database = AppDatabase.getInstance(context)
            val allEntities = database.bikeDao().getAllBikesList()
            val allBikes = allEntities.map { it.toDomain() }

            val prefsRepo = PreferencesRepositoryImpl(context)
            val riderWeight = prefsRepo.riderWeightFlow.first()
            val pressureUnit = prefsRepo.pressureUnitFlow.first()

            val storedBikeId = WidgetPreferencesHelper.getSingleBikeWidgetBikeId(context, appWidgetId)
            val selectedBike = allBikes.find { it.id == storedBikeId }
                ?: allBikes.find { it.isDefault }
                ?: allBikes.firstOrNull()

            val isDark = WidgetPreferencesHelper.isDarkTheme(context, appWidgetId)
            val rootLayoutId = if (isDark) R.layout.widget_single_bike_dark else R.layout.widget_single_bike_light
            val remoteViews = RemoteViews(context.packageName, rootLayoutId)

            // Intent to open Main App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Intent to open Widget Configuration
            val configIntent = Intent(context, SingleBikeWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 100000,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_btn_settings, configPendingIntent)

            if (selectedBike == null) {
                // Empty state
                remoteViews.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.widget_setups_container, View.GONE)
                remoteViews.setTextViewText(R.id.widget_bike_name, context.getString(R.string.app_name))
                remoteViews.setTextViewText(R.id.widget_discipline_badge, "")
                remoteViews.setTextViewText(R.id.widget_bike_weight, "")
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                return
            }

            // Populate Bike Info
            remoteViews.setViewVisibility(R.id.widget_empty_view, View.GONE)
            remoteViews.setViewVisibility(R.id.widget_setups_container, View.VISIBLE)
            remoteViews.setTextViewText(R.id.widget_bike_name, selectedBike.name)
            remoteViews.setTextViewText(R.id.widget_discipline_badge, selectedBike.category.displayName.uppercase(Locale.getDefault()))
            remoteViews.setTextViewText(
                R.id.widget_bike_weight,
                String.format(Locale.getDefault(), "• %.1f kg", selectedBike.bikeWeightKg)
            )

            // Populate Setups
            remoteViews.removeAllViews(R.id.widget_setups_container)
            val setupsToShow = selectedBike.setups.take(2) // ColorOS widget card accommodates up to 2 setups cleanly

            for (setup in setupsToShow) {
                val itemLayoutId = if (isDark) R.layout.widget_single_bike_setup_item_dark else R.layout.widget_single_bike_setup_item_light
                val setupViews = RemoteViews(context.packageName, itemLayoutId)

                val effectiveStyle = setup.getEffectiveRideStyle(selectedBike.category)
                val baseInput = PressureCalculationInput(
                    riderWeightKg = riderWeight,
                    bikeWeightKg = selectedBike.bikeWeightKg,
                    luggageWeightKg = setup.luggageWeightKg,
                    rideStyle = effectiveStyle,
                    frontTire = setup.frontTire,
                    rearTire = setup.rearTire,
                    frontWheel = setup.frontWheel,
                    rearWheel = setup.rearWheel
                )

                val dryRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.DRY))
                val wetRes = calculator.calculate(baseInput.copy(surfaceCondition = SurfaceCondition.WET))

                setupViews.setTextViewText(R.id.setup_item_name, setup.name)
                setupViews.setViewVisibility(R.id.setup_item_style_badge, View.VISIBLE)
                setupViews.setTextViewText(R.id.setup_item_style_badge, getDisciplineLabel(context, effectiveStyle))

                if (dryRes.hasHooklessWarning) {
                    setupViews.setViewVisibility(R.id.setup_item_warning, View.VISIBLE)
                } else {
                    setupViews.setViewVisibility(R.id.setup_item_warning, View.GONE)
                }

                val frontAbbr = context.getString(R.string.widget_front_abbr)
                val rearAbbr = context.getString(R.string.widget_rear_abbr)
                setupViews.setTextViewText(R.id.setup_front_abbr, frontAbbr)
                setupViews.setTextViewText(R.id.setup_rear_abbr, rearAbbr)

                val frontDry = formatPressure(dryRes.front, pressureUnit)
                val rearDry = formatPressure(dryRes.rear, pressureUnit)
                setupViews.setTextViewText(R.id.setup_front_dry_val, frontDry)
                setupViews.setTextViewText(R.id.setup_rear_dry_val, rearDry)

                if (setup.showWet) {
                    val wetLabel = context.getString(R.string.overview_wet)
                    val frontWet = formatPressure(wetRes.front, pressureUnit)
                    val rearWet = formatPressure(wetRes.rear, pressureUnit)
                    setupViews.setViewVisibility(R.id.setup_wet_info, View.VISIBLE)
                    setupViews.setTextViewText(
                        R.id.setup_wet_info,
                        "🌧️ $wetLabel : $frontWet • $rearWet"
                    )
                } else {
                    setupViews.setViewVisibility(R.id.setup_wet_info, View.GONE)
                }

                remoteViews.addView(R.id.widget_setups_container, setupViews)
            }

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        fun formatPressure(wheelResult: WheelPressureResult, unit: PressureUnit): String {
            return when (unit) {
                PressureUnit.BAR -> String.format(Locale.US, "%.1f %s", wheelResult.pressureBar, unit.symbol)
                PressureUnit.PSI -> String.format(Locale.US, "%.0f %s", wheelResult.pressurePsi, unit.symbol)
                PressureUnit.KPA -> String.format(Locale.US, "%.0f %s", wheelResult.pressureKpa, unit.symbol)
            }
        }

        private fun getDisciplineLabel(context: Context, style: RideStyle): String {
            return when (style) {
                RideStyle.ROAD -> context.getString(R.string.style_road)
                RideStyle.GRAVEL -> context.getString(R.string.style_gravel)
                RideStyle.CYCLOCROSS -> context.getString(R.string.style_cross)
                RideStyle.XC_MTB -> context.getString(R.string.style_xc_mtb)
                RideStyle.TRAIL_MTB -> context.getString(R.string.style_trail_mtb)
                RideStyle.ENDURO_MTB -> context.getString(R.string.style_enduro_mtb)
                RideStyle.DOWNHILL_MTB -> context.getString(R.string.style_dh_mtb)
                RideStyle.FAT_BIKE -> context.getString(R.string.style_fat)
                RideStyle.COMMUTE -> context.getString(R.string.style_commute)
            }.uppercase(Locale.getDefault())
        }
    }
}
