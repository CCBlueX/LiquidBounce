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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.chat.party

import com.mojang.authlib.GameProfile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import net.ccbluex.axochat.party.PartyMember
import net.ccbluex.axochat.party.Position
import net.ccbluex.axochat.party.Relation
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

private const val NETHER_SCALE = 8.0

private const val STALE_AFTER = 10_000L

private val ARMOR_SLOTS = arrayOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)

/**
 * Stand-ins for party members beyond render distance, so ESP, Tracers and Nametags show them.
 * They never join the world.
 */
object PartyStandIns : EventListener, Iterable<RemotePlayer> {

    private class StandIn(val player: RemotePlayer) {
        var status: JsonObject? = null
        var target: Vec3 = player.position()
    }

    private val standIns = HashMap<String, StandIn>()

    @Volatile
    private var players = emptyList<RemotePlayer>()

    // turning LiquidChat off stops the updates, so its stand-ins must not linger
    private val current get() = if (running) players else emptyList()

    override fun iterator() = current.iterator()

    fun isStandIn(entity: Entity) = entity is RemotePlayer && entity in current

    /**
     * The far plane would clip a stand-in at its real distance, so it is drawn closer and scaled down
     * to keep its size on screen.
     */
    fun anchor(position: Vec3, camera: Vec3): Pair<Vec3, Double> {
        val limit = mc.options.effectiveRenderDistance * 16.0
        val distance = position.distanceTo(camera)
        if (distance <= limit) {
            return position to 1.0
        }
        val scale = limit / distance
        return camera.add(position.subtract(camera).scale(scale)) to scale
    }

    private fun convert(position: Position, level: ClientLevel): Vec3? {
        val own = level.dimension()
        val raw = Vec3(position.x, position.y, position.z)
        val dimension = position.dimension
        return when {
            dimension == own.identifier().toString() -> raw
            own == Level.NETHER && dimension == Level.OVERWORLD.identifier().toString() ->
                Vec3(raw.x / NETHER_SCALE, raw.y, raw.z / NETHER_SCALE)
            own == Level.OVERWORLD && dimension == Level.NETHER.identifier().toString() ->
                Vec3(raw.x * NETHER_SCALE, raw.y, raw.z * NETHER_SCALE)
            else -> null
        }
    }

    private fun wanted(level: ClientLevel): Map<PartyMember, Vec3> {
        val now = System.currentTimeMillis()
        return PartyManager.others.mapNotNull { member ->
            if (member.relation != Relation.World && member.relation != Relation.Instance) {
                return@mapNotNull null
            }
            val player = member.player ?: return@mapNotNull null
            if (level.getPlayerByUUID(player.uuid) != null) {
                return@mapNotNull null
            }
            val state = PartyMemberStates[member.user.id] ?: return@mapNotNull null
            val position = state.position?.takeIf { now - state.positionAt < STALE_AFTER } ?: return@mapNotNull null
            convert(position, level)?.let { member to it }
        }.toMap()
    }

    private fun update(standIn: StandIn, member: PartyMember, level: ClientLevel) {
        val player = standIn.player
        val state = PartyMemberStates[member.user.id]

        // eases towards the latest shared position, which arrives a few times a second
        player.setOldPosAndRot()
        player.setPos(player.position().lerp(standIn.target, 0.5))
        state?.position?.let {
            player.yRot = it.yaw
            player.yHeadRot = it.yaw
            player.xRot = it.pitch
        }
        player.tickCount++

        val status = state?.status
        if (status != null && status !== standIn.status) {
            standIn.status = status
            status.amount("max_health")?.let { player.getAttribute(Attributes.MAX_HEALTH)?.baseValue = it.toDouble() }
            status.amount("health")?.let { player.health = it }
            status.amount("absorption")?.let { player.absorptionAmount = it }
            player.setItemSlot(EquipmentSlot.MAINHAND, PartyItems.decode(status["main_hand"], level))
            player.setItemSlot(EquipmentSlot.OFFHAND, PartyItems.decode(status["off_hand"], level))
            (status["armor_items"] as? JsonArray)?.forEachIndexed { index, item ->
                ARMOR_SLOTS.getOrNull(index)?.let { player.setItemSlot(it, PartyItems.decode(item, level)) }
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = (FIRST_PRIORITY + 1).toShort()) {
        val level = mc.level
        if (level == null) {
            clear()
            return@handler
        }

        val wanted = wanted(level)
        standIns.keys.retainAll(wanted.keys.mapTo(hashSetOf()) { it.user.id })
        for ((member, target) in wanted) {
            val profile = member.player!!.let { GameProfile(it.uuid, it.name) }
            val standIn = standIns.getOrPut(member.user.id) {
                StandIn(RemotePlayer(level, profile).apply {
                    id = level.nextLocalEntityId()
                    setPos(target)
                })
            }
            standIn.target = target
            update(standIn, member, level)
        }
        players = standIns.values.map { it.player }
    }

    private fun clear() {
        standIns.clear()
        players = emptyList()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    override fun parent() = GlobalSettingsClientChat

}
