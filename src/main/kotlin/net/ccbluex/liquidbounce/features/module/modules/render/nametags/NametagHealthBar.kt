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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.hasHealthScoreboard
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.TextColor
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import kotlin.math.abs

/**
 * A bar under the nametag whose fill eases towards the health of the entity instead of jumping to it.
 */
object NametagHealthBar : ToggleableValueGroup(ModuleNametags, "HealthBar", false) {

    internal val height by float("Height", 2f, 1f..6f)
    private val speed by float("Speed", 0.25f, 0.05f..1f)

    private val TRACK_COLOR = Color4b(0, 0, 0, 120)
    private val ABSORPTION_COLOR = ChatFormatting.GOLD.toColor()

    private const val SETTLED = 0.01f

    private val healths = Reference2ObjectOpenHashMap<LivingEntity, SmoothedHealth>()

    /**
     * Counts the collections of nametags, so that health no longer tagged can be dropped.
     */
    private var generation = 0

    internal class SmoothedHealth(entity: LivingEntity) {
        @JvmField var previousHealth = entity.healthNow
        @JvmField var health = previousHealth
        @JvmField var previousAbsorption = entity.absorptionNow
        @JvmField var absorption = previousAbsorption
        @JvmField var maxHealth = entity.maxHealth
        @JvmField var color = healthColor(health + absorption)
        @JvmField var generation = 0

        fun step(entity: LivingEntity, speed: Float) {
            val targetHealth = entity.healthNow
            val targetAbsorption = entity.absorptionNow
            previousHealth = health
            previousAbsorption = absorption
            health = health.approach(targetHealth, speed)
            absorption = absorption.approach(targetAbsorption, speed)
            maxHealth = maxOf(entity.maxHealth, targetHealth)
            color = healthColor(targetHealth + targetAbsorption)
        }
    }

    /**
     * Eases the health of [entity] by one tick and returns it to be drawn under its nametag.
     */
    internal fun track(entity: LivingEntity): SmoothedHealth {
        val health = healths.getOrPut(entity) { SmoothedHealth(entity) }
        health.step(entity, speed)
        health.generation = generation
        return health
    }

    /**
     * Starts a new collection of nametags.
     */
    internal fun beginCollection() {
        generation++
    }

    /**
     * Drops the health of every entity that did not get a nametag in this collection.
     */
    internal fun endCollection() {
        healths.values.removeIf { it.generation != generation }
    }

    /**
     * The health of [entity] the bar shows, or `null` if it has no bar.
     */
    fun displayedHealth(entity: LivingEntity): Float? = healths[entity]?.health

    override fun onDisabled() {
        healths.clear()
    }

    internal fun GuiGraphicsExtractor.drawHealthBar(
        health: SmoothedHealth,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        tickDelta: Float,
    ) {
        val shownHealth = Mth.lerp(tickDelta, health.previousHealth, health.health)
        val shownAbsorption = Mth.lerp(tickDelta, health.previousAbsorption, health.absorption)
        val capacity = (health.maxHealth + shownAbsorption).coerceAtLeast(1f)
        val width = x2 - x1
        val healthEnd = x1 + width * (shownHealth / capacity).coerceIn(0f, 1f)
        val absorptionEnd = (healthEnd + width * (shownAbsorption / capacity)).coerceAtMost(x2)

        drawRoundedRect(x1, y1, x2, y2, radius = 0f, fillColor = TRACK_COLOR)
        if (healthEnd > x1) {
            drawRoundedRect(x1, y1, healthEnd, y2, radius = 0f, fillColor = health.color)
        }
        if (absorptionEnd > healthEnd) {
            drawRoundedRect(healthEnd, y1, absorptionEnd, y2, radius = 0f, fillColor = ABSORPTION_COLOR)
        }
    }

    private fun Float.approach(target: Float, speed: Float): Float {
        val next = this + (target - this) * speed
        return if (abs(target - next) < SETTLED) target else next
    }

}

private val LivingEntity.healthNow get() = getActualHealth()

private val LivingEntity.absorptionNow get() = if (hasHealthScoreboard()) 0f else absorptionAmount

private fun healthColor(health: Float) = healthFormatting(health).toColor()

private fun ChatFormatting.toColor() = Color4b.fullAlpha(TextColor.fromLegacyFormat(this)!!.value)
