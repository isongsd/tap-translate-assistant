package com.ttt.liveassistant

import android.content.Context

object AppSettings {
    private const val PREFS_NAME = "tap_translate_settings"
    private const val KEY_TRANSLATION_ENDPOINT = "translation_endpoint"
    private const val KEY_SOURCE_LANGUAGE = "source_language"
    private const val KEY_TARGET_LANGUAGE = "target_language"
    private const val KEY_TRANSLATION_SUFFIX = "translation_suffix"
    private const val KEY_SPACE_BEFORE_SUFFIX = "space_before_suffix"

    fun translationEndpoint(context: Context): String =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TRANSLATION_ENDPOINT, "")
            .orEmpty()
            .trim()

    fun saveTranslationEndpoint(context: Context, value: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRANSLATION_ENDPOINT, value.trim())
            .apply()
    }

    fun translationSuffix(context: Context): String =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TRANSLATION_SUFFIX, "")
            .orEmpty()

    fun saveTranslationSuffix(context: Context, value: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRANSLATION_SUFFIX, value)
            .apply()
    }

    fun addSpaceBeforeSuffix(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SPACE_BEFORE_SUFFIX, true)

    fun saveAddSpaceBeforeSuffix(context: Context, value: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SPACE_BEFORE_SUFFIX, value)
            .apply()
    }

    fun formattedTranslationSuffix(context: Context): String {
        val suffix = translationSuffix(context)
        if (suffix.isBlank()) {
            return suffix
        }

        val alreadyHasLeadingSpace = suffix.firstOrNull()?.isWhitespace() == true
        return if (addSpaceBeforeSuffix(context) && !alreadyHasLeadingSpace) {
            " $suffix"
        } else {
            suffix
        }
    }

    fun sourceLanguage(context: Context): TranslationLanguage =
        TranslationLanguage.fromCode(
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SOURCE_LANGUAGE, TranslationLanguage.TraditionalChinese.code)
                .orEmpty(),
        )

    fun targetLanguage(context: Context): TranslationLanguage =
        TranslationLanguage.fromCode(
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TARGET_LANGUAGE, TranslationLanguage.SimplifiedChinese.code)
                .orEmpty(),
        )

    fun saveTranslationLanguages(
        context: Context,
        source: TranslationLanguage,
        target: TranslationLanguage,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOURCE_LANGUAGE, source.code)
            .putString(KEY_TARGET_LANGUAGE, target.code)
            .apply()
    }
}
