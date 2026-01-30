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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.AABB
import kotlin.math.max

/**
 * Allows the crystal aura to send a break packet right when a crystal is placed by predicting the
 * expected entity id.
 */
object SubmoduleIdPredict : ToggleableValueGroup(ModuleCrystalAura, "IDPredict", false) {

    /**
     * Sends a packet for all included offsets.
     */
    private val offsetRange by intRange("OffsetRange", 1..2, 1..100)

    /**
     * Swings before every attack. Otherwise, it will only swing once.
     *
     * Only works when [SubmoduleCrystalDestroyer.swingMode] is enabled.
     */
    private val swingAlways by boolean("SwingAlways", false)

    /**
     * Sends an additional rotation packet.
     */
    private object Rotate : ToggleableValueGroup(this, "Rotate", true) {

        val back by boolean("Back", false)

        var oldRotation: Rotation? = null

        fun sendRotation(rotation: Rotation) {
            if (!enabled) {
                return
            }

            oldRotation = RotationManager.serverRotation
            network.send(
                ServerboundMovePlayerPacket.PosRot(
                player.x,
                player.y,
                player.z,
                rotation.yaw,
                rotation.pitch,
                player.onGround(),
                player.horizontalCollision
            ))
        }

        fun rotateBack() {
            if (!enabled || !back || oldRotation == null) {
                return
            }

            network.send(
                ServerboundMovePlayerPacket.PosRot(
                player.x,
                player.y,
                player.z,
                oldRotation!!.yaw,
                oldRotation!!.pitch,
                player.onGround(),
                player.horizontalCollision
            ))
        }

    }

    init {
        tree(Rotate)
    }

    private var highestId = 0
        set(value) {
            field = value
            ModuleDebug.debugParameter(ModuleCrystalAura, "Highest ID", highestId)
        }

    override fun onEnabled() {
        reset()
    }

    fun run(placePos: BlockPos) {
        if (!enabled) {
            return
        }

        val (rotation, _) =
            raytraceBox(
                player.eyePosition,
                AABB(placePos).inflate(0.5, 0.0, 0.5).setMaxY(placePos.y + 2.0),
                range = SubmoduleCrystalDestroyer.range.toDouble(),
                wallsRange = SubmoduleCrystalDestroyer.wallsRange.toDouble(),
            ) ?: return

        Rotate.sendRotation(rotation.normalize())

        val swingMode = SubmoduleCrystalDestroyer.swingMode
        if (!swingAlways) {
            swingMode.swing(InteractionHand.MAIN_HAND)
        }

        offsetRange.forEach { idOffset ->
            val id = highestId + idOffset

            // don't attack other entities in case the highest ID is wrong
            val entity = world.getEntity(id)
            if (entity != null && entity !is EndCrystal) {
                return@forEach
            }

            if (swingAlways) {
                swingMode.swing(InteractionHand.MAIN_HAND)
            }

            val packet = ServerboundInteractPacket(id, player.isShiftKeyDown, ServerboundInteractPacket.ATTACK_ACTION)
            network.send(packet)
            SubmoduleCrystalDestroyer.postAttackHandlers.forEach { it.attacked(id) }
        }

        SubmoduleCrystalDestroyer.chronometer.reset()
        Rotate.rotateBack()
    }

    private fun reset() {
        highestId = 0
        world.entitiesForRendering().forEach {
            highestId = max(it.id, highestId)
        }
    }

    @Suppress("unused")
    private val entitySpawnHandler = handler<PacketEvent> {
        when(val packet = it.packet) {
            // TODO: I guess this packet is merged into EntitySpawnS2CPacket
//            is ExperienceOrbSpawnS2CPacket -> highestId = max(packet.entityId, highestId)
            is ClientboundAddEntityPacket -> highestId = max(packet.id, highestId)
            is ClientboundLoginPacket -> highestId = max(packet.playerId, highestId)
        }
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

}
