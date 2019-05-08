package dev.olog.msc.presentation.tabs.di

import dagger.Subcomponent
import dagger.android.AndroidInjector
import dev.olog.msc.core.dagger.scope.PerFragment
import dev.olog.msc.presentation.tabs.TabFragment

@Subcomponent(modules = arrayOf(
        TabFragmentModule::class,
        TabFragmentViewModelModule::class,
        TabFragmentPodcastModule::class
))
@PerFragment
interface TabFragmentSubComponent : AndroidInjector<TabFragment> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<TabFragment>()

}