package dev.olog.msc.music.service.notification

import android.support.v4.media.session.PlaybackStateCompat
import dev.olog.msc.music.service.model.MediaEntity
import java.util.concurrent.TimeUnit

data class MusicNotificationState (
        var id: Long = -1,
        var title: String = "",
        var artist: String = "",
        var album: String = "",
        var image: String = "",
        var isPlaying: Boolean = false,
        var bookmark: Long = -1
) {

    private fun isValidState(): Boolean{
        return id != -1L &&
                title.isNotBlank() &&
                artist.isNotBlank() &&
                album.isNotBlank() &&
                image.isNotBlank() &&
                bookmark != -1L
    }

    fun updateMetadata(metadata: MediaEntity): Boolean {
        this.id = metadata.id
        this.title = metadata.title
        this.artist = metadata.artist
        this.album = metadata.album
        this.image = metadata.image
        return isValidState()
    }

    fun updateState(state: PlaybackStateCompat): Boolean {
        this.isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        this.bookmark = state.position
        return isValidState()
    }

    fun isDifferentMetadata(metadata: MediaEntity): Boolean {
        return this.id != metadata.id ||
                this.title != metadata.title ||
                this.artist != metadata.artist ||
                this.album != metadata.album ||
                this.image != metadata.image
    }

    fun isDifferentState(state: PlaybackStateCompat): Boolean{
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val bookmark = TimeUnit.MILLISECONDS.toSeconds(state.position)
        return this.isPlaying != isPlaying ||
                TimeUnit.MILLISECONDS.toSeconds(this.bookmark) != bookmark
    }

}