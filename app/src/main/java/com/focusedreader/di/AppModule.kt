package com.focusedreader.di

import android.content.Context
import androidx.room.Room
import com.focusedreader.data.AppDatabase
import com.focusedreader.data.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "focused-reader.db")
            // Migrations are added here as the schema evolves. Destructive
            // fallback intentionally NOT enabled — losing reading position
            // on every schema bump would be hostile. Add real Migration(N, N+1)
            // when needed and write a paired migration test under
            // app/src/androidTest/.../MigrationsTest.kt.
            .build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessions()
}
