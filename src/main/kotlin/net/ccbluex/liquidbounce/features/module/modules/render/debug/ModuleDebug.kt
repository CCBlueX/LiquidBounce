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
package net.ccbluex.liquidbounce.features.module.modules.render.debug

import net.ccbluex.fastutil.forEachFloat
import net.ccbluex.fastutil.step
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.MutableVertexList
import net.ccbluex.liquidbounce.utils.math.vector2f
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.minecraft.client.gui.screens.ChatScreen
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Debug module
 *
 * Only of interest to developers.
 */

object ModuleDebug : ClientModule("Debug", ModuleCategories.RENDER) {

    private val sections by clientState { ConcurrentHashMap<DebuggedOwner, DebugSection>() }
    private val nextOrder by clientState { AtomicInteger() }

    /**
     * Bumped after parameters appear, disappear or change priority, so the overview lays out again.
     */
    private val structureVersion by clientState { AtomicInteger() }

    private val parameters by boolean("Parameters", true).onChanged { _ ->
        sections.values.forEach { it.parameters.clear() }
        structureVersion.incrementAndGet()
    }

    /**
     * Shows owners outside of modules, such as the server and timer state, which report every tick.
     */
    private val showGlobal by boolean("Global", false).onChanged { _ ->
        sections.values.forEach { if (it.global) it.parameters.clear() }
        structureVersion.incrementAndGet()
    }
    private val geometry by boolean("Geometry", true).onChanged { _ ->
        sections.values.forEach { it.geometry.clear() }
    }

    private val expireTime by int("Expires", 5, 1..30, "secs")
    private val width by int("Width", 180, 120..400, "px")
    private val titleFilter by regex("TitleFilter", Regex(".*")).onChanged { _ ->
        sections.clear()
        structureVersion.incrementAndGet()
    }

    private val debugLog by boolean("DebugLog", false)

    private val outputDir = ConfigSystem.rootFolder.resolve("debug-log").apply { mkdirs() }
    // TSV: timestamp, owner path, name, value
    private var outputFile: File? by clientState { null }

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    object RenderSimulatedPlayer : ToggleableValueGroup(this, "SimulatedPlayer", false) {

        private val ticksToPredict by int("TicksToPredict", 20, 5..100)

        @Suppress("unused")
        private val movementInputHandler = handler<MovementInputEvent> { _ ->
            PlayerSimulationCache.getSimulationForLocalPlayer().simulateUntil(this.ticksToPredict)
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            val cachedPositions = PlayerSimulationCache
                .getSimulationForLocalPlayer()
                .getSnapshotsBetween(0 until this.ticksToPredict)

            event.renderEnvironment {
                drawLineStrip(
                    Color4b.BLUE.argb,
                    positions = MutableVertexList(cachedPositions.size)
                        .addAllRelativeToCamera(cachedPositions, camera) { it.pos },
                )
            }
        }

    }

    object Graph : ToggleableValueGroup(this, "Graph", false) {

        private val curve = curve(
            "Curve", mutableListOf(
                0f vector2f 120f,
                50f vector2f 60f,
                140f vector2f 120f,
                180f vector2f 90f
            ),
            xAxis = "X Axis" axis 0f..180f,
            yAxis = "Y Axis" axis 40f..120f
        )

        @Suppress("unused")
        private val screenRenderHandler = handler<OverlayRenderEvent> { event ->
            val context = event.context

            with(context) {
                var posX = 300
                var posY = 500

                fontRenderer.draw("Graph".withFormat()) {
                    x = posX.toFloat()
                    y = posY.toFloat()
                    shadow = true
                    scale = 0.3f
                }

                curve.xAxis.range.step(0.1f).forEachFloat { x ->
                    var y = curve.transform(x)
                    this.drawQuad(
                        posX + x,
                        posY - y,
                        posX + x + 1,
                        posY - y + 1,
                        Color4b.GREEN
                    )
                }

                val points = curve.get()
                for (point in curve.get()) {
                    var x = point[0]
                    var y = point[1]

                    this.drawQuad(
                        posX + x - 2,
                        posY - y - 2,
                        posX + x + 2,
                        posY - y + 2,
                        Color4b.WHITE
                    )
                }
            }
        }

    }

    init {
        tree(RenderSimulatedPlayer)
        tree(Graph)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (!geometry) {
            return@handler
        }

        event.renderEnvironment {
            for (section in sections.values) {
                section.geometry.values.forEach { it.render() }
            }
        }
    }

    @Suppress("unused")
    private val expireHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        val earliest = System.currentTimeMillis() - expireTime * 1000L

        var removed = false
        for (section in sections.values) {
            removed = section.parameters.values.removeIf { it.time <= earliest } || removed
        }
        if (removed) {
            structureVersion.incrementAndGet()
        }
    }

    @Suppress("unused")
    private val screenRenderHandler = handler<OverlayRenderEvent> { event ->
        if (mc.options.keyPlayerList.isDown || !parameters) {
            return@handler
        }

        with(event.context) {
            DebugOverview.render(sections.values, structureVersion.get(), width, expireTime * 1000L)
        }
    }

    @Suppress("unused")
    private val scrollHandler = handler<MouseScrollEvent> { event ->
        if (!parameters || mc.gui.screen() !is ChatScreen) {
            return@handler
        }

        val window = mc.window
        val mouseX = mc.mouseHandler.xpos() * window.guiScaledWidth / window.screenWidth
        val mouseY = mc.mouseHandler.ypos() * window.guiScaledHeight / window.screenHeight
        if (DebugOverview.scroll(event.vertical.toFloat(), mouseX.toFloat(), mouseY.toFloat())) {
            event.cancelEvent()
        }
    }

    @PublishedApi
    internal fun parameterSection(owner: DebuggedOwner): DebugSection? =
        if (!running || !parameters) null else section(owner)?.takeIf { showGlobal || !it.global }

    @PublishedApi
    internal fun geometrySection(owner: DebuggedOwner): DebugSection? =
        if (!running || !geometry) null else section(owner)

    private fun section(owner: DebuggedOwner): DebugSection? =
        (sections[owner] ?: sections.computeIfAbsent(owner, ::createSection)).takeIf { it.accepted }

    private fun createSection(owner: DebuggedOwner): DebugSection {
        val chain = generateSequence(owner) { (it as? EventListener)?.parent() }.take(16).toList().asReversed()
        val path = chain.joinToString("/") { it.debugName }

        return DebugSection(
            root = chain.first(),
            label = chain.drop(1).joinToString(" / ") { it.debugName }.ifEmpty { null },
            path = path,
            accepted = titleFilter.containsMatchIn(path),
            global = chain.first() !is ClientModule,
        )
    }

    @PublishedApi
    internal fun putParameter(section: DebugSection, name: String, priority: DebugPriority, value: Any?) {
        val now = System.currentTimeMillis()
        val parameter = section.parameters[name] ?: run {
            val created = DebugParameter(name, nextOrder.getAndIncrement(), priority)
            section.parameters.putIfAbsent(name, created) ?: created.also { structureVersion.incrementAndGet() }
        }

        if (parameter.priority != priority) {
            parameter.priority = priority
            structureVersion.incrementAndGet()
        }
        parameter.update(value, now)

        outputFile?.appendText("$now\t${section.path}\t$name\t$value\n")
    }

    @PublishedApi
    internal fun putGeometry(section: DebugSection, name: String, geometry: DebuggedGeometry?) {
        if (geometry != null) {
            section.geometry[name] = geometry
        } else {
            section.geometry.remove(name)
        }
    }

    override fun onEnabled() {
        if (debugLog) {
            outputFile = outputDir.resolve(LocalDateTime.now().toUnderlinedString() + ".tsv")
        }
        super.onEnabled()
    }

    override fun onDisabled() {
        sections.clear()
        structureVersion.incrementAndGet()
        DebugOverview.clear()
        outputFile = null
        super.onDisabled()
    }

}
