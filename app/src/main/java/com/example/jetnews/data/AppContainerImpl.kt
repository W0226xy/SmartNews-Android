/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews.data

import android.content.Context
import androidx.room.Room
import com.example.jetnews.BuildConfig
import com.example.jetnews.data.preferences.DataStoreNewsPreferencesRepository
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.data.local.JetnewsDatabase
import com.example.jetnews.data.local.RoomSavedPostsLocalDataSource
import com.example.jetnews.data.local.SavedPostsLocalDataSource
import com.example.jetnews.data.posts.impl.GuardianPostsRepository
import com.example.jetnews.data.remote.GuardianNewsService
import com.example.jetnews.data.posts.PostsRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {//interface接口，凡是叫AppContainer的必须提供文章仓库和兴趣仓库
    val postsRepository: PostsRepository//文章仓库接口，用于获取文章数据
    //变量名叫 postsRepository，类型是 PostsRepository
    val newsPreferencesRepository: NewsPreferencesRepository
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class AppContainerImpl(private val applicationContext: Context) : AppContainer {
    private val json = Json { ignoreUnknownKeys = true }

    // AppContainerImpl 是 AppContainer 接口的具体实现类
    // 它创建并持有应用中实际使用的仓库实例。
    // 两个仓库都通过 by lazy 懒加载：只有第一次访问时才创建实例，
    // 同一个实例在应用整个生命周期中共享。
    private val guardianNewsService: GuardianNewsService by lazy {
        Retrofit.Builder()
            .baseUrl(GUARDIAN_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GuardianNewsService::class.java)
    }

    private val database: JetnewsDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            JetnewsDatabase::class.java,
            "jetnews.db",
        )
            .addMigrations(JetnewsDatabase.MIGRATION_1_2)
            .build()
    }

    private val savedPostsLocalDataSource: SavedPostsLocalDataSource by lazy {
        RoomSavedPostsLocalDataSource(database.savedPostsDao())
    }

    override val newsPreferencesRepository: NewsPreferencesRepository by lazy {
        DataStoreNewsPreferencesRepository(applicationContext)
    }

    override val postsRepository: PostsRepository by lazy {
        GuardianPostsRepository(
            service = guardianNewsService,
            savedPostsLocalDataSource = savedPostsLocalDataSource,
            apiKey = BuildConfig.GUARDIAN_API_KEY,
            newsPreferencesRepository = newsPreferencesRepository,
        )
    }

    private companion object {
        const val GUARDIAN_BASE_URL = "https://content.guardianapis.com/"
    }
}
