package uk.co.armedpineapple.innoextract

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uk.co.armedpineapple.innoextract.gogapi.GogApi
import uk.co.armedpineapple.innoextract.gogapi.OkHttpGogApi
import uk.co.armedpineapple.innoextract.services.DefaultFirstLaunchService
import uk.co.armedpineapple.innoextract.services.FirstLaunchService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    @Binds
    @Singleton
    abstract fun bindFirstLaunchService(service: DefaultFirstLaunchService): FirstLaunchService

    @Binds
    @Singleton
    abstract fun bindGogApi(api: OkHttpGogApi): GogApi

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

        @Provides
        @Singleton
        fun provideOkHttpGogApi(@ApplicationContext context: Context): OkHttpGogApi {
            return OkHttpGogApi(context = context)
        }
    }
}
