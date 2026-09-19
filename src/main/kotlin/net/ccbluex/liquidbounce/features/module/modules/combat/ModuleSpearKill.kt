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
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.isWithinWorldBorder
import net.ccbluex.liquidbounce.utils.entity.useItem
import net.ccbluex.liquidbounce.utils.item.isSpear
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.item.component.KineticWeapon
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

/**
 * Spear kill module
 *
 * Automatically attacks enemies using a charged spear.
 */
object ModuleSpearKill : ClientModule("SpearKill", ModuleCategories.COMBAT, aliases = listOf("AutoSpear")) {

    private val maxTargetDistance by float("MaxTargetDistance", 50f, 3f..200f)
    private val maxAllowedSpeed by float("MaxSpeed", 7f, 2f..10f)

    private object Preview : ToggleableValueGroup(this, "Preview", true) {
        val fillColor by color("FillColor", Color4b.RED.alpha(67))
        val outlineColor by color("OutlineColor", Color4b.WHITE.alpha(167))
    }

    init {
        tree(Preview)
    }

    private val attackMovements = ArrayDeque<Vec3>()
    private var previewTarget: LivingEntity? = null

    private val currentMovement get() = attackMovements.firstOrNull() ?: Vec3.ZERO

    internal val currentChargeAttackMovement
        get() = currentMovement.takeIf { it.lengthSqr() > 0.0 }

    private val isUsingSpear get() = player.isUsingItem && player.useItem.isSpear

    private val KineticWeapon.isChargeAttackActive
        get() = player.ticksUsingItem < computeDamageUseDuration() - delayTicks

    private val KineticWeapon.hasChargeStarted
        get() = player.ticksUsingItem > delayTicks

    private val KineticWeapon.isChargeSpent
        get() = player.ticksUsingItem > computeDamageUseDuration()

    private fun resetAttack() {
        previewTarget = null
        if (attackMovements.isNotEmpty()) player.deltaMovement = Vec3.ZERO
        attackMovements.clear()
    }

    private fun findTarget(): Pair<LivingEntity, Double>? {
        val eye = player.eyePosition
        val lookEnd = eye.add(player.lookAngle.scale(maxTargetDistance.toDouble()))
        var best: Pair<LivingEntity, Double>? = null
        var bestDist = Double.MAX_VALUE

        for (entity in world.getEntitiesOfClass(
            LivingEntity::class.java,
            player.boundingBox.expandTowards(lookEnd.subtract(eye)).inflate(player.bbWidth / 2.0)
        ) { it !== player && it.isAlive && it.isWithinWorldBorder && it.boundingBox.clip(eye, lookEnd).isPresent }) {

            val hitPosition = entity.boundingBox.clip(eye, lookEnd).orElse(null) ?: continue
            val attackRange = player.getAttackRangeWith(player.useItem)
            if (!hasLineOfSight(eye, hitPosition)) continue

            val distanceToTarget = hitPosition.distanceTo(eye)
            val distanceToDamage = (distanceToTarget - attackRange.effectiveMaxRange(player)).coerceAtLeast(0.0)
            if (distanceToDamage <= 0.0) continue

            val distSq = distanceToDamage * distanceToDamage
            if (distSq >= bestDist) continue

            val hit = traceFromPlayer(range = distanceToDamage, block = ClipContext.Block.COLLIDER)
            if (hit.type == HitResult.Type.MISS || hit.location.distanceTo(eye) >= distanceToDamage) {
                best = entity to distanceToDamage
                bestDist = distSq
            }
        }
        return best
    }

    private fun createAttackMovement(target: LivingEntity, distance: Double) {
        val ticks = ceil(distance / maxAllowedSpeed).toInt().coerceAtLeast(1)
        val velocity = distance / ticks

        val direction = PositionExtrapolation.getBestForEntity(target)
            .getPositionInTicks(ticks.toDouble())
            .subtract(player.eyePosition)
            .normalize()
            .takeIf { it.lengthSqr() > 0 } ?: player.lookAngle.normalize()

        val movement = direction.scale(velocity)
        val reverse = movement.scale(-1.0)

        repeat(ticks) { attackMovements += movement }
        repeat(ticks) { attackMovements += reverse }
        attackMovements += Vec3.ZERO
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FINAL_DECISION) {
        if (!isUsingSpear) {
            resetAttack()
            return@handler
        }

        val target = if (Preview.enabled) findTarget() else null
        previewTarget = target?.first

        val spear = player.useItem.get(DataComponents.KINETIC_WEAPON) ?: run {
            resetAttack()
            return@handler
        }

        if (mc.options.keyUse.isDown && spear.isChargeSpent) {
            val hand = player.usedItemHand
            interaction.releaseUsingItem(player)
            useItem(hand)
        }

        if (!spear.hasChargeStarted) {
            attackMovements.clear()
            return@handler
        }

        if (attackMovements.isNotEmpty()) {
            player.deltaMovement = attackMovements.removeFirst()
            return@handler
        }

        if (!spear.isChargeAttackActive || !mc.options.keyAttack.isDown) return@handler

        val (entity, distance) = target ?: findTarget() ?: return@handler
        previewTarget = entity
        createAttackMovement(entity, distance)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (!Preview.enabled) return@handler
        previewTarget?.let { target ->
            event.renderEnvironment {
                withPositionRelativeToCamera {
                    if (target is EnderDragon) {
                        target.subEntities.forEach {
                            drawBox(it.boundingBox, Preview.fillColor, Preview.outlineColor)
                        }
                    } else {
                        drawBox(target.boundingBox, Preview.fillColor, Preview.outlineColor)
                    }
                }
            }
        }
    }
}
