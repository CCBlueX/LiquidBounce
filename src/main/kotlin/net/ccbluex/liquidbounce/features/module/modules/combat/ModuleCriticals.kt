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
package net.ccbluex.liquidbounce.features.module.modules.combat

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals.VisualsConfigurable.showCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.debugRecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.combat.findEnemies
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.block.CobwebBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Vec3d

/**
 * Criticals module
 *
 * Automatically crits every time you attack someone.
 */
object ModuleCriticals : Module("Criticals", Category.COMBAT) {

    init {
        enableLock()
    }

    val modes = choices<Choice>("Mode", { PacketCrit }) {
        arrayOf(
            NoneChoice(it),
            PacketCrit,
            NoGroundCrit,
            JumpCrit
        )
    }

    private object PacketCrit : Choice("Packet") {

        private val mode by enumChoice("Mode", Mode.NO_CHEAT_PLUS)
        private val packetType by enumChoice("PacketType", MovePacketType.FULL)

        private object WhenSprinting : ToggleableConfigurable(ModuleCriticals, "WhenSprinting", true) {
            val unSprint by boolean("UnSprint", false)
        }

        init {
            tree(WhenSprinting)
        }

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val attackHandler = handler<AttackEvent> { event ->
            if (!enabled || event.enemy !is LivingEntity) {
                return@handler
            }

            val ignoreSprinting = !WhenSprinting.enabled || (WhenSprinting.enabled && WhenSprinting.unSprint)

            if (!canCritNow(true, ignoreSprinting)) {
                return@handler
            }

            if (WhenSprinting.enabled && WhenSprinting.unSprint && player.isSprinting) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                player.isSprinting = false
            }

            when (mode) {
                Mode.VANILLA -> {
                    modVelocity(0.2)
                    modVelocity(0.01)
                    showCriticals(event.enemy)
                }

                Mode.NO_CHEAT_PLUS -> {
                    modVelocity(0.11)
                    modVelocity(0.1100013579)
                    modVelocity(0.0000013579)
                    showCriticals(event.enemy)
                }

                Mode.FALLING -> {
                    modVelocity(0.0625)
                    modVelocity(0.0625013579)
                    modVelocity(0.0000013579)
                    showCriticals(event.enemy)
                }

                Mode.BLOCKSMC -> {
                    if (player.age % 4 == 0) {
                        modVelocity(0.0011, true)
                        modVelocity(0.0)
                        showCriticals(event.enemy)
                    }
                }

                Mode.GRIM -> {
                    if (!player.isOnGround) {
                        // If player is in air, go down a little bit.
                        // Vanilla still crits and movement is too small
                        // for simulation checks.

                        // Requires packet type to be .FULL
                        modVelocity(-0.000001)

                        showCriticals(event.enemy)
                    }
                }
            }
        }

        private fun modVelocity(mod: Double, onGround: Boolean = false) {
            network.sendPacket(packetType.generatePacket().apply {
                this.y += mod
                this.onGround = onGround
            })
        }

        enum class Mode(override val choiceName: String) : NamedChoice {
            VANILLA("Vanilla"),
            NO_CHEAT_PLUS("NoCheatPlus"),
            FALLING("Falling"),
            GRIM("Grim"),
            BLOCKSMC("BlocksMC")
        }

    }


    /**
     * Same thing as NoGround NoFall mode
     */
    object NoGroundCrit : Choice("NoGround") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = false
            }

        }

    }

    object JumpCrit : Choice("Jump") {

        override val parent: ChoiceConfigurable<*>
            get() = modes

        // There are different possible jump heights to crit enemy
        //   Hop: 0.1 (like in Wurst-Client)
        //   LowJump: 0.3425 (for some weird AAC version)
        //
        val height by float("Height", 0.42f, 0.1f..0.42f)

        // Jump crit should just be active until an enemy is in your reach to be attacked
        val range by float("Range", 4f, 1f..6f)

        val optimizeForCooldown by boolean("OptimizeForCooldown", true)

        val checkKillaura by boolean("CheckKillaura", false)
        val checkAutoClicker by boolean("CheckAutoClicker", false)
        val canBeSeen by boolean("CanBeSeen", true)

        /**
         * Should the upwards velocity be set to the `height`-value on next jump?
         *
         * Only true when auto-jumping is currently taking place so that normal jumps
         * are not affected.
         */
        var adjustNextJump = false

        val movementInputEvent = handler<MovementInputEvent> {
            if (!isActive()) {
                return@handler
            }

            if (!canCrit(true)) {
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
                it.jumping = true
                adjustNextJump = true
            }
        }

        val onJump = handler<PlayerJumpEvent> { event ->
            // The `value`-option only changes *normal jumps* with upwards velocity 0.42.
            // Jumps with lower velocity (i.e. from honey blocks) are not affected.
            val isJumpNormal = event.motion == 0.42f

            // Is the jump a normal jump and auto-jumping is enabled.
            if (isJumpNormal && adjustNextJump) {
                event.motion = height
                adjustNextJump = false
            }
        }

    }

    fun shouldWaitForJump(initialMotion: Float = 0.42f): Boolean {
        if (!canCrit(true) || !enabled) {
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

    /**
     * Just some visuals.
     */
    private object VisualsConfigurable : ToggleableConfigurable(this, "Visuals", false) {

        val fake by boolean("Fake", false)

        val critParticles by int("CritParticles", 1, 0..20)
        val magicParticles by int("MagicParticles", 0, 0..20)

        @Suppress("unused")
        val attackHandler = handler<AttackEvent> { event ->
            if (event.enemy !is LivingEntity) {
                return@handler
            }

            if (!fake && !wouldCrit()) {
                return@handler
            }

            showCriticals(event.enemy)
        }

        fun showCriticals(entity: Entity) {
            if (!enabled) {
                return
            }

            repeat(critParticles) {
                player.addCritParticles(entity)
            }

            repeat(magicParticles) {
                player.addEnchantedHitParticles(entity)
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

    fun isActive(): Boolean {
        if (!enabled)
            return false

        // if both module checks are disabled, we can safely say that we are active
        if (!JumpCrit.checkKillaura && !JumpCrit.checkAutoClicker)
            return true

        return (ModuleKillAura.enabled && JumpCrit.checkKillaura) ||
            (ModuleAutoClicker.enabled && JumpCrit.checkAutoClicker)
    }

    /**
     * This function simulates a chase between the player and the target. The target continues its motion, the player
     * too but changes their rotation to the target after some reaction time.
     */
    fun predictPlayerPos(target: PlayerEntity, ticks: Int): Pair<Vec3d, Vec3d> {
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
                simulatedPlayer.yaw = RotationManager.makeRotation(target.pos, simulatedPlayer.pos).yaw
            }

            simulatedPlayer.tick()
            simulatedTarget.tick()
        }

        return simulatedPlayer.pos to simulatedTarget.pos
    }

    /**
     * Sometimes when the player is almost at the highest point of his jump, the KillAura
     * will try to attack the enemy anyways. To maximise damage, this function is used to determine
     * whether or not it is worth to wait for the fall
     */
    fun shouldWaitForCrit(target: Entity, ignoreState: Boolean = false): Boolean {
        if (!isActive() && !ignoreState) {
            return false
        }

        if (!canCrit() || player.velocity.y < -0.08) {
            return false
        }

        val nextPossibleCrit =
            calculateTicksUntilNextCrit()

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

    fun canCrit(ignoreOnGround: Boolean = false): Boolean {
        val blockingEffects = arrayOf(LEVITATION, BLINDNESS, SLOW_FALLING)

        val blockingConditions = arrayOf(
            // Modules
            ModuleFly.enabled,
            ModuleLiquidWalk.enabled && ModuleLiquidWalk.standingOnWater(),
            player.isInLava, player.isTouchingWater, player.hasVehicle(),
            // Cobwebs
            collideBlockIntersects(player.box, checkCollisionShape = false) { it is CobwebBlock },
            // Effects
            blockingEffects.any(player::hasStatusEffect),
            // Disabling conditions
            player.isClimbing, player.hasNoGravity(), player.isRiding,
            player.abilities.flying,
            // On Ground
            player.isOnGround && !ignoreOnGround
        )

        // Do not replace this with .none() since it is equivalent to .isEmpty()
        return blockingConditions.none { it }
    }

    fun canCritNow(ignoreOnGround: Boolean = false, ignoreSprint: Boolean = false) =
        canCrit(ignoreOnGround) && player.getAttackCooldownProgress(0.5f) > 0.9f &&
            (!player.isSprinting || ignoreSprint)

    fun getCooldownDamageFactorWithCurrentTickDelta(tickDelta: Float): Float {
        val base = ((player.lastAttackedTicks.toFloat() + tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    private fun getCooldownDamageFactor(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    fun wouldCrit(ignoreSprint: Boolean = false): Boolean {
        return canCritNow(false, ignoreSprint) && player.fallDistance > 0.0
    }

}
