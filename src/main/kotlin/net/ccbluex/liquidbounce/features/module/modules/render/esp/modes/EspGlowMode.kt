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

package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import net.ccbluex.liquidbounce.render.effect.ShaderStyle
import net.ccbluex.liquidbounce.render.effect.StyleChain
import net.ccbluex.liquidbounce.render.effect.VanillaStyle
import net.ccbluex.liquidbounce.render.effect.postStyles
import net.minecraft.client.renderer.LevelTargetBundle
import net.minecraft.client.renderer.PostChain

object EspGlowMode : EspMode("Glow", requiresTrueSight = true) {

    val styles = modes<ShaderStyle>("Style", SMOOTH) {
        arrayOf(VanillaStyle(it), *postStyles(it).toTypedArray())
    }

    private val chain = StyleChain(styles, LevelTargetBundle.ENTITY_OUTLINE_TARGET_ID)

    /**
     * The chain of the chosen style in place of the game's entity outline post chain.
     */
    @JvmStatic
    fun replace(original: PostChain?): PostChain? = if (running) chain.get() ?: original else original

    private const val SMOOTH = 1

}
