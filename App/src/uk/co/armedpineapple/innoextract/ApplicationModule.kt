package uk.co.armedpineapple.innoextract

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import uk.co.armedpineapple.innoextract.services.DefaultFirstLaunchService
import uk.co.armedpineapple.innoextract.services.FirstLaunchService
import javax.inject.Singleton

@Module
class ApplicationModule(private val application: BaseApplication) {

    @Provides
    @Singleton
    fun provideApplicationContext(): Context {
        return application
    }

    @Provides
    @Singleton
    fun provideFirstLaunchService(): FirstLaunchService {
        return DefaultFirstLaunchService(provideSharedPreferences())
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application)
    }
}