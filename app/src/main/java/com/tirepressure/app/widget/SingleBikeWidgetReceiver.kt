package com.tirepressure.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.tirepressure.app.MainActivity
import com.tirepressure.app.R
import com.tirepressure.app.core.model.PressureUnit
import com.tirepressure.app.data.local.AppDatabase
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import com.tirepressure.app.domain.model.WheelPressureResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class SingleBikeWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }

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
        @Suppress("DEPRECATION")
        suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val database = AppDatabase.getInstance(context)
            val allEntities = database.bikeDao().getAllBikesList()
            val allBikes = allEntities.map { it.toDomain() }

            val storedBikeId = WidgetPreferencesHelper.getSingleBikeWidgetBikeId(context, appWidgetId)
            val selectedBike = allBikes.find { it.id == storedBikeId }
                ?: allBikes.find { it.isDefault }
                ?: allBikes.firstOrNull()

            val isDark = WidgetPreferencesHelper.isDarkTheme(context, appWidgetId)
            val rootLayoutId = if (isDark) R.layout.widget_single_bike_dark else R.layout.widget_single_bike_light
            val remoteViews = RemoteViews(context.packageName, rootLayoutId)

            val (locContext, targetLocale) = WidgetUpdateHelper.getWidgetLocalizedContext(context)

            val prefs = PreferencesRepositoryImpl(context)
            val accentColor = prefs.accentColorFlow.first()
            val accentColorInt = accentColor.getPrimaryColorInt(isDark)

            remoteViews.setInt(R.id.widget_bike_icon, "setColorFilter", accentColorInt)
            remoteViews.setTextColor(R.id.widget_discipline_badge, accentColorInt)
            remoteViews.setContentDescription(R.id.widget_bike_icon, locContext.getString(R.string.bike_name_label))
            remoteViews.setContentDescription(R.id.widget_btn_settings, locContext.getString(R.string.widget_config_title))

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
                remoteViews.setViewVisibility(R.id.widget_setups_list, View.GONE)
                remoteViews.setTextViewText(R.id.widget_bike_name, locContext.getString(R.string.app_name))
                remoteViews.setTextViewText(R.id.widget_discipline_badge, "")
                remoteViews.setTextViewText(R.id.widget_bike_weight, "")
                remoteViews.setTextViewText(R.id.widget_empty_title, locContext.getString(R.string.widget_no_bikes))
                remoteViews.setTextViewText(R.id.widget_empty_desc, locContext.getString(R.string.widget_tap_to_open))
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                return
            }

            // Populate Bike Info
            remoteViews.setViewVisibility(R.id.widget_empty_view, View.GONE)
            remoteViews.setViewVisibility(R.id.widget_setups_list, View.VISIBLE)
            remoteViews.setTextViewText(R.id.widget_bike_name, selectedBike.name)
            remoteViews.setTextViewText(R.id.widget_discipline_badge, locContext.getString(selectedBike.category.labelRes).uppercase(targetLocale))
            val massUnit = prefs.massUnitFlow.first()
            val bikeWeight = com.tirepressure.app.core.util.UnitConverter.fromKg(selectedBike.bikeWeightKg, massUnit)
            remoteViews.setTextViewText(
                R.id.widget_bike_weight,
                String.format(targetLocale, "• %.1f %s", bikeWeight, massUnit.symbol)
            )

            // Populate Setups via RemoteViewsService-backed ListView
            val serviceIntent = Intent(context, SingleBikeWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            remoteViews.setRemoteAdapter(R.id.widget_setups_list, serviceIntent)

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_setups_list)
        }

        fun formatPressure(wheelResult: WheelPressureResult, unit: PressureUnit): String {
            return when (unit) {
                PressureUnit.BAR -> String.format(Locale.US, "%.1f %s", wheelResult.pressureBar, unit.symbol)
                PressureUnit.PSI -> String.format(Locale.US, "%.0f %s", wheelResult.pressurePsi, unit.symbol)
                PressureUnit.KPA -> String.format(Locale.US, "%.0f %s", wheelResult.pressureKpa, unit.symbol)
            }
        }
    }
}
