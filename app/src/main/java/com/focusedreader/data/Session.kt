package com.focusedreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_session")
data class Session(
    @PrimaryKey val id: Int = 0,
    val text: String,
    val position: Int,
    val source: ImportSource,
    val importedAt: Long
)
