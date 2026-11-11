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
package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect

import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes.JumpEffectImage
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes.JumpEffectSimple
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.minecraft.world.phys.Vec3

object ModuleJumpEffect : ClientModule("JumpEffect", ModuleCategories.RENDER) {

    val circles = ExpiringList<Vec3>()

    val modes = choices("Mode", 0) {
        arrayOf(
            JumpEffectSimple,
            JumpEffectImage
        )
    }.apply {
        tagBy(this)
        onChanged {
            circles.clear()
        }
    }

    @Suppress("unused")
    val playerJumpHandler = handler<PlayerJumpEvent> { _ ->
        // Adds new circle when the player jumps
        circles.add(player.position(), modes.activeMode.lifetime.last)
    }

    override fun onDisabled() {
        circles.clear()
    }

}
