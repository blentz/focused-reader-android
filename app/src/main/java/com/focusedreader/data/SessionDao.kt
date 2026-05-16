package com.focusedreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: Session)

    @Query("UPDATE current_session SET position = :position WHERE id = 0")
    suspend fun updatePosition(position: Int)

    @Query("SELECT * FROM current_session WHERE id = 0")
    suspend fun get(): Session?

    @Query("SELECT * FROM current_session WHERE id = 0")
    fun observe(): Flow<Session?>

    @Query("DELETE FROM current_session")
    suspend fun clear()
}
