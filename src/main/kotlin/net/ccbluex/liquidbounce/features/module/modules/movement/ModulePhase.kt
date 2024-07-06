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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.fakelag.FakeLag.LagResult
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.entity.Entity
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShapes

/**
 * Phase module
 *
 * Allows you to phase through blocks.
 */

object ModulePhase : Module("Phase", Category.MOVEMENT) {

    val modes = choices<Choice>("Mode", GlassClip, arrayOf(GlassClip))

    object GlassClip : Choice("GlassClip") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val onlyFirst by boolean("OnlyFirstTime", true)

        private val blinkAfter by boolean("BlinkAfter", false)

        private val endBlinkCondition by enumChoice("EndBlinkCondition", EndBlinkCondition.CHAT_MESSAGE)

        private val message by text("ChatTrigger", "")

        enum class EndBlinkCondition(override val choiceName: String) : NamedChoice {
            CHAT_MESSAGE("ChatMessage"),
            SETBACK("Setback")
        }

        private var done = false
        private var clipping = false
        var blinking = false
        private var handleBlinking = false

        // this is stupid but we get in a recursive loop
        private var chill = false

        // taken from mc decompiled
        fun Entity.collidesWithBlock(pos: BlockPos, state: BlockState): Boolean {
            val voxelShape = state.getCollisionShape(world, pos, ShapeContext.of(this))
            val voxelShape2 = voxelShape.offset(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            return VoxelShapes.matchesAnywhere(voxelShape2,
                VoxelShapes.cuboid(this.boundingBox.offset(0.0, -0.0625, 0.0)),
                BooleanBiFunction.AND)
        }

        @Suppress("unused")
        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (chill) return@handler
            if (done && onlyFirst) return@handler

            chill = true

            if (event.state.block == Blocks.GLASS) {
                if (player.collidesWithStateAtPos(event.pos, event.state)) {
                    event.shape = VoxelShapes.empty()
                    clipping = true
                } else if (clipping) {
                    done = true
                    clipping = false
                }
            } else if (player.collidesWithStateAtPos(event.pos, event.state)) {
                done = true
            }

            if (blinkAfter) {
                blinking = true
                handleBlinking = true
            }

            chill = false
        }

        @Suppress("unused")
        val chatMessage = handler<ChatReceiveEvent> {
            if (endBlinkCondition == EndBlinkCondition.CHAT_MESSAGE && blinking && it.message.contains(message)) {
                blinking = false
                handleBlinking = false
            }
        }

        @Suppress("unused")
        val packetHandler = handler<PacketEvent> {
            if (mc.world != null && it.origin == TransferOrigin.RECEIVE) {
                if (it.packet is PlayerPositionLookS2CPacket
                    && endBlinkCondition == EndBlinkCondition.SETBACK && blinking) {
                    blinking = false
                    handleBlinking = false
                }
            }
        }

        @Suppress("unused")
        val worldChange = handler<WorldChangeEvent> {
            if (onlyFirst) {
                done = false
                clipping = false
            }

            blinking = false
            handleBlinking = false
        }
    }

    fun shouldBlink(packet: Packet<*>): LagResult? {
        if (!this.enabled) return null

        if (modes.activeChoice == GlassClip && GlassClip.blinking) {
            if (packet !is PlayerMoveC2SPacket) return null

            if (player.lastX == player.x && player.lastBaseY == player.y && player.lastZ == player.z) {
                FakeLag.firstPosition()?.vec?.run {
                    packet.x = x
                    packet.y = y
                    packet.z = z
                } ?: return LagResult.QUEUE
                return LagResult.PASS
            }
        }

        return null
    }

    fun shouldBlinkOverall(): Boolean {
        if (!this.enabled) return false

        if (modes.activeChoice == GlassClip && GlassClip.blinking) {
            return true
        }

        return false
    }

}
