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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.Matrix7145FlagLongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.VulcanLongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.nocheatplus.NoCheatPlusBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.nocheatplus.NoCheatPlusBow
import net.ccbluex.liquidbounce.utils.entity.moving

object ModuleLongJump : ClientModule("LongJump", Category.MOVEMENT) {

    init {
        enableLock()
    }

    val mode = choices(
        "Mode", NoCheatPlusBoost, arrayOf(
            // NoCheatPlus
            NoCheatPlusBoost,
            NoCheatPlusBow,
            VulcanLongJump,
            Matrix7145FlagLongJump
        )
    ).apply { tagBy(this) }
    private val autoJump by boolean("AutoJump", false)
    val autoDisable by boolean("DisableAfterFinished", false)

    var jumped = false
    var canBoost = false
    var boosted = false

    val tickHandler = handler<MovementInputEvent> {
        if (jumped) {
            if (player.isOnGround || player.abilities.flying) {
                if (autoDisable && boosted) {
                    enabled = false
                }

                jumped = false
            }
        }

        // AutoJump
        if (autoJump && player.isOnGround && player.moving
            && mode.activeChoice != NoCheatPlusBow) {
            player.jump()
            jumped = true
        }
    }

    @Suppress("unused")
    val manualJumpHandler = handler<PlayerJumpEvent> {
        jumped = true
        canBoost = true
    }
}
