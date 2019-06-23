package dev.olog.data.repository.podcast

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import dev.olog.core.dagger.ApplicationContext
import dev.olog.core.entity.track.Artist
import dev.olog.core.entity.track.Song
import dev.olog.core.gateway.HasLastPlayed
import dev.olog.core.gateway.Id
import dev.olog.core.gateway.PodcastArtistGateway2
import dev.olog.core.prefs.BlacklistPreferences
import dev.olog.core.prefs.SortPreferences
import dev.olog.data.db.dao.AppDatabase
import dev.olog.data.mapper.toArtist
import dev.olog.data.queries.ArtistQueries
import dev.olog.data.repository.BaseRepository
import dev.olog.data.repository.ContentUri
import dev.olog.data.utils.queryAll
import dev.olog.shared.assertBackground
import dev.olog.shared.assertBackgroundThread
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.flow.asFlow
import javax.inject.Inject

internal class PodcastArtistRepository2 @Inject constructor(
    @ApplicationContext context: Context,
    sortPrefs: SortPreferences,
    blacklistPrefs: BlacklistPreferences,
    appDatabase: AppDatabase
) : BaseRepository<Artist, Id>(context), PodcastArtistGateway2 {

    private val queries = ArtistQueries(contentResolver, blacklistPrefs, sortPrefs, true)
    private val lastPlayedDao = appDatabase.lastPlayedArtistDao()

    override fun registerMainContentUri(): ContentUri {
        return ContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
    }

    private fun extractArtists(cursor: Cursor): List<Artist> {
        assertBackgroundThread()
        return context.contentResolver.queryAll(cursor) { it.toArtist() }
            .groupBy { it.id }
            .map { (_, list) ->
                val artist = list[0]
                artist.copy(songs = list.size)
            }
    }

    override fun queryAll(): List<Artist> {
        assertBackgroundThread()
        val cursor = queries.getAll()
        return extractArtists(cursor)
    }

    override fun getByParam(param: Id): Artist? {
        assertBackgroundThread()
        return channel.valueOrNull?.find { it.id == param }
    }

    override fun observeByParam(param: Id): Flow<Artist?> {
        return channel.asFlow().map { list -> list.find { it.id == param } }
            .assertBackground()
    }

    override fun getTrackListByParam(param: Id): List<Song> {
        return listOf()
    }

    override fun observeTrackListByParam(param: Id): Flow<List<Song>> {
        return flow { }
    }

    override fun observeLastPlayed(): Flow<List<Artist>> {
        return observeAll().combineLatest(lastPlayedDao.getAll().asFlow()) { all, lastPlayed ->
            if (all.size < HasLastPlayed.MIN_ITEMS) {
                listOf() // too few album to show recents
            } else {
                lastPlayed.asSequence()
                    .mapNotNull { last -> all.firstOrNull { it.id == last.id } }
                    .take(HasLastPlayed.MAX_ITEM_TO_SHOW)
                    .toList()
            }
        }.assertBackground()
    }

    override suspend fun addLastPlayed(id: Id) {
        assertBackgroundThread()
        lastPlayedDao.insertOne(id)
    }

    override fun observeRecentlyAdded(): Flow<List<Artist>> {
        val cursor = queries.getRecentlyAdded()
        return observeByParamInternal(ContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)) {
            extractArtists(cursor)
        }.distinctUntilChanged()
            .assertBackground()
    }
}