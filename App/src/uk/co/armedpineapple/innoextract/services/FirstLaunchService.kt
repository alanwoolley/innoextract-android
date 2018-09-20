package uk.co.armedpineapple.innoextract.services

import android.content.Context
import android.content.SharedPreferences
import org.jetbrains.anko.defaultSharedPreferences
import uk.co.armedpineapple.innoextract.BuildConfig

private const val launchVersionPreference = "lastlaunch_version"

interface FirstLaunchService {
    val isFirstLaunch: Boolean
    val isFirstLaunchForVersion: Boolean
}

class DefaultFirstLaunchService(private val context: Context) : FirstLaunchService {

    override val isFirstLaunch: Boolean
    override val isFirstLaunchForVersion: Boolean

    init {
        isFirstLaunch = !preferences.contains(launchVersionPreference)

        val thisVersion = BuildConfig.VERSION_CODE
        val lastLaunchedVersion = preferences.getInt(launchVersionPreference, 0)

        isFirstLaunchForVersion = thisVersion > lastLaunchedVersion

        preferences.edit().putInt(launchVersionPreference, thisVersion).apply()
    }

    private val preferences: SharedPreferences
        get() = context.defaultSharedPreferences
}