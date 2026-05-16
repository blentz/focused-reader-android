package com.focusedreader.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val dao: SessionDao) {
    suspend fun import(text: String, source: ImportSource) {
        dao.upsert(Session(text = text, position = 0, source = source, importedAt = System.currentTimeMillis()))
    }
    suspend fun updatePosition(position: Int) = dao.updatePosition(position)
    suspend fun current(): Session? = dao.get()
    fun observe(): Flow<Session?> = dao.observe()
    suspend fun clear() = dao.clear()
}
