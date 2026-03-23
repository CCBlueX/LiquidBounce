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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.item.isSpear
import net.ccbluex.liquidbounce.utils.raytracing.clip
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

object ModuleSpearKill : ClientModule("SpearKill", ModuleCategories.COMBAT, aliases = listOf("AutoSpear")) {

    private const val MIN_SPEAR_DISTANCE = 3f
    private val maxTargetDistance by float("MaxTargetDistance", 50f, 2f..128f)
    private val maxSpeed by float("MaxSpeed", 7f, 2f..10f)
    private val autoAttackOnCharge by boolean("AutoAttackOnCharge", true)

    private object Preview : ToggleableValueGroup(this, "Preview", true) {
        val fillColor by color("FillColor", Color4b.RED.alpha(67))
        val outlineColor by color("OutlineColor", Color4b.WHITE.alpha(167))
    }

    init {
        tree(Preview)
    }

    private var state: State = State.Idle
    private var target: LivingEntity? = null


    private sealed class State {
        data object Idle : State()
        data class ChargeDelay(val ticksRemaining: Int) : State()
        data class Attack(val ticks: Int, val duration: Int, val speed: Double, val direction: Vec3) : State()
    }

    internal val currentAttackVelocity: Double
        get() {
            val attack = state as? State.Attack ?: return 0.0
            if (attack.ticks >= attack.duration * 2) return 0.0
            return attack.speed * if (attack.ticks < attack.duration) 1.0 else -1.0
        }

    internal val currentAttackDirection: Vec3
        get() = (state as? State.Attack)?.direction ?: Vec3.ZERO

    private fun freeLookDistance(max: Double): Double {
        val eye = player.eyePosition
        val hit = world.clip(
            eye,
            eye.add(player.lookAngle.scale(max)),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        )
        return if (hit.type == HitResult.Type.MISS) max else hit.location.distanceTo(eye)
    }

    private fun attackDist(d: Float) = d - maxSpeed / 2.0 * (d / (d + maxSpeed / 2.0))

    private fun isTargetReachable(d: Float): Boolean =
        d in MIN_SPEAR_DISTANCE..maxTargetDistance && attackDist(d).let { travel -> freeLookDistance(travel) >= travel }

    private fun selectTarget(): LivingEntity? {
        val eye = player.eyePosition
        val look = player.lookAngle.scale(maxTargetDistance.toDouble())
        val lookEnd = eye.add(look)
        val candidates = world.getEntitiesOfClass(
            LivingEntity::class.java,
            player.boundingBox.expandTowards(look).inflate(1.0)
        ) { e ->
            e != player && e.isAlive && e.boundingBox.clip(eye, lookEnd).isPresent
        }

        return candidates
            .asSequence()
            .filter { hasLineOfSight(eye, it.boundingBox.center) }
            .filter { isTargetReachable(player.distanceTo(it)) }
            .minByOrNull { player.distanceToSqr(it) }
    }

    private fun startAttack(): State {
        val t = target ?: return State.Idle
        val travel = freeLookDistance(attackDist(player.distanceTo(t)))
        val duration = ceil(travel / maxSpeed).toInt().coerceAtLeast(1)
        val direction = t.boundingBox.center.subtract(player.eyePosition).normalize().let {
            if (it.lengthSqr() > 0.0) it else player.lookAngle.normalize()
        }
        return State.Attack(0, duration, travel / duration, direction)
    }

    private fun resetState() {
        target = null
        if (state is State.Attack) player.deltaMovement = Vec3.ZERO
        state = State.Idle
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (player.mainHandItem.isSpear || player.offhandItem.isSpear) {
            target = selectTarget()
        } else {
            resetState()
            return@handler
        }

        if (!player.isUsingItem || !player.useItem.isSpear) {
            resetState()
            return@handler
        }

        val kineticWeapon = player.useItem.get(DataComponents.KINETIC_WEAPON) ?: return@handler
        val ticksUsingItem = player.ticksUsingItem
        val chargeFullDuration = kineticWeapon.computeDamageUseDuration() - kineticWeapon.delayTicks
        val canAttack = target != null && ticksUsingItem < chargeFullDuration

        state = when {
            ticksUsingItem == 1 -> State.ChargeDelay(kineticWeapon.delayTicks)
            ticksUsingItem >= chargeFullDuration && state !is State.Attack -> State.Idle
            else -> when (val s = state) {
                State.Idle -> when {
                    mc.options.keyAttack.isDown && canAttack && autoAttackOnCharge -> startAttack()
                    else -> State.Idle
                }

                is State.ChargeDelay -> if (s.ticksRemaining > 1) {
                    State.ChargeDelay(s.ticksRemaining - 1)
                } else if (canAttack) {
                    startAttack()
                } else {
                    State.Idle
                }

                is State.Attack -> if (s.ticks >= s.duration * 2) {
                    player.deltaMovement = Vec3.ZERO
                    State.Idle
                } else {
                    player.deltaMovement = s.direction.scale(currentAttackVelocity)
                    player.hurtMarked = true
                    State.Attack(s.ticks + 1, s.duration, s.speed, s.direction)
                }
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (!Preview.enabled) return@handler
        val t = target ?: return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera {
                if (t is EnderDragon) {
                    t.subEntities.forEach {
                        drawBox(it.boundingBox, Preview.fillColor, Preview.outlineColor)
                    }
                } else {
                    drawBox(t.boundingBox, Preview.fillColor, Preview.outlineColor)
                }
            }
        }
    }
}
