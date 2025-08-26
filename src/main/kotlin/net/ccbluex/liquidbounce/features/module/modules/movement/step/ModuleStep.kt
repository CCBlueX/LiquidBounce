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
package net.ccbluex.liquidbounce.features.module.modules.movement.step

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.canStep
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.stat.Stats

/**
 * Step module
 *
 * Allows you to step up blocks.
 */

object ModuleStep : ClientModule("Step", Category.MOVEMENT) {

    var modes = choices("Mode", Instant, arrayOf(
        Instant,
        Legit,
        Vulcan286,
        BlocksMC,
        Hypixel
    )).apply { tagBy(this) }

    object Legit : Choice("Legit") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val autoJumpHandler = handler<MinecraftAutoJumpEvent> { event ->
            event.autoJump = true
        }

    }

    object Instant : Choice("Instant") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        /**
         * A common jump looks like this:
         * PlayerMoveC2SPacket 207.30000001192093 62.0 149.86302076816213 0.0 0.0 true
         * PlayerMoveC2SPacket 207.30000001192093 62.41999998688698 149.86517219525172 0.0 0.0 false
         * PlayerMoveC2SPacket 207.30000001192093 62.7531999805212 149.8656024806536 0.0 0.0 false
         * PlayerMoveC2SPacket 207.28040473521648 63.00133597911215 149.86603276605547 0.0 0.0 false
         * PlayerMoveC2SPacket 207.23709917391574 63.166109260938214 149.8665921370619 0.0 0.0 false
         * PlayerMoveC2SPacket 207.17221725301053 63.24918707874468 149.86715150806833 0.0 0.0 false
         * PlayerMoveC2SPacket 207.0877008442994 63.25220334025373 149.86771087907476 0.0 0.0 false
         * PlayerMoveC2SPacket 206.98531705116994 63.17675927506424 149.8682702500812 0.0 0.0 false
         * PlayerMoveC2SPacket 206.86667393775122 63.024424088213685 149.86882962108763 0.0 0.0 false
         * PlayerMoveC2SPacket 206.73323484244284 63.0 149.86938899209406 0.0 0.0 true
         */
        private val jumpOrder = arrayOf(
            0.0, // This is for the sake of configuration simplicity. A normal human considers 0 to be NOTHING.
            0.41999998688698,
            0.7531999805212,
            1.00133597911215,
            1.166109260938214,
            1.24918707874468,
            1.25220334025373,
            1.17675927506424,
            1.024424088213685
        )

        private val height by float("Height", 1.0F, 0.6F..5.0F)
        private val trim by boolean("Trim", false)

        /**
         * Simulates a jump by sending multiple packets. The range of the jump order is configured by the user.
         * 0..2 is the default - it bypasses common anti-cheats like NoCheatPlus.
         */
        private val simulateJumpOrder by intRange("SimulateJumpOrder", 0..2,
            jumpOrder.indices)
        private val wait by intRange("Wait", 0..0, 0..60, "ticks")
        private val packetType by enumChoice("PacketType", MovePacketType.FULL,
            arrayOf(MovePacketType.FULL, MovePacketType.POSITION_AND_ON_GROUND))

        private var ticksWait = 0

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (ticksWait > 0) {
                ticksWait--
            }
        }

        @Suppress("unused")
        private val stepHandler = handler<PlayerStepEvent> {
            if (ticksWait > 0) {
                return@handler
            }

            it.height = height
        }

        @Suppress("unused")
        private val stepSuccessEvent = handler<PlayerStepSuccessEvent> { event ->
            val stepHeight = event.adjustedVec.y

            ModuleDebug.debugParameter(ModuleStep, "StepHeight", stepHeight)

            if (stepHeight <= 0.5) {
                return@handler
            }

            // If we have configured 0..0 then we will send nothing.
            // That makes sense because the first entry of the array is 0.0
            // @see jumpOrder
            if (simulateJumpOrder == 0..0) {
                return@handler
            }

            player.incrementStat(Stats.JUMP)

            // Used to trim the additional height to the maximum step height
            val trimHeight = player.y + stepHeight

            // Slice the array to the specified range and send the packets
            jumpOrder.sliceArray(simulateJumpOrder)
                .filter { it != 0.0 } // This should not happen, but just in case.
                .map { additionalY ->
                    val destinationY = player.y + additionalY

                    packetType.generatePacket().apply {
                        this.x = player.x
                        this.y = destinationY.let { y ->
                            if (trim) y.coerceAtMost(trimHeight) else y
                        }
                        this.z = player.z
                    }
                }.forEach(network::sendPacket)
            ticksWait = wait.random()
        }

    }

    /**
     * InspectorBoat Vulcan Step
     *
     * @author InspectorBoat (and translated by 1zuna)
     */
    object Vulcan286 : Choice("Vulcan286") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private var stepCounter = 0
        private var stepping = false

        @Suppress("unused")
        private val movementInputHandler = sequenceHandler<MovementInputEvent> { event ->
            if (player.canStep(1.0) && !stepping) {
                event.jump = true
                stepCounter++

                stepping = true
                waitTicks(2)
                if (stepCounter % 2 == 0) {
                    player.velocity.y = 0.24680001947880004
                    player.velocity = player.velocity.withStrafe(speed = 0.2)
                }
                waitTicks(1)
                if (stepCounter % 2 == 0) {
                    player.velocity.y = 0.0
                }
                waitTicks(1)
                player.velocity.y = -0.17
                stepping = false
            }
        }

        override fun disable() {
            stepping = false
            stepCounter = 0
            super.disable()
        }


    }

    /**
     * BlocksMC Step
     * for 1.9+
     *
     * @author @liquidsquid1
     */
    object BlocksMC : Choice("BlocksMC") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private var baseTimer by float("BaseTimer", 3.0f, 0.1f..5.0f)
        private var recoveryTimer by float("RecoveryTimer", 0.6f, 0.1f..5.0f)

        private var stepping = false

        @Suppress("unused")
        private val movementInputHandler = sequenceHandler<MovementInputEvent> { event ->
            if (player.canStep(1.0) && !stepping) {
                event.jump = true

                stepping = true
                Timer.requestTimerSpeed(baseTimer, Priority.IMPORTANT_FOR_USAGE_1, ModuleStep, 3)
                player.velocity.y = 0.42
                waitTicks(1)
                player.velocity.y = 0.33
                waitTicks(1)
                player.velocity.y = 0.25
                waitTicks(2)
                player.velocity = player.velocity.withStrafe(speed = 0.281)
                player.velocity.y -= player.y % 1.0
                Timer.requestTimerSpeed(recoveryTimer, Priority.IMPORTANT_FOR_USAGE_1, ModuleStep, 2)
                stepping = false
            }
        }

        @Suppress("unused")
        private val fakeLagHandler = handler<QueuePacketEvent> { event ->
            if (event.origin == TransferOrigin.OUTGOING && stepping) {
                event.action = PacketQueueManager.Action.QUEUE
            }
        }

        override fun disable() {
            stepping = false
            super.disable()
        }

        override val running: Boolean
            get() = super.running && !ModuleSpeed.running

    }

    /**
     * does not seem to work above a certain y level for some reason
     */
    object Hypixel : Choice("Hypixel") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val alternateBypass by boolean("AlternateBypass", false)
        val spoof by boolean("Spoof", false)

        private var stepping = false

        private val stepHeight get() = when {
            player.canStep(1.0) -> 1.0
            player.canStep(1.25) -> 1.25
            else -> 1.5
        }

        @Suppress("unused")
        private val movementInputHandler = sequenceHandler<MovementInputEvent> { event ->
            if (player.canStep(1.5) && !stepping) {
                val currentStepHeight = stepHeight
                event.jump = true

                stepping = true
                player.velocity.y = 0.42
                waitTicks(1)
                if(currentStepHeight > 1.0) {
                    player.velocity.y += 0.061
                }
                waitTicks(2)
                if (currentStepHeight == 1.0) {
                    player.velocity.y -= 0.14
                } else {
                    player.velocity.y -= 0.095
                    if (currentStepHeight > 1.25) {
                        waitTicks(5)
                        if(alternateBypass) {
                            player.isOnGround = true
                        } else {
                            player.velocity.y = 0.42
                        }
                    }
                }
                stepping = false
                player.velocity = player.velocity.withStrafe(speed = 0.1838601407459074)
            }
        }

        @Suppress("unused")
        private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
            if(spoof && player.airTicks == 8) {
                event.ground = true
            }
        }

        override fun disable() {
            stepping = false
            super.disable()
        }

    }

}
