package com.example.jetnews.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_posts")
data class FavoritePostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val sourceName: String,
    val authorName: String,
    val publishedDate: String,
    val url: String,
    val readTimeMinutes: Int,
    val paragraphsJson: String,
    val isSummaryContent: Boolean,
    val favoritedAt: Long,
)
