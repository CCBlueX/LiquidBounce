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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import kotlin.math.max

/**
 * Allows the crystal aura to send a break packet right when a crystal is placed by predicting the
 * expected entity id.
 */
object SubmoduleIdPredict : ToggleableConfigurable(ModuleCrystalAura, "IDPredict", false) {

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
    private object Rotate : ToggleableConfigurable(this, "Rotate", true) {

        val back by boolean("Back", false)

        var oldRotation: Rotation? = null

        fun sendRotation(rotation: Rotation) {
            if (!enabled) {
                return
            }

            oldRotation = RotationManager.serverRotation
            network.sendPacket(PlayerMoveC2SPacket.Full(
                player.x,
                player.y,
                player.z,
                rotation.yaw,
                rotation.pitch,
                player.isOnGround,
                player.horizontalCollision
            ))
        }

        fun rotateBack() {
            if (!enabled || !back || oldRotation == null) {
                return
            }

            network.sendPacket(PlayerMoveC2SPacket.Full(
                player.x,
                player.y,
                player.z,
                oldRotation!!.yaw,
                oldRotation!!.pitch,
                player.isOnGround,
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
                player.eyePos,
                Box(placePos).expand(0.5, 0.0, 0.5).withMaxY(placePos.y + 2.0),
                range = SubmoduleCrystalDestroyer.range.toDouble(),
                wallsRange = SubmoduleCrystalDestroyer.wallsRange.toDouble(),
            ) ?: return

        Rotate.sendRotation(rotation.normalize())

        val swingMode = SubmoduleCrystalDestroyer.swingMode
        if (!swingAlways) {
            swingMode.swing(Hand.MAIN_HAND)
        }

        offsetRange.forEach { idOffset ->
            val id = highestId + idOffset

            // don't attack other entities in case the highest ID is wrong
            val entity = world.getEntityById(id)
            if (entity != null && entity !is EndCrystalEntity) {
                return@forEach
            }

            if (swingAlways) {
                swingMode.swing(Hand.MAIN_HAND)
            }

            val packet = PlayerInteractEntityC2SPacket(id, player.isSneaking, PlayerInteractEntityC2SPacket.ATTACK)
            network.sendPacket(packet)
            SubmoduleCrystalDestroyer.postAttackHandlers.forEach { it.attacked(id) }
        }

        SubmoduleCrystalDestroyer.chronometer.reset()
        Rotate.rotateBack()
    }

    private fun reset() {
        highestId = 0
        world.entities.forEach {
            highestId = max(it.id, highestId)
        }
    }

    @Suppress("unused")
    private val entitySpawnHandler = handler<PacketEvent> {
        when(val packet = it.packet) {
            is ExperienceOrbSpawnS2CPacket -> highestId = max(packet.entityId, highestId)
            is EntitySpawnS2CPacket -> highestId = max(packet.entityId, highestId)
            is GameJoinS2CPacket -> highestId = max(packet.playerEntityId, highestId)
        }
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

}
