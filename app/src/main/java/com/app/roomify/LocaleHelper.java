package com.app.roomify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "AppSettings";
    private static final String LANGUAGE_KEY = "App_Language";

    public static void setLocale(Context context, String languageCode) {
        saveLanguage(context, languageCode);
        updateResources(context, languageCode);
    }

    public static void loadLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String language = prefs.getString(LANGUAGE_KEY, "en");
        updateResources(context, language);
    }

    private static void updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0 and above
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            context.createConfigurationContext(config);
        } else {
            // Older Android versions
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private static void saveLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LANGUAGE_KEY, language);
        editor.apply();
    }

    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LANGUAGE_KEY, "en");
    }

    // IMPORTANT: This method restarts the activity to apply language changes
    public static void changeLanguage(Activity activity, String languageCode) {
        if (getCurrentLanguage(activity).equals(languageCode)) {
            return; // Already using this language
        }

        setLocale(activity, languageCode);

        // Restart the activity to apply changes
        Intent intent = activity.getIntent();
        activity.finish();
        activity.startActivity(intent);

        // Optional: Add animation
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}