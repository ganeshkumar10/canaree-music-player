package dev.olog.data.queries

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore.Audio.Media.*
import dev.olog.contentresolversql.querySql
import dev.olog.core.entity.sort.SortArranging
import dev.olog.core.entity.sort.SortType
import dev.olog.core.gateway.Id
import dev.olog.core.prefs.BlacklistPreferences
import dev.olog.core.prefs.SortPreferences

internal class AlbumsQueries(
    private val contentResolver: ContentResolver,
    blacklistPrefs: BlacklistPreferences,
    sortPrefs: SortPreferences,
    isPodcast: Boolean
) : BaseQueries(blacklistPrefs, sortPrefs, isPodcast) {

    fun getAll(): Cursor {
        val query = """
             SELECT
                $ALBUM_ID,
                $ARTIST_ID,
                $ARTIST,
                $ALBUM,
                ${Columns.ALBUM_ARTIST},
                $DATA,
                $IS_PODCAST
            FROM $EXTERNAL_CONTENT_URI
            WHERE ${defaultSelection()}
            ORDER BY ${sortOrder()}
        """

        return contentResolver.querySql(query)
    }

    fun getById(id: Id): Cursor {
        val query = """
             SELECT
                $ALBUM_ID,
                $ARTIST_ID,
                $ARTIST,
                $ALBUM,
                ${Columns.ALBUM_ARTIST},
                $DATA,
                $IS_PODCAST
            FROM $EXTERNAL_CONTENT_URI
            WHERE $ALBUM_ID = ? AND ${defaultSelection()}
            ORDER BY ${sortOrder()}
        """

        return contentResolver.querySql(query, arrayOf("$id"))
    }

    fun getRecentlyAdded(): Cursor {

        val query = """
            SELECT 
                $ALBUM_ID,
                $ARTIST_ID,
                $ARTIST,
                $ALBUM,
                ${Columns.ALBUM_ARTIST},
                $DATA,
                $IS_PODCAST
            FROM $EXTERNAL_CONTENT_URI
            WHERE ${defaultSelection()} AND ${isRecentlyAdded()}

            ORDER BY $DATE_ADDED DESC
        """
        return contentResolver.querySql(query)
    }

    private fun defaultSelection(): String {
        return "${isPodcast()} AND ${notBlacklisted()}"
    }

    private fun sortOrder(): String {
        if (isPodcast) {
            return "lower($ALBUM) COLLATE UNICODE"
        }

        val (type, arranging) = sortPrefs.getAllAlbumsSortOrder()
        var sort = when (type) {
            SortType.ALBUM -> "lower($ALBUM)"
            SortType.ARTIST -> "lower($ARTIST)"
            SortType.ALBUM_ARTIST -> "lower(${Columns.ALBUM_ARTIST})"
            else -> "lower($ALBUM)"
        }

        sort += " COLLATE UNICODE "

        if (arranging == SortArranging.DESCENDING) {
            sort += " DESC"
        }
        return sort
    }

}