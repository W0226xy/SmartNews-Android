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

package com.example.jetnews

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetnews.ui.home.HomeFeedScreen
import com.example.jetnews.ui.home.HomeUiState
import com.example.jetnews.ui.theme.JetnewsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadError_showsRetryAction() {
        composeTestRule.setContent {
            JetnewsTheme {
                HomeFeedScreen(
                    uiState = HomeUiState.Error("加载失败"),
                    showTopAppBar = false,
                    onToggleFavorite = {},
                    onSelectPost = {},
                    onRefreshPosts = {},
                    openDrawer = {},
                    homeListLazyListState = rememberLazyListState(),
                    onSearchInputChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }
}
