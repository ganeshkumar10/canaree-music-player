package dev.olog.msc.presentation.detail.di

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.olog.msc.core.MediaId
import dev.olog.msc.core.dagger.qualifier.ApplicationContext
import dev.olog.msc.core.entity.podcast.PodcastArtist
import dev.olog.msc.core.entity.sort.SortType
import dev.olog.msc.core.entity.track.Artist
import dev.olog.msc.core.entity.track.Song
import dev.olog.msc.core.gateway.PlaylistGateway
import dev.olog.msc.core.gateway.PodcastPlaylistGateway
import dev.olog.msc.presentation.detail.domain.GetPodcastRelatedArtistsUseCase
import dev.olog.msc.presentation.detail.domain.GetRelatedArtistsUseCase
import dev.olog.msc.core.interactor.added.GetRecentlyAddedUseCase
import dev.olog.msc.core.interactor.played.GetMostPlayedSongsUseCase
import dev.olog.msc.core.interactor.sort.GetSortOrderUseCase
import dev.olog.msc.core.interactor.sort.GetSortedSongListByParamUseCase
import dev.olog.msc.presentation.detail.DetailFragmentViewModel
import dev.olog.msc.presentation.detail.R
import dev.olog.msc.presentation.detail.domain.GetTotalSongDurationUseCase
import dev.olog.msc.shared.TrackUtils
import dev.olog.msc.shared.extensions.mapToList
import dev.olog.msc.shared.ui.TimeUtils
import dev.olog.msc.shared.utils.TextUtils
import dev.olog.presentation.base.model.DisplayableItem
import io.reactivex.Observable

@Module
class DetailFragmentModuleSongs {

    @Provides
    @IntoMap
    @StringKey(DetailFragmentViewModel.RECENTLY_ADDED)
    internal fun provideRecentlyAdded(
            mediaId: MediaId,
            useCase: GetRecentlyAddedUseCase) : Observable<List<DisplayableItem>> {

        return useCase.execute(mediaId)
                .mapToList { it.toRecentDetailDisplayableItem(mediaId) }
    }

    @Provides
    @IntoMap
    @StringKey(DetailFragmentViewModel.MOST_PLAYED)
    internal fun provideMostPlayed(
            mediaId: MediaId,
            useCase: GetMostPlayedSongsUseCase) : Observable<List<DisplayableItem>> {

        return useCase.execute(mediaId)
                .mapToList { it.toMostPlayedDetailDisplayableItem(mediaId) }
    }

    @Provides
    @IntoMap
    @StringKey(DetailFragmentViewModel.SONGS)
    internal fun provideSongList(
            @ApplicationContext context: Context,
            mediaId: MediaId,
            useCase: GetSortedSongListByParamUseCase,
            sortOrderUseCase: GetSortOrderUseCase,
            songDurationUseCase: GetTotalSongDurationUseCase) : Observable<List<DisplayableItem>> {

        return useCase.execute(mediaId)
                .flatMapSingle { songList ->
                    sortOrderUseCase.execute(mediaId)
                            .firstOrError()
                            .map { sort -> songList.map { it.toDetailDisplayableItem(mediaId, sort) } }
                }
                .flatMapSingle { songList -> songDurationUseCase.execute(mediaId)
                .map { createDurationFooter(context, songList.size, it) }
                .map {
                    if (songList.isNotEmpty()){
                        val list = songList.toMutableList()
                        list.add(it)
                        list
                    } else mutableListOf()
                }
        }
    }

    @Provides
    @IntoMap
    @StringKey(DetailFragmentViewModel.RELATED_ARTISTS)
    internal fun provideRelatedArtists(
            resources: Resources,
            mediaId: MediaId,
            useCase: GetRelatedArtistsUseCase,
            podcastUseCase: GetPodcastRelatedArtistsUseCase): Observable<List<DisplayableItem>> {

        if (mediaId.isPodcastPlaylist){
            return podcastUseCase.execute(mediaId).mapToList { it.toRelatedArtist(resources) }
        }

        return useCase.execute(mediaId)
                .mapToList { it.toRelatedArtist(resources)}
    }

}

private fun createDurationFooter(context: Context, songCount: Int, duration: Int): DisplayableItem {
    val songs = DisplayableItem.handleSongListSize(context.resources, songCount)
    val time = TimeUtils.formatMillis(context, duration)

    return DisplayableItem(R.layout.item_detail_footer, MediaId.headerId("duration footer"),
            songs + TextUtils.MIDDLE_DOT_SPACED + time)
}

private fun Artist.toRelatedArtist(resources: Resources): DisplayableItem {
    val songs = DisplayableItem.handleSongListSize(resources, songs)
    var albums = DisplayableItem.handleAlbumListSize(resources, albums)
    if (albums.isNotBlank()) albums+= TextUtils.MIDDLE_DOT_SPACED

    return DisplayableItem(
            R.layout.item_detail_related_artist,
            MediaId.artistId(this.id),
            this.name,
            albums + songs,
            this.image
    )
}

private fun PodcastArtist.toRelatedArtist(resources: Resources): DisplayableItem {
    val songs = DisplayableItem.handleSongListSize(resources, songs)
    var albums = DisplayableItem.handleAlbumListSize(resources, albums)
    if (albums.isNotBlank()) albums+= TextUtils.MIDDLE_DOT_SPACED

    return DisplayableItem(
            R.layout.item_detail_related_artist,
            MediaId.podcastArtistId(this.id),
            this.name,
            albums + songs,
            this.image
    )
}

private fun Song.toDetailDisplayableItem(parentId: MediaId, sortType: SortType): DisplayableItem {
    val viewType = when {
        parentId.isAlbum || parentId.isPodcastAlbum -> R.layout.item_detail_song_with_track
        (parentId.isPlaylist || parentId.isPodcastPlaylist) && sortType == SortType.CUSTOM -> {
            val playlistId = parentId.categoryValue.toLong()
            if (PlaylistGateway.isAutoPlaylist(playlistId) || PodcastPlaylistGateway.isPodcastAutoPlaylist(playlistId)) {
                R.layout.item_detail_song
            } else R.layout.item_detail_song_with_drag_handle
        }
        parentId.isFolder && sortType == SortType.TRACK_NUMBER -> R.layout.item_detail_song_with_track_and_image
        else -> R.layout.item_detail_song
    }

    val subtitle = when {
        parentId.isArtist || parentId.isPodcastArtist -> TrackUtils.adjustAlbum(this.album)
        else -> TrackUtils.adjustArtist(this.artist)
    }

    val track = when {
        parentId.isPlaylist || parentId.isPodcastPlaylist -> this.trackNumber.toString()
        this.trackNumber == 0 -> "-"
        else -> this.trackNumber.toString()
    }

    return DisplayableItem(
            viewType,
            MediaId.playableItem(parentId, id),
            this.title,
            subtitle,
            this.image,
            true,
            track
    )
}

private fun Song.toMostPlayedDetailDisplayableItem(parentId: MediaId): DisplayableItem {
    return DisplayableItem(
            R.layout.item_detail_song_most_played,
            MediaId.playableItem(parentId, id),
            this.title,
            TrackUtils.adjustArtist(this.artist),
            this.image,
            true
    )
}

private fun Song.toRecentDetailDisplayableItem(parentId: MediaId): DisplayableItem {
    return DisplayableItem(
            R.layout.item_detail_song_recent,
            MediaId.playableItem(parentId, id),
            this.title,
            TrackUtils.adjustArtist(this.artist),
            this.image,
            true
    )
}