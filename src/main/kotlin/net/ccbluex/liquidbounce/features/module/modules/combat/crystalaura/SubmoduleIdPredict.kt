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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.ThrowablePotionItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.phys.AABB
import kotlin.math.max

/**
 * Allows the crystal aura to send a break packet right when a crystal is placed by predicting the
 * expected entity id.
 */
object SubmoduleIdPredict : ToggleableValueGroup(ModuleCrystalAura, "IDPredict", false) {

    /**
     * Sends a packet for all included offsets.
     * Defaults to `1..1` because attacking `N + 2` after `N + 1` explodes can hit newly dropped
     * `ItemEntity` instances and trigger a vanilla invalid-entity disconnect (#7630).
     */
    private val offsetRange by intRange("OffsetRange", 1..1, 1..20)

    /**
     * Swings before every attack. Otherwise, it will only swing once.
     *
     * Only works when [SubmoduleCrystalDestroyer.swingMode] is enabled.
     */
    private val swingAlways by boolean("SwingAlways", false)

    /**
     * Pauses ID prediction briefly when items, XP orbs, or projectiles spawn nearby so predicted IDs
     * do not collide with non-attackable vanilla entities.
     */
    private object Safe : ToggleableValueGroup(this, "Safe", true) {
        val pauseTicks by int("PauseTicks", 3, 1..20, "ticks")

        @Volatile
        var unsafeTicks = 0

        fun markUnsafe() {
            if (enabled) {
                unsafeTicks = pauseTicks
            }
        }

        fun isBlocked(): Boolean {
            if (!enabled) {
                return false
            }

            if (unsafeTicks > 0) {
                return true
            }

            return isSpawnerItem(player.mainHandItem.item) || isSpawnerItem(player.offhandItem.item)
        }

        private fun isSpawnerItem(item: Item): Boolean {
            return item is ExperienceBottleItem ||
                item is ThrowablePotionItem ||
                item is EnderpearlItem ||
                item is BowItem ||
                item is CrossbowItem ||
                item is TridentItem ||
                item is SnowballItem ||
                item is EggItem
        }
    }

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
                )
            )
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
                )
            )
        }

    }

    init {
        treeAll(Safe, Rotate)
    }

    private val HAZARDOUS_ENTITY_TYPES: Set<EntityType<*>> = hashSetOf(
        EntityTypes.ITEM,
        EntityTypes.EXPERIENCE_ORB,
        EntityTypes.EXPERIENCE_BOTTLE,
        EntityTypes.SPLASH_POTION,
        EntityTypes.LINGERING_POTION,
        EntityTypes.ENDER_PEARL,
        EntityTypes.ARROW,
        EntityTypes.SPECTRAL_ARROW,
        EntityTypes.TRIDENT,
        EntityTypes.FIREWORK_ROCKET,
        EntityTypes.SNOWBALL,
        EntityTypes.EGG,
        EntityTypes.WIND_CHARGE,
        EntityTypes.FISHING_BOBBER,
        EntityTypes.AREA_EFFECT_CLOUD
    )

    @Volatile
    private var highestId = 0

    private fun updateHighestId(id: Int) {
        if (id > highestId) {
            highestId = id
            ModuleCrystalAura.debugParameter("Highest ID") { highestId }
        }
    }

    override fun onEnabled() {
        reset()
    }

    fun run(placePos: BlockPos) {
        if (!enabled || mc.level == null || Safe.isBlocked()) {
            return
        }

        if (highestId <= 0) {
            reset()
            if (highestId <= 0) {
                return
            }
        }

        val (rotation, _) =
            raytraceBox(
                player.eyePosition,
                AABB(placePos).inflate(0.5, 0.0, 0.5).setMaxY(placePos.y + 2.0),
                range = SubmoduleCrystalDestroyer.range.toDouble(),
                wallsRange = SubmoduleCrystalDestroyer.wallsRange.toDouble(),
            ) ?: return

        SubmoduleCrystalDestroyer.AntiWeakness.executeWithWeapon {
            dispatchPredictedAttacks(rotation)
            SubmoduleCrystalDestroyer.chronometer.reset()
        }
    }

    private fun dispatchPredictedAttacks(rotation: Rotation) {
        Rotate.sendRotation(rotation.normalize())

        val currentWorld = mc.level ?: return
        val swingMode = SubmoduleCrystalDestroyer.swingMode
        var swungOnce = false

        for (idOffset in offsetRange) {
            val id = highestId + idOffset
            if (id <= 0 || id == player.id) {
                continue
            }

            // don't attack other entities in case the highest ID is wrong
            val entity = currentWorld.getEntity(id)
            if (entity != null && entity !is EndCrystal) {
                continue
            }

            if (swingAlways || !swungOnce) {
                swingMode.attackSwing()
                swungOnce = true
            }

            network.send(ServerboundAttackPacket(id))
            SubmoduleCrystalDestroyer.postAttackHandlers.forEach { it.attacked(id) }
        }

        Rotate.rotateBack()
    }

    private fun reset() {
        val currentWorld = mc.level
        if (currentWorld == null) {
            highestId = 0
            return
        }

        var maxFound = mc.player?.id ?: 0
        currentWorld.entitiesForRendering().forEach {
            maxFound = max(it.id, maxFound)
        }
        highestId = maxFound
        Safe.unsafeTicks = 0
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (Safe.unsafeTicks > 0) {
            Safe.unsafeTicks--
        }
    }

    @Suppress("unused")
    private val entitySpawnHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundAddEntityPacket -> {
                updateHighestId(packet.id)
                if (packet.type in HAZARDOUS_ENTITY_TYPES) {
                    Safe.markUnsafe()
                }
            }

            is ClientboundLoginPacket -> updateHighestId(packet.playerId)
            is ClientboundRespawnPacket -> reset()
            is ClientboundExplodePacket -> {
                if (Safe.enabled && offsetRange.last > 1) {
                    Safe.markUnsafe()
                }
            }
        }
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

}
