package dev.olog.presentation

import android.graphics.Typeface
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Priority
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import dev.olog.image.provider.CoverUtils
import dev.olog.image.provider.GlideApp
import dev.olog.presentation.model.DisplayableFile
import dev.olog.presentation.model.DisplayableItem
import dev.olog.presentation.model.DisplayableQueueSong
import dev.olog.presentation.model.SpecialThanksModel
import dev.olog.presentation.ripple.RippleTarget
import dev.olog.presentation.widgets.QuickActionView

object BindingsAdapter {

    private const val OVERRIDE_SMALL = 150
    private const val OVERRIDE_MID = 400

    @JvmStatic
    @BindingAdapter("fileTrackLoader")
    fun loadFile(view: ImageView, item: DisplayableFile){
        val context = view.context
        GlideApp.with(context).clear(view)

//        GlideApp.with(context)
//                .load(AudioFileCover(item.path!!))
//                .override(OVERRIDE_SMALL)
//                .placeholder(CoverUtils.getGradient(context, MediaId.songId(item.path.hashCode().toLong())))
//                .transition(DrawableTransitionOptions.withCrossFade())
//                .into(view) TODO move to glide module
    }

    @JvmStatic
    @BindingAdapter("fileDirLoader")
    fun loadDirImage(view: ImageView, item: DisplayableFile){
        val path = item.path ?: ""
        val displayableItem = DisplayableItem(
            0, MediaId.createCategoryValue(MediaIdCategory.FOLDERS, path),
            "", ""
        )
        loadImageImpl(
            view,
            displayableItem,
            OVERRIDE_SMALL
        )
    }

    @JvmStatic
    private fun loadImageImpl(
        view: ImageView,
        item: DisplayableItem,
        override: Int,
        priority: Priority = Priority.HIGH,
        crossfade: Boolean = true){

        val mediaId = item.mediaId
        val context = view.context

        GlideApp.with(context).clear(view)

        var builder = GlideApp.with(context)
                .load(mediaId)
                .override(override)
                .priority(priority)
                .placeholder(CoverUtils.getGradient(context, mediaId))
        if (crossfade){
            builder = builder.transition(DrawableTransitionOptions.withCrossFade())
        }
        if (mediaId.isLeaf) {
            builder.into(view)
        } else {
            builder.into(RippleTarget(view))
        }
    }

    @BindingAdapter("imageSong")
    @JvmStatic
    fun loadSongImage(view: ImageView, item: DisplayableItem) {
        loadImageImpl(
            view,
            item,
            OVERRIDE_SMALL
        )
    }

    @BindingAdapter("imageSong")
    @JvmStatic
    fun loadSongImage(view: ImageView, item: DisplayableQueueSong) {
        loadImageImpl(
            view,
            DisplayableItem(item.type, item.mediaId, "", ""), OVERRIDE_SMALL
        )
    }

    @BindingAdapter("imageAlbum")
    @JvmStatic
    fun loadAlbumImage(view: ImageView, item: DisplayableItem) {
        loadImageImpl(
            view,
            item,
            OVERRIDE_MID,
            Priority.HIGH
        )
    }

    @BindingAdapter("imageBigAlbum")
    @JvmStatic
    fun loadBigAlbumImage(view: ImageView, item: DisplayableItem) {
        loadImageImpl(
            view,
            item,
            Target.SIZE_ORIGINAL,
            Priority.IMMEDIATE,
            crossfade = false
        )
    }

    @BindingAdapter("imageSpecialThanks")
    @JvmStatic
    fun loadSongImage(view: ImageView, item: SpecialThanksModel) {
        GlideApp.with(view)
                .load(ContextCompat.getDrawable(view.context, item.image))
                .into(view)
    }

    @BindingAdapter("setBoldIfTrue")
    @JvmStatic
    fun setBoldIfTrue(view: TextView, setBold: Boolean){
        val style = if (setBold) Typeface.BOLD else Typeface.NORMAL
        view.setTypeface(null, style)
    }

    @BindingAdapter("quickActionItem")
    @JvmStatic
    fun quickActionItem(view: QuickActionView, item: DisplayableItem){
        view.setId(item.mediaId)
    }

}