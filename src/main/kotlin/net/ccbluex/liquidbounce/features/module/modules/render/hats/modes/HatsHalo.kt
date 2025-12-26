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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getPointX
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getPointZ
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getTorusPoints
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition

// RU - Объект, представляющий режим "Нимб" (Halo) для модуля Hats.
// EN - Object representing the "Halo" mode for the Hats module.
object HatsHalo : HatsMode("Halo") {

    // RU - Настройки смещения по высоте и базового цвета.
    // EN - Height offset and base color settings.
    private val height by float("HeightOffset", 0.2f, 0f..1f)
    private val color by color("Color", Color4b(0, 0, 255, 125))

    // RU - Вложенные настройки для радиуса, толщины и видимости от первого лица.
    // EN - Nested settings for radius, thickness, and first-person view visibility.
    private object HatSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.3f, 0.1f..2f)
        val tubeRadius by float("Thickness", 0.05f, 0.01f..1f)
        val showInFirstPerson by boolean("FirstPersonView", true)
    }

    // RU - Инициализация дерева конфигураций в ClickGUI.
    // EN - Initialization of the configuration tree within the ClickGUI.
    init {
        tree(HatSettings)
    }


    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>{
        val player = mc.player ?: return@handler

        // RU - Проверка, нужно ли отображать нимб от первого лица.
        // EN - Check if the halo should be rendered in first-person view.
        if (mc.options.cameraType.isFirstPerson && !HatSettings.showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            // RU - Получение интерполированной позиции игрока для плавного рендера.
            // EN - Get the player's interpolated position for smooth rendering.
            val pos = player.interpolateCurrentPosition(it.partialTicks)

            // RU - Перемещение позиции рендера относительно камеры и головы игрока.
            // EN - Translate render position relative to the camera and player's head.
            withPositionRelativeToCamera(pos.add(0.0, (player.bbHeight + height).toDouble(), 0.0)) {

                // RU - Начало отрисовки кастомного меша (набора треугольников).
                // EN - Start custom mesh rendering (set of triangles).
                drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->

                    val mainSegments = 40
                    val tubeSegments = 12

                    // RU - Основной цикл для создания "бублика" (тора) по сегментам.
                    // EN - Main loop for creating the torus (donut) using segments.
                    for (mainI in 0 until mainSegments) {

                        val mainCurrentAngleTorus = getAngle(mainI, mainSegments)
                        val mainNextAngleTorus = getNextAngle(mainI, mainSegments)

                        // RU - Эти переменные не используются и могут быть удалены.
                        // EN - These variables are unused and can be removed.
                        val centerX1 = getPointX(mainCurrentAngleTorus, HatSettings.radius)
                        val centerZ1 = getPointZ(mainCurrentAngleTorus, HatSettings.radius)
                        val centerX2 = getPointX(mainNextAngleTorus, HatSettings.radius)
                        val centerZ2 = getPointZ(mainNextAngleTorus, HatSettings.radius)

                        // RU - Вложенный цикл для отрисовки "толщины" бублика.
                        // EN - Nested loop for rendering the torus "thickness".
                        for (tubeI in 0 until tubeSegments) {

                            val tubeCurrentAngleTorus = getAngle(tubeI, tubeSegments)
                            val tubeNextAngleTorus = getNextAngle(tubeI, tubeSegments)

                            // RU - Расчет 4-х вершин для создания двух треугольников (одной грани тора).
                            // EN - Calculate 4 vertices to create two triangles (one face of the torus).
                            val p1 = getTorusPoints(mainCurrentAngleTorus, tubeCurrentAngleTorus, HatSettings.radius, HatSettings.tubeRadius)
                            val p2 = getTorusPoints(mainCurrentAngleTorus, tubeNextAngleTorus, HatSettings.radius, HatSettings.tubeRadius)
                            val p3 = getTorusPoints(mainNextAngleTorus, tubeCurrentAngleTorus, HatSettings.radius, HatSettings.tubeRadius)
                            val p4 = getTorusPoints(mainNextAngleTorus, tubeNextAngleTorus, HatSettings.radius, HatSettings.tubeRadius)

                            // RU - Добавление первого треугольника грани.
                            // EN - Add the first triangle of the face.
                            addVertex(matrix, p1.first, p1.second, p1.third).color(color)
                            addVertex(matrix, p2.first, p2.second, p2.third).color(color)
                            addVertex(matrix, p3.first, p3.second, p3.third).color(color)

                            // RU - Добавление второго треугольника грани.
                            // EN - Add the second triangle of the face.
                            addVertex(matrix, p2.first, p2.second, p2.third).color(color)
                            addVertex(matrix, p4.first, p4.second, p4.third).color(color)
                            addVertex(matrix, p3.first, p3.second, p3.third).color(color)
                        }
                    }
                }
            }
        }
    }
}
