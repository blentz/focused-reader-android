package com.focusedreader.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
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

    /** Serialise the current session as a small JSON document. */
    suspend fun exportJson(): String? {
        val s = dao.get() ?: return null
        return JSONObject().apply {
            put("text", s.text)
            put("position", s.position)
            put("source", s.source.name)
            put("importedAt", s.importedAt)
        }.toString()
    }

    /** Replace the current session with a previously-exported backup. */
    suspend fun importBackup(json: String) {
        val o = JSONObject(json)
        val text = o.getString("text")
        val position = o.optInt("position", 0)
        val sourceStr = o.optString("source", ImportSource.SHARE.name)
        val source = runCatching { ImportSource.valueOf(sourceStr) }.getOrDefault(ImportSource.SHARE)
        val importedAt = o.optLong("importedAt", System.currentTimeMillis())
        dao.upsert(Session(text = text, position = position, source = source, importedAt = importedAt))
    }
}
