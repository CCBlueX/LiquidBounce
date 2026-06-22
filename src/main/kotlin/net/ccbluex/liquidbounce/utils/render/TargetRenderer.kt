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
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ccbluex.fastutil.toEnumSet
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawCircle
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.AnimatedValueGroup
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.lastRenderPos
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.toDegrees
import net.ccbluex.liquidbounce.utils.render.WorldToScreen.calculateScreenPos
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetRenderer(
    owner: ToggleableValueGroup,
    val target: () -> Entity?,
) : ToggleableValueGroup(owner, "TargetRendering", true) {

    constructor(module: ToggleableValueGroup, targetTracker: TargetTracker) : this(module, targetTracker::target)

    init {
        doNotIncludeAlways()
    }

    private val appearance = modes(owner, "Mode", 3) {
        arrayOf(
            TargetRenderAppearance.World.Legacy(it),
            TargetRenderAppearance.World.Circle(owner, it),
            TargetRenderAppearance.World.Image(owner, it),
            TargetRenderAppearance.World.GlowingCircle(owner, it),
            TargetRenderAppearance.World.Ghost(it),
            TargetRenderAppearance.World.Hearts(it),
            TargetRenderAppearance.Gui.Text(owner, it),
            TargetRenderAppearance.Gui.Arrow(it),
        )
    }

    @Suppress("unused")
    private val worldRenderHandler = handler<WorldRenderEvent> { event ->
        val mode = appearance.activeMode as? TargetRenderAppearance.World ?: return@handler

        val target = target() ?: return@handler

        with(mode) {
            renderEnvironmentForWorld(event.matrixStack) {
                render(target, event.partialTicks)
            }
        }
    }

    @Suppress("unused")
    private val guiRenderHandler = handler<OverlayRenderEvent> { event ->
        val mode = appearance.activeMode as? TargetRenderAppearance.Gui ?: return@handler

        val target = target() ?: return@handler

        with(mode) {
            event.context.render(target, event.tickDelta)
        }
    }

}

private sealed class TargetRenderAppearance<Ctx : Any>(name: String) : Mode(name) {
    abstract fun Ctx.render(entity: Entity, partialTicks: Float)

    sealed class World(name: String) : TargetRenderAppearance<WorldRenderEnvironment>(name) {

        class Ghost(override val parent: ModeValueGroup<*>) : World("Ghost") {

            private val color by color("Color", Color4b.BLUE)
            private val size by float("Size", 0.5f, 0.4f..0.7f)
            private val length by int("Length", 25, 15..40)

            override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                poseStack.pushPose()

                val interpolated = entity.lastRenderPos().lerp(entity.position(), partialTicks.toDouble())
                    .add(0.2, 1.25, 0.0)

                poseStack.translate(interpolated - camera.position())

                drawParticle(
                    { sin, cos -> Vec3(sin, cos, -cos) },
                    { sin, cos -> Vec3(-sin, -cos, cos) }
                )

                drawParticle(
                    { sin, cos -> Vec3(-sin, sin, -cos) },
                    { sin, cos -> Vec3(sin, -sin, cos) }
                )

                drawParticle(
                    { sin, cos -> Vec3(-sin, -sin, cos) },
                    { sin, cos -> Vec3(sin, sin, -cos) }
                )

                poseStack.popPose()
            }

            private inline fun WorldRenderEnvironment.drawParticle(
                translationsBefore: PoseStack.(Double, Double) -> Vec3,
                translateAfter: PoseStack.(Double, Double) -> Vec3
            ) {
                val radius = 0.67
                val distance = 10.0 + (length * 0.2)
                val alphaFactor = 15

                for (i in 0..<length) {
                    val angle: Double = 0.15f * (clientStartDurationMs - (i * distance)) / (30)
                    val sin = sin(angle) * radius
                    val cos = cos(angle) * radius

                    with(poseStack) {
                        translate(translationsBefore(sin, cos))

                        translate(-size / 2.0, -size / 2.0, 0.0)
                        mulPose(Axis.YP.rotationDegrees(-camera.yRot()))
                        mulPose(Axis.XP.rotationDegrees(camera.xRot()))
                        translate(size / 2.0, size / 2.0, 0.0)
                    }

                    val alpha = Mth.clamp(color.a - (i * alphaFactor), 0, color.a)
                    val renderColor = color.alpha(alpha)

                    drawSquareTexture(ghostModeTexture, size, renderColor.argb)

                    with(poseStack) {
                        translate(-size / 2.0, -size / 2.0, 0.0)
                        mulPose(Axis.XP.rotationDegrees(-camera.xRot()))
                        mulPose(Axis.YP.rotationDegrees(camera.yRot()))
                        translate(size / 2.0, size / 2.0, 0.0)

                        translate(translateAfter(sin, cos))
                    }
                }
            }
        }

        class Legacy(override val parent: ModeValueGroup<*>) : World("Legacy") {

            private val size by float("Size", 0.5f, 0.1f..2f)

            private val height by float("Height", 0.1f, 0.02f..2f)

            private val color by color("Color", defaultColor)

            private val extraYOffset by float("ExtraYOffset", 0.1f, 0f..1f)

            override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                val box = AABB(
                    -size.toDouble(), 0.0, -size.toDouble(),
                    size.toDouble(), height.toDouble(), size.toDouble()
                )

                val pos = entity.interpolateCurrentPosition(partialTicks)
                    .add(0.0, entity.bbHeight.toDouble() + extraYOffset.toDouble(), 0.0)

                withPositionRelativeToCamera(pos) {
                    drawBox(box, color)
                }
            }
        }

        class Image(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) : World("Image") {

            private val textureMode = modes("Source", 0) {
                arrayOf(
                    TextureMode.Custom(it),
                    TextureMode.Builtin(it, PresetTexture.MARKER1, PresetTexture.entries.toEnumSet())
                )
            }
            private val scale by vec2f("Scale", Vector2f(1f, 1f))
            private val color by color("ColorModulator", Color4b.WHITE)
            private val rotate = tree(object : AnimatedValueGroup("Rotate") {
                override val curve = curve("Curve") {
                    "Progress" x 0f..1f
                    "Degrees" y -180f..180f
                    points(Vector2f(0f, 0f), Vector2f(1f, 0f))
                }
            })

            private val heightMode = modes(owner, "HeightMode") {
                arrayOf(
                    HeightMode.Feet(it),
                    HeightMode.Top(it),
                    HeightMode.Relative(it),
                    HeightMode.Health(it),
                    HeightMode.Animated(it),
                )
            }

            private enum class PresetTexture(override val tag: String, val path: String) : TextureMode.Builtin.Preset {
                MARKER1("Marker1", "target_renderer/target.png"),
                MARKER2("Marker2", "target_renderer/target2.png");

                override val texture = LiquidBounce.resource(this.path)
                    .readNativeImage().asTexture { "TargetRenderer Image $tag" }
            }

            private val quaternion = Quaternionf()

            override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                val texture = textureMode.activeMode.texture ?: return

                val height = heightMode.activeMode.getHeight(entity, partialTicks)
                val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

                withPositionRelativeToCamera(pos) {
                    poseStack.mulPose(camera.rotation())
                    poseStack.mulPose(
                        quaternion.scaling(1f)
                            .rotateLocalZ(rotate.current().toRadians())
                    )
                    poseStack.last().scale(scale.x(), scale.y(), 1f)
                    drawTexQuad(texture, color.argb)
                }
            }
        }

        class Circle(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) : World("Circle") {

            private val radius by float("Radius", 0.85f, 0.1f..2f)
            private val innerRadius by float("InnerRadius", 0f, 0f..2f)
                .onChange { min(radius, it) }

            private val heightMode = modes(owner, "HeightMode") {
                arrayOf(
                    HeightMode.Feet(it),
                    HeightMode.Top(it),
                    HeightMode.Relative(it),
                    HeightMode.Health(it),
                    HeightMode.Animated(it),
                )
            }

            private val outerColor by color("OuterColor", defaultColor)
            private val innerColor by color("InnerColor", defaultColor)

            private val outlineColor by color("Color", Color4b.fullAlpha(0x007CFF))

            override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                val height = heightMode.activeMode.getHeight(entity, partialTicks)
                val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

                withPositionRelativeToCamera(pos) {
                    drawGradientCircle(radius, innerRadius, outerColor, innerColor)
                    drawCircleOutline(radius, outlineColor)
                }
            }

        }

        class GlowingCircle(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) :
            World("GlowingCircle") {
            private val radius by float("Radius", 0.85f, 0.1f..2f)

            private val heightMode = modes(owner, "HeightMode") {
                arrayOf(
                    HeightMode.Feet(it),
                    HeightMode.Top(it),
                    HeightMode.Relative(it),
                    HeightMode.Health(it),
                    HeightMode.Animated(it),
                )
            }

            private val color by color("OuterColor", defaultColor)
            private val glowColor by color("GlowColor", Color4b.LIQUID_BOUNCE.alpha(0))

            private val glowHeightSetting by float("GlowHeight", 0.3f, -1f..1f)

            private val outlineColor by color("Color", Color4b.fullAlpha(0x007CFF))

            override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                val height = heightMode.activeMode.getHeight(entity, partialTicks)
                val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)

                val currentHeightMode = heightMode.activeMode

                val glowHeight = if (currentHeightMode is HeightMode.WithGlow) {
                    currentHeightMode.getGlowHeight(entity, partialTicks) - height
                } else {
                    glowHeightSetting.toDouble()
                }

                withPositionRelativeToCamera(pos) {
                    drawGradientCircle(
                        radius,
                        radius,
                        color,
                        glowColor,
                        Vector3f(0f, glowHeight.toFloat(), 0f)
                    )

                    drawCircle(radius, color)
                    drawCircleOutline(radius, outlineColor)
                }
            }

        }

        class Hearts(override val parent: ModeValueGroup<*>) : World("Hearts") {

            private val color by color("Color", Color4b.WHITE.alpha(180))
            private val dynamicCount by boolean("DynamicCount", true)
            private val heartCount by int("HeartCount", 10, 1..32)
            private val yOffset by float("YOffset", 0.1f, -1f..3f)
            private val size by float("Size", 0.15f, 0.05f..1f).onChange {
                heartLayoutDirty = true
                it
            }
            private class OrbitSettings : ValueGroup("Orbit") {
                val radius by float("Radius", 0.5f, 0.1f..1f)
                val speed by float("Speed", 35f, -360f..360f, "deg/s")
                val squeezeStrength by float("SqueezeStrength", 0.25f, 0f..1f)
                val squeezeSpeed by int("SqueezeSpeed", 2, 1..4)
            }
            private val orbit = tree(OrbitSettings())
            private val canBeCovered by boolean("CanBeCovered", false)

            private var currentTargetId: Int = -1
            private var damageFlashStrength = 0f
            private var damageSqueezeStrength = 0f
            private var lastUpdTime = 0L
            private var heartLayoutDirty = true
            private val heartLayout = ArrayList<HeartPlacement>()

            private fun ensureHeartLayout(requiredCount: Int) {
                if (heartLayoutDirty) {
                    heartLayout.clear()
                    heartLayoutDirty = false
                }

                if (heartLayout.size >= requiredCount) {
                    return
                }

                heartLayout.ensureCapacity(requiredCount)
                val minAngleDistance = size * 115.0
                val minHeightDistance = size * 2.0f
                val attemptLimit = max(64, requiredCount * 24)
                var attempts = 0

                while (heartLayout.size < requiredCount && attempts < attemptLimit) {
                    attempts++

                    val candidate = HeartPlacement(
                        baseOrbitAngle = Random.nextDouble(0.0, 360.0),
                        heightFactor = Random.nextFloat(),
                    )

                    if (heartLayout.none { it.overlaps(candidate, minAngleDistance, minHeightDistance) }) {
                        heartLayout += candidate
                    }
                }

                while (heartLayout.size < requiredCount) {
                    heartLayout += HeartPlacement(
                        baseOrbitAngle = Random.nextDouble(0.0, 360.0),
                        heightFactor = Random.nextFloat(),
                    )
                }
            }

            override fun WorldRenderEnvironment.render(entity: Entity, partialTicks: Float) {
                val target = entity as? LivingEntity ?: return
                val heartSlots = heartSlots(target)

                updateState(target, heartSlots.size)

                val nowSeconds = System.currentTimeMillis() / 1000.0
                val targetPos = target.interpolateCurrentPosition(partialTicks)

                for (index in heartSlots.indices) {
                    val instance = heartLayout[index]
                    val heartSlot = heartSlots[index]

                    val orbitAngleDegrees = instance.baseOrbitAngle + nowSeconds * orbit.speed

                    val orbitAngle = orbitAngleDegrees.toRadians()
                    val orbitDistance =
                        (orbit.radius.toDouble() - damageSqueezeStrength)
                            .coerceIn(0.05, orbit.radius.toDouble())

                    val localOffset = Vec3(
                        cos(orbitAngle) * orbitDistance,
                        yOffset.toDouble() + target.bbHeight.toDouble() * instance.heightFactor,
                        sin(orbitAngle) * orbitDistance
                    )

                    val worldPos = targetPos.add(localOffset)
                    val baseColor = when (heartSlot.type) {
                        HeartType.Health -> color
                        HeartType.Absorption -> Color4b(255, 214, 72, color.a)
                    }

                    val renderColor = baseColor.interpolateTo(
                        Color4b.RED.alpha(color.a),
                        damageFlashStrength.toDouble()
                    )

                    drawHeart(worldPos, targetPos, renderColor, heartSlot.fill)
                }
            }

            private fun updateState(target: LivingEntity, heartCount: Int) {
                val now = System.currentTimeMillis()
                var deltaSeconds =
                    if (lastUpdTime != 0L) ((now - lastUpdTime) / 1000f).coerceAtMost(0.25f) else 0f

                lastUpdTime = now

                if (target.id != currentTargetId) {
                    currentTargetId = target.id
                    damageFlashStrength = 0f
                    damageSqueezeStrength = 0f
                    deltaSeconds = 0f
                    heartLayoutDirty = true
                }

                damageFlashStrength =
                    if (target.hurtTime in 8..10) 1f else max(0f, damageFlashStrength - deltaSeconds * 3.5f)

                val orbitSqueezeStrength = orbit.squeezeStrength

                val inAnim = (5 + orbit.squeezeSpeed)..10
                val outAnim = (1 + orbit.squeezeSpeed)..(4 + orbit.squeezeSpeed)

                damageSqueezeStrength +=
                    when (target.hurtTime) {
                        in inAnim -> max(0f, deltaSeconds * (orbitSqueezeStrength * 5))
                        in outAnim -> -min(damageSqueezeStrength, deltaSeconds * (orbitSqueezeStrength * 5))
                        else -> -damageSqueezeStrength
                    }

                ensureHeartLayout(heartCount)
            }

            private fun WorldRenderEnvironment.drawHeart(pos: Vec3, targetPos: Vec3, color: Color4b, fill: Float) {
                withPositionRelativeToCamera(pos) {
                    val directionToTarget = targetPos.subtract(pos)
                    val targetYaw = atan2(directionToTarget.x, directionToTarget.z).toDegrees().toFloat()
                    poseStack.mulPose(Axis.YP.rotationDegrees(targetYaw))

                    drawHeartSDF(color.alpha((color.a * 0.25f).toInt()), size)
                    drawHeartSDF(color, size, fill)
                }
            }

            private fun WorldRenderEnvironment.drawHeartSDF(color: Color4b, size: Float, fill: Float = 1f) {
                val clampedFill = fill.coerceIn(0f, 1f)
                if (clampedFill <= 0f) {
                    return
                }

                val argb = color.argb
                drawCustomMesh(ClientRenderPipelines.heart(noDepthTest = !canBeCovered)) { pose ->
                    // Preserve the native aspect ratio of sdHeart() in heart.fsh.
                    val halfWidth = size * 1.0938363f
                    val right = -halfWidth + halfWidth * 2f * clampedFill
                    addVertex(pose, -halfWidth, -size, 0f).setUv(0f, 0f).setColor(argb)
                    addVertex(pose, -halfWidth,  size, 0f).setUv(0f, 1f).setColor(argb)
                    addVertex(pose,  right,  size, 0f).setUv(clampedFill, 1f).setColor(argb)
                    addVertex(pose,  right, -size, 0f).setUv(clampedFill, 0f).setColor(argb)
                }
            }

            private fun heartSlots(target: LivingEntity): List<HeartSlot> {
                fun MutableList<HeartSlot>.addSlots(type: HeartType, amount: Float) {
                    val hearts = amount.coerceAtLeast(0f) / 2f
                    val fullHearts = hearts.toInt()
                    val partialHeart = hearts - fullHearts

                    repeat(fullHearts) {
                        add(HeartSlot(type, 1f))
                    }

                    if (partialHeart > 0f) {
                        add(HeartSlot(type, partialHeart))
                    }
                }

                return buildList {
                    if (dynamicCount) {
                        addSlots(HeartType.Health, target.health)
                    } else {
                        repeat(heartCount) {
                            add(HeartSlot(HeartType.Health, 1f))
                        }
                    }

                    addSlots(HeartType.Absorption, target.absorptionAmount)
                }
            }

            private data class HeartSlot(
                val type: HeartType,
                val fill: Float,
            )

            private enum class HeartType {
                Health,
                Absorption,
            }

            private data class HeartPlacement(
                val baseOrbitAngle: Double,
                val heightFactor: Float,
            ) {
                fun overlaps(other: HeartPlacement, minAngleDistance: Double, minHeightDistance: Float): Boolean {
                    val angleDiff = abs(baseOrbitAngle - other.baseOrbitAngle)
                    val wrappedAngleDiff = min(angleDiff, 360.0 - angleDiff)

                    return wrappedAngleDiff < minAngleDistance &&
                        abs(heightFactor - other.heightFactor) < minHeightDistance
                }
            }
        }

    }

    sealed class Gui(name: String) : TargetRenderAppearance<GuiGraphicsExtractor>(name) {

        class Text(owner: ToggleableValueGroup, override val parent: ModeValueGroup<*>) : Gui("Text2D") {

            private val textScale by float("Scale", 1f, 0.01f..10f)
            private val textShadow by boolean("Shadow", true)
            private val color by color("Color", Color4b.RED)

            private val texts by textList("Text", mutableListOf("TARGET"))

            private val heightMode = modes(owner, "HeightMode") {
                arrayOf(
                    HeightMode.Feet(it),
                    HeightMode.Top(it),
                    HeightMode.Relative(it),
                    HeightMode.Health(it),
                    HeightMode.Animated(it),
                )
            }

            private val fontRenderer get() = FontManager.FONT_RENDERER

            override fun GuiGraphicsExtractor.render(entity: Entity, partialTicks: Float) {
                val height = heightMode.activeMode.getHeight(entity, partialTicks)
                val pos = entity.interpolateCurrentPosition(partialTicks).add(0.0, height, 0.0)
                val screenPos = calculateScreenPos(pos) ?: return

                texts.forEachIndexed { i, text ->
                    fontRenderer.draw(text.asPlainText(Style.EMPTY + color)) {
                        horizontalAnchor = HorizontalAnchor.CENTER
                        verticalAnchor = VerticalAnchor.MIDDLE
                        x = screenPos.x
                        y = screenPos.y + i * fontRenderer.height
                        shadow = textShadow
                        scale = textScale
                    }
                }
            }
        }

        class Arrow(override val parent: ModeValueGroup<*>) : Gui("Arrow") {

            private val color by color("Color", Color4b.RED)
            private val outlineColor by color("OutlineColor", Color4b.TRANSPARENT)
            private val size by float("Size", 1.5f, 0.5f..20f)

            override fun GuiGraphicsExtractor.render(entity: Entity, partialTicks: Float) {
                val pos = entity.interpolateCurrentPosition(partialTicks)
                    .add(0.0, entity.bbHeight.toDouble(), 0.0)

                val screenPos = calculateScreenPos(pos) ?: return
                val minX = screenPos.x - 5 * size
                val midX = screenPos.x
                val maxX = screenPos.x + 5 * size
                val minY = screenPos.y - 10 * size
                val maxY = screenPos.y
                drawTriangle(
                    x0 = minX, y0 = minY,
                    x1 = midX, y1 = maxY,
                    x2 = maxX, y2 = minY,
                    color,
                    outlineColor,
                )
            }
        }
    }
}

private val defaultColor = Color4b.LIQUID_BOUNCE.alpha(100)

private val ghostModeTexture = LiquidBounce.resource("particles/glow.png")
    .readNativeImage().asTexture { "TargetRenderer Ghost" }

private sealed class HeightMode(name: String) : Mode(name) {
    abstract fun getHeight(entity: Entity, partialTicks: Float): Double

    interface WithGlow {
        fun getGlowHeight(entity: Entity, partialTicks: Float): Double
    }

    class Feet(override val parent: ModeValueGroup<*>) : HeightMode("Feet") {
        private val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float): Double = offset.toDouble()
    }

    class Top(override val parent: ModeValueGroup<*>) : HeightMode("Top") {
        private val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float) = entity.box.maxY - entity.box.minY + offset
    }

    // Lets the user chose the height relative to the entity's height
    // Use 1 for it to always be at the top of the entity
    // Use 0 for it to always be at the feet of the entity

    class Relative(override val parent: ModeValueGroup<*>) : HeightMode("Relative") {
        private val height by float("Height", 0.5f, -0.5f..1.5f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return height * entityHeight
        }
    }

    class Health(override val parent: ModeValueGroup<*>) : HeightMode("Health") {
        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            if (entity !is LivingEntity) return 0.0
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return entity.health / entity.maxHealth * entityHeight
        }
    }

    class Animated(override val parent: ModeValueGroup<*>) : HeightMode("Animated"), WithGlow {
        private val speed by float("Speed", 0.18f, 0.01f..1f)
        private val heightMultiplier by float("HeightMultiplier", 0.4f, 0.1f..1f)
        private val heightOffset by float("HeightOffset", 1.3f, 0f..2f)
        private val glowOffset by float("GlowOffset", -1f, -3.1f..3.1f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.tickCount + partialTicks) * speed)
        }

        override fun getGlowHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.tickCount + partialTicks) * speed + glowOffset)
        }

        private fun calculateHeight(time: Float) =
            (sin(time.toDouble()) * heightMultiplier + heightOffset)
    }
}
