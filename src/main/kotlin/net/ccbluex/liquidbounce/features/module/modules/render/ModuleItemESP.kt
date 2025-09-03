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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items.*
import net.minecraft.util.math.Box

/**
 * ItemESP module
 *
 * Allows you to see dropped items through walls.
 */

object ModuleItemESP : ClientModule("ItemESP", Category.RENDER) {
    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    override val baseKey: String
        get() = "liquidbounce.module.itemEsp"

    private val modes = choices("Mode", OutlineMode, arrayOf(GlowMode, OutlineMode, BoxMode,NametagsMode))
    private val colorMode = choices("ColorMode", 2) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(255, 179, 72, 255)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it),
        )
    }
    private val defaultItems = setOf(
        DIAMOND,
        IRON_INGOT,
        GOLD_INGOT,
        EMERALD,
        NETHERITE_INGOT,
        ENCHANTED_BOOK,

        TRIDENT,
        BOW,
        FISHING_ROD,
        CROSSBOW,
        SHIELD,
        TOTEM_OF_UNDYING,


        DIAMOND_SWORD,
        DIAMOND_PICKAXE,
        DIAMOND_AXE,
        DIAMOND_SHOVEL,
        DIAMOND_HELMET,
        DIAMOND_CHESTPLATE,
        DIAMOND_LEGGINGS,
        DIAMOND_BOOTS,

        // Utility items
        ELYTRA,
        ENDER_PEARL,
        FIREWORK_ROCKET,
        WATER_BUCKET,
        LAVA_BUCKET,
        OBSIDIAN,
        ENDER_CHEST,

        POTION,
        SPLASH_POTION,
        LINGERING_POTION,

        ARROW,
        SPECTRAL_ARROW,


        ENCHANTED_GOLDEN_APPLE,
        GOLDEN_APPLE,

        SNOWBALL,
        TNT,
        FLINT_AND_STEEL
    )

    val filteredItems by items(
        "Items",
        defaultItems.toMutableSet()
    )

    private object NametagsMode : Choice("Nametags") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes
        private val scale by float("Scale", 1f, 0.5f..4f)
        private val maximumDistance by float("MaximumDistance", 50f, 1f..256f)
        private val number by boolean("Number", true)
        @Suppress("unused")
        val renderHandler = handler<OverlayRenderEvent> { event ->
            val camera = mc.cameraEntity ?: return@handler

            val items = world.entities.filterIsInstance<ItemEntity>()
                .filter { it.stack.item in this@ModuleItemESP.filteredItems }
                .filter { it.squaredDistanceTo(camera) <= maximumDistance * maximumDistance }

            renderEnvironmentForGUI {
                fontRenderer.withBuffers { buf ->
                    for (item in items) {
                        val pos = item.interpolateCurrentPosition(event.tickDelta)
                        val screenPos = WorldToScreen.calculateScreenPos(
                            pos.add(0.0, item.height + 0.25, 0.0)) ?: continue

                        matrixStack.push()
                        matrixStack.translate(screenPos.x, screenPos.y, 0f)

                        val distanceScale = mc.player?.let {
                            (1f / (it.distanceTo(item) * 0.1f)).coerceIn(0.5f, 2f) /5 * scale } ?: scale
                        matrixStack.scale(distanceScale, distanceScale, 1f)
                        val displayName = item.stack.itemName.string
                        val count = item.stack.count
                        val text = if (number && count > 1) "$displayName x$count" else displayName

                        val processed = fontRenderer.process(text)
                        val textWidth = fontRenderer.getStringWidth(processed)
                        val textHeight = fontRenderer.height

                        fontRenderer.draw(
                            processed,
                            -textWidth / 2f,
                            -textHeight / 2f,
                            true,
                            z = 0.001f
                        )
                        fontRenderer.commit(buf)

                        matrixStack.pop()
                    }
                }
            }

        }

    }


    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val box = Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val base = getColor()
            val baseColor = base.with(a = 50)
            val outlineColor = base.with(a = 100)

            val filtered = world.entities.filter(::shouldRender)

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    for (entity in filtered) {
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)

                        withPositionRelativeToCamera(pos) {
                            drawBox(box, baseColor, outlineColor)
                        }
                    }
                }
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

    fun shouldRender(entity: Entity?): Boolean {
        if (entity !is ItemEntity) return false
        return entity.stack.item in filteredItems
    }

    fun getColor() = this.colorMode.activeChoice.getColor(null)
}
