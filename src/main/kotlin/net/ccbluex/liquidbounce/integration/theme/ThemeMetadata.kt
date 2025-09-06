/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.integration.theme

import com.google.gson.JsonObject

data class ThemeMetadata(
    val id: String,
    val name: String,
    val version: String,
    val authors: List<String>,
    val screens: List<String>,
    val overlays: List<String>,
    val components: List<String>,
    val fonts: List<String>,
    val backgrounds: List<Background>,
    val values: List<JsonObject>? = null
) {
    @Suppress("RedundantRequireNotNullCall")
    fun checkNotNull() {
        checkNotNull(id)
        checkNotNull(name)
        checkNotNull(version)
        checkNotNull(authors)
        checkNotNull(screens)
        checkNotNull(overlays)
        checkNotNull(components)
        checkNotNull(fonts)
        checkNotNull(backgrounds)
    }
}

data class Background(
    val name: String,
    val types: List<String>
)
