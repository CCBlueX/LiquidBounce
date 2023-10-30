/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.enforced
import net.ccbluex.liquidbounce.utils.client.moveKeys
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.movement.zeroXZ
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

object ModuleLongJump : Module("LongJump", Category.MOVEMENT) {

    val mode = choices(
        "Mode", NCP, arrayOf(
            // NCP
            NCP, NCPBow/*,

            // AAC
            AACv1, AACv2, AACv3,

            // Mineplex
            Mineplex, Mineplex2, Mineplex3,

            // Other
            Redesky, Hycraft*/
        )
    )
    private val autoJump by boolean("AutoJump", false)
    private val autoDisable by boolean("DisableAfterFinished", false)

    var jumped = false
    var canBoost = false
    var boosted = false

    /**
     * @anticheat NoCheatPlus
     * @anticheatVersion 3.16.1-SNAPSHOT-sMD5NET-b115s
     * @testedOn eu.loyisa.cn
     */
    private object NCP : Choice("NCP") {
        override val parent: ChoiceConfigurable
            get() = mode

        val ncpBoost by float("NCPBoost", 4.25f, 1f..10f)

        val repeatable = repeatable {
            if (canBoost) {
                player.velocity.x *= ncpBoost.toDouble()
                player.velocity.z *= ncpBoost.toDouble()
                boosted = true
            }
            canBoost = false
        }

        val moveHandler = handler<PlayerMoveEvent> {
            if (!player.moving && jumped) {
                player.zeroXZ()
            }
        }
    }

    /**
     * @anticheat NoCheatPlus
     * @anticheatVersion 3.16.1-SNAPSHOT-sMD5NET-b115s
     * @testedOn eu.loyisa.cn
     */

    private object NCPBow : Choice("NCP-bow") {

        override val parent: ChoiceConfigurable
            get() = mode

        var arrowBoost = 0f
        var shotArrows = 0f

        val rotations = tree(RotationsConfigurable())
        val charged by int("Charged", 4, 3..20)
        val speed by float("Speed", 2.5f, 0f..20f)
        val arrowsToShoot by int("ArrowsToShoot", 8, 0..20)
        val fallDistance by float("FallDistanceToJump", 0.42f, 0f..2f)
        val ticks by int("TicksToWork", 10, 3..500)

        val repeatable = repeatable {
            if (arrowBoost <= arrowsToShoot) {
                mc.options.useKey.isPressed = true
                RotationManager.aimAt(
                    Rotation(player.yaw, -90f), configurable = rotations
                )
                // Stops moving
                moveKeys.forEach {
                    it.enforced = false
                }
                // Shoots arrow
                if (player.itemUseTime >= charged) {
                    interaction.stopUsingItem(player)
                    shotArrows++
                }
            } else {
                mc.options.useKey.isPressed = false
                if (player.isUsingItem) interaction.stopUsingItem(player)
                shotArrows = 0f
                wait { 5 }
                player.jump()
                player.strafe(speed = speed.toDouble())
                wait { ticks }
                arrowBoost = 0f
            }
        }

        val movementRepeatable = repeatable {
            if (arrowBoost <= arrowsToShoot) return@repeatable
            if (player.fallDistance >= fallDistance) {
                player.jump()
                player.fallDistance = 0f
            }
        }
        val velocityHandler = handler<PacketEvent> {
            val packet = it.packet
            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id && shotArrows > 0.0) {
                shotArrows--
                arrowBoost++
            }
        }

        override fun disable() {
            shotArrows = 0.0f
            arrowBoost = 0.0f
        }
    }

    val repeatable = repeatable {
        if (jumped) {
            if (player.isOnGround || player.abilities.flying) {
                if (autoDisable && boosted) enabled = false
                jumped = false
            }
        }
        // AutoJump
        if (autoJump && player.isOnGround && player.moving && mode.activeChoice != NCPBow) {
            player.jump()
            jumped = true
        }
    }

    val manualJumpHandler = handler<PlayerJumpEvent> {
        jumped = true
        canBoost = true
    }
}
