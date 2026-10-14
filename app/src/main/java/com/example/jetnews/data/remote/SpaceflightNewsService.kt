/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.jetnews.data.remote

import com.example.jetnews.data.remote.model.SpaceflightArticleDto
import com.example.jetnews.data.remote.model.SpaceflightArticlesResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Spaceflight News API 的 Retrofit 接口。 */
interface SpaceflightNewsService {

    @GET("articles/")
    suspend fun getArticles(@Query("limit") limit: Int): Response<SpaceflightArticlesResponseDto>

    @GET("articles/{id}/")
    suspend fun getArticle(@Path("id") id: Long): Response<SpaceflightArticleDto>
}
