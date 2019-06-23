package dev.olog.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.olog.data.db.entities.PodcastPlaylistEntity
import dev.olog.data.db.entities.PodcastPlaylistTrackEntity
import io.reactivex.Flowable

@Dao
abstract class PodcastPlaylistDao {

    @Query("""
        SELECT playlist.*, count(*) as size
        FROM podcast_playlist playlist JOIN podcast_playlist_tracks tracks
            ON playlist.id = tracks.playlistId
        GROUP BY playlistId
    """)
    abstract fun getAllPlaylists(): List<PodcastPlaylistEntity>

    @Query("""
        SELECT playlist.*, count(*) as size
        FROM podcast_playlist playlist JOIN podcast_playlist_tracks tracks
            ON playlist.id = tracks.playlistId
        GROUP BY playlistId
    """)
    abstract fun observeAllPlaylists(): Flowable<List<PodcastPlaylistEntity>>

    @Query("""
        SELECT playlist.*, count(*) as size
        FROM podcast_playlist playlist JOIN podcast_playlist_tracks tracks
            ON playlist.id = tracks.playlistId
        where playlist.id = :id
        GROUP BY playlistId
    """)
    abstract fun getPlaylistById(id: Long): PodcastPlaylistEntity?

    @Query("""
        SELECT playlist.*, count(*) as size
        FROM podcast_playlist playlist JOIN podcast_playlist_tracks tracks
            ON playlist.id = tracks.playlistId
        where playlist.id = :id
        GROUP BY playlistId
    """)
    abstract fun observePlaylistById(id: Long): Flowable<PodcastPlaylistEntity?>

    @Query("""
        SELECT tracks.*
        FROM podcast_playlist playlist JOIN podcast_playlist_tracks tracks
            ON playlist.id = tracks.playlistId
        WHERE playlistId = :playlistId
    """)
    abstract fun getPlaylistTracks(playlistId: Long): Flowable<List<PodcastPlaylistTrackEntity>>

    @Query("""
        SELECT max(idInPlaylist)
        FROM podcast_playlist playlist JOIN podcast_playlist_tracks tracks
            ON playlist.id = tracks.playlistId
        WHERE playlistId = :playlistId
    """)
    abstract fun getPlaylistMaxId(playlistId: Long): Int

    @Insert
    abstract fun createPlaylist(playlist: PodcastPlaylistEntity): Long

    @Query("""
        UPDATE podcast_playlist SET name = :name WHERE id = :id
    """)
    abstract fun renamePlaylist(id: Long, name: String)

    @Query("""DELETE FROM podcast_playlist WHERE id = :id""")
    abstract fun deletePlaylist(id: Long)

    @Insert
    abstract fun insertTracks(tracks: List<PodcastPlaylistTrackEntity>)

    @Query("""
        DELETE FROM podcast_playlist_tracks
        WHERE playlistId = :playlistId AND id = :idInPlaylist
    """)
    abstract fun deleteTrack(playlistId: Long, idInPlaylist: Long)

    @Query("""
        DELETE FROM podcast_playlist_tracks WHERE playlistId = :id
    """)
    abstract fun clearPlaylist(id: Long)

    @Query("""
        DELETE FROM podcast_playlist_tracks
        WHERE EXISTS (
            SELECT count(*) as items
            FROM podcast_playlist_tracks
            WHERE playlistId = :id
            GROUP BY id, playlistId
            HAVING items > 1
        )
    """)
    abstract fun removeDuplicated(id: Long)

}