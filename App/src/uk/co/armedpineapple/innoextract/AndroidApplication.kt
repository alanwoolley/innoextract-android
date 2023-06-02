package uk.co.armedpineapple.innoextract

import android.app.Application

abstract class BaseApplication : Application() {

    fun initDaggerComponent(): ApplicationComponent {
        return DaggerApplicationComponent.builder().applicationModule(ApplicationModule(this))
            .build()
    }
}

class AndroidApplication : BaseApplication() {
    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        val component = initDaggerComponent()
        component.inject(this)
        this.component = component
    }
}

