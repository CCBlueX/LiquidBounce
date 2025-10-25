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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission
import net.minecraft.entity.projectile.SpectralArrowEntity
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.util.math.Box

/**
 * ItemESP module
 *
 * Allows you to see dropped items through walls.
 */

object ModuleItemESP : ClientModule("ItemESP", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.itemEsp"

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val items by items("Items", ReferenceOpenHashSet())

    private object ShowArrows : ToggleableConfigurable(this, "ShowArrows", true) {
        val regularArrows by boolean("RegularArrows", true)
        val spectralArrows by boolean("SpectralArrows", true)
        val arrowsWithEffects by boolean("ArrowsWithEffects", true)
    }

    init {
        tree(ShowArrows)
    }

    private val showTridents by boolean("ShowTridents", true)

    private val modes = choices("Mode", OutlineMode, arrayOf(GlowMode, OutlineMode, BoxMode))
    private val colorMode = choices("ColorMode", 0) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(255, 179, 72, 255)),
            GenericRainbowColorMode(it)
        )
    }

    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val box = Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125)

        private val entities = mutableListOf<Entity>()

        override fun disable() {
            entities.clear()
            super.disable()
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            entities.clear()
            world.entities.filterTo(entities, ::shouldRender)
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (entities.isEmpty()) return@handler

            val matrixStack = event.matrixStack

            val base = getColor()
            val baseColor = base.with(a = 50)
            val outlineColor = base.with(a = 100)

            renderEnvironmentForWorld(matrixStack) {
                startBatch()
                for (entity in entities) {
                    val pos = entity.interpolateCurrentPosition(event.partialTicks)

                    withPositionRelativeToCamera(pos) {
                        drawBox(box, baseColor, outlineColor)
                    }
                }
                commitBatch()
            }
        }
    }

    object GlowMode : Choice("Glow") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes
    }

    object OutlineMode : Choice("Outline") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes
    }

    fun shouldRender(it: Entity?) : Boolean {
        if (it is ItemEntity) return filter(it.stack.item, items)
        if (it is TridentEntity) return showTridents

        // arrow checks
        // The server never sends the actual pickupType of arrows fired
        // from Infinity-enchanted bows to clients. :(
        // Therefore, those arrows are still rendered as collectible, even though they shouldn't be.
        // The same applies to tridents thrown and arrows fired by players in Creative mode.

        // However, it's not completely useless — arrows shot by mobs such as skeletons and pillagers are not rendered.
        val isArrow = it is ArrowEntity || it is SpectralArrowEntity
        if (!isArrow || !ShowArrows.enabled || it.pickupType != PickupPermission.ALLOWED) {
            return false
        }

        return when (it) {
            is SpectralArrowEntity -> ShowArrows.spectralArrows
            is ArrowEntity -> if (it.color == -1) ShowArrows.regularArrows else ShowArrows.arrowsWithEffects
            else -> false
        }
    }

    fun getColor() = this.colorMode.activeChoice.getColor(null)
}
