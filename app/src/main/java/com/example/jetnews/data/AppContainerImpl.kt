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
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.posts.impl.FakePostsRepository

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {//interface接口，凡是叫AppContainer的必须提供文章仓库和兴趣仓库
    val postsRepository: PostsRepository//文章仓库接口，用于获取文章数据
    //变量名叫 postsRepository，类型是 PostsRepository
    val interestsRepository: InterestsRepository//兴趣仓库接口，用于获取兴趣数据
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class AppContainerImpl(private val applicationContext: Context) : AppContainer {
    // AppContainerImpl 是 AppContainer 接口的具体实现类
    // 它创建并持有应用中实际使用的仓库实例。
    // 两个仓库都通过 by lazy 懒加载：只有第一次访问时才创建实例，
    // 同一个实例在应用整个生命周期中共享。
    override val postsRepository: PostsRepository by lazy {
        FakePostsRepository()
    }
    //by lazy{}是Kotlin 的属性委托语法，作用是：第一次使用这个变量时才创建对象，之后一直复用第一次创建的对象。
    override val interestsRepository: InterestsRepository by lazy {
        FakeInterestsRepository()
    }
}
