package dev.olog.presentation.widget_home_screen

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.android.AndroidInjection
import dev.olog.shared_android.WidgetConstants

abstract class AbsWidgetApp : AppWidgetProvider() {

    companion object {
        private var metadata : WidgetMetadata? = null
        private var state : WidgetState? = null
        private var actions: WidgetActions? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)

        when (intent.action){
            WidgetConstants.METADATA_CHANGED -> {

                val appWidgetIds = intent.extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()){

                    val id = intent.getLongExtra(WidgetConstants.ARGUMENT_SONG_ID, 0)
                    val title = intent.getStringExtra(WidgetConstants.ARGUMENT_TITLE)
                    val subtitle = intent.getStringExtra(WidgetConstants.ARGUMENT_SUBTITLE)
                    val image = intent.getStringExtra(WidgetConstants.ARGUMENT_IMAGE)
                    val duration = intent.getLongExtra(WidgetConstants.ARGUMENT_DURATION, 0)
                    metadata = WidgetMetadata(id, title, subtitle, image, duration)
                    onMetadataChanged(context, metadata!!, appWidgetIds)
                }
            }
            WidgetConstants.STATE_CHANGED -> {
                val appWidgetIds = intent.extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()){
                    val isPlaying = intent.getBooleanExtra(WidgetConstants.ARGUMENT_IS_PLAYING, false)
//                    val bookmark = intent.getLongExtra(WidgetConstants.ARGUMENT_BOOKMARK, 0)
                    state = WidgetState(isPlaying/*, bookmark*/)
                    onPlaybackStateChanged(context, state!!, appWidgetIds)
                }
            }
            WidgetConstants.ACTION_CHANGED -> {
                val appWidgetIds = intent.extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()){
                    val showPrevious = intent.getBooleanExtra(WidgetConstants.ARGUMENT_SHOW_PREVIOUS, true)
                    val showNext = intent.getBooleanExtra(WidgetConstants.ARGUMENT_SHOW_NEXT, true)
                    actions = WidgetActions(showPrevious, showNext)
                    onActionVisibilityChanged(context, actions!!, appWidgetIds)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                // when a new widget is added
                metadata?.let {
                    val appWidgetIds = intent.extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()){
                        onMetadataChanged(context, it, appWidgetIds)
                    }
                }
                state?.let {
                    val appWidgetIds = intent.extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()){
                        onPlaybackStateChanged(context, it, appWidgetIds)
                    }
                }
                actions?.let {
                    val appWidgetIds = intent.extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()){
                        onActionVisibilityChanged(context, it, appWidgetIds)
                    }
                }
            }
        }
    }

    protected abstract fun onActionVisibilityChanged(context: Context, actions: WidgetActions, appWidgetIds: IntArray)

    protected abstract fun onMetadataChanged(context: Context, metadata: WidgetMetadata, appWidgetIds: IntArray)

    protected abstract fun onPlaybackStateChanged(context: Context, state: WidgetState, appWidgetIds: IntArray)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        val newSize = WidgetSize(minWidth, maxWidth, minHeight, maxHeight)

        onSizeChanged(context, appWidgetManager, appWidgetId, newSize)
    }

    /**
     * height:  tiles   min max
     *          1 tile  58  100
     *          2 tile  133 216
     *          3 tile  208  332
     *
     * width:  tiles   min max
     *         4 tile  395 612
     *         3 tile  313 486
     *         2 tile  58  100
     */
    protected abstract fun onSizeChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, size: WidgetSize)

}

