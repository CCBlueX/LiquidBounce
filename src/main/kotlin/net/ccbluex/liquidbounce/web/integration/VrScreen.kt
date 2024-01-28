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
 *
 */
package net.ccbluex.liquidbounce.web.integration

import net.ccbluex.liquidbounce.render.shader.shaders.BackgroundShader.Companion.BACKGROUND_SHADER
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.text.Text

class VrScreen(val screen: String, title: Text = "VS $screen".asText(),
               val originalScreen: Screen? = null) : Screen(title) {

    override fun init() {
        IntegrationHandler.virtualOpen(screen)
    }

    override fun close() {
        IntegrationHandler.virtualClose()
        mc.mouse.lockCursor()
        super.close()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // render nothing
        BACKGROUND_SHADER.startShader()

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
        buffer.vertex(0.0, height.toDouble(), 0.0)
            .next()
        buffer.vertex(width.toDouble(), height.toDouble(), 0.0)
            .next()
        buffer.vertex(width.toDouble(), 0.0, 0.0)
            .next()
        buffer.vertex(0.0, 0.0, 0.0)
            .next()
        tessellator.draw()

        BACKGROUND_SHADER.stopShader()
    }

    override fun shouldPause(): Boolean {
        // preventing game pause
        return false
    }

}
