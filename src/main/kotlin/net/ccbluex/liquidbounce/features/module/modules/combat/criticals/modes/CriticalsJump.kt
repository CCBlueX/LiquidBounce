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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.allowsCriticalHit
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.findEnemies
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d

object CriticalsJump : Choice("Jump") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleCriticals.modes

    // There are different possible jump heights to crit enemy
    //   Hop: 0.1 (like in Wurst-Client)
    //   LowJump: 0.3425 (for some weird AAC version)
    //
    val height by float("Height", 0.42f, 0.1f..0.42f)

    // Jump crit should just be active until an enemy is in your reach to be attacked
    val range by float("Range", 4f, 1f..6f)

    private val optimizeForCooldown by boolean("OptimizeForCooldown", true)

    private val checkKillaura by boolean("CheckKillaura", false)
    private val checkAutoClicker by boolean("CheckAutoClicker", false)
    private val canBeSeen by boolean("CanBeSeen", true)

    /**
     * Should the upwards velocity be set to the `height`-value on next jump?
     *
     * Only true when auto-jumping is currently taking place so that normal jumps
     * are not affected.
     */
    private var adjustNextJump = false

    @Suppress("unused")
    private val movementInputEvent = handler<MovementInputEvent> { event ->
        if (!isActive()) {
            return@handler
        }

        if (!allowsCriticalHit(true)) {
            return@handler
        }

        if (optimizeForCooldown && shouldWaitForJump()) {
            return@handler
        }

        val enemies = world.findEnemies(0f..range)
            .filter { (entity, _) -> !canBeSeen || player.canSee(entity) }

        // Change the jump motion only if the jump is a normal jump (small jumps, i.e. honey blocks
        // are not affected) and currently.
        if (enemies.isNotEmpty() && player.isOnGround) {
            event.jump = true
            adjustNextJump = true
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        // The `value`-option only changes *normal jumps* with upwards velocity 0.42.
        // Jumps with lower velocity (i.e. from honey blocks) are not affected.
        val isJumpNormal = event.motion == 0.42f

        // Is the jump a normal jump and auto-jumping is enabled.
        if (isJumpNormal && adjustNextJump) {
            event.motion = height
            adjustNextJump = false
        }
    }

    /**
     * Sometimes when the player is almost at the highest point of his jump, the KillAura
     * will try to attack the enemy anyway. To maximise damage, this function is used to determine
     * whether it is worth to wait for the fall.
     */
    fun shouldWaitForCrit(target: Entity, ignoreState: Boolean = false): Boolean {
        if (!isActive() && !ignoreState) {
            return false
        }

        if (player.isGliding) {
            return false
        }

        if (!allowsCriticalHit() || player.velocity.y < -0.08) {
            return false
        }

        val nextPossibleCrit = calculateTicksUntilNextCrit()
        val gravity = 0.08
        val ticksTillFall = (player.velocity.y / gravity).toFloat()
        val ticksTillCrit = nextPossibleCrit.coerceAtLeast(ticksTillFall)
        val hitProbability = 0.75f
        val damageOnCrit = 0.5f * hitProbability
        val damageLostWaiting = getCooldownDamageFactor(player, ticksTillCrit)

        val (simulatedPlayerPos, simulatedTargetPos) = if (target is PlayerEntity) {
            predictPlayerPos(target, ticksTillCrit.toInt())
        } else {
            player.pos to target.pos
        }

        ModuleDebug.debugParameter(ModuleCriticals, "timeToCrit", ticksTillCrit)

        GenericDebugRecorder.recordDebugInfo(ModuleCriticals, "critEstimation", JsonObject().apply {
            addProperty("ticksTillCrit", ticksTillCrit)
            addProperty("damageOnCrit", damageOnCrit)
            addProperty("damageLostWaiting", damageLostWaiting)
            add("player", GenericDebugRecorder.debugObject(player))
            add("target", GenericDebugRecorder.debugObject(target))
            addProperty("simulatedPlayerPos", simulatedPlayerPos.toString())
            addProperty("simulatedTargetPos", simulatedTargetPos.toString())
        })

        GenericDebugRecorder.debugEntityIn(target, ticksTillCrit.toInt())

        if (damageOnCrit <= damageLostWaiting) {
            return false
        }

        if (FallingPlayer.fromPlayer(player).findCollision((ticksTillCrit * 1.3f).toInt()) == null) {
            return true
        }

        return false
    }

    private fun calculateTicksUntilNextCrit(): Float {
        val durationToWait = player.attackCooldownProgressPerTick * 0.9F - 0.5F
        val waitedDuration = player.lastAttackedTicks.toFloat()

        return (durationToWait - waitedDuration).coerceAtLeast(0.0f)
    }

    private fun getCooldownDamageFactor(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    /**
     * This function simulates a chase between the player and the target. The target continues its motion, the player
     * too but changes their rotation to the target after some reaction time.
     */
    private fun predictPlayerPos(target: PlayerEntity, ticks: Int): Pair<Vec3d, Vec3d> {
        // Ticks until the player
        val reactionTime = 10

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input))
        )
        val simulatedTarget = SimulatedPlayer.fromOtherPlayer(
            target,
            SimulatedPlayer.SimulatedPlayerInput.guessInput(target)
        )

        for (i in 0 until ticks) {
            // Rotate to the target after some time
            if (i == reactionTime) {
                simulatedPlayer.yaw = Rotation.lookingAt(point = target.pos, from = simulatedPlayer.pos).yaw
            }

            simulatedPlayer.tick()
            simulatedTarget.tick()
        }

        return simulatedPlayer.pos to simulatedTarget.pos
    }

    fun shouldWaitForJump(initialMotion: Float = 0.42f): Boolean {
        if (!allowsCriticalHit(true) || !running) {
            return false
        }

        val ticksTillFall = initialMotion / 0.08f
        val nextPossibleCrit = calculateTicksUntilNextCrit()

        var ticksTillNextOnGround = FallingPlayer(
            player,
            player.x,
            player.y,
            player.z,
            player.velocity.x,
            player.velocity.y + initialMotion,
            player.velocity.z,
            player.yaw
        ).findCollision((ticksTillFall * 3.0f).toInt())?.tick

        if (ticksTillNextOnGround == null) {
            ticksTillNextOnGround = ticksTillFall.toInt() * 2
        }

        if (ticksTillNextOnGround + ticksTillFall < nextPossibleCrit) {
            return false
        }

        return ticksTillFall + 1.0f < nextPossibleCrit
    }

    private fun isActive(): Boolean {
        if (!ModuleCriticals.running) {
            return false
        }

        // if both module checks are disabled, we can safely say that we are active
        if (!checkKillaura && !checkAutoClicker) {
            return true
        }

        return (ModuleKillAura.running && checkKillaura) ||
            (ModuleAutoClicker.running && checkAutoClicker)
    }

}


