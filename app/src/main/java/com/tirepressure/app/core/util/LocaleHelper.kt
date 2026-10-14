package com.tirepressure.app.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.tirepressure.app.core.model.AppLanguage
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_APP_LANGUAGE = "app_language"

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_APP_LANGUAGE, null) ?: return AppLanguage.SYSTEM
        return runCatching { AppLanguage.valueOf(raw) }.getOrDefault(AppLanguage.SYSTEM)
    }

    fun resolveLocale(context: Context, language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.FRENCH -> Locale.FRENCH
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.SYSTEM -> {
                val sysLoc = Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
                if (sysLoc.language.startsWith("fr")) Locale.FRENCH else Locale.ENGLISH
            }
        }
    }

    fun getSavedLocale(context: Context): Locale {
        return resolveLocale(context, getSavedLanguage(context))
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        applyLocale(context, resolveLocale(context, language))
    }

    fun wrapContext(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLocale(context: Context, locale: Locale) {
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        val appRes = context.applicationContext.resources
        if (appRes !== res) {
            val appConfig = Configuration(appRes.configuration)
            appConfig.setLocale(locale)
            appConfig.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        }

        // Synchronize with Android 13+ OS-level Per-App Language Manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val savedLang = getSavedLanguage(context)
                val targetLocaleList = when (savedLang) {
                    AppLanguage.FRENCH -> LocaleList(Locale.FRENCH)
                    AppLanguage.ENGLISH -> LocaleList(Locale.ENGLISH)
                    AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
                }
                if (localeManager != null && localeManager.applicationLocales != targetLocaleList) {
                    localeManager.applicationLocales = targetLocaleList
                }
            } catch (_: Exception) {
                // Ignore any system/security exception on vendor ROMs
            }
        }
    }
}

