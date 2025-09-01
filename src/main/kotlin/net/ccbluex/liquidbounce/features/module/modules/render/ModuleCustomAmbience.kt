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

import com.mojang.blaze3d.platform.GlStateManager
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinBackgroundRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.shader.shaders.BlendShader
import net.ccbluex.liquidbounce.render.shader.shaders.BlendShaderData
import net.minecraft.block.enums.CameraSubmersionType
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.render.Camera
import net.minecraft.client.render.Fog
import net.minecraft.client.render.FogShape
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL13

/**
 * CustomAmbience module
 *
 * Override the ambience of the game
 */
object ModuleCustomAmbience : ClientModule("CustomAmbience", Category.RENDER, aliases = arrayOf("FogChanger")) {

    val weather = enumChoice("Weather", WeatherType.SNOWY)
    private val time = enumChoice("Time", TimeType.NIGHT)

    object Precipitation : ToggleableConfigurable(this, "ModifyPrecipitation", true) {
        val gradient by float("Gradient", 0.7f, 0.1f..1f)
        val layers by int("Layers", 3, 1..14)
    }

    object FogConfigurable : ToggleableConfigurable(this, "Fog", true) {

        private val color by color("Color", Color4b(47, 128, 255, 201))
        private val backgroundColor by color("BackgroundColor", Color4b(47, 128, 255, 201))
        private val fogStart by float("Distance", 0f, -8f..500f)
        private val density by float("Density", 10f, 0f..100f)
        private val fogShape by enumChoice("FogShape", Shape.SPHERE)

        /**
         * [MixinBackgroundRenderer]
         */
        fun modifyFog(camera: Camera, viewDistance: Float, fog: Fog): Fog {
            if (!this.running) {
                return fog
            }

            val start = MathHelper.clamp(fogStart, -8f, viewDistance)
            val end = MathHelper.clamp(fogStart + density, 0f, viewDistance)

            var shape = fog.shape
            val type = camera.submersionType
            if (type == CameraSubmersionType.NONE) {
                shape = fogShape.fogShape
            }

            return Fog(start, end, shape, color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
        }

        fun modifyClearColor(): Boolean {
            if (!this.running || backgroundColor.a == 0) {
                return false
            }

            GlStateManager._clearColor(
                backgroundColor.r / 255f,
                backgroundColor.g / 255f,
                backgroundColor.b / 255f,
                backgroundColor.a / 255f
            )
            return true
        }

        @Suppress("unused")
        private enum class Shape(override val choiceName: String, val fogShape: FogShape) : NamedChoice {
            SPHERE("Sphere", FogShape.SPHERE),
            CYLINDER("Cylinder", FogShape.CYLINDER);
        }

    }

    object CustomLightColor :
        ToggleableConfigurable(this, "CustomLightColor", true), AutoCloseable {

        private val lightColor by color("LightColor", Color4b(70, 119, 255, 255)).onChanged {
            update()
        }

        val framebuffer: Framebuffer = SimpleFramebuffer(16, 16, false)

        init {
            framebuffer.setTexFilter(9729)
            framebuffer.setClearColor(1f, 1f, 1f, 1f)
        }

        fun update() {
            framebuffer.clear()
            framebuffer.beginWrite(true)
            GlStateManager._activeTexture(GL13.GL_TEXTURE0)
            GlStateManager._bindTexture(mc.gameRenderer.lightmapTextureManager.lightmapFramebuffer.colorAttachment)
            BlendShaderData.color = lightColor
            BlendShader.blit()
            framebuffer.endWrite()
        }

        override fun close() {
            framebuffer.delete()
        }

    }

    init {
        tree(Precipitation)
        tree(FogConfigurable)
        tree(CustomLightColor)
    }

    @JvmStatic
    fun getTime(original: Long): Long {
        return if (running) {
            when (time.get()) {
                TimeType.NO_CHANGE -> original
                TimeType.DAWN -> 23041L
                TimeType.DAY -> 1000L
                TimeType.NOON -> 6000L
                TimeType.DUSK -> 12610L
                TimeType.NIGHT -> 13000L
                TimeType.MID_NIGHT -> 18000L
            }
        } else {
            original
        }
    }

    @Suppress("unused")
    enum class WeatherType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        SUNNY("Sunny"),
        RAINY("Rainy"),
        SNOWY("Snowy"),
        THUNDER("Thunder")
    }

    enum class TimeType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("MidNight")
    }

}
