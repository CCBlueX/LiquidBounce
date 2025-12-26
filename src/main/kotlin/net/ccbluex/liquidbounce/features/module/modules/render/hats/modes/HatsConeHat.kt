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
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import org.joml.Vector3f

/**
 * @author minecrrrr
 */
// RU - Объект, представляющий режим "Конус" (Cone) для модуля Hats.
// EN - Object representing the "Cone" mode for the Hats module.
object HatsConeHat : HatsMode("Cone") {

    // RU - Настройки смещения по высоте и базового цвета.
    // EN - Height offset and base color settings.
    private val height by float("HeightOffset", 0.1f, 0f..1f)

    private val color by color("Color", Color4b(0, 0, 255, 125))

    // RU - Вложенные настройки для радиуса основания, высоты пика и видимости от первого лица.
    // EN - Nested settings for base radius, peak height, and first-person view visibility.
    private object HatSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.6f, 0.1f..2f)
        val peak by float("Peak", 0.3f, 0.01f..2f)
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

        // RU - Проверка, нужно ли отображать конус от первого лица.
        // EN - Check if the cone should be rendered in first-person view.
        if (mc.options.cameraType.isFirstPerson && !HatSettings.showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            // RU - Получение интерполированной позиции игрока для плавного рендера.
            // EN - Get the player's interpolated position for smooth rendering.
            val pos = player.interpolateCurrentPosition(it.partialTicks)

            // RU - Создание вектора смещения для вершины конуса (пика).
            // EN - Create an offset vector for the cone's peak.
            val peakOffset = Vector3f(0f, HatSettings.peak, 0f)

            // RU - Перемещение позиции рендера относительно камеры и головы игрока.
            // EN - Translate render position relative to the camera and player's head.
            withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {

                // RU - Отрисовка градиентного круга, который формирует основание конуса с вершиной в peakOffset.
                // EN - Draw a gradient circle forming the cone base with the apex at peakOffset.
                drawGradientCircle(
                    outerRadius = HatSettings.radius,
                    innerRadius = 0f,
                    outerColor = color,
                    innerColor = color,
                    innerOffset = peakOffset,
                )
            }
        }
    }
}
