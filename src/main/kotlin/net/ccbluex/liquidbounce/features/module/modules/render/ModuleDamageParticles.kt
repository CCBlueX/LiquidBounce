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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * DamageParticles module
 *
 * Show health changes of entities
 */
object ModuleDamageParticles : Module("DamageParticles", Category.RENDER) {

    private val scale by float("Scale", 1.5F, 0.25F..4F)
    private val ttl by float("TimeToLive", 1.5F, 0.5F..5.0F, "s")
    private val transitionY by float("TransitionY", 1.0F, -2.0F..2.0F)
    private val transitionType = choices(
        "TransitionType", Linear, arrayOf(
            Linear, Sine, Exponential, EaseInOut, Cubic, Elastic, Back, Step
        )
    )

    private fun Double.doTransition() = transitionType.activeChoice(this)

    private val healthMap = hashMapOf<LivingEntity, Float>()
    private val particles = hashSetOf<Particle>()

    private const val EPSILON = 0.05F
    private const val FORMATTER = "%.1f"

    private val fontRenderer by lazy {
        Fonts.DEFAULT_FONT.get()
    }

    override fun disable() {
        healthMap.clear()
        particles.clear()
    }

    val tickHandler = repeatable {
        val entities = world.entities.filterIsInstanceTo(hashSetOf<LivingEntity>())
        entities.remove(player)

        healthMap.keys.removeIf { it !in entities || it.isDead }

        val now = System.currentTimeMillis()

        particles.removeIf { now - it.startTime > ttl * 1000F }

        entities.forEach {
            val currentHealth = it.health
            val prevHealth = healthMap[it]

            if (prevHealth != null) {
                val delta = abs(prevHealth - currentHealth)
                if (delta > EPSILON)
                    particles += Particle(
                        now,
                        FORMATTER.format(delta),
                        if (prevHealth > currentHealth) Color4b.RED else Color4b.GREEN,
                        it.box.center.add(it.movement),
                    )
            }

            healthMap[it] = currentHealth
        }
    }

    val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                val now = System.currentTimeMillis()
                particles.forEachIndexed { i, particle ->
                    val progress = (now - particle.startTime).toDouble() / (ttl * 1000.0)
                    val currentPos = particle.pos.add(0.0, transitionY * progress.doTransition(), 0.0)
                    val screenPos = WorldToScreen.calculateScreenPos(currentPos) ?: return@forEachIndexed

                    val c = size
                    val fontScale = 1.0F / (c * 0.15F) * scale
                    val text = process(particle.text, particle.color)

                    draw(
                        text,
                        screenPos.x - text.widthWithShadow * 0.5F,
                        screenPos.y,
                        shadow = true,
                        z = 1000.0F * i / particles.size,
                        scale = fontScale
                    )
                }
                commit(this@renderEnvironmentForGUI, buf)
            }
        }
    }

    data class Particle(val startTime: Long, val text: String, val color: Color4b, val pos: Vec3d)

    /**
     * range = domain = 0.0..1.0
     */
    sealed class TransitionChoice(name: String) : Choice(name), (Double) -> Double {
        override val parent
            get() = transitionType
    }

    private object Linear : TransitionChoice("Linear") {
        override fun invoke(t: Double): Double = t
    }

    private object Sine : TransitionChoice("Sine") {
        override fun invoke(t: Double): Double = sin(PI * t * 0.5)
    }

    private object Exponential : TransitionChoice("Exponential") {
        override fun invoke(t: Double): Double = if (t == 0.0) 0.0 else 2.0.pow(10 * (t - 1))
    }

    private object EaseInOut : TransitionChoice("EaseInOut") {
        override fun invoke(t: Double): Double = if (t < 0.5) 2 * t * t else -1 + (4 - 2 * t) * t
    }

    private object Cubic : TransitionChoice("Cubic") {
        override fun invoke(t: Double): Double = t * t * t
    }

    private object Elastic : TransitionChoice("Elastic") {
        override fun invoke(t: Double): Double = sin(6.5 * Math.PI * t) * 2.0.pow(10 * (t - 1))
    }

    private object Back : TransitionChoice("Back") {
        override fun invoke(t: Double): Double = t * t * (2.5 * t - 1.5)
    }

    private object Step : TransitionChoice("Step") {
        override fun invoke(t: Double): Double = if (t < 0.5) 0.0 else 1.0
    }

}
