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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

// TODO on tick and interpolation
/**
 * Breadcrumbs module
 *
 * Leaves traces behind players.
 */
object ModuleBreadcrumbs : Module("Breadcrumbs", Category.RENDER, aliases = arrayOf("PlayerTrails")) {

    private val onlyOwn by boolean("OnlyOwn", true)
    private val color by color("Color", Color4b(255, 179, 72, 255))
    private val colorRainbow by boolean("Rainbow", false)
    private val height by float("Height", 0f, 0f..2f)
    private val alive by int("Alive", 2500, 10..10000, "ms")
    private val fade by boolean("Fade", true)

    private val positions = mutableMapOf<Entity, MutableList<TrailPart>>()
    private val lastPositions = mutableMapOf<Entity, Array<Double>>()

    override fun disable() {
        clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val time = System.currentTimeMillis()
        update(time)

        val matrixStack = event.matrixStack
        val color = if (colorRainbow) rainbow() else color

        renderEnvironmentForWorld(matrixStack) {
            if (height > 0) {
                RenderSystem.disableCull()
            }

            positions.values.forEach { doubles ->
                val lines = makeAndVerifyVertexList(time, doubles)
                    .map { Pair(relativeToCamera(it.first).toVec3(), it.second) }
                    .toTypedArray()

                @Suppress("SpreadOperator")
                draw(matrixStack, color, *lines)
            }

            if (height > 0) {
                RenderSystem.enableCull()
            }
        }
    }

    private fun makeAndVerifyVertexList(time: Long, positions: MutableList<TrailPart>): List<Pair<Vec3d, Float>> {
        val mutableList = mutableListOf<Pair<Vec3d, Float>>()
        val toRemove = mutableListOf<TrailPart>()
        val aliveDuration = alive.toLong()
        val alpha = color.a / 255f

        positions.forEach { position ->
            val deltaTime = time - position.creationTime
            if (deltaTime > aliveDuration) {
                toRemove += position
            } else {
                val calcAlpha = if (fade) (1f - (deltaTime / aliveDuration).toFloat()) * alpha else alpha
                mutableList += Pair(Vec3d(position.x, position.y, position.z), calcAlpha)
            }
        }

        positions.removeAll(toRemove)
        return mutableList
    }

    private fun draw(matrixStack: MatrixStack, color: Color4b, vararg positions: Pair<Vec3, Float>) {
        val matrix = matrixStack.peek().positionMatrix

        @Suppress("SpellCheckingInspection")
        val tessellator = RenderSystem.renderThreadTesselator()
        val bufferBuilder = tessellator.buffer
        val lines = height == 0f
        val red = color.r / 255f
        val green = color.g / 255f
        val blue = color.b / 255f

        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }

        with(bufferBuilder) {
            begin(if (lines) DrawMode.DEBUG_LINES else DrawMode.QUADS, VertexFormats.POSITION_COLOR)

            for (i in positions.indices) {
                if (i - 1 < 0) {
                    continue
                }

                val v0 = positions[i]
                val v2 = positions[i - 1]

                vertex(matrix, v0.first.x, v0.first.y, v0.first.z).color(red, green, blue, v0.second).next()
                vertex(matrix, v2.first.x, v2.first.y, v2.first.z).color(red, green, blue, v2.second).next()
                if (!lines) {
                    vertex(matrix, v2.first.x, v2.first.y + height, v2.first.z).color(red, green, blue, v2.second).next()
                    vertex(matrix, v0.first.x, v0.first.y + height, v0.first.z).color(red, green, blue, v0.second).next()
                }
            }
        }

        tessellator.draw()
    }

    /**
     * Updates all trails.
     */
    fun update(time: Long) {
        if (onlyOwn) {
            updateEntry(time, player)
            if (positions.size > 1) {
                positions.entries.removeIf { it.key != player }
            }

            return
        }

        val actualPresent = world.players.toSet()
        actualPresent.forEach { player -> updateEntry(time, player) }
        positions.entries.removeIf { it.key !in actualPresent }
    }

    private fun updateEntry(time: Long, entity: Entity) {
        val last = lastPositions[entity]
        if (last != null && entity.x == last[0] && entity.y == last[1] && entity.z == last[2]) {
            return
        }

        lastPositions[entity] = arrayOf(entity.x, entity.y, entity.z)
        positions.computeIfAbsent(entity) {
            mutableListOf()
        }.add(TrailPart(entity.x, entity.y, entity.z, time))
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    private fun clear() {
        lastPositions.clear()
        positions.clear()
    }

    @JvmRecord
    private data class TrailPart(val x: Double, val y: Double, val z: Double, val creationTime: Long)

}
