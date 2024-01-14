/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.item.MilkBucketItem
import net.minecraft.item.PotionItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * FastUse module
 *
 * Allows you to use items faster.
 */

object ModuleFastUse : Module("FastUse", Category.PLAYER) {

    private val modes = choices("Mode", Immediate, arrayOf(Immediate, ItemUseTime))
    private val stopInput by boolean("StopInput", false)

    /**
     * The packet type to send to speed up item usage.
     *
     * @see PacketType for more information.
     * @see PlayerMoveC2SPacket for more information about the packet.
     *
     * PacketType FULL is the most likely to bypass, since it uses the C06 duplicate exempt exploit.
     *
     * AntiCheat: Grim
     * Tested AC Version: 2.5.34
     * Tested on: eu.loyisa.cn, anticheat-test.com
     * Usable MC version: 1.17-1.20.4
     * Q: Why this works?
     * A: https://github.com/GrimAnticheat/Grim/blob/9660021d024a54634605fbcdf7ce1d631b442da1/src/main/java/ac/grim/grimac/checks/impl/movement/TimerCheck.java#L99
     */
    private val packetType by enumChoice("PacketType", PacketType.FULL, PacketType.values())

    val consumesItem: Boolean
        get() = player.isUsingItem && (player.activeItem.isFood || player.activeItem.item is MilkBucketItem
            || player.activeItem.item is PotionItem)

    val movementInputHandler = handler<MovementInputEvent> { event ->
        if (mc.options.useKey.isPressed && stopInput) {
            event.directionalInput = DirectionalInput.NONE
        }
    }

    private object Immediate : Choice("Immediate") {

        override val parent: ChoiceConfigurable
            get() = modes

        val delay by int("Delay", 0, 0..10)
        val timer by float("Timer", 1f, 0.1f..5f)

        /**
         * This is the amount of times the packet is sent per tick.
         *
         * This means we will speed up the eating process by 20 ticks on each tick.
         */
        val speed by int("Speed", 20, 1..35)

        val repeatable = repeatable {
            if (consumesItem) {
                Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFastUse,
                    resetAfterTicks = 1 + delay)

                waitTicks(delay)
                repeat(speed) {
                    network.sendPacket(packetType.generatePacket())
                }
                player.stopUsingItem()
            }
        }

    }

    private object ItemUseTime : Choice("ItemUseTime") {

        override val parent: ChoiceConfigurable
            get() = modes

        val consumeTime by int("ConsumeTime", 15, 0..20)
        val speed by int("Speed", 20, 1..35)

        val repeatable = repeatable {
            if (consumesItem && player.itemUseTime >= consumeTime) {
                repeat(speed) {
                    network.sendPacket(packetType.generatePacket())
                }

                player.stopUsingItem()
            }
        }

    }

    enum class PacketType(override val choiceName: String, val generatePacket: () -> PlayerMoveC2SPacket)
        : NamedChoice {
        ON_GROUND_ONLY("OnGroundOnly", {
            PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround)
        }),
        POSITION_AND_ON_GROUND("PositionAndOnGround", {
            PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, player.isOnGround)
        }),
        LOOK_AND_ON_GROUND("LookAndOnGround", {
            PlayerMoveC2SPacket.LookAndOnGround(player.yaw, player.pitch, player.isOnGround)
        }),
        FULL("Full", {
            PlayerMoveC2SPacket.Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround)
        });
    }

}
