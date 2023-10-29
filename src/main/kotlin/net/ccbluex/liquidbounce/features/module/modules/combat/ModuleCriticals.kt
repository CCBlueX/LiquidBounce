/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleLiquidWalk
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.exactPosition
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * Criticals module
 *
 * Automatically crits every time you attack someone.
 */
object ModuleCriticals : Module("Criticals", Category.COMBAT) {

    val modes = choices("Mode", PacketCrit) {
        arrayOf(
            NoneChoice(it), PacketCrit, JumpCrit
        )
    }

    private object PacketCrit : Choice("Packet") {

        private object WhenSprinting : ToggleableConfigurable(ModuleCriticals, "WhenSprinting", true) {
            val unSprint by boolean("UnSprint", false)
        }

        init {
            tree(WhenSprinting)
        }

        override val parent: ChoiceConfigurable
            get() = modes

        val attackHandler = handler<AttackEvent> { event ->
            if (!enabled || event.enemy !is LivingEntity) {
                return@handler
            }

            if (!canCritNow(player, true, WhenSprinting.enabled)) {
                return@handler
            }

            if(WhenSprinting.unSprint && player.isSprinting) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                player.isSprinting = false
            }

            val (x, y, z) = player.exactPosition

            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.11, z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.1100013579, z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0000013579, z, false))
        }
    }

    private object JumpCrit : Choice("Jump") {

        override val parent: ChoiceConfigurable
            get() = modes

        // There are different possible jump heights to crit enemy
        //   Hop: 0.1 (like in Wurst-Client)
        //   LowJump: 0.3425 (for some weird AAC version)
        //
        val height by float("Height", 0.42f, 0.1f..0.42f)

        // Jump crit should just be active until an enemy is in your reach to be attacked
        val range by float("Range", 4f, 1f..6f)

        val optimizeForCooldown by boolean("OptimizeForCooldown", true)

        val tickHandler = handler<PlayerTickEvent> {
            if (!enabled) return@handler

            if (!canCrit(player, true)) {
                return@handler
            }

            if (optimizeForCooldown && shouldWaitForJump()) {
                return@handler
            }

            world.findEnemy(0f..range) ?: return@handler

            if (player.isOnGround) {
                // Simulate player jumping and send jump stat increment
                player.jump()
            }
        }

        val onJump = handler<PlayerJumpEvent> { event ->
            // Only change if there is nothing affecting the default motion (like a honey block)
            if (event.motion == 0.42f) {
                event.motion = height
            }
        }

    }

    fun shouldWaitForJump(initialMotion: Float = 0.42f): Boolean {
        if (!canCrit(player, true)) {
            return false
        }

        val ticksTillFall = initialMotion / 0.08f

        val nextPossibleCrit =
            (player.attackCooldownProgressPerTick - 0.5f - player.lastAttackedTicks.toFloat()).coerceAtLeast(0.0f)

        var ticksTillNextOnGround = FallingPlayer(
            player,
            player.x,
            player.y,
            player.z,
            player.velocity.x,
            player.velocity.y + initialMotion,
            player.velocity.z,
            player.yaw
        ).findCollision((ticksTillFall * 2.25f).toInt())?.tick

        if (ticksTillNextOnGround == null) {
            ticksTillNextOnGround = ticksTillFall.toInt() * 2
        }

        if (ticksTillNextOnGround + ticksTillFall < nextPossibleCrit) {
            return false
        }

        return ticksTillFall + 1.0f < nextPossibleCrit
    }

    /**
     * Just some visuals.
     */
    private object VisualsConfigurable : ToggleableConfigurable(this, "Visuals", true) {

        val fake by boolean("Fake", false)

        val critParticles by int("CritParticles", 1, 0..20)
        val magicParticles by int("MagicParticles", 0, 0..20)

        val attackHandler = handler<AttackEvent> { event ->
            if (event.enemy !is LivingEntity) {
                return@handler
            }

            if (!fake && !canCrit(player, ignoreOnGround = true)) {
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

    var ticksOnGround = 0

    val repeatable = repeatable {
        if (player.isOnGround) {
            ticksOnGround++
        } else {
            ticksOnGround = 0
        }
    }

    init {
        tree(VisualsConfigurable)
    }

    /**
     * Sometimes when the player is almost at the highest point of his jump, the KillAura
     * will try to attack the enemy anyways. To maximise damage, this function is used to determine
     * whether or not it is worth to wait for the fall
     */
    fun shouldWaitForCrit(): Boolean {
        if (!enabled) return false

        val player = player

        if (!canCrit(player) || player.velocity.y < -0.08) {
            return false
        }

        val nextPossibleCrit =
            (player.attackCooldownProgressPerTick - 0.5f - player.lastAttackedTicks.toFloat()).coerceAtLeast(0.0f)

        val gravity = 0.08

        val ticksTillFall = (player.velocity.y / gravity).toFloat()

        val ticksTillCrit = nextPossibleCrit.coerceAtLeast(ticksTillFall)

        val hitProbability = 0.6f

        val damageOnCrit = 0.5f * hitProbability

        val damageLostWaiting = getCooldownDamageFactor(player, ticksTillCrit)

        if (damageOnCrit > damageLostWaiting) {
            if (FallingPlayer.fromPlayer(player).findCollision((ticksTillCrit * 1.3f).toInt()) == null) {
                return true
            }
        }

        return false
    }

    fun canCrit(player: ClientPlayerEntity, ignoreOnGround: Boolean = false) =
        !player.isInLava && !player.isTouchingWater && !player.isClimbing && !player.hasNoGravity() && !player.hasStatusEffect(
            StatusEffects.LEVITATION
        ) && !player.hasStatusEffect(StatusEffects.BLINDNESS) && !player.hasStatusEffect(StatusEffects.SLOW_FALLING) && !player.isRiding && (!player.isOnGround || ignoreOnGround) && !ModuleFly.enabled && !(ModuleLiquidWalk.enabled && ModuleLiquidWalk.standingOnWater())

    fun canCritNow(player: ClientPlayerEntity, ignoreOnGround: Boolean = false, ignoreSprint: Boolean = false) =
        canCrit(player, ignoreOnGround) &&
            ModuleCriticals.player.getAttackCooldownProgress(0.5f) > 0.9f &&
            (!ModuleCriticals.player.isSprinting || ignoreSprint)
    fun getCooldownDamageFactorWithCurrentTickDelta(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((player.lastAttackedTicks.toFloat() + tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    private fun getCooldownDamageFactor(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    fun wouldCrit(ignoreSprint: Boolean = false): Boolean {
        return canCritNow(player, false, ignoreSprint) && player.fallDistance > 0.0
    }

}
