package com.tirepressure.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.tirepressure.app.MainActivity
import com.tirepressure.app.R
import com.tirepressure.app.data.repository.PreferencesRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AllBikesWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
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
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val isDark = WidgetPreferencesHelper.isDarkTheme(context, appWidgetId)
            val rootLayoutId = if (isDark) R.layout.widget_all_bikes_dark else R.layout.widget_all_bikes_light
            val remoteViews = RemoteViews(context.packageName, rootLayoutId)

            val (locContext, _) = WidgetUpdateHelper.getWidgetLocalizedContext(context)

            val prefs = PreferencesRepositoryImpl(context)
            val accentColor = runBlocking { prefs.accentColorFlow.first() }
            val accentColorInt = accentColor.getPrimaryColorInt(isDark)

            remoteViews.setInt(R.id.widget_bike_icon, "setColorFilter", accentColorInt)
            remoteViews.setTextColor(R.id.widget_title_pressure, accentColorInt)
            remoteViews.setContentDescription(R.id.widget_bike_icon, locContext.getString(R.string.app_name))
            remoteViews.setContentDescription(R.id.widget_btn_settings, locContext.getString(R.string.widget_config_title))
            remoteViews.setTextViewText(R.id.widget_all_bikes_subtitle, locContext.getString(R.string.widget_all_bikes_name))
            remoteViews.setTextViewText(R.id.widget_empty_title, locContext.getString(R.string.widget_no_bikes))
            remoteViews.setTextViewText(R.id.widget_empty_desc, locContext.getString(R.string.widget_tap_to_open))

            // Service Intent for ListView
            val serviceIntent = Intent(context, AllBikesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            remoteViews.setRemoteAdapter(R.id.widget_all_bikes_list, serviceIntent)
            remoteViews.setEmptyView(R.id.widget_all_bikes_list, R.id.widget_all_bikes_empty)

            // PendingIntent Template for List items (mutable so fillInIntent can supply extras)
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            remoteViews.setPendingIntentTemplate(R.id.widget_all_bikes_list, clickPendingIntent)

            // Click on Empty View opens app
            val emptyPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 200000,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_all_bikes_empty, emptyPendingIntent)

            // Click on Settings opens AllBikesWidgetConfigureActivity
            val configIntent = Intent(context, AllBikesWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 300000,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_btn_settings, configPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_all_bikes_list)
        }
    }
}
