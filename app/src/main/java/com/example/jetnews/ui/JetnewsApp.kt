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

package com.example.jetnews.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.example.jetnews.data.AppContainer
import com.example.jetnews.ui.components.AppNavRail
import com.example.jetnews.ui.home.HomeKey
import com.example.jetnews.ui.interests.InterestsKey
import com.example.jetnews.ui.navigation.NAVIGATION_ITEMS
import com.example.jetnews.ui.navigation.Navigator
import com.example.jetnews.ui.navigation.PopUpTo
import com.example.jetnews.ui.navigation.rememberNavigationState
import com.example.jetnews.ui.theme.JetnewsTheme
import kotlinx.coroutines.launch


@Composable//表示这个函数是一个 Compose UI 函数，该函数返回的是界面的描述，告诉Compose应该画什么UI
fun JetnewsApp(appContainer: AppContainer, isBackEnabled: Boolean, initialBackStack: List<NavKey>) {
    //appContainer：应用级的依赖容器，包含文章、兴趣等数据仓库对象，用于提供数据访问和业务逻辑。
    //isBackEnabled：是否启用返回操作的标志，决定是否显示返回按钮或允许返回操作。
    //initialBackStack：初始导航返回栈，表示应用启动时的导航状态，通常用于处理深链接或恢复上次的导航状态。

    // rememberNavigationState 创建一个可感知 Compose 生命周期的导航状态对象，并记住它，用于管理页面栈和当前顶级页面
    val navigationState = rememberNavigationState(
        //HomeKey 和 InterestsKey不是页面本身，而是页面的“身份标识”,
        primaryTopLevelKey = HomeKey, //默认顶级页面为首页，用于决定默认显示的页面
        topLevelKeys = setOf(HomeKey, InterestsKey),//首页和兴趣页都是“顶级页面”,它们通常通过侧边抽屉或导航栏切换，而不是像文章详情页那样层层进入。
        initialBackStack = initialBackStack,//将 MainActivity 传进来的页面栈交给导航系统
        //比如普通启动：initialBackStack = [HomeKey]
        //深链接启动：initialBackStack = [HomeKey, ArticleKey("某篇文章")]
    )

    //创建导航控制器 navigator，用于处理页面跳转和返回操作。它依赖于 navigationState 来管理导航状态。
    //remember:只要 navigationState 没变，就复用之前创建的 Navigator
    val navigator = remember(navigationState) { Navigator(navigationState) }

    //创建协程作用域：用于异步开关抽屉
    //协程：不阻塞界面的异步任务
    val coroutineScope = rememberCoroutineScope()

    //获取当前窗口尺寸信息，用于判断是否是手机还是平板
    //WindowSizeClass 将屏幕分为紧凑、中等、展开等类型，帮助适配不同设备
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    //如果当前窗口宽度达到 Expanded 的最低宽度标准，则认为是大屏模式
    val isExpandedScreen = remember(windowSizeClass) {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    }

    //JetnewsTheme：应用的主题组件，为整个界面提供统一的颜色、字体等视觉风格
    JetnewsTheme {
        //创建“随屏幕大小变化”的抽屉状态
        val sizeAwareDrawerState = rememberSizeAwareDrawerState(isExpandedScreen)

        //ModalNavigationDrawer：模态式导航抽屉，侧边菜单栏
        //drawerContent：抽屉中显示的内容（AppDrawer）
        //gesturesEnabled：是否允许通过滑动手势打开抽屉（宽屏时禁用）
        ModalNavigationDrawer(//ModalNavigationDrawer 是 Material 3 的抽屉组件
            drawerContent = {//抽屉里要显示的内容
                AppDrawer(
                    //让抽屉内部可以知道当前是打开还是关闭
                    drawerState = sizeAwareDrawerState,
                    //当前顶级页面：标明抽屉里哪个菜单项是高亮选中的
                    //比如当前用户在首页navigationState.topLevelKey = HomeKey，则首页菜单应该高亮
                    currentTopLevelKey = navigationState.topLevelKey,
                    //菜单项点击时的跳转逻辑：切换到对应顶级页面
                    navigate = { navKey -> navigator.navigate(navKey, PopUpTo(navKey)) },
                    navigationItems = NAVIGATION_ITEMS,//菜单项，包含HomeKey 和 InterestsKey
                    //点击菜单项后自动关闭抽屉
                    closeDrawer = { coroutineScope.launch { sizeAwareDrawerState.close() } },
                )
            },
            drawerState = sizeAwareDrawerState,
            // Only enable opening the drawer via gestures if the screen is not expanded
            gesturesEnabled = !isExpandedScreen,
        ) {
            // Row：水平排列布局，将导航栏和内容区域并排显示
            Row {
                if (isExpandedScreen) {
                    // AppNavRail：宽屏设备（如平板）上显示的侧边导航栏
                    // 用于在顶级页面（首页、兴趣页）之间切换
                    AppNavRail(
                        currentTopLevelKey = navigationState.topLevelKey,
                        navigationItems = NAVIGATION_ITEMS,
                        navigate = { navKey -> navigator.navigate(navKey) },
                    )
                }
                // JetnewsNavDisplay：根据导航状态显示当前页面内容（首页、文章详情、兴趣页等）
                // 负责处理页面切换、返回操作和打开抽屉等导航行为
                JetnewsNavDisplay(
                    navigationState = navigationState,//当前在哪个页面、页面栈是什么
                    navigator = navigator,//导航器，负责处理页面跳转和返回操作
                    appContainer = appContainer,//提供文章和兴趣数据
                    onBack = navigator::goUp,//页面需要返回时，调用 navigator.goUp()
                    isExpandedScreen = isExpandedScreen,//页面当前是手机还是平板大屏
                    isBackEnabled = isBackEnabled,//控制是否允许返回
                    // openDrawer：通过协程异步打开抽屉，避免阻塞UI线程
                    openDrawer = { coroutineScope.launch { sizeAwareDrawerState.open() } },
                )
            }
        }
    }
}

/**
 * Determine the drawer state to pass to the modal drawer.
 * 根据屏幕宽度决定抽屉的状态：
 * - 非宽屏（手机）：使用真实的抽屉状态，允许打开/关闭
 * - 宽屏（平板）：强制锁定抽屉为关闭状态，因为宽屏使用侧边导航栏
 */
@Composable
private fun rememberSizeAwareDrawerState(isExpandedScreen: Boolean): DrawerState {
    // rememberDrawerState：创建并记住抽屉的初始状态（默认关闭）
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    return if (!isExpandedScreen) {
        // If we want to allow showing the drawer, we use a real, remembered drawer
        // state defined above
        // 手机设备：返回真实的抽屉状态，用户可以通过点击或滑动打开
        drawerState
    } else {
        // If we don't want to allow the drawer to be shown, we provide a drawer state
        // that is locked closed. This is intentionally not remembered, because we
        // don't want to keep track of any changes and always keep it closed
        // 平板设备：返回一个锁定关闭的抽屉状态（不记忆），始终关闭
        DrawerState(DrawerValue.Closed)
    }
}
