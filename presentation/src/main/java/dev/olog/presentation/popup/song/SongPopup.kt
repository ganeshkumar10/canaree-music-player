package dev.olog.presentation.popup.song

import android.view.View
import dev.olog.presentation.R
import dev.olog.intents.AppConstants
import dev.olog.core.entity.track.Song
import dev.olog.presentation.popup.AbsPopup
import dev.olog.presentation.popup.AbsPopupListener

class SongPopup(
    view: View,
    song: Song,
    listener: AbsPopupListener

) : AbsPopup(view) {

    init {
        inflate(R.menu.dialog_song)

        addPlaylistChooser(view.context, listener.playlists)

        setOnMenuItemClickListener(listener)

        if (song.artist == AppConstants.UNKNOWN){
            menu.removeItem(R.id.viewArtist)
        }
        if (song.album == AppConstants.UNKNOWN){
            menu.removeItem(R.id.viewAlbum)
        }
    }

}