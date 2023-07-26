/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.ColorUtils
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import java.awt.Color


/**
 * ESP module
 *
 * Allows you to see targets through walls.
 */

object ModuleESP : Module("ESP", Category.RENDER) {

    override val translationBaseKey: String
        get() = "liquidbounce.module.esp"

    private val modes = choices("Mode", OutlineMode, arrayOf(BoxMode, OutlineMode, GlowMode))

    private val colorModes = choices("ColorMode", StaticMode, arrayOf(StaticMode, RainbowMode))

    private object StaticMode : Choice("Static") {
        override val parent: ChoiceConfigurable
            get() = colorModes

        val color by color("Color", Color4b.WHITE)
    }

    private object RainbowMode : Choice("Rainbow") {
        override val parent: ChoiceConfigurable
            get() = colorModes
    }

    val teamColor by boolean("TeamColor", true)

    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable
            get() = modes

        private val outline by boolean("Outline", true)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val entitiesWithBoxes = world.entities.filter { it.shouldBeShown() }.groupBy { entity ->
                val dimensions = entity.getDimensions(entity.pose)

                val d = dimensions.width.toDouble() / 2.0

                Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)
            }

            renderEnvironment(matrixStack) {
                entitiesWithBoxes.forEach { box, entities ->
                    for (entity in entities) {
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)
                        val color = getColor(entity)

                        val baseColor = color.alpha(50)
                        val outlineColor = color.alpha(100)

                        withPosition(pos) {
                            withColor(baseColor) {
                                drawSolidBox(box)
                            }

                            if (outline) {
                                withColor(outlineColor) {
                                    drawOutlinedBox(box)
                                }
                            }
                        }
                    }
                }
            }


        }

    }

    object GlowMode : Choice("Glow") {
        
        override val parent: ChoiceConfigurable
            get() = modes
        
    }

    object OutlineMode : Choice("Outline") {
        override val parent: ChoiceConfigurable
            get() = modes

        val width by float("Width", 3F, 0.5F..5F)
    }

    private fun getBaseColor(): Color4b {
        return if (RainbowMode.isActive) rainbow() else StaticMode.color
    }

    fun getColor(entity: Entity): Color4b {
        run {
            if (entity is LivingEntity) {
                if (entity.hurtTime > 0) {
                    return Color4b(255, 0, 0)
                }

                if (entity is PlayerEntity && FriendManager.isFriend(entity.gameProfile.name)) {
                    return Color4b(0, 0, 255)
                }

                ModuleMurderMystery.getColor(entity)?.let { return it }

                if (teamColor) {
                    val chars: CharArray = (entity.displayName ?: return@run).string.toCharArray()
                    var color = Int.MAX_VALUE

                    val colors = "0123456789abcdef"

                    for (i in chars.indices) {
                        if (chars[i] != '§' || i + 1 >= chars.size) {
                            continue
                        }

                        val index = colors.indexOf(chars[i + 1])

                        if (index < 0 || index > 15) {
                            continue
                        }

                        color = ColorUtils.hexColors[index]
                        break
                    }

                    return Color4b(Color(color))
                }
            }
        }

        return getBaseColor()
    }

}
