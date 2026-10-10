package com.tirepressure.app.widget

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration

object WidgetPreferencesHelper {

    private const val PREFS_NAME = "tire_pressure_widget_prefs"
    private const val KEY_THEME_PREFIX = "widget_theme_"
    private const val KEY_DEFAULT_THEME = "default_widget_theme"
    private const val KEY_BIKE_PREFIX = "widget_bike_id_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getWidgetTheme(context: Context, appWidgetId: Int): WidgetTheme {
        val prefs = getPrefs(context)
        val defaultTheme = getDefaultWidgetTheme(context)
        val name = prefs.getString("$KEY_THEME_PREFIX$appWidgetId", defaultTheme.name) ?: defaultTheme.name
        return runCatching { WidgetTheme.valueOf(name) }.getOrDefault(defaultTheme)
    }

    fun setWidgetTheme(context: Context, appWidgetId: Int, theme: WidgetTheme) {
        getPrefs(context).edit()
            .putString("$KEY_THEME_PREFIX$appWidgetId", theme.name)
            .apply()
    }

    fun getDefaultWidgetTheme(context: Context): WidgetTheme {
        val name = getPrefs(context).getString(KEY_DEFAULT_THEME, WidgetTheme.SYSTEM.name) ?: WidgetTheme.SYSTEM.name
        return runCatching { WidgetTheme.valueOf(name) }.getOrDefault(WidgetTheme.SYSTEM)
    }

    fun setDefaultWidgetTheme(context: Context, theme: WidgetTheme) {
        getPrefs(context).edit()
            .putString(KEY_DEFAULT_THEME, theme.name)
            .apply()
    }

    fun getSingleBikeWidgetBikeId(context: Context, appWidgetId: Int): String? {
        return getPrefs(context).getString("$KEY_BIKE_PREFIX$appWidgetId", null)
    }

    fun setSingleBikeWidgetBikeId(context: Context, appWidgetId: Int, bikeId: String) {
        getPrefs(context).edit()
            .putString("$KEY_BIKE_PREFIX$appWidgetId", bikeId)
            .apply()
    }

    fun deleteWidgetPrefs(context: Context, appWidgetId: Int) {
        getPrefs(context).edit()
            .remove("$KEY_THEME_PREFIX$appWidgetId")
            .remove("$KEY_BIKE_PREFIX$appWidgetId")
            .apply()
    }

    fun isDarkTheme(context: Context, appWidgetId: Int): Boolean {
        return when (getWidgetTheme(context, appWidgetId)) {
            WidgetTheme.DARK -> true
            WidgetTheme.LIGHT -> false
            WidgetTheme.SYSTEM -> {
                val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
