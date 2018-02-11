package dev.olog.msc.app

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dev.olog.msc.data.RepositoryHelperModule
import dev.olog.msc.data.RepositoryModule
import dev.olog.msc.data.prefs.PreferenceModule
import dev.olog.msc.floating.window.service.di.FloatingInfoServiceInjector
import dev.olog.msc.music.service.di.MusicServiceInjector
import dev.olog.msc.presentation.about.di.AboutActivityInjector
import dev.olog.msc.presentation.app.widget.WidgetBindingModule
import dev.olog.msc.presentation.main.di.MainActivityInjector
import dev.olog.msc.presentation.neural.network.di.NeuralNetworkActivityInjector
import dev.olog.msc.presentation.preferences.di.PreferencesActivityInjector
import dev.olog.msc.presentation.shortcuts.di.ShortcutsActivityInjector
import dev.olog.msc.presentation.splash.di.SplashActivityInjector
import dev.olog.msc.stylize.images.service.di.StylizeImageServiceInjector
import javax.inject.Singleton

@Component(modules = arrayOf(
        AppModule::class,
        SchedulersModule::class,
        WidgetBindingModule::class,

//        // data
        RepositoryModule::class,
        RepositoryHelperModule::class,
        PreferenceModule::class,
//
//        // presentation
        AndroidSupportInjectionModule::class,
        SplashActivityInjector::class,
        MainActivityInjector::class,
        AboutActivityInjector::class,
        ShortcutsActivityInjector::class,
        PreferencesActivityInjector::class,
        NeuralNetworkActivityInjector::class,

//        // music service
        MusicServiceInjector::class,
        EqualizerModule::class,

//        // floating info service
        FloatingInfoServiceInjector::class,

        // stylize service
        StylizeImageServiceInjector::class
))
@Singleton
interface AppComponent: AndroidInjector<App> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<App>() {

        internal abstract fun module(module: AppModule): Builder

        override fun seedInstance(instance: App) {
            module(AppModule(instance))
        }

    }

}