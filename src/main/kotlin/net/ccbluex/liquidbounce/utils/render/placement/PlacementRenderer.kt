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
package net.ccbluex.liquidbounce.utils.render.placement

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.EMPTY_BOX
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

/**
 * Render boxes, manages fade-in/-out and culling.
 *
 * Modules that want to provide a color not set in here can simply extend this class, register all settings except for
 * the color settings, and override the color getters as needed.
 */
open class PlacementRenderer(
    name: String,
    enabled: Boolean,
    val module: Module,
    val keep: Boolean = true,
    clump: Boolean = true
) : ToggleableConfigurable(module, name, enabled) {

    val clump by boolean("Clump", clump)

    val startSize by float("StartSize", 1f, 0f..2f)
    val startSizeCurve by curve("StartCurve", Easing.LINEAR)

    val endSize by float("EndSize", 0.8f, 0f..2f)
    val endSizeCurve by curve("EndCurve", Easing.LINEAR)

    val fadeInCurve by curve("FadeInCurve", Easing.LINEAR)
    val fadeOutCurve by curve("FadeOutCurve", Easing.LINEAR)

    val inTime by int("InTime", 500, 0..5000, "ms")
    val outTime by int("OutTime", 500, 0..5000, "ms")

    private val colorSetting by color("Color", Color4b(0, 255, 0, 150))
    private val outlineColorSetting by color("OutlineColor", Color4b(0, 255, 0, 150))

    /**
     * The [PlacementRenderHandler]s managed by this renderer.
     *
     * Handler should be added/removed only on the main thread because this map is not thread save.
     *
     * By default, there is a handler registered with the number `0`.
     */
    var placementRenderHandlers = Int2ObjectOpenHashMap<PlacementRenderHandler>()

    init {
        placementRenderHandlers.put(0, PlacementRenderHandler(this))
    }

    private var outAnimationsFinished = true

    val renderHandler = handler<WorldRenderEvent> { event ->
        val time = System.currentTimeMillis()
        placementRenderHandlers.values.forEach { it.render(event, time) }
    }

    @Suppress("unused")
    private val repeatable = repeatable {
        if (!outAnimationsFinished && placementRenderHandlers.values.all { it.isFinished() }) {
            outAnimationsFinished = true
        }
    }

    /**
     * Adds a block to be rendered. First it will make an appear-animation, then
     * it will continue to get rendered until it's removed or the world changes.
     *
     * @param handlerId To which handler the block should be added.
     */
    fun addBlock(pos: BlockPos, update: Boolean = true, box: Box = FULL_BOX, handlerId: Int = 0) {
        // return if the renderer is deactivated or the box is empty, as there wouldn't be anything to render
        if (!enabled || box == EMPTY_BOX) {
            return
        }

        val handler = placementRenderHandlers[handlerId] ?: return
        handler.addBlock(pos, update, box)
    }

    /**
     * Removes a block from the rendering, it will get an out animation tho.
     *
     * @param handlerId From which handler the block should be removed.
     */
    fun removeBlock(pos: BlockPos, handlerId: Int = 0) {
        if (!enabled) {
            return
        }

        val handler = placementRenderHandlers[handlerId] ?: return
        handler.removeBlock(pos)
    }

    /**
     * Updates all culling data.
     *
     * This can be useful to reduce overhead when adding a bunch of positions,
     * so that positions don't get updated multiple times.
     *
     * @param handlerId On which handler the update should be performed.
     */
    fun updateAll(handlerId: Int = 0) {
        if (!clump) {
            return
        }

        val handler = placementRenderHandlers[handlerId] ?: return
        handler.updateAll()
    }

    /**
     * Puts all currently rendered positions in the out-animation state and keeps it being rendered until
     * all animations have been finished even though the module might be already disabled.
     *
     * Performed on all handlers in this renderer.
     */
    fun clearSilently() {
        placementRenderHandlers.values.forEach { it.clearSilently() }
        outAnimationsFinished = false
    }

    override fun disable() {
        placementRenderHandlers.values.forEach { it.clear() }
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        placementRenderHandlers.values.forEach { it.clear() }
    }

    /**
     * Only run when the module and this is enabled or out-animations are running.
     */
    override fun handleEvents(): Boolean {
        return module.handleEvents() && enabled || !outAnimationsFinished
    }

    /**
     * Returns the box color.
     *
     * @param id The handler requesting the color.
     */
    open fun getColor(id: Int): Color4b = colorSetting

    /**
     * Returns the outline color.
     *
     * @param id The handler requesting the color.
     */
    open fun getOutlineColor(id: Int): Color4b = outlineColorSetting

}
