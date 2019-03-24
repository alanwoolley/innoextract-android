package uk.co.armedpineapple.innoextract

import dagger.Component
import uk.co.armedpineapple.innoextract.services.FirstLaunchService
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
    fun inject(application: AndroidApplication)
    fun inject(firstLaunchService: FirstLaunchService)
    fun inject(mainActivity: MainActivity)
}