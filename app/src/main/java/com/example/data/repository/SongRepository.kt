package com.example.data.repository

import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()

    fun getSongById(id: Int): Flow<SongEntity?> = songDao.getSongById(id)

    suspend fun insertSong(song: SongEntity): Long = songDao.insertSong(song)

    suspend fun updateSong(song: SongEntity) = songDao.updateSong(song)

    suspend fun deleteSong(song: SongEntity) = songDao.deleteSong(song)
}
