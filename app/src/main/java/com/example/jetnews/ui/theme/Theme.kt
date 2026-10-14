/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.jetnews.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val SmartNewsLightColorScheme = lightColorScheme(
    primary = smartNewsLightPrimary,
    onPrimary = smartNewsLightOnPrimary,
    primaryContainer = smartNewsLightPrimaryContainer,
    onPrimaryContainer = smartNewsLightOnPrimaryContainer,
    secondary = smartNewsLightSecondary,
    onSecondary = smartNewsLightOnSecondary,
    secondaryContainer = smartNewsLightSecondaryContainer,
    onSecondaryContainer = smartNewsLightOnSecondaryContainer,
    tertiary = smartNewsLightTertiary,
    onTertiary = smartNewsLightOnTertiary,
    tertiaryContainer = smartNewsLightTertiaryContainer,
    onTertiaryContainer = smartNewsLightOnTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = smartNewsLightBackground,
    onBackground = smartNewsLightOnBackground,
    surface = smartNewsLightSurface,
    onSurface = smartNewsLightOnSurface,
    surfaceVariant = smartNewsLightSurfaceVariant,
    onSurfaceVariant = smartNewsLightOnSurfaceVariant,
    outline = smartNewsLightOutline,
    inverseOnSurface = smartNewsLightInverseOnSurface,
    inverseSurface = smartNewsLightInverseSurface,
    inversePrimary = smartNewsLightInversePrimary,
    surfaceTint = smartNewsLightSurfaceTint,
)

val SmartNewsDarkColorScheme = darkColorScheme(
    primary = smartNewsDarkPrimary,
    onPrimary = smartNewsDarkOnPrimary,
    primaryContainer = smartNewsDarkPrimaryContainer,
    onPrimaryContainer = smartNewsDarkOnPrimaryContainer,
    secondary = smartNewsDarkSecondary,
    onSecondary = smartNewsDarkOnSecondary,
    secondaryContainer = smartNewsDarkSecondaryContainer,
    onSecondaryContainer = smartNewsDarkOnSecondaryContainer,
    tertiary = smartNewsDarkTertiary,
    onTertiary = smartNewsDarkOnTertiary,
    tertiaryContainer = smartNewsDarkTertiaryContainer,
    onTertiaryContainer = smartNewsDarkOnTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = smartNewsDarkBackground,
    onBackground = smartNewsDarkOnBackground,
    surface = smartNewsDarkSurface,
    onSurface = smartNewsDarkOnSurface,
    surfaceVariant = smartNewsDarkSurfaceVariant,
    onSurfaceVariant = smartNewsDarkOnSurfaceVariant,
    outline = smartNewsDarkOutline,
    inverseOnSurface = smartNewsDarkInverseOnSurface,
    inverseSurface = smartNewsDarkInverseSurface,
    inversePrimary = smartNewsDarkInversePrimary,
    surfaceTint = smartNewsDarkSurfaceTint,
)

// Glance 小组件暂时复用这两个兼容名称。
val LightColors = SmartNewsLightColorScheme
val DarkColors = SmartNewsDarkColorScheme

@Composable
fun SmartNewsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) SmartNewsDarkColorScheme else SmartNewsLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = JetnewsShapes,
        typography = JetnewsTypography,
        content = content,
    )
}

/** 兼容现有页面入口，内部统一使用 SmartNews 主题。 */
@Composable
fun JetnewsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    SmartNewsTheme(darkTheme, content)
