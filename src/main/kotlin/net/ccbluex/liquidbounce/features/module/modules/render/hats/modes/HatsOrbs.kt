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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.modes

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getPointX
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getPointZ
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import kotlin.math.cos
import kotlin.math.sin

// RU - Объект, представляющий режим "Орбиты" (Orbs) для модуля Hats.
// EN - Object representing the "Orbs" mode for the Hats module.
object HatsOrbs : HatsMode("Orbs") {

    // RU - Настройки смещения по высоте и базового цвета.
    // EN - Height offset and base color settings.
    private val height by float("HeightOffset", 0.2f, 0f..1f)
    val color by color("color", Color4b(0, 0, 255, 125))

    // RU - Вложенный объект для управления радиусом, скоростью вращения всей системы и количеством орбит.
    // EN - Nested object to manage radius, system rotation speed, and orbit count.
    private object HatSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.5f, 0f..2f)
        val speed by float("Speed", 0.5f, 0.1f..10f)
        val size by float("OrbsSize", 0.1f, 0.01f..0.5f)
        val count by int("OrbsCount", 6, 1..12)

        // RU - Конфигурация эффекта покачивания (волна вверх-вниз).
        // EN - Configuration for the wave (up and down motion) effect.
        object WaveSettings : ToggleableConfigurable(this@HatsOrbs, "Wave", true) {
            val waveHeight by float("WaveHeight", 0.1f, 0.01f..1f)
            val waveSpeed by float("WaveSpeed", 2.0f, 0.1f..10f)
        }

        // RU - Конфигурация вращения ромбов вокруг своей оси.
        // EN - Configuration for the self-rotation of the rhombi.
        object OrbRotation : ToggleableConfigurable(this@HatsOrbs, "OrbRotation", true) {
            val speedRot by float("RotationSpeed", 2.0f, 0.1f..10f)
        }

    }

    // RU - Инициализация дерева конфигураций в ClickGUI.
    // EN - Initialization of the configuration tree within the ClickGUI.
    init {
        tree(HatSettings)
        tree(HatSettings.WaveSettings)
        tree(HatSettings.OrbRotation)
    }

    // RU - Основной обработчик события WorldRenderEvent для отрисовки в мире.
    // EN - Main handler for the WorldRenderEvent for in-world rendering.
    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val player = mc.player ?: return@handler
        val pos = player.interpolateCurrentPosition(it.partialTicks)

        renderEnvironmentForWorld(it.matrixStack) {
            withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {
                drawCustomMesh(ClientRenderPipelines.Triangles) {
                        matrix ->
                    // RU - Расчет текущего времени для плавной анимации.
                    // EN - Calculation of current time for smooth animation.
                    val time = (System.currentTimeMillis() / 1000.0) * HatSettings.speed

                    // RU - Цикл для отрисовки каждого отдельного шарика (орбиты).
                    // EN - Loop for rendering each individual orb (orbit).
                    for (i in 0 until HatSettings.count) {

                        // RU - Расчет угла для текущего шарика, плюс смещение по времени для вращения системы.
                        // EN - Calculation of the angle for the current orb, plus time offset for system rotation.
                        val angle = getAngle(i, HatSettings.count) + time

                        // RU - Получение базовых координат X и Z из утилит.
                        // EN - Retrieve base X and Z coordinates from utilities.
                        val x = getPointX(angle, HatSettings.radius)
                        val z = getPointZ(angle, HatSettings.radius)

                        // RU - Расчет координаты Y с учетом эффекта волны (sin).
                        // EN - Calculate the Y coordinate with the wave effect (sin).
                        val y = if (HatSettings.WaveSettings.enabled) {
                            sin(time * HatSettings.WaveSettings.waveSpeed + i).toFloat() * HatSettings.WaveSettings.waveHeight
                        } else 0f

                        // RU - Расчет угла и смещений для вращения ромба вокруг своей оси (sin/cos).
                        // EN - Calculate angle and offsets for rotating the rhombus around its own axis (sin/cos).
                        val rotAngle = if(HatSettings.OrbRotation.enabled) time * HatSettings.OrbRotation.speedRot else 0.0
                        val sinA = (sin(rotAngle)).toFloat() * HatSettings.size
                        val cosA = (cos(rotAngle)).toFloat() * HatSettings.size

                        // RU - Определение верхних/нижних точек и точек "пояса" ромба для упрощения отрисовки.
                        // EN - Define top/bottom points and "belt" points of the rhombus for simplified rendering.
                        val top = y + HatSettings.size
                        val bottom = y - HatSettings.size

                        val ax = x + sinA; val az = z + cosA // RU - Передняя точка | EN - Front point
                        val bx = x + cosA; val bz = z - sinA // RU - Правая точка | EN - Right point
                        val cx = x - sinA; val cz = z - cosA // RU - Задняя точка | EN - Back point
                        val dx = x - cosA; val dz = z + sinA // RU - Левая точка | EN - Left point

                        // RU - Отрисовка верхней части ромба (4 грани/8 треугольников).
                        // EN - Rendering of the top part of the rhombus (4 faces/8 triangles).
                        // Верхняя часть
                        addVertex(matrix, x, top, z).color(color); addVertex(matrix, dx, y, dz).color(color); addVertex(matrix, ax, y, az).color(color)
                        addVertex(matrix, x, top, z).color(color); addVertex(matrix, ax, y, az).color(color); addVertex(matrix, bx, y, bz).color(color)
                        addVertex(matrix, x, top, z).color(color); addVertex(matrix, bx, y, bz).color(color); addVertex(matrix, cx, y, cz).color(color)
                        addVertex(matrix, x, top, z).color(color); addVertex(matrix, cx, y, cz).color(color); addVertex(matrix, dx, y, dz).color(color)

                        // RU - Отрисовка нижней части ромба (4 грани/8 треугольников).
                        // EN - Rendering of the bottom part of the rhombus (4 faces/8 triangles).
                        // НИЗ (4 грани)
                        addVertex(matrix, x, bottom, z).color(color); addVertex(matrix, dx, y, dz).color(color); addVertex(matrix, ax, y, az).color(color)
                        addVertex(matrix, x, bottom, z).color(color); addVertex(matrix, ax, y, az).color(color); addVertex(matrix, bx, y, bz).color(color)
                        addVertex(matrix, x, bottom, z).color(color); addVertex(matrix, bx, y, bz).color(color); addVertex(matrix, cx, y, cz).color(color)
                        addVertex(matrix, x, bottom, z).color(color); addVertex(matrix, cx, y, cz).color(color); addVertex(matrix, dx, y, dz).color(color)
                    }

                }

            }

        }

    }

}
