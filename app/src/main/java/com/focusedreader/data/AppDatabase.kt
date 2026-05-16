package com.focusedreader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class SourceConverter {
    @TypeConverter fun toString(s: ImportSource): String = s.name
    @TypeConverter fun fromString(s: String): ImportSource = ImportSource.valueOf(s)
}

@Database(entities = [Session::class], version = 1, exportSchema = true)
@TypeConverters(SourceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
}
