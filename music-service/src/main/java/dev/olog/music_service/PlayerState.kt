package dev.olog.music_service

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import dev.olog.domain.interactor.music_service.BookmarkUseCase
import dev.olog.domain.interactor.music_service.ToggleSkipToNextVisibilityUseCase
import dev.olog.domain.interactor.music_service.ToggleSkipToPreviousVisibilityUseCase
import dev.olog.music_service.di.PerService
import dev.olog.music_service.model.PositionInQueue
import dev.olog.shared.ApplicationContext
import dev.olog.shared_android.AppShortcutInfo
import dev.olog.shared_android.WidgetConstants
import dev.olog.shared_android.extension.getAppWidgetsIdsFor
import dev.olog.shared_android.interfaces.ShortcutActivityClass
import dev.olog.shared_android.interfaces.WidgetClasses
import dev.olog.shared_android.isNougat_MR1
import javax.inject.Inject

@PerService
class PlayerState @Inject constructor(
        @ApplicationContext private val context: Context,
        private val mediaSession: MediaSessionCompat,
        private val bookmarkUseCase: BookmarkUseCase,
        private val toggleSkipToNextVisibilityUseCase: ToggleSkipToNextVisibilityUseCase,
        private val toggleSkipToPreviousVisibilityUseCase: ToggleSkipToPreviousVisibilityUseCase,
        private val widgetClasses: WidgetClasses,
        private val shortcutActivityClass: ShortcutActivityClass

){

    private val shortcutManager: ShortcutManager by lazy {
        context.getSystemService(ShortcutManager::class.java) as ShortcutManager
    }

    private val builder = PlaybackStateCompat.Builder()
    private var activeQueueId = MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()

    init {
        builder.setState(PlaybackStateCompat.STATE_PAUSED, bookmarkUseCase.get(), 0f)
                .setActions(getActions())
    }

    fun prepare(id: Long, bookmark: Long) {
        builder.setActiveQueueItemId(id)
        mediaSession.setPlaybackState(builder.build())

        notifyWidgetsOfStateChanged(false, bookmark)
    }

    fun update(state: Int, bookmark: Long): PlaybackStateCompat {
        return update(state, bookmark, null)
    }

    /**
     * @param state one of: PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_PAUSED
     */
    fun update(state: Int, bookmark: Long, id: Long?): PlaybackStateCompat {
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING

        if (isPlaying){
            disablePlayShortcut()
        } else {
            enablePlayShortcut()
        }

        builder.setState(state, bookmark, (if (isPlaying) 1 else 0).toFloat())

        bookmarkUseCase.set(bookmark)

        if (id != null) {
            activeQueueId = id
            builder.setActiveQueueItemId(activeQueueId)
        }

        val playbackState = builder.build()
        mediaSession.setPlaybackState(playbackState)

        notifyWidgetsOfStateChanged(isPlaying, bookmark)

        return playbackState
    }

    fun toggleSkipToActions(positionInQueue: PositionInQueue) {

        when {
            positionInQueue === PositionInQueue.FIRST -> {
                toggleSkipToPreviousVisibilityUseCase.set(false)
                toggleSkipToNextVisibilityUseCase.set(true)
                notifyWidgetsActionChanged(false, true)
            }
            positionInQueue === PositionInQueue.LAST -> {
                toggleSkipToPreviousVisibilityUseCase.set(true)
                toggleSkipToNextVisibilityUseCase.set(false)
                notifyWidgetsActionChanged(true, false)
            }
            positionInQueue === PositionInQueue.IN_MIDDLE -> {
                toggleSkipToNextVisibilityUseCase.set(true)
                toggleSkipToPreviousVisibilityUseCase.set(true)
                notifyWidgetsActionChanged(true, true)
            }
            positionInQueue == PositionInQueue.BOTH -> {
                toggleSkipToNextVisibilityUseCase.set(false)
                toggleSkipToPreviousVisibilityUseCase.set(false)
                notifyWidgetsActionChanged(false, false)
            }
        }

    }

    fun skipTo(toNext: Boolean) {
        val state = if (toNext) {
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT
        } else {
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS
        }
        builder.setState(state, 0, 1f)

        mediaSession.setPlaybackState(builder.build())
    }

    fun setEmptyQueue(){
        val localBuilder = PlaybackStateCompat.Builder(builder.build())
        localBuilder.setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, context.getString(R.string.error_empty_queue))

        mediaSession.setPlaybackState(localBuilder.build())
    }

    private fun getActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
//                PlaybackStateCompat.ACTION_SET_RATING or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    }

    private fun notifyWidgetsOfStateChanged(isPlaying: Boolean, bookmark: Long){
        for (clazz in widgetClasses.get()) {
            val ids = context.getAppWidgetsIdsFor(clazz)

            val intent = Intent(context, clazz).apply {
                action = WidgetConstants.STATE_CHANGED
                putExtra(WidgetConstants.ARGUMENT_IS_PLAYING, isPlaying)
                putExtra(WidgetConstants.ARGUMENT_BOOKMARK, bookmark)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }

            context.sendBroadcast(intent)
        }
    }

    private fun notifyWidgetsActionChanged(showPrevious: Boolean, showNext: Boolean){
        for (clazz in widgetClasses.get()) {
            val ids = context.getAppWidgetsIdsFor(clazz)

            val intent = Intent(context, clazz).apply {
                action = WidgetConstants.ACTION_CHANGED
                putExtra(WidgetConstants.ARGUMENT_SHOW_PREVIOUS, showPrevious)
                putExtra(WidgetConstants.ARGUMENT_SHOW_NEXT, showNext)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }

            context.sendBroadcast(intent)
        }
    }

    @SuppressLint("NewApi")
    private fun disablePlayShortcut(){
        if (isNougat_MR1()){
            shortcutManager.removeDynamicShortcuts(listOf(AppShortcutInfo.SHORTCUT_PLAY))
        }
    }

    @SuppressLint("NewApi")
    private fun enablePlayShortcut(){
        if (isNougat_MR1()){
            shortcutManager.addDynamicShortcuts(
                    listOf(AppShortcutInfo.play(context, shortcutActivityClass.get())))
        }
    }

}