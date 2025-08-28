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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.EntityHealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.math.Vec3d
import java.text.DecimalFormat
import kotlin.math.abs

/**
 * DamageParticles module
 *
 * Show health changes of entities
 */
object ModuleDamageParticles : ClientModule("DamageParticles", Category.RENDER) {

    private val scale by float("Scale", 2F, 0.25F..4F)
    private val ttl by float("TimeToLive", 2F, 0.5F..5.0F, "s")
    private val transition by vec3d("Transition", Vec3d(0.0, 1.0, 0.0))
    private val easing by easing("Easing", Easing.QUAD_OUT)

    /**
     * Ordered by startTime
     */
    private val particles = ArrayDeque<Particle>()

    private const val EPSILON = 0.05F
    private val FORMATTER = DecimalFormat("0.#")

    override fun onDisabled() {
        particles.clear()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        particles.clear()
    }

    @Suppress("unused")
    private val entityHealthUpdateHandler = handler<EntityHealthUpdateEvent> {
        val entity = it.entity
        val oldHealth = it.old
        val newHealth = it.new
        val maxHealth = it.max

        val delta = abs(oldHealth - newHealth)
        if (delta > EPSILON) {
            particles += Particle(
                System.currentTimeMillis(),
                FORMATTER.format(delta),
                if (oldHealth > newHealth) Color4b.RED else Color4b.GREEN,
                entity.box.center.add(entity.movement),
            )
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val earliest = System.currentTimeMillis() - (ttl * 1000).toLong()
        while (particles.isNotEmpty() && particles.first().startTime < earliest) {
            particles.removeFirst()
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val now = System.currentTimeMillis()
        particles.forEachIndexed { i, particle ->
            val progress = (now - particle.startTime).toFloat() / (ttl * 1000.0F)

            val currentPos = particle.pos.add(transition * easing.transform(progress).toDouble())
            val screenPos = WorldToScreen.calculateScreenPos(currentPos) ?: return@forEachIndexed

            with(event.context) {
                matrices.push()
                matrices.translate(screenPos.x, screenPos.y, screenPos.z)
                matrices.scale(scale, scale, 1.0F)

                drawCenteredTextWithShadow(
                    mc.textRenderer,
                    particle.text,
                    0,
                    0,
                    particle.color.toARGB(),
                )
                matrices.pop()
            }
        }

    }

    @JvmRecord
    private data class Particle(val startTime: Long, val text: String, val color: Color4b, val pos: Vec3d)

}
