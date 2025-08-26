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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

abstract class RotationMode(
    name: String,
    private val configurable: ChoiceConfigurable<RotationMode>,
    val module: ClientModule,
) : Choice(name), MinecraftShortcuts {

    /**
     * Already sends the packet on post-move.
     * This might get us a little advantage because the packets are added a little bit earlier to the server tick queue.
     *
     * The downside is that it is not legit and will flag post-rotation checks on some anti-cheats.
     */
    val postMove by boolean("PostMove", false)

    /**
     * Instantly sends the action if possible.
     * This does not account for packet order and might flag on some anti-cheats.
     *
     * PostMove might be irrelevant if this is enabled.
     */
    val instant by boolean("Instant", false)

    abstract fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit)

    override val parent: ChoiceConfigurable<*>
        get() = configurable

}

class NormalRotationMode(
    configurable: ChoiceConfigurable<RotationMode>,
    module: ClientModule,
    val priority: Priority = Priority.IMPORTANT_FOR_USAGE_2,

    // some modules might want to aim even tho it was instantly executed because the player's rotation should not
    // snap back as the same rotation might be needed for the next action
    private val aimAfterInstantAction: Boolean = false
) : RotationMode("Normal", configurable, module) {

    val rotations = tree(RotationsConfigurable(this))
    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    override fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit) {
        if (instant && isFinished()) {
            onFinished()
            if (aimAfterInstantAction) {
                mc.execute {
                    RotationManager.setRotationTarget(rotation, !ignoreOpenInventory, rotations, priority, module)
                }
            }

            return
        }

        mc.execute {
            RotationManager.setRotationTarget(
                rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                provider = module,
                priority = priority,
                whenReached = RestrictedSingleUseAction(canExecute = isFinished, action = {
                    PostRotationExecutor.addTask(module, postMove, task = onFinished, priority = true)
                })
            )
        }
    }

}

class NoRotationMode(configurable: ChoiceConfigurable<RotationMode>, module: ClientModule)
    : RotationMode("None", configurable, module) {

    val send by boolean("SendRotationPacket", false)

    override fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit) {
        val task = {
            if (send) {
                val fixedRotation = rotation.normalize()
                network.sendPacket(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        fixedRotation.yaw, fixedRotation.pitch, player.isOnGround,
                        player.horizontalCollision
                    )
                )
            }

            onFinished()
        }

        if (instant) {
            task()
            return
        }

        PostRotationExecutor.addTask(module, postMove, task)
    }

}


