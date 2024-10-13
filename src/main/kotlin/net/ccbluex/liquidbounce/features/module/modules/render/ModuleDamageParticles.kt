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

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.client.Curves
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

/**
 * DamageParticles module
 *
 * Show health changes of entities
 */
object ModuleDamageParticles : Module("DamageParticles", Category.RENDER) {

    private val scale by float("Scale", 1.5F, 0.25F..4F)
    private val ttl by float("TimeToLive", 1.5F, 0.5F..5.0F, "s")
    private val transitionY by float("TransitionY", 1.0F, -2.0F..2.0F)
    private val transitionType by curve("TransitionType", Curves.EASE_OUT)

    private val healthMap = Object2FloatOpenHashMap<LivingEntity>()
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

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        healthMap.clear()
        particles.clear()
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        healthMap.clear()
        particles.clear()
    }

    @Suppress("unused")
    private val tickHandler = repeatable {
        val entities = world.entities.filterIsInstanceTo(hashSetOf<LivingEntity>())
        entities.remove(player)

        val now = System.currentTimeMillis()

        entities.forEach {
            val currentHealth = it.health

            if (healthMap.containsKey(it)) {
                val prevHealth = healthMap.getFloat(it)
                val delta = abs(prevHealth - currentHealth)
                if (delta > EPSILON) {
                    particles += Particle(
                        now,
                        FORMATTER.format(delta),
                        if (prevHealth > currentHealth) Color4b.RED else Color4b.GREEN,
                        it.box.center.add(it.movement),
                    )
                }
            }

            healthMap.put(it, currentHealth)
        }

        healthMap.keys.removeIf { it !in entities || it.isDead }

        particles.removeIf { now - it.startTime > ttl * 1000F }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                val now = System.currentTimeMillis()
                val c = size
                val fontScale = 1.0F / (c * 0.15F) * scale
                particles.forEachIndexed { i, particle ->
                    val progress = (now - particle.startTime).toFloat() / (ttl * 1000.0F)

                    val currentPos = particle.pos.add(0.0, (transitionY * transitionType(progress)).toDouble(), 0.0)
                    val screenPos = WorldToScreen.calculateScreenPos(currentPos) ?: return@forEachIndexed

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
                commit(buf)
            }
        }
    }

    data class Particle(val startTime: Long, val text: String, val color: Color4b, val pos: Vec3d)

}
