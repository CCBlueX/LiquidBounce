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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.renderer.fog.FogData

/**
 * CustomAmbience module
 *
 * Override the ambience of the game
 */
object ModuleCustomAmbience : ClientModule("CustomAmbience", ModuleCategories.RENDER, aliases = listOf("FogChanger")) {

    val weather = enumChoice("Weather", WeatherType.SNOWY)
    private val time = enumChoice("Time", TimeType.NIGHT)

    object Precipitation : ToggleableValueGroup(this, "ModifyPrecipitation", true) {
        val gradient by float("Gradient", 0.7f, 0.1f..1f)
//        val layers by int("Layers", 3, 1..14)
    }

    /**
     * @see FogData
     */
    object FogValueGroup : ToggleableValueGroup(this, "Fog", true) {

        val disableWorldFog by boolean("DisableWorldFog", false)

        object FogColorOverride : ToggleableValueGroup(this, "FogColorOverride", false) {
            val color by color("Color", Color4b(47, 128, 255, 201))
        }

        private val backgroundColor by color("BackgroundColor", Color4b(47, 128, 255, 201))

        private val environmental by floatRange("Environmental", 0f..1024f, -16f..2048f)
        private val renderDistance by floatRange("RenderDistance", 230f..256f, 0f..1024f)
        private val skyEnd by float("SkyEnd", 256f, 0f..1024f)
        private val cloudEnd by float("CloudEnd", 20480f, 0f..4096f)

        /**
         * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinFogRenderer
         */
        fun modifyFogData(fogData: FogData) {
            if (!this.running) {
                return
            }

            fogData.environmentalStart = this.environmental.start
            fogData.environmentalEnd = this.environmental.endInclusive
            fogData.renderDistanceStart = this.renderDistance.start
            fogData.renderDistanceEnd = this.renderDistance.endInclusive
            fogData.skyEnd = this.skyEnd
            fogData.cloudEnd = this.cloudEnd
        }

        fun modifyClearColor(original: Int): Int {
            if (!this.running || backgroundColor.a == 0) {
                return original
            }

            return backgroundColor.argb
        }
    }

    /**
     * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinLightmap
     *
     * FIXME: redesign
     */
    object CustomLightmap : ToggleableValueGroup(this, "CustomLightmap", false) {
        val color by color("Color", Color4b.LIQUID_BOUNCE)
    }

    object SkyColor : ToggleableValueGroup(this, "SkyColor", false) {
        val color by color("Color", Color4b.BLUE)
    }

    init {
        tree(Precipitation)
        tree(FogValueGroup)
        tree(CustomLightmap)
        tree(SkyColor)
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
    enum class WeatherType(override val tag: String) : Tagged {
        NO_CHANGE("NoChange"),
        SUNNY("Sunny"),
        RAINY("Rainy"),
        SNOWY("Snowy"),
        THUNDER("Thunder")
    }

    enum class TimeType(override val tag: String) : Tagged {
        NO_CHANGE("NoChange"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("MidNight")
    }

}
