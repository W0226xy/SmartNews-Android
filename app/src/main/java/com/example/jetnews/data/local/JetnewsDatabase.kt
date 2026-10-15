package com.example.jetnews.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoritePostEntity::class, ReadingHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class JetnewsDatabase : RoomDatabase() {
    abstract fun savedPostsDao(): SavedPostsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite_posts ADD COLUMN authorName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE favorite_posts ADD COLUMN paragraphsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE favorite_posts ADD COLUMN isSummaryContent INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE reading_history ADD COLUMN authorName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reading_history ADD COLUMN paragraphsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE reading_history ADD COLUMN isSummaryContent INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
