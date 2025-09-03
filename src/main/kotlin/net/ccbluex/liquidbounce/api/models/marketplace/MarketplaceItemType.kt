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
package net.ccbluex.liquidbounce.api.models.marketplace

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.script.ScriptManager

enum class MarketplaceItemType(
    override val choiceName: String,
    val isListable: Boolean,
    val isSubscribable: Boolean
) : NamedChoice {
    @SerializedName("Config")
    CONFIG("Config", false, false),
    @SerializedName("Script")
    SCRIPT("Script", true, true),
    @SerializedName("Theme")
    THEME("Theme", true, true),
    @SerializedName("Other")
    OTHER("Other", false, false);

    suspend fun reload() = when (this) {
        THEME -> ThemeManager.load()
        SCRIPT -> ScriptManager.reload()
        else -> { }
    }
}
