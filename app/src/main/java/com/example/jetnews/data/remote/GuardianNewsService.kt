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

import com.example.jetnews.data.remote.model.GuardianItemResponseDto
import com.example.jetnews.data.remote.model.GuardianSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** The Guardian Content API 的 Retrofit 接口。 */
interface GuardianNewsService {

    @GET("search")
    suspend fun getLatestArticles(
        @Query("section") section: String? = null,
        @Query("order-by") orderBy: String = "newest",
        @Query("page") page: Int = 1,
        @Query("page-size") pageSize: Int = 20,
        @Query("show-fields") showFields: String = DEFAULT_SHOW_FIELDS,
        @Query("api-key") apiKey: String,
    ): Response<GuardianSearchResponseDto>

    @GET("{contentId}")
    suspend fun getArticle(
        @Path("contentId", encoded = true) contentId: String,
        @Query("show-fields") showFields: String = DEFAULT_SHOW_FIELDS,
        @Query("api-key") apiKey: String,
    ): Response<GuardianItemResponseDto>

    private companion object {
        const val DEFAULT_SHOW_FIELDS = "body,trailText,byline,thumbnail,wordcount"
    }
}
