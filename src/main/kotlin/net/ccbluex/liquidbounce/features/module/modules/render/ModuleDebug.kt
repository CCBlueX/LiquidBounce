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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import net.ccbluex.fastutil.forEachFloat
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.fastutil.step
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.italic
import net.ccbluex.liquidbounce.utils.client.textOf
import net.ccbluex.liquidbounce.utils.client.underline
import net.ccbluex.liquidbounce.utils.client.vector2f
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleDebug : ClientModule("Debug", ModuleCategories.RENDER) {

    private val parameters by boolean("Parameters", true).onChanged { _ ->
        debugParameters.clear()
    }
    private val geometry by boolean("Geometry", true).onChanged { _ ->
        debuggedGeometry.clear()
    }

    private val expireTime by int("Expires", 5, 1..30, "secs")

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

            renderEnvironmentForWorld(event.matrixStack) {
                drawLineStrip(
                    Color4b.BLUE.argb,
                    positions = cachedPositions.mapToArray { relativeToCamera(it.pos).toVec3f() },
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

                fontRenderer.draw("Graph".asPlainText()) {
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

    @JvmRecord
    private data class DebuggedKey(val owner: DebuggedOwner, val name: String)

    @JvmRecord
    private data class ParameterCapture(val time: Long = System.currentTimeMillis(), val value: Any?)

    private val debugParameters = Object2ObjectOpenHashMap<DebuggedKey, ParameterCapture>()

    private val debuggedGeometry = Object2ObjectOpenHashMap<DebuggedKey, DebuggedGeometry>()

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (!geometry) {
            return@handler
        }

        renderEnvironmentForWorld(event.matrixStack) {
            startBatch()
            debuggedGeometry.values.forEach { geometry ->
                geometry.render()
            }
            commitBatch()
        }
    }

    @Suppress("unused")
    private val scaffoldDebugging = handler<GameTickEvent> {
        if (!ModuleScaffold.running) {
            return@handler
        }

        val pos0 = Vec3(77.0, 75.0, -52.0)
        val face = AlignedFace(pos0, pos0.add(1.0, 1.0, 0.0))

        debugGeometry(
            ModuleScaffold,
            "targetFace",
            DebuggedBox(AABB(face.from, face.to), Color4b(255, 0, 0, 64))
        )

        val line = LineSegment(player.eyePosition, player.lookAngle, 0.0..10.0)

        debugGeometry(
            ModuleScaffold,
            "daLine",
            DebuggedLineSegment(line.endPoints.first, line.endPoints.second, Color4b(0, 0, 255, 255))
        )

        val pointTo = face.nearestPointTo(line)

        debugGeometry(
            ModuleScaffold,
            "targetPoint",
            DebuggedPoint(pointTo, Color4b(0, 0, 255, 255), size = 0.05)
        )
    }

    @Suppress("unused")
    private val expireHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        val earliest = System.currentTimeMillis() - expireTime * 1000

        debugParameters.entries.removeIf { (_, capture) ->
            capture.time <= earliest
        }
    }

    @Suppress("unused")
    private val screenRenderHandler = handler<OverlayRenderEvent> { event ->
        val context = event.context

        if (mc.options.keyPlayerList.isDown || !parameters) {
            return@handler
        }

        /**
         * Separate the debugged owner from its parameter
         * Structure should be like this:
         * Owner ->
         *   Parameter Name: Parameter Value
         *   Parameter Name: Parameter Value
         *   Parameter Name: Parameter Value
         */
        val textList = mutableListOf<Component>()

        val debuggedOwners = debugParameters.keys.groupBy { it.owner }

        val currentTime = System.currentTimeMillis()

        fun ownerName(owner: DebuggedOwner): Component {
            return when (owner) {
                is ClientModule -> owner.name.asText().withStyle(ChatFormatting.GOLD).bold(true)
                is Command -> "Command ${owner.name}".asText().withStyle(ChatFormatting.GOLD).underline(true)
                is EventListener -> listOfNotNull(
                    owner.parent()?.let { ownerName(it) },
                    "::".asPlainText(ChatFormatting.GRAY),
                    owner.javaClass.simpleName.asText().withStyle(ChatFormatting.DARK_AQUA).italic(true),
                ).asText()

                is CoroutineScope -> owner.coroutineContext[CoroutineName]?.name?.asPlainText(ChatFormatting.GRAY)
                    ?: owner.toString().asPlainText()

                else -> owner.javaClass.simpleName.asPlainText(ChatFormatting.BLUE)
            }
        }

        debuggedOwners.forEach { (owner, parameter) ->
            textList += ownerName(owner)

            parameter.forEach { debuggedParameter ->
                val parameterName = debuggedParameter.name
                val parameterCapture = debugParameters[debuggedParameter] ?: return@forEach
                val duration = (currentTime - parameterCapture.time) / 1000
                textList += textOf(
                    "$parameterName: ".asPlainText(ChatFormatting.WHITE),
                    parameterCapture.value.toString().asPlainText(ChatFormatting.GREEN),
                    " [${duration}s ago]".asPlainText(ChatFormatting.GRAY),
                )
            }
        }

        with(event.context) {
            // Draw
            fontRenderer.draw("Debugging".asPlainText()) {
                x = 120f
                y = 22f
                shadow = true
                scale = 0.3f
            }

            // Draw text line one by one
            textList.forEachIndexed { index, text ->
                fontRenderer.draw(text) {
                    x = 120f
                    y = 40 + ((fontRenderer.height * 0.17f) * index)
                    shadow = true
                    scale = 0.17f
                }
            }
        }
    }

    fun debugGeometry(owner: DebuggedOwner, name: String, geometry: DebuggedGeometry) {
        // Do not take any new debugging while the module is off
        if (!running) {
            return
        }

        debuggedGeometry[DebuggedKey(owner, name)] = geometry
    }

    inline fun DebuggedOwner.debugGeometry(name: String, lazyGeometry: () -> DebuggedGeometry) {
        if (!running) {
            return
        }

        debugGeometry(owner = this, name, lazyGeometry())
    }

    fun debugParameter(owner: DebuggedOwner, name: String, value: Any?) {
        if (!running) {
            return
        }

        debugParameters[DebuggedKey(owner, name)] = ParameterCapture(value = value)
    }

    inline fun DebuggedOwner.debugParameter(name: String, lazyValue: () -> Any?) {
        if (!running) {
            return
        }

        debugParameter(owner = this, name, lazyValue())
    }

    fun getArrayEntryColor(idx: Int, length: Int): Color4b {
        val hue = idx.toFloat() / length.toFloat()
        return Color4b.ofHSB(hue, 1f, 1f, alpha = 32f / 255f)
    }

    fun interface DebuggedGeometry {
        context(env: WorldRenderEnvironment)
        fun render()
    }

    class DebuggedLine(line: Line, val color: Color4b) : DebuggedGeometry {
        val from: Vec3
        val to: Vec3

        init {
            val normalizedDirection = line.direction.normalize()

            this.from = line.position.subtract(normalizedDirection.scale(100.0))
            this.to = line.position.add(normalizedDirection.scale(100.0))
        }

        context(env: WorldRenderEnvironment)
        override fun render() {
            env.drawLine(
                env.relativeToCamera(from).toVec3f(),
                env.relativeToCamera(to).toVec3f(),
                color.argb,
            )
        }
    }

    class DebuggedTriangle(
        val p1: Vec3,
        val p2: Vec3,
        val p3: Vec3,
        val color: Color4b,
    ) : DebuggedGeometry {
        context(env: WorldRenderEnvironment)
        override fun render() {
            env.drawTriangle(
                p1 = env.relativeToCamera(p1).toVec3f(),
                p2 = env.relativeToCamera(p2).toVec3f(),
                p3 = env.relativeToCamera(p2).toVec3f(),
                argb = color.argb,
            )
        }
    }

    class DebuggedLineSegment(val from: Vec3, val to: Vec3, val color: Color4b) : DebuggedGeometry {
        context(env: WorldRenderEnvironment)
        override fun render() {
            env.drawLine(
                env.relativeToCamera(from).toVec3f(),
                env.relativeToCamera(to).toVec3f(),
                color.argb,
            )
        }
    }

    open class DebuggedBox(val box: AABB, val color: Color4b) : DebuggedGeometry {
        context(env: WorldRenderEnvironment)
        override fun render() {
            env.drawBox(box.move(env.camera.position().reverse()), color)
        }
    }

    class DebuggedPoint(point: Vec3, color: Color4b, size: Double = 0.2) : DebuggedBox(
        AABB.ofSize(point, size, size, size),
        color
    )

    class DebugCollection(val geometry: Collection<DebuggedGeometry>) : DebuggedGeometry {
        context(env: WorldRenderEnvironment)
        override fun render() {
            this.geometry.forEach { it.render() }
        }
    }

    override fun onDisabled() {
        // Might clean up some memory if we disable the module
        debuggedGeometry.clear()
        debugParameters.clear()
        super.onDisabled()
    }

}
