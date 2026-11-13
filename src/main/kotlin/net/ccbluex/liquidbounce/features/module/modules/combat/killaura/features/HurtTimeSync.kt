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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.world.entity.LivingEntity

/**
 * Times the attacks of KillAura to the damage immunity of the target.
 *
 * A hit sets `hurtResistantTime` to 20 ticks and the target only takes damage again once it counted down to
 * 10, so faster swings are cancelled and still count in NCP's fight speed check. The next hit goes out 10
 * ticks after ours was sent: both take half a ping to arrive, which cancels the ping out.
 */
internal object HurtTimeSync : ToggleableValueGroup(ModuleKillAura, "HurtTimeSync", false) {

    /** Ticks of the 20 after which a hit of the same damage lands again. */
    private const val IMMUNITY_TICKS = 10.0

    private const val NONE = -1.0

    /** Slack so a hit does not arrive a tick before the server accepts one. */
    private val margin by int("Margin", 1, 0..4, "ticks")

    /** Arm swings into the immunity window; a swing is no attack, so no fight check counts it. */
    private val spareSwings by int("SpareSwings", 1, 0..2, "swings")

    /** Ticks a hit waits for the fall that makes it critical. */
    private val critWindow by int("CritWindow", 2, 0..4, "ticks")

    private var target: LivingEntity? = null

    private var lastHurtTime = 0

    /** [Timer.realTicks] when the last attack went out, [NONE] while none is outstanding. */
    private var attackedAt = NONE

    /** Whether the server showed the hurt animation of that attack. */
    private var landed = false

    /** [Timer.realTicks] from which the target takes damage again. */
    private var immuneUntil = 0.0

    /** [Timer.realTicks] when the hit started waiting for a critical fall, [NONE] before. */
    private var waitingSince = NONE

    private var spares = 0

    /** Ping of one way, in ticks of real time. */
    private val oneWay get() = player.ping / 100.0

    private fun serverTicks(ticks: Double) = ticks / ServerObserver.tickSpeed.coerceAtLeast(0.1)

    /**
     * Whether an attack sent now can damage [entity].
     */
    fun allowsAttack(entity: LivingEntity): Boolean {
        if (!running) {
            return true
        }

        begin(entity)
        val now = Timer.realTicks
        retryFailedAttack(now)

        return now >= immuneUntil && !waitingForCrit(now)
    }

    /**
     * A guaranteed click on the tick [tick] ticks from now, so the hit goes out the moment the immunity ends.
     */
    fun forcedAt(tick: Int): Boolean {
        if (!running || target == null) {
            return false
        }

        val at = Timer.realTicks + tick
        if (at < immuneUntil) {
            return false
        }

        // While waiting for the fall, the click is due once the wait is over
        return !waitsForCrit() || waitingSince != NONE && at - waitingSince >= critWindow
    }

    /**
     * Called after an attack packet for [entity] went out.
     */
    fun onAttackSent(entity: LivingEntity) {
        if (!running) {
            return
        }

        begin(entity)
        attackedAt = Timer.realTicks
        landed = false
        immuneUntil = attackedAt + serverTicks(IMMUNITY_TICKS + margin)
        waitingSince = NONE
        spares = spareSwings
    }

    /** Swings the arm into the immunity window, which no fight check counts. */
    fun swingSpare(): Boolean {
        if (!running || spares <= 0) {
            return false
        }

        spares--
        SwingMode.DO_NOT_HIDE.attackSwing()
        return true
    }

    fun reset() {
        target = null
        lastHurtTime = 0
        attackedAt = NONE
        landed = false
        immuneUntil = 0.0
        waitingSince = NONE
        spares = 0
    }

    override fun onDisabled() {
        reset()
    }

    /** Takes over the immunity of a target that was damaged before we attacked it. */
    private fun begin(entity: LivingEntity) {
        if (entity === target) {
            return
        }

        target = entity
        lastHurtTime = entity.hurtTime
        attackedAt = NONE
        landed = false
        waitingSince = NONE
        spares = spareSwings

        // Hurt time counts client ticks, which held ticks did not move on
        immuneUntil = if (entity.hurtTime > 0) {
            val elapsed = entity.hurtDuration - entity.hurtTime +
                Timer.heldTicksWithin(entity.hurtDuration - entity.hurtTime)
            Timer.realTicks + serverTicks(IMMUNITY_TICKS - elapsed - oneWay + margin)
        } else {
            0.0
        }
    }

    /** A cancelled hit shows no hurt animation; opening the window again keeps it from stalling the next one. */
    private fun retryFailedAttack(now: Double) {
        if (landed || attackedAt == NONE || now < attackedAt + 2 * oneWay + 3) {
            return
        }

        attackedAt = NONE
        immuneUntil = 0.0
    }

    /** Whether the open damage window waits a few ticks for the fall that makes the hit critical. */
    private fun waitingForCrit(now: Double): Boolean {
        if (!waitsForCrit()) {
            return false
        }

        if (waitingSince == NONE) {
            waitingSince = now
        }

        return now - waitingSince < critWindow
    }

    /** A crit needs a fall, so the hit waits for it unless the Criticals module spoofs one. */
    private fun waitsForCrit(): Boolean {
        if (critWindow <= 0 || ModuleCriticals.running || ModuleCriticals.wouldDoCriticalHit()) {
            return false
        }

        if (player.onGround() || player.isSprinting || ModuleFly.running) {
            return false
        }

        return ModuleCriticals.allowsCriticalHit(ignoreOnGround = true)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val target = this.target ?: return@handler

        if (target.isDeadOrDying || target.isRemoved) {
            reset()
            return@handler
        }

        // The animation started half a ping ago, when the server applied the hit
        if (target.hurtTime > lastHurtTime) {
            immuneUntil = maxOf(
                immuneUntil,
                Timer.realTicks - 2 * oneWay + serverTicks(IMMUNITY_TICKS + margin)
            )
            landed = true
        }

        lastHurtTime = target.hurtTime
    }

}
