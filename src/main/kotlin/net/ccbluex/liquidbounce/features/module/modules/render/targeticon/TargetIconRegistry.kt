/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render.targeticon

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.readNativeImage
import net.minecraft.client.renderer.texture.DynamicTexture

@Suppress("unused")
enum class TargetIconRegistry(
    override val tag: String,
    hasTexture: Boolean = true,
) : TextureMode.Builtin.Preset {
    NONE("None", false),
    WEARY("Weary"),
    ANGRY("Angry"),
    SUNGLASSES("Sunglasses"),
    VOMIT("Vomit"),
    HEART("Heart"),
    TUNG("Tung"),
    BOYKISSER("Boykisser"),
    EATSUKI("Eatsuki"),
    CAT("Cat"),
    SUNNA("Sunna");

    override val texture: DynamicTexture? = if (hasTexture) {
        LiquidBounce.resource("images/${name.lowercase()}.png")
            .readNativeImage()
            .asTexture { "TargetIcon: $tag" }
    } else {
        null
    }
}
