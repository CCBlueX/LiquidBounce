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

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.minecraft.world.entity.LivingEntity

/**
 * Following the target on elytra.
 * Works with [ModuleKillAura] together
 *
 * https://youtu.be/1wa8uKH_apY
 *
 * @author sqlerrorthing
 */
@Suppress("MagicNumber")
object ModuleElytraTarget : ClientModule("ElytraTarget", ModuleCategories.COMBAT) {
    private val targetTracker = tree(TargetTracker())

    init {
        tree(ElytraRotationProcessor)
        tree(AutoFirework)
    }

    private val targetRenderer = tree(TargetRenderer(this, targetTracker))
    private val safe by boolean("Safe", true)
    private val alwaysGlide by boolean("AlwaysGlide", false)

    @JvmStatic
    @get:JvmName("canAlwaysGlide")
    val canAlwaysGlide get() =
        alwaysGlide
        && target != null
        && super.running
        && !player.abilities.flying

    val canIgnoreKillAuraRotations get() =
        running
        && ElytraRotationProcessor.ignoreKillAura

    fun isSameTargetRendering(target: LivingEntity) =
        running
        && targetRenderer.enabled
        && targetTracker.target
            ?.takeIf { it == target } != null

    override val running: Boolean
        get() = super.running && player.isFallFlying

    internal val target get() = targetTracker.target

    @Suppress("unused")
    private val targetUpdateHandler = tickHandler {
        targetTracker.reset()
        targetTracker.selectFirst { potentialTarget ->
            player.hasLineOfSight(potentialTarget)
        }

        if (safe && !world.noCollision(player.boundingBox.move(player.deltaMovement))) {
            player.push(0.0, 0.1, 0.0)
        }
    }

    override fun onDisabled() {
        targetTracker.reset()
    }
}
