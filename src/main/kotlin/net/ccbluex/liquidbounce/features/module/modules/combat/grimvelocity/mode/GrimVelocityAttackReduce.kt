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

package net.ccbluex.liquidbounce.features.module.modules.combat.grimvelocity.mode


import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleGrimVelocity
import net.ccbluex.liquidbounce.features.module.modules.combat.grimvelocity.GrimVelocityMode
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.util.Hand

object GrimVelocityAttackReduce : GrimVelocityMode("AttackReduce") {

    private enum class AttackMode(override val choiceName: String) : NamedChoice {
        ONE_TIME("OneTime"),
        PER_TICK("PerTick")
    }

    private val attackMode by enumChoice("AttackMode", AttackMode.ONE_TIME)
    private val attackCount by intRange("AttackCount", 3..3, 0..20)
    private val debug by boolean("Debug", false)

    private var targetEntity: Entity? = null
    private var attackQueue = 0
    private var receiveDamage = false

    override fun disable() {
        targetEntity = null
        attackQueue = 0
        receiveDamage = false
    }

    private fun findTarget(): Entity? {
        if (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null) {
            return ModuleKillAura.targetTracker.target
        }

        return raytraceEntity(
            ModuleKillAura.range.toDouble(),
            RotationManager.serverRotation
        ) { !it.isRemoved && it.shouldBeAttacked() }?.entity
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            receiveDamage = true
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && receiveDamage) {
            receiveDamage = false
            targetEntity = findTarget() ?: return@handler
            attackQueue = attackCount.random()
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (targetEntity != null && attackQueue > 0) {
            if (attackMode == AttackMode.ONE_TIME) {
                while (attackQueue >= 1) {
                    val entityHitResult = raytraceEntity(
                        ModuleKillAura.range.toDouble(),
                        RotationManager.serverRotation
                    ) { it == targetEntity }

                    if (entityHitResult == null) {
                        if (debug) chat("Fail to attack the target", ModuleGrimVelocity)
                        attackQueue = 0
                        break
                    }

                    network.sendPacket(PlayerInteractEntityC2SPacket.attack(targetEntity, false))
                    player.setVelocity(
                        player.velocity.x * 0.6,
                        player.velocity.y,
                        player.velocity.z * 0.6
                    )
                    player.isSprinting = false
                    player.swingHand(Hand.MAIN_HAND)
                    attackQueue--
                }
            } else if (attackMode == AttackMode.PER_TICK) {
                if (attackQueue >= 1) {
                    network.sendPacket(PlayerInteractEntityC2SPacket.attack(targetEntity, false))
                    player.setVelocity(
                        player.velocity.x * 0.6,
                        player.velocity.y,
                        player.velocity.z * 0.6
                    )
                    player.isSprinting = false
                    player.swingHand(Hand.MAIN_HAND)
                }
                attackQueue--
            }
        }
    }

}
