/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.exactPosition
import net.ccbluex.liquidbounce.utils.entity.upwards
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * Criticals module
 *
 * Automatically crits every time you attack someone
 */
object ModuleCriticals : Module("Criticals", Category.COMBAT) {

    /**
     * Should criticals be active or passive?
     */
    private object ActiveOption : ToggleableConfigurable(this, "Active", true) {

        val modes = choices("Mode", "Packet") {
            NoneChoice(it)
            PacketCrit
            JumpCrit
        }
    }

    private object PacketCrit : Choice("Packet", ActiveOption.modes) {

        val attackHandler = handler<AttackEvent> { event ->
            if (!ActiveOption.enabled || event.enemy !is LivingEntity) {
                return@handler
            }

            val (x, y, z) = player.exactPosition

            network.sendPacket(PlayerMoveC2SPacket.PositionOnly(x, y + 0.11, z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionOnly(x, y + 0.1100013579, z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionOnly(x, y + 0.0000013579, z, false))
        }

    }

    private object JumpCrit : Choice("Jump", ActiveOption.modes) {

        // There are diffrent possible jump heights to crit enemy
        //   Hop: 0.1 (like in Wurst-Client)
        //   LowJump: 0.3425 (for some weird AAC version)
        //
        val height by float("Height", 0.42f, 0.1f..0.42f)

        // Jump crit should just be active until an enemy is in your reach to be attacked
        val range by float("Range", 4f, 1f..6f)

        val tickHandler = handler<PlayerTickEvent> {
            if (!ActiveOption.enabled) return@handler

            val (_, _) = world.findEnemy(range) ?: return@handler

            if (player.isOnGround) {
                player.upwards(height)
            }
        }

    }

    /**
     * Just some visuals.
     */
    private object VisualsConfigurable : ToggleableConfigurable(this, "Visuals", true) {

        val critParticles by int("CritParticles", 1, 0..20)
        val magicParticles by int("MagicParticles", 0, 0..20)

        val attackHandler = handler<AttackEvent> { event ->
            if (event.enemy !is LivingEntity) {
                return@handler
            }

            repeat(critParticles) {
                player.addCritParticles(event.enemy)
            }

            repeat(magicParticles) {
                player.addEnchantedHitParticles(event.enemy)
            }
        }

    }

    init {
        tree(VisualsConfigurable)
        tree(ActiveOption)
    }

    /**
     * Sometimes when the player is almost at the highest point of his jump, the KillAura
     * will try to attack the enemy anyways. To maximise damage, this function is used to determine
     * whether or not it is worth to wait for the fall
     */
    fun shouldWaitForCrit(): Boolean {
        if (!enabled) return false

        val player = player

        if (!canCrit(player) || player.velocity.y < -0.08)
            return false

        val nextPossibleCrit = (player.attackCooldownProgressPerTick - 0.5f - player.lastAttackedTicks.toFloat()).coerceAtLeast(0.0f)

        val gravity = 0.08

        val ticksTillFall = (player.velocity.y / gravity).toFloat()

        val ticksTillCrit = nextPossibleCrit.coerceAtLeast(ticksTillFall)

        val hitProbability = 0.6f

        val damageOnCrit = getCooldownDamageFactorWithCurrentTickDelta(player, ticksTillCrit) * (1.0f + 0.5f * hitProbability)

        val damageLostWaiting = 1.0f + getCooldownDamageFactor(player, ticksTillCrit)

        return damageOnCrit > damageLostWaiting
    }

    fun canCrit(player: ClientPlayerEntity) =
        !player.isInLava && !player.isTouchingWater && !player.isHoldingOntoLadder && !player.hasNoGravity() && !player.hasStatusEffect(
            StatusEffects.LEVITATION
        ) && !player.hasStatusEffect(StatusEffects.BLINDNESS) && !player.hasStatusEffect(StatusEffects.SLOW_FALLING) && !player.isRiding && !player.isOnGround

    fun getCooldownDamageFactorWithCurrentTickDelta(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((player.lastAttackedTicks.toFloat() + tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    private fun getCooldownDamageFactor(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    fun wouldCrit(ignoreSprint: Boolean = false): Boolean {
        return canCrit(player) && player.velocity.y < -0.08 && (!player.isSprinting || ignoreSprint)
    }

}
