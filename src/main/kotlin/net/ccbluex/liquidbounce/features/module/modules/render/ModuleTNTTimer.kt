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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.WorldFeatureSubmitEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.submitTextAlwaysOnTop
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.ccbluex.liquidbounce.utils.world.EntityLookup.Companion.EntityLookup
import net.ccbluex.liquidbounce.utils.world.filterTo
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.network.chat.Style
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityTypes
import kotlin.math.sin

/**
 * TNTTimer module
 *
 * Highlight the active TNTs.
 */
object ModuleTNTTimer : ClientModule("TNTTimer", ModuleCategories.RENDER) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.tntTimer"

    // Glow ESP
    val esp by boolean("ESP", true)

    private object ShowTimer : ToggleableValueGroup(this, "ShowTimer", false) {
        private val scale by float("Scale", 1.5F, 0.25F..4F)
        private val renderY by float("RenderY", 1.0F, -2.0F..2.0F)
        private val ownerName by boolean("OwnerName", true)
        private val timeUnit by enumChoice("TimeUnit", TimeUnit.TICKS)

        private val tntEntities by EntityLookup { set ->
            filterTo(set, EntityTypes.TNT) { it.fuse > 0 }
        }

        override fun onDisabled() {
            tntEntities.clear()
            super.onDisabled()
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldFeatureSubmitEvent> { event ->
            for (tnt in tntEntities) {
                val pos = tnt.boundingBox.center.add(0.0, renderY.toDouble(), 0.0)

                // Yellow #ffff00 -> Red #ff0000
                val color = Color4b(255, Mth.floor(255F * tnt.fuse / DEFAULT_FUSE).coerceAtMost(255), 0)

                var text = timeUnit.format(tnt.fuse).asPlainText(Style.EMPTY + color)

                if (ownerName) {
                    tnt.owner?.name?.let {
                        text = textOf(
                            text,
                            " (".asPlainText(),
                            it,
                            ")".asPlainText(),
                        )
                    }
                }

                val formattedText = text.visualOrderText
                val font = mc.font

                val camera = event.camera
                val cameraPos = camera.position()
                event.poseStack.withPush {
                    translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)
                    mulPose(event.camera.rotation())
                    scale(
                        EntityRenderer.NAMETAG_SCALE * scale,
                        -EntityRenderer.NAMETAG_SCALE * scale,
                        EntityRenderer.NAMETAG_SCALE * scale,
                    )
                    event.submitNodeStorage.submitTextAlwaysOnTop(
                        this,
                        -font.width(formattedText) * 0.5f,
                        -font.lineHeight * 0.5f,
                        formattedText,
                        true,
                        Font.DisplayMode.SEE_THROUGH,
                        LightCoordsUtil.FULL_BRIGHT,
                        color.argb,
                        0,
                        0,
                    )
                }
            }
        }
    }

    init {
        tree(ShowTimer)
    }

    private const val DEFAULT_FUSE = 80

    /**
     * Cycle light periodically according to the remaining time (`fuse`). The less time left, the faster the cycle.
     */
    fun getTntColor(fuse: Int): Color4b {
        val red = Mth.floor(255.0 * (1.0 + 0.5 * sin(2400.0 / (12 + fuse)))).coerceIn(0, 255)
        return Color4b(red, 0, 0)
    }

}
