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

package com.example.jetnews.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.unveilIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.jetnews.data.AppContainer
import com.example.jetnews.ui.home.HomeKey
import com.example.jetnews.ui.home.homeEntry
import com.example.jetnews.ui.interests.interestsEntry
import com.example.jetnews.ui.navigation.NavigationState
import com.example.jetnews.ui.navigation.Navigator
import com.example.jetnews.ui.navigation.PopUpTo
import com.example.jetnews.ui.navigation.rememberListDetailSceneStrategy
import com.example.jetnews.ui.post.PostKey
import com.example.jetnews.ui.post.postEntry
import com.example.jetnews.ui.saved.SavedKey
import com.example.jetnews.ui.saved.savedEntry

const val PIVOT_FRACTION_OFFSET = .2f//返回动画的缩放中心偏移量

@OptIn(ExperimentalAnimationApi::class)//显式启用实验性动画 API
@Composable
fun JetnewsNavDisplay(
    navigationState: NavigationState,//当前页面栈
    navigator: Navigator,//提供导航操作（如跳转、返回）的工具
    appContainer: AppContainer,//提供文章、兴趣数据的总容器
    onBack: () -> Unit,//页面需要返回时调用的函数
    isExpandedScreen: Boolean,//当前是否为平板/宽屏模式
    isBackEnabled: Boolean,//当前是否允许系统返回
    openDrawer: () -> Unit,//页面点击菜单按钮时打开左侧抽屉（目前宽屏用不上）
) {
    // Because the entryProvider is used within a `remember` block during NavEntry decoration,
    // using rememberUpdatedState allows the current value to be accessed even as window
    // configuration changes.
    // 窗口发生变化时，比如竖屏转横屏，后面创建的代码仍然能获取最新的isExpandedScreen
    val currentIsExpandedScreen by rememberUpdatedState(isExpandedScreen)
    //rememberUpdatedState(isExpandedScreen),保存一个“会自动指向最新值”的状态引用

    // 注册各个页面的入口配置（Home、Post、Interests），指定各自的数据来源和跳转行为
    val entryProvider = entryProvider {//把“页面标识”变成真正的界面
        homeEntry(
            postsRepository = appContainer.postsRepository,//首页需要显示文章列表，所以把文章仓库传进去
            newsPreferencesRepository = appContainer.newsPreferencesRepository,
            isExpandedScreen = { currentIsExpandedScreen },//判断是手机还是平板，是否需要双栏显示
            openDrawer = openDrawer,
            navigateToPost = { navigator.navigate(PostKey(it), popUpTo = PopUpTo(HomeKey)) },//用户点击文章时处理逻辑
            //比如点击了新闻卡片，home页面会传navigate(PostKey(it)),这个it是文章id
            //PostKey(it)表示跳转到文章详情页，此时导航状态从[HomeKey]变为[HomeKey, PostKey(it)]
            //PopUpTo(HomeKey)表示从首页跳转时清掉中间的页面
        )
        postEntry(
            postsRepository = appContainer.postsRepository,
            isExpandedScreen = { currentIsExpandedScreen },
            onBack = navigator::goUp,//文章详情页返回时，回到上一个页面（即首页）
            //navigator.goUp()会弹出当前栈顶的PostKey，恢复导航栈为[HomeKey]
        )
        interestsEntry(
            newsPreferencesRepository = appContainer.newsPreferencesRepository,
            isExpandedScreen = { currentIsExpandedScreen },
            openDrawer = openDrawer,
        )
        savedEntry(
            postsRepository = appContainer.postsRepository,
            isExpandedScreen = { currentIsExpandedScreen },
            openDrawer = openDrawer,
            navigateToPost = { navigator.navigate(PostKey(it), popUpTo = PopUpTo(SavedKey)) },
        )
    }

    val navEntries = navigationState.toDecoratedEntries(entryProvider = entryProvider)

    //会启用“列表—详情双栏模式”
    val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>(isExpandedScreen)

    SharedTransitionLayout {
        Surface {
            val sceneState = rememberSceneState(
                entries = navEntries,
                sceneStrategies = listOf(listDetailSceneStrategy),
                sharedTransitionScope = this,
                onBack = onBack,
            )

            val scene = sceneState.currentScene

            val currentInfo = SceneInfo(scene)
            val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
            val navigationEventState = rememberNavigationEventState(
                currentInfo = currentInfo,
                backInfo = previousSceneInfos,
            )

            NavigationBackHandler(//返回键处理
                state = navigationEventState,
                isBackEnabled = isBackEnabled && scene.previousEntries.isNotEmpty(),
                onBackCompleted = {
                    repeat(navEntries.size - scene.previousEntries.size) { onBack() }
                },
            )

            NavDisplay(
                sceneState = sceneState,
                navigationEventState = navigationEventState,
                predictivePopTransitionSpec = {
                    unveilIn() togetherWith scaleOut(
                        targetScale = .8f,
                        transformOrigin = when (it) {
                            NavigationEvent.EDGE_LEFT -> TransformOrigin(1 - PIVOT_FRACTION_OFFSET, .5f)
                            NavigationEvent.EDGE_RIGHT -> TransformOrigin(PIVOT_FRACTION_OFFSET, .5f)
                            else -> TransformOrigin.Center
                        },
                    )
                },
            )
        }
    }
}
