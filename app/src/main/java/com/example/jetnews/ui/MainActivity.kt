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
//Bundle 是 Android 用来保存页面状态的容器，例如旋转屏幕前的一些状态可以放进去
import android.os.Bundle
//ComponentActivity 是支持 Jetpack Compose 的 Activity 基类
import androidx.activity.ComponentActivity
//setContent 用于显示 Compose 界面
import androidx.activity.compose.setContent
//用于让内容延伸到状态栏和底部导航栏区域。
import androidx.activity.enableEdgeToEdge
//getValue/setValue：Kotlin 属性委托，配合 mutableStateOf 实现 Compose 状态读写
import androidx.compose.runtime.getValue
//mutableStateOf：创建一个可观察的 Compose 状态，值改变时自动重组 UI
import androidx.compose.runtime.mutableStateOf
//remember：在重组期间记住某个值，不会因重组而丢失
import androidx.compose.runtime.remember
//rememberSaveable：在 Activity 重建（如旋转屏幕）时也能保留状态
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
//JetnewsApplication：应用类，保存全局依赖容器
import com.example.jetnews.JetnewsApplication
//handleDeepLink：处理应用深链接，解析链接并返回对应的导航栈
import com.example.jetnews.deeplink.handleDeepLink
//HomeKey：主页面（首页）的导航键
import com.example.jetnews.ui.home.HomeKey
//rememberInitialBackStack：记住初始导航返回栈，Activity 重建时恢复
import com.example.jetnews.ui.navigation.rememberInitialBackStack
//NewIntentEffect：监听 onNewIntent 回调的 Compose Effect
import com.example.jetnews.ui.utils.NewIntentEffect

class MainActivity : ComponentActivity() { //    ComponentActivity 是支持 Jetpack Compose 的 Activity 基类
    override fun onCreate(savedInstanceState: Bundle?) {//onCreate：Android 创建这个页面时的生命周期入口。
        // enableEdgeToEdge：开启边缘到边缘显示，让内容延伸到状态栏和导航栏后面（就是让APP页面可以铺到屏幕边缘，具体哪些组件需要留出距离可以设计）
        enableEdgeToEdge()
        // 调用父类的 onCreate，必须放在最前面
        super.onCreate(savedInstanceState)//savedInstanceState：用于恢复因旋转屏幕、系统回收等而暂时销毁前的状态；

        // 获取应用级的容器，用于依赖注入
        //application不是当前页面，是整个JetNews APP的全局对象
        //as 表示类型转换，即告诉 Kotlin： application 是 JetnewsApplication 类型，
        //这样就可以访问 JetnewsApplication 中定义的 container 属性。
        //container 中保存了文章、兴趣等数据仓库对象，供整个应用共享使用。
        //这样各处可以直接获取需要的 Repository，而不用每次都新建实例。
        val appContainer = (application as JetnewsApplication).container

        // setContent：设置 Compose UI 的内容，这是 Compose 界面的入口
        // 类似把{}的内容画到屏幕上
        setContent {
            // 处理深链接（Deep Link），如果是通过链接打开的，获取对应的页面栈。
            // 深链接：一个链接可以直接打开 App 里的某个具体页面，而不只是打开 App 首页。
            // 页面栈：Android 用“后进先出”的栈结构来管理页面跳转历史。
            // 打开一个新页面 = 往上压一个页面，按返回键 = 把最上面的页面弹出去，下面露出上一个页面
            // 比如刚打开APP是[HomeKey]表示在首页，
            // 从首页点击了某个文章[HomeKey, ArticleKey],表示经过首页，现在位于文章详情页
            // 点击返回[HomeKey]，又回到首页，中文通常叫“页面栈”或“返回栈”

            //intent 是系统传进来的启动信息,如果携带了深链接数据(intent.data 不为 null，比如intent.data = jetnews://post/123）
            //就通过 handleDeepLink 解析出对应的应用能理解的“页面访问路径”
            //remember{}表示第一次进入 Compose 时计算并保存结果；后续界面因状态刷新时，不要反复解析同一条链接。
            val initialDeepLinkBackStack = remember { intent.data?.handleDeepLink() }

            // 记录是否是通过深链接打开的
            //initialDeepLinkBackStack != null说明成功解析到页面栈，说明通过深链接打开，否则是普通打开
            //mutableStateOf是Jetpack Compose 里“可观察状态”的入口。把普通对象变为状态对象，状态值变了，Compose自动刷新UI
            //rememberSaveable{}跨重组保存，还能在配置变化后恢复 比如旋转屏幕、Activity 重建时，值通常还能回来
            var isOpenedByDeepLink by rememberSaveable { mutableStateOf(initialDeepLinkBackStack != null) }

            // 初始化导航返回栈（rememberInitialBackStack 会在 Activity 重建时恢复之前的导航状态）
            // ?: 叫 Elvis 运算符，可以理解为“如果左边没有值，就使用右边”。
            // 有深链接时用深链接对应的页面栈，没有则用[HomeKey]
            var initialBackStack by rememberInitialBackStack(initialDeepLinkBackStack ?: listOf(HomeKey))


            // App 已运行时收到新链接，比如JetNews已经打开或在后台，用户又点了一条JetNews文章链接
            //系统不一定重新创建APP，而是把新Intent发给现有的MainActivity
            // Intent 是 Android 系统传给 Activity 的“启动信息”(点击APP图标启动，点击链接打开文章等)

            // NewIntentEffect 监听 onNewIntent 回调，处理应用已在运行但再次收到 Intent 的情况
            // 当用户点击新链接时，newIntent.data 携带新的深链接地址
            // handleDeepLink() 解析这个链接得到新的页面栈 deepLinkBackStack
            // ?.let{}表示前面不为null才执行{}的代码
            // 如果解析成功：标记是通过深链接打开，并更新导航栈跳到新页面
            // 如果解析失败（newIntent.data 为 null 或链接无法识别），则保持当前页面不变
            NewIntentEffect { newIntent ->
                newIntent.data?.handleDeepLink()?.let { deepLinkBackStack ->
                    isOpenedByDeepLink = true
                    initialBackStack = deepLinkBackStack
                }
            }

            // 启动应用的主界面
            // JetnewsApp 是应用的主入口 Composable，接收依赖容器和导航状态
            JetnewsApp(
                //把数据层对象交给 UI 层使用
                appContainer,
                // 告诉 App 启动时显示哪一个页面，或者当前深链接希望打开哪一个页面
                isBackEnabled = !isOpenedByDeepLink || isTaskRoot,
                initialBackStack,
            )
        }
    }
}
