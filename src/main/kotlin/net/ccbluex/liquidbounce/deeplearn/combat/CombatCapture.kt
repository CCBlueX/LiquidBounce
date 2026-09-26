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
package net.ccbluex.liquidbounce.deeplearn.combat

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.BLOCKING
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.BURST
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.CRITICAL_HIT
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.DEATH
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.FALL_FLYING
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.HORIZONTAL_COLLISION
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.HURT
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.IN_WATER
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.ITEM_SWITCH
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.KNOCKBACK
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.MAGIC_CRITICAL_HIT
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.OFFHAND_SHIELD
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.ON_GROUND
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.POSITION_SYNC
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.POSITION_UPDATE
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.ROTATION_UPDATE
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.SNEAKING
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.SPRINTING
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.SWING
import net.ccbluex.liquidbounce.deeplearn.combat.CombatTrack.Companion.USING_ITEM
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAi
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ServerboundPunchPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.ProjectileWeaponItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * One tick of one fighter, in world coordinates.
 */
@UnstableAddonApi
class CombatFrame(
    val entityId: Int,
    val position: Vec3,
    val yaw: Float,
    val pitch: Float,
    val state: Int,
    val events: Int,
    val damageCause: Int,
    val hurtTime: Int,
    val health: Int,
    val attackDelay: Int,
    val attackStrength: Int,
    val item: CombatItem,
    val width: Float,
    val height: Float,
    val eyeHeight: Float,
) {
    val eyePosition: Vec3 get() = position.add(0.0, eyeHeight.toDouble(), 0.0)

    val box: AABB
        get() = AABB(
            position.x - width / 2.0, position.y, position.z - width / 2.0,
            position.x + width / 2.0, position.y + height, position.z + width / 2.0
        )

    fun copy(yaw: Float = this.yaw, pitch: Float = this.pitch, events: Int = this.events) = CombatFrame(
        entityId, position, yaw, pitch, state, events, damageCause, hurtTime, health, attackDelay, attackStrength, item,
        width, height, eyeHeight,
    )

    fun withEvents(extra: Int) = copy(events = events or extra)
}

/**
 * Incoming entity packets of the current tick. Vanilla applies them before [GameTickEvent].
 */
@UnstableAddonApi
object CombatPackets : EventListener {
    private val queue = ConcurrentLinkedQueue<Packet<*>>()
    private val events = Int2IntOpenHashMap()
    private val moves = Int2IntOpenHashMap()
    private val consumers = CopyOnWriteArraySet<EventListener>()

    override val running get() = KillAuraAi.active || consumers.any { it.running }

    /** Keeps capturing while [consumer] runs, for frames sampled outside of KillAura. */
    fun capture(consumer: EventListener) {
        consumers += consumer
    }

    /** Backtrack and incoming blink hold back packets, so the world no longer matches arrival times. */
    val delayed: Boolean
        get() = ModuleBacktrack.running || BlinkManager.packetQueue.any { it.origin == TransferOrigin.INCOMING }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        if (event.isCancelled) {
            return@handler
        }
        val packet = event.packet
        val relevant = if (event.origin == TransferOrigin.INCOMING) {
            packet is ClientboundAnimatePacket ||
                packet is ClientboundSetEntityMotionPacket || packet is ClientboundSetEquipmentPacket ||
                packet is ClientboundMoveEntityPacket || packet is ClientboundEntityPositionSyncPacket ||
                packet is ClientboundTeleportEntityPacket || packet is ClientboundEntityEventPacket
        } else {
            packet is ServerboundPunchPacket ||
                packet is ServerboundSetCarriedItemPacket
        }
        if (relevant) {
            queue.add(packet)
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        events.clear()
        moves.clear()
        val level = mc.level
        val player = mc.player
        while (true) {
            val packet = queue.poll() ?: break
            if (level == null || player == null) {
                continue
            }
            when (packet) {
                is ClientboundAnimatePacket -> when (packet.action) {
                    ClientboundAnimatePacket.CRITICAL_HIT -> mark(packet.id, CRITICAL_HIT)
                    ClientboundAnimatePacket.MAGIC_CRITICAL_HIT -> mark(packet.id, MAGIC_CRITICAL_HIT)
                }
                is ClientboundSetEntityMotionPacket -> mark(packet.id, KNOCKBACK)
                is ClientboundSetEquipmentPacket -> if (packet.slots.any { it.first == EquipmentSlot.MAINHAND }) {
                    mark(packet.entity, ITEM_SWITCH)
                }
                is ClientboundMoveEntityPacket -> packet.getEntity(level)?.let { entity ->
                    move(entity.id, (if (packet.hasPosition()) POSITION_UPDATE else 0) or
                        (if (packet.hasRotation()) ROTATION_UPDATE else 0))
                }
                is ClientboundEntityPositionSyncPacket ->
                    move(packet.id, POSITION_SYNC or POSITION_UPDATE or ROTATION_UPDATE)
                is ClientboundTeleportEntityPacket -> move(packet.id, POSITION_SYNC or POSITION_UPDATE)
                is ClientboundEntityEventPacket -> if (packet.eventId == EntityEvent.DEATH) {
                    packet.getEntity(level)?.let { mark(it.id, DEATH) }
                }
                is ServerboundPunchPacket -> mark(player.id, SWING)
                is ServerboundSetCarriedItemPacket -> mark(player.id, ITEM_SWITCH)
            }
        }
    }

    /**
     * Packet flags may be off by a tick, since a packet can arrive after vanilla handled this
     * tick's packets. Anything that has to line up with positions is read from entity state.
     */
    fun events(entityId: Int) = events.get(entityId)

    fun clear() {
        queue.clear()
        events.clear()
        moves.clear()
    }

    private fun mark(entityId: Int, flags: Int) {
        events.put(entityId, events.get(entityId) or flags)
    }

    private fun move(entityId: Int, flags: Int) {
        mark(entityId, if (moves.addTo(entityId, 1) >= 2) flags or BURST else flags)
    }
}

@UnstableAddonApi
object CombatSampler {
    private const val PROBE_DISTANCE = 0.6
    private const val NEAR_SWING = 4.0

    fun frame(entity: LivingEntity): CombatFrame {
        val local = entity is LocalPlayer
        // Remote entities interpolate towards the last packet; the packet values are what the server saw.
        val position = if (local) entity.position() else entity.positionCodec.base
        val interpolation = if (local) null else entity.clientPositionAndRotation
        // Vanilla sets these when it handles the packet, and the entity tick after this event advances them.
        val swung = !local && entity.isSwinging && entity.swingState.ticks == 0
        val hurt = entity.hurtTime == entity.hurtDuration && entity.hurtDuration > 0
        val events = CombatPackets.events(entity.id) or (if (swung) SWING else 0) or (if (hurt) HURT else 0)
        return CombatFrame(
            entityId = entity.id,
            position = position,
            yaw = interpolation?.yRot() ?: entity.yRot,
            pitch = interpolation?.xRot() ?: entity.xRot,
            state = state(entity, local),
            events = events,
            damageCause = if (hurt) entity.lastDamageSource?.entity?.id ?: -1 else -1,
            hurtTime = entity.hurtTime.coerceIn(0, 10),
            health = health(entity),
            attackDelay = attackDelay(entity),
            attackStrength = if (local) {
                ((entity as Player).getAttackStrengthScale(0f) * 100f).roundToInt().coerceIn(0, 100)
            } else {
                CombatTrack.UNKNOWN
            },
            item = item(entity.mainHandItem),
            width = entity.bbWidth,
            height = entity.bbHeight,
            eyeHeight = entity.eyeHeight,
        )
    }

    /** Players within 16 blocks of us, which is as far as nearby fighters are measured. */
    fun nearbyPlayers(): List<CombatFrame> = mc.level?.players().orEmpty()
        .filter { it !== mc.player && mc.player?.let { player -> it.distanceToSqr(player) < 256.0 } == true }
        .map(::frame)

    private fun state(entity: LivingEntity, local: Boolean) = listOf(
        entity.onGround() to ON_GROUND,
        entity.isSprinting to SPRINTING,
        entity.isShiftKeyDown to SNEAKING,
        entity.isUsingItem to USING_ITEM,
        entity.isBlocking to BLOCKING,
        entity.isInWater to IN_WATER,
        (local && entity.horizontalCollision) to HORIZONTAL_COLLISION,
        entity.offhandItem.has(DataComponents.BLOCKS_ATTACKS) to OFFHAND_SHIELD,
        entity.isFallFlying to FALL_FLYING,
    ).fold(0) { state, (value, bit) -> if (value) state or bit else state }

    private fun health(entity: LivingEntity): Int {
        val maxHealth = entity.maxHealth
        val health = entity.health
        return if (maxHealth > 0f && health.isFinite()) {
            (health / maxHealth * 254f).roundToInt().coerceIn(0, 254)
        } else {
            CombatTrack.UNKNOWN
        }
    }

    fun lineOfSight(level: Level, self: CombatFrame, target: CombatFrame): Boolean {
        val from = self.eyePosition
        val to = target.eyePosition
        return to.distanceTo(from) <= 128.0 && level.clip(
            ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())
        ).type == HitResult.Type.MISS
    }

    /** Bit `i` is set when moving towards world yaw `i * 22.5` stays on solid, free ground. */
    fun probes(level: Level, frame: CombatFrame): Int {
        val box = frame.box
        var result = 0
        for (direction in 0 until 16) {
            val angle = Math.toRadians(direction * 22.5)
            val moved = box.move(-sin(angle) * PROBE_DISTANCE, 0.0, cos(angle) * PROBE_DISTANCE)
            if (level.noCollision(moved) && !level.noCollision(moved.move(0.0, -0.6, 0.0))) {
                result = result or (1 shl direction)
            }
        }
        return result
    }

    fun nearestOther(self: CombatFrame, target: CombatFrame, frames: Collection<CombatFrame>): Int {
        val distance = frames.minOfOrNull { other ->
            if (other.entityId == self.entityId || other.entityId == target.entityId) {
                Double.MAX_VALUE
            } else {
                other.position.distanceTo(self.position)
            }
        } ?: Double.MAX_VALUE
        return if (distance < 15.9) (distance * 16).roundToInt() else CombatTrack.UNKNOWN
    }

    fun othersSwingNear(self: CombatFrame, target: CombatFrame, frames: Collection<CombatFrame>) = frames.any {
        it.entityId != self.entityId && it.entityId != target.entityId && it.events and SWING != 0 &&
            it.position.distanceToSqr(self.position) < NEAR_SWING * NEAR_SWING
    }

    private fun attackDelay(entity: LivingEntity): Int {
        val speed = if (entity is LocalPlayer) {
            entity.getAttributeValue(Attributes.ATTACK_SPEED)
        } else {
            entity.mainHandItem.attackSpeed
        }
        return if (speed > 0.0) (200.0 / speed).roundToInt().coerceIn(0, 254) else CombatTrack.UNKNOWN
    }

    private fun item(stack: ItemStack) = when {
        stack.isEmpty -> CombatItem.EMPTY
        stack.`is`(ItemTags.SWORDS) -> CombatItem.SWORD
        stack.`is`(ItemTags.AXES) -> CombatItem.AXE
        stack.item is MaceItem -> CombatItem.MACE
        stack.item is ProjectileWeaponItem || stack.item is TridentItem -> CombatItem.RANGED
        stack.has(DataComponents.CONSUMABLE) -> CombatItem.CONSUMABLE
        stack.item is BlockItem -> CombatItem.BLOCK
        else -> CombatItem.OTHER
    }
}
