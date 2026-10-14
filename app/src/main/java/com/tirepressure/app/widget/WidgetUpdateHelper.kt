package com.tirepressure.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.tirepressure.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        // 1. Single Bike Widgets
        val singleBikeComponent = ComponentName(context, SingleBikeWidgetReceiver::class.java)
        val singleBikeIds = appWidgetManager.getAppWidgetIds(singleBikeComponent)
        if (singleBikeIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                for (id in singleBikeIds) {
                    SingleBikeWidgetReceiver.updateWidget(context, appWidgetManager, id)
                }
            }
        }

        // 2. All Bikes Widgets
        val allBikesComponent = ComponentName(context, AllBikesWidgetReceiver::class.java)
        val allBikesIds = appWidgetManager.getAppWidgetIds(allBikesComponent)
        if (allBikesIds.isNotEmpty()) {
            for (id in allBikesIds) {
                AllBikesWidgetReceiver.updateWidget(context, appWidgetManager, id)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(allBikesIds, R.id.widget_all_bikes_list)
        }
    }

    fun getWidgetLocalizedContext(context: Context): Pair<Context, java.util.Locale> {
        val sysLocale = android.content.res.Resources.getSystem().configuration.locales[0] ?: java.util.Locale.getDefault()
        val targetLocale = if (sysLocale.language.startsWith("fr", ignoreCase = true)) java.util.Locale.FRENCH else java.util.Locale.ENGLISH
        val config = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
        return Pair(context.createConfigurationContext(config), targetLocale)
    }
}
