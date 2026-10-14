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

package com.example.jetnews.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay

//“平板上列表和详情同时显示” 的自适应导航规则。
class ListDetailScene<T : Any>(//<T : Any>泛型，表示可以处理不同的导航页面，Any类似Java中的Object
    override val key: Any,
    //NavEntry:导航栈中的一个页面记录，比如首页，详情页
    val listEntry: NavEntry<T>,//列表页，比如Home页面或文章列表页
    val detailEntry: NavEntry<T>?,//详情页，可空，因为可能没有打开详情页
    override val previousEntries: List<NavEntry<T>>,//之前的导航记录，Navigation 3 框架需要的导航上下文信息，现在知道它用于保存“双栏场景之前的历史页面”即可。
) : Scene<T> {
    //entries当前页面栈，只包含合并后显示的页面（列表页和可选的详情页）
    override val entries: List<NavEntry<T>> = listOfNotNull(listEntry, detailEntry)//listOfNotNull()创建一个列表，忽略null
    //listEntry 本身一定存在，但 detailEntry 可以不存在

    //content：真正画出左右布局
    override val content: @Composable (() -> Unit) = {//content类型是一个只读的Compose 函数,() -> Unit 表示不接收参数、没有返回值
        //获取列表页的布局配置，如果没有配置则使用默认配置
        val listConfiguration = listEntry.metadata[ListMetadataKey] ?: ListConfiguration()

        Row {//Compose 的横向布局容器
            Box(modifier = listConfiguration.modifier) {//一个布局容器，存放列表页（Home和interests）
                listEntry.Content()
            }
            Box(//详情页，如果详情页存在且设置了DetailMetadataKey，则用它自己的布局装饰，否则用Modifier（不额外设置样式）
                modifier = detailEntry?.metadata?.get(DetailMetadataKey)?.modifier ?: Modifier,
            ) {
                AnimatedContent(//动画容器：在详情页和占位内容之间做淡入淡出切换
                    targetState = detailEntry,//表示被观察的状态是当前详情页
                    //为每一种内容提供唯一标识，帮助动画系统判断“内容是否真的变了”,
                    //没有选择文章 → key 为 detailPlaceholder
                    contentKey = { entry -> entry?.contentKey ?: "detailPlaceholder" },
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()//进入时淡入，退出时淡出
                    },
                ) { entry ->
                    if (entry == null) {//没有详情页时，显示占位内容（通常是提示用户选择文章）
                        listConfiguration.detailPlaceholder()//调用布局配置中提供的占位内容
                    } else {//有详情页时
                        entry.Content()//显示详情页的实际内容
                    }
                }
            }
        }
    }
    //给页面附加的“标签/说明”，告诉系统它是列表页还是详情页
    override val metadata = metadata {//给这个场景附加元数据，控制导航动画行为
        //给 Navigation 3 框架提供的动画信息
        //TransitionKey：正常前进导航时的动画；
        //PopTransitionKey：按返回键返回时的动画；
        //KeepUntilTransitionsFinished：旧页面不要立刻被销毁，要保留到动画结束
        put(NavDisplay.TransitionKey) {
            fadeIn() togetherWith ExitTransition.KeepUntilTransitionsFinished
        }//进入场景时的动画
        put(NavDisplay.PopTransitionKey) {
            fadeIn() togetherWith ExitTransition.KeepUntilTransitionsFinished
        }//返回（弹出）时的动画
    }

    //sealed受限继承，只能在指定范围被已知几个类继承/实现
    sealed interface PaneConfiguration {//定义面板布局配置的接口
        val modifier: Modifier//面板的修饰符，控制大小、位置等
    }

    //左侧列表配置（列表页的布局和质量参数）
    //val detailPlaceholder: @Composable () -> Unit = {}
    //detailPlaceholder是一个只读的，类型为@Composable的函数，无参数，无返回值 ，={}表示这个compose函数是空的，不显示任何内容。
    data class ListConfiguration(override val modifier: Modifier = Modifier, val detailPlaceholder: @Composable () -> Unit = {}) :
        PaneConfiguration

    //右侧详情配置（详情页的布局和质量参数）
    data class DetailConfiguration(override val modifier: Modifier = Modifier) : PaneConfiguration

    object ListMetadataKey : NavMetadataKey<ListConfiguration>//列表页配置的标识键
    object DetailMetadataKey : NavMetadataKey<DetailConfiguration>//详情页配置的标识键

    companion object {//companion object：给类提供类似静态方法的工具，类似java中的static
        //调用ListDetailScene.list(),不用先创建ListDetailScene 对象。
        fun list(listConfiguration: ListConfiguration = ListConfiguration()) = metadata {//为列表页创建元数据（配置）
            put(ListMetadataKey, listConfiguration)//存入列表配置
        }

        fun detail(detailConfiguration: DetailConfiguration = DetailConfiguration()) = metadata {//为详情页创建元数据（配置）
            put(DetailMetadataKey, detailConfiguration)//存入详情配置
        }
    }
}

//判断当前是否能组合成双栏布局
@Composable
fun <T : Any> rememberListDetailSceneStrategy(isExpandedScreen: Boolean) = remember(isExpandedScreen) {
    ListDetailSceneStrategy<T>(isExpandedScreen)
}

//决定是否使用双栏布局
class ListDetailSceneStrategy<T : Any>(val isExpandedScreen: Boolean) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!isExpandedScreen) return null //不是平板大屏时，不合并页面，维持手机的单页导航
        //取出导航栈最后一个页面
        val lastEntry = entries.lastOrNull() ?: return null
        // 只有最后一个页面（当前显示页）是列表页或详情页时，才考虑合并
        if (ListDetailScene.ListMetadataKey !in lastEntry.metadata &&
            ListDetailScene.DetailMetadataKey !in lastEntry.metadata
        ) {
            return null
        }

        // 在页面栈中从后往前找最近的列表页，作为左侧列表
        val listEntryIndex = entries.indexOfLast { ListDetailScene.ListMetadataKey in it.metadata }
        if (listEntryIndex == -1) return null

        val listEntry = entries[listEntryIndex]
        // 列表页后面紧跟着的详情页，如果有就显示在右侧，没有则只显示列表
        val detailEntry = entries.getOrNull(listEntryIndex + 1)
            ?.takeIf { ListDetailScene.DetailMetadataKey in it.metadata }

        // 返回合并后的场景：左侧列表 + 右侧详情
        return ListDetailScene(
            key = listEntry.contentKey,//用列表页的标识作为合并后场景的标识
            listEntry = listEntry,//左侧的列表页
            detailEntry = detailEntry,//右侧的详情页（可能为null）
            previousEntries = entries.take(listEntryIndex),//列表中列表页之前的页面（不在本场景中显示）
        )
    }
}
