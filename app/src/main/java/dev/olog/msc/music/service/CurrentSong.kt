package dev.olog.msc.music.service

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import dev.olog.msc.dagger.PerService
import dev.olog.msc.dagger.ServiceLifecycle
import dev.olog.msc.domain.interactor.detail.most.played.InsertMostPlayedUseCase
import dev.olog.msc.domain.interactor.favorite.IsFavoriteSongUseCase
import dev.olog.msc.domain.interactor.music.service.InsertHistorySongUseCase
import dev.olog.msc.domain.interactor.prefs.MusicPreferencesUseCase
import dev.olog.msc.music.service.interfaces.PlayerLifecycle
import dev.olog.msc.music.service.model.MediaEntity
import dev.olog.msc.utils.MediaId
import dev.olog.msc.utils.k.extension.unsubscribe
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@PerService
class CurrentSong @Inject constructor(
        @ServiceLifecycle lifecycle: Lifecycle,
        insertMostPlayedUseCase: InsertMostPlayedUseCase,
        insertHistorySongUseCase: InsertHistorySongUseCase,
        private val musicPreferencesUseCase: MusicPreferencesUseCase,
        private val isFavoriteSongUseCase: IsFavoriteSongUseCase,
//        private val setFloatingInfoRequestUseCase: SetFloatingInfoRequestUseCase,
        playerLifecycle: PlayerLifecycle

) : DefaultLifecycleObserver {

    private val publisher = BehaviorProcessor.create<MediaEntity>()

    private val subscriptions = CompositeDisposable()
    private var isFavoriteDisposable : Disposable? = null

    private val insertToMostPlayedFlowable = publisher
            .observeOn(Schedulers.io())
            .flatMapMaybe { createMostPlayedId(it) }
            .flatMapCompletable { insertMostPlayedUseCase.execute(it) }

    private val insertHistorySongFlowable = publisher
            .observeOn(Schedulers.io())
            .flatMapCompletable { insertHistorySongUseCase.execute(it.id) }

    private val playerListener = object : PlayerLifecycle.Listener {
        override fun onPrepare(entity: MediaEntity) {
            updateFavorite(entity)
            saveLastMetadata(entity)
        }

        override fun onPlay(entity: MediaEntity) {
            publisher.onNext(entity)
            updateFavorite(entity)
            saveLastMetadata(entity)
        }
    }

    init {
        lifecycle.addObserver(this)

        playerLifecycle.addListener(playerListener)

        insertToMostPlayedFlowable.subscribe({}, Throwable::printStackTrace)
                .addTo(subscriptions)
        insertHistorySongFlowable.subscribe({}, Throwable::printStackTrace)
                .addTo(subscriptions)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        subscriptions.clear()
        isFavoriteDisposable.unsubscribe()
    }

    private fun updateFavorite(mediaEntity: MediaEntity){
        isFavoriteDisposable.unsubscribe()
        isFavoriteDisposable = isFavoriteSongUseCase
                .execute(mediaEntity.id)
                .subscribe()
    }

    private fun saveLastMetadata(entity: MediaEntity){
        musicPreferencesUseCase.setLastTitle(entity.title)
        musicPreferencesUseCase.setLastSubtitle(entity.artist)
    }

    private fun createMostPlayedId(entity: MediaEntity): Maybe<MediaId> {
        try {
            return Maybe.just(MediaId.playableItem(entity.mediaId, entity.id))
        } catch (ex: Exception){
            return Maybe.empty()
        }
    }

}