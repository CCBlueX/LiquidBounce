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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
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

    private val modes = choices("Mode", GlowMode, arrayOf(BoxMode, OutlineMode, GlowMode))
    private val colorModes = choices<GenericColorMode<LivingEntity>>("ColorMode", { it.choices[0] },
        { arrayOf(
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE.alpha(100)),
            GenericRainbowColorMode(it)
        ) }
    )

    val friendColor by color("Friends", Color4b(0, 0, 255))
    val teamColor by boolean("TeamColor", true)


    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val entitiesWithBoxes = findRenderedEntities().map { entity ->
                val dimensions = entity.getDimensions(entity.pose)

                val d = dimensions.width.toDouble() / 2.0

                entity to Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)
            }

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    entitiesWithBoxes.forEach { (entity, box) ->
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)
                        val color = getColor(entity)

                        val baseColor = color.alpha(50)
                        val outlineColor = color.alpha(100)

                        withPositionRelativeToCamera(pos) {
                            drawBox(
                                box,
                                baseColor,
                                outlineColor.takeIf { outline }
                            )
                        }
                    }
                }
            }

        }
    }

    fun findRenderedEntities() = world.entities.filterIsInstance<LivingEntity>().filter { it.shouldBeShown() }

    object GlowMode : Choice("Glow") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

    }

    object OutlineMode : Choice("Outline") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes
    }

    private fun getBaseColor(entity: LivingEntity): Color4b {
        if (entity is PlayerEntity) {
            if (FriendManager.isFriend(entity) && friendColor.a > 0) {
                return friendColor
            }

            ModuleMurderMystery.getColor(entity)?.let { return it }

            if (teamColor) {
                getTeamColor(entity)?.let { return it }
            }
        }

        return colorModes.activeChoice.getColor(entity)
    }

    fun getColor(entity: LivingEntity): Color4b {
        val baseColor = getBaseColor(entity)

        if (entity.hurtTime > 0) {
            return Color4b.RED
        }

        return baseColor
    }

    /**
     * Returns the team color of the [entity] or null if the entity is not in a team.
     */
     fun getTeamColor(entity: Entity)
        = entity.displayName?.style?.color?.rgb?.let { Color4b(Color(it)) }

}
