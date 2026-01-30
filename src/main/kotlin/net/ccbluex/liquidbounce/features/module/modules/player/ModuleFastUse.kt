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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastUse.Immediate.speed
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.effect.MobEffects

/**
 * FastUse module
 *
 * Allows you to use items faster on legacy servers.
 */

object ModuleFastUse : ClientModule("FastUse", ModuleCategories.PLAYER, aliases = listOf("FastEat")) {

    private val modes = choices("Mode", Immediate, arrayOf(Immediate, ItemUseTime)).apply { tagBy(this) }

    private val conditions by multiEnumChoice("Conditions", UseConditions.NOT_IN_THE_AIR)
    private val stopInput by boolean("StopInput", false)

    /**
     * The move packet type to send.
     *
     * @see MovePacketType
     * @see ServerboundMovePlayerPacket
     *
     * [MovePacketType.FULL] is the most likely to bypass, since vanilla 1.17+ clients
     * typically send those when using items. Most anticheats have excluded 1.17+ clients
     * from timer checks.
     *
     * AntiCheat: Grim (Tested 2.5.34), Vulcan, Vanilla
     * Tested on: eu.loyisa.cn, anticheat-test.com
     * Usable Client version: 1.17-1.20.4
     * Usable Server version: >=1.8.9
     * A: Legacy servers depend on the clientside tickrate to calculate eating speed.
     * Grim exemption: https://github.com/GrimAnticheat/Grim/blob/9660021d024a54634605fbcdf7ce1d631b442da1/src/main/java/ac/grim/grimac/checks/impl/movement/TimerCheck.java#L99
     */
    private val packetType by enumChoice("PacketType", MovePacketType.FULL)

    val accelerateNow: Boolean
        get() = if (conditions.any { it.meetsConditions() }) {
            false
        } else {
            player.isUsingItem && player.useItem.isConsumable
        }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (mc.options.keyUse.isDown && stopInput) {
            event.directionalInput = DirectionalInput.NONE
        }
    }

    private object Immediate : Mode("Immediate") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        val delay by int("Delay", 0, 0..10, "ticks")
        val timer by float("Timer", 1f, 0.1f..5f)

        /**
         * This is the amount of times the packet is sent per tick.
         *
         * Having [speed] as 20 means that the server will simulate eating process 20 times each tick.
         */
        val speed by int("Speed", 20, 1..35, "packets")

        @Suppress("unused")
        val repeatable = tickHandler {
            if (accelerateNow) {
                Timer.requestTimerSpeed(
                    timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFastUse,
                    resetAfterTicks = 1 + delay
                )

                waitTicks(delay)
                repeat(speed) {
                    network.send(packetType.generatePacket())
                }
                player.releaseUsingItem()
            }
        }

    }

    private object ItemUseTime : Mode("ItemUseTime") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        val consumeTime by int("ConsumeTime", 15, 0..20)
        val speed by int("Speed", 20, 1..35, "packets")

        @Suppress("unused")
        val repeatable = tickHandler {
            if (accelerateNow && player.ticksUsingItem >= consumeTime) {
                repeat(speed) {
                    network.send(packetType.generatePacket())
                }

                player.releaseUsingItem()
            }
        }

    }

    @Suppress("unused")
    private enum class UseConditions(
        override val tag: String,
        val meetsConditions: () -> Boolean
    ) : Tagged {
        NOT_IN_THE_AIR("NotInTheAir", {
            !player.onGround()
        }),
        NOT_DURING_MOVE("NotDuringMove", {
            player.moving
        }),
        NOT_DURING_REGENERATION("NotDuringRegeneration", {
            player.hasEffect(MobEffects.REGENERATION)
        })
    }
}
