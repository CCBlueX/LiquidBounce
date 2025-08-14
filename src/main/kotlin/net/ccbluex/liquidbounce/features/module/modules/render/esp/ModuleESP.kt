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
package net.ccbluex.liquidbounce.features.module.modules.render.esp

import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.Esp2DMode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspBoxMode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspOutlineMode
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity

/**
 * ESP module
 *
 * Allows you to see targets through walls.
 */
object ModuleESP : ClientModule("ESP", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.esp"

    val modes = choices("Mode", EspGlowMode, arrayOf(
        EspBoxMode,
        Esp2DMode,
        EspOutlineMode,
        EspGlowMode
    ))

    private val colorModes = choices("ColorMode", 0) {
        arrayOf(
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.Companion.WHITE.with(a = 100)),
            GenericRainbowColorMode(it)
        )
    }
    private val friendColor by color("Friends", Color4b.Companion.GREEN)

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
    }

    fun getColor(entity: LivingEntity): Color4b {
        val baseColor = getBaseColor(entity)

        if (entity.hurtTime > 0) {
            return Color4b.Companion.RED
        }

        return baseColor
    }

    private fun getBaseColor(entity: LivingEntity): Color4b {
        if (entity is PlayerEntity) {
            if (FriendManager.isFriend(entity) && friendColor.a > 0) {
                return friendColor
            }

            EntityTaggingManager.getTag(entity).color?.let { return it }
        }

        return colorModes.activeChoice.getColor(entity)
    }

    /**
     * Check if the entity requires true sight to be shown with the current ESP mode
     */
    fun requiresTrueSight(entity: LivingEntity) =
        modes.activeChoice.requiresTrueSight && entity.shouldBeShown()

}
