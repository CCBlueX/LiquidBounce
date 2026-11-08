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

import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes.JumpEffectImage
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes.JumpEffectSimple
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.minecraft.world.entity.player.Player
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

    private val otherPlayers by boolean("OtherPlayers", false).onChanged {
        groundHeights.clear()
    }

    /**
     * The height other players last stood at, while they are on the ground; NaN for anyone not seen standing.
     */
    private val groundHeights = Reference2DoubleOpenHashMap<Player>().apply {
        defaultReturnValue(Double.NaN)
    }

    @Suppress("unused")
    val playerJumpHandler = handler<PlayerJumpEvent> { _ ->
        // Adds new circle when the player jumps
        circles.add(player.position(), modes.activeMode.lifetime.last)
    }

    /**
     * Other players only arrive as positions, so a jump is them leaving the ground upwards.
     */
    @Suppress("unused")
    private val otherPlayersHandler = handler<GameTickEvent> {
        if (!otherPlayers) {
            return@handler
        }

        groundHeights.keys.removeIf { it.isRemoved }

        for (other in world.players()) {
            if (other === player) {
                continue
            }

            if (other.onGround()) {
                groundHeights.put(other, other.y)
                continue
            }

            val groundHeight = groundHeights.removeDouble(other)
            if (other.y > groundHeight) {
                circles.add(Vec3(other.x, groundHeight, other.z), modes.activeMode.lifetime.last)
            }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        groundHeights.clear()
    }

    override fun onDisabled() {
        circles.clear()
        groundHeights.clear()
    }

}
