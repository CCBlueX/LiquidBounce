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

package net.ccbluex.liquidbounce.render.effect

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.minecraft.client.renderer.LevelTargetBundle
import net.minecraft.client.renderer.PostChain

/**
 * Style choices of the glow modes, which all draw their silhouettes into the game's entity outline target. That
 * target can only go through one post chain, so with several glow modes running the first created one draws it.
 */
object OutlineGlow {

    // Smooth
    private const val DEFAULT_STYLE = 1

    private val owners = ArrayList<Pair<Mode, StyleChain>>()

    fun styles(owner: Mode): ModeValueGroup<ShaderStyle> {
        val styles = owner.modes<ShaderStyle>(owner, "Style", { DEFAULT_STYLE }) {
            arrayOf(VanillaStyle(it), *postStyles(it).toTypedArray())
        }
        owners += owner to StyleChain(styles, LevelTargetBundle.ENTITY_OUTLINE_TARGET_ID)
        return styles
    }

    /**
     * The chain of the running glow mode in place of the game's entity outline post chain.
     */
    @JvmStatic
    fun replace(original: PostChain?): PostChain? =
        owners.firstOrNull { (owner, _) -> owner.running }?.second?.get() ?: original

}
