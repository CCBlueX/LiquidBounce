@file:Suppress("All")

package net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.modes

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.BufferBuilder
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.BreadcrumbsMode
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.ModuleBreadcrumbs.colorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.resource.Resource
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix4f
import org.joml.Quaternionf
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

object DashTrailMode : BreadcrumbsMode("DashTrail") {
    private val showDashSegments by boolean("DashSegments", true)
    private val showDashDots by boolean("DashDots", false)
    private val animationTime by int("AnimTime", 20, 1..50)
    private val animationDuration by int("Time", 4, 1..20)
    private val maxRenderDistance by int("MaxRenderDistance", 50, 1..200)
    private var maxRenderDistanceSq = maxRenderDistance.toDouble().pow(2)

    private const val MIN_ENTITY_SPEED = 0.04
    private const val SPEED_DIVISOR = 0.045
    private const val MIN_DASH_COUNT = 1
    private const val MAX_DASH_COUNT = 16
    private const val POSITION_OFFSET_BASE = 0.0875f
    private const val POSITION_OFFSET_RANGE = 0.175f
    private const val Y_OFFSET_MULTIPLIER = 0.7f

    private val dashCubicTextures: MutableList<TextureResource> = ArrayList()
    private val dashCubicAnimatedTextures: MutableList<MutableList<TextureResource>> = ArrayList()
    private val randomGenerator = Random()
    private val dashCubics: MutableList<DashCubic> = ArrayList()

    private val DASH_CUBIC_BLOOM_TEXTURE =
        "image/dashtrail/dashbloomsample.png".registerAsDynamicImageFromClientResources()

    init {
        loadDashCubicTextures()
        loadDashCubicAnimatedTextures()
        randomGenerator.setSeed(1234567891L)
    }

    private fun loadDashCubicTextures() {
        val totalDashTextures = 21
        for (i in 0 until totalDashTextures) {
            dashCubicTextures.add(
                TextureResource("image/dashtrail/dashcubics/dashcubic${i + 1}.png".registerAsDynamicImageFromClientResources())
            )
        }
    }

    private fun loadDashCubicAnimatedTextures() {
        val animatedDashGroupCounts = intArrayOf(11, 23, 32, 16, 32)
        var groupIndex = 0
        for (dashFragmentCount in animatedDashGroupCounts) {
            groupIndex++
            val animatedTexturesList: MutableList<TextureResource> = ArrayList()
            for (fragIndex in 0 until dashFragmentCount) {
                animatedTexturesList.add(
                    TextureResource("image/dashtrail/dashcubics/group_dashs/group$groupIndex/dashcubic${fragIndex + 1}.png".registerAsDynamicImageFromClientResources())
                )
            }
            if (animatedTexturesList.isNotEmpty()) {
                dashCubicAnimatedTextures.add(animatedTexturesList)
            }
        }
    }

    private fun getDashCubicColor(dashCubic: DashCubic): Color4b {
        val color1 = colorMode.activeChoice.getColors(mc.player).first
        return color1.withAlpha((dashCubic.fadeOut * 255).toInt().coerceIn(0, 255))
    }

    private fun getTextureResolution(identifier: Identifier): IntArray {
        return try {
            val res: Resource = mc.resourceManager.getResource(identifier).orElse(null) ?: return intArrayOf(0, 0)
            res.inputStream.use { stream ->
                val image: BufferedImage = ImageIO.read(stream)
                intArrayOf(image.width, image.height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            intArrayOf(0, 0)
        }
    }


    private fun getRandomDashCubicTextureIndex(): Int = randomGenerator.nextInt(dashCubicTextures.size)
    private fun getRandomAnimatedTextureGroupIndex(): Int = randomGenerator.nextInt(dashCubicAnimatedTextures.size)
    private fun getDashCubicTextureByIndex(index: Int): TextureResource = dashCubicTextures[index]
    private fun getDashCubicAnimatedTextureGroupByIndex(index: Int): List<TextureResource> =
        dashCubicAnimatedTextures[index]

    private fun shouldUseAnimatedTexture(): Boolean = randomGenerator.nextInt(100) > 40

    private fun getDashRenderOptions(): BooleanArray = booleanArrayOf(showDashSegments, showDashDots)

    private fun withDashRenderState(renderAction: () -> Unit, useTexture2D: Boolean, bloom: Boolean) {
        RenderSystem.enableBlend()
        if (bloom) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE)
        } else {
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
            )
        }

        RenderSystem.lineWidth(1.0f)

        RenderSystem.setShader(if (useTexture2D) ShaderProgramKeys.POSITION_TEX_COLOR else ShaderProgramKeys.POSITION_COLOR)

        RenderSystem.disableCull()
        RenderSystem.depthMask(false)
        RenderSystem.disableDepthTest()

        try {
            renderAction()
        } finally {
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
            RenderSystem.enableCull()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
            RenderSystem.disableBlend()
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        }
    }

    private fun getFilteredDashCubics(): List<DashCubic> = dashCubics

    private fun getAnimationDurationTime(): Int = animationDuration

    @Suppress("unused")
    val updateHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val entitySpeed = getEntitySpeed(player)
        val dashCount = if (entitySpeed < MIN_ENTITY_SPEED) {
            MIN_DASH_COUNT
        } else {
            MathHelper.clamp((entitySpeed / SPEED_DIVISOR).toInt(), MIN_DASH_COUNT, MAX_DASH_COUNT)
        }
        val renderOptions = getDashRenderOptions()
        for (i in 0 until dashCount) {
            dashCubics.add(
                DashCubic(
                    DashBase(
                        player,
                        0.04f,
                        DashTexture(true),
                        i.toFloat() / dashCount,
                    ),
                    renderOptions[0] || renderOptions[1]
                )
            )
        }


        dashCubics.removeIf { it.isExpired }
        dashCubics.forEach { it.processMotion(null) }
    }
    val renderHandler = handler<WorldRenderEvent> { event ->
        val partialTicks = event.partialTicks
        val camera = mc.gameRenderer.camera
        val camX = camera.pos.x
        val camY = camera.pos.y
        val camZ = camera.pos.z
        val viewMatrix = Matrix4f().rotation(camera.rotation.conjugate(Quaternionf()))
        viewMatrix.translate(-camera.pos.x.toFloat(), -camera.pos.y.toFloat(), -camera.pos.z.toFloat())
        val projectionMatrix = mc.gameRenderer.getBasicProjectionMatrix(70.0f)
        val frustum = Frustum(viewMatrix, projectionMatrix)

        val renderOptions = getDashRenderOptions()

        val filteredCubics = getFilteredDashCubics().filter { dashCubic ->
            val entity = dashCubic.base.entity
            if (entity != mc.player) return@filter false
            val distanceSq = mc.player?.squaredDistanceTo(entity) ?: return@filter false
            if (distanceSq > maxRenderDistanceSq) return@filter false

            val x = dashCubic.getRenderPosX(partialTicks)
            val y = dashCubic.getRenderPosY(partialTicks)
            val z = dashCubic.getRenderPosZ(partialTicks)
            val bbox = Box(x, y, z, x, y, z).expand(
                0.2 * dashCubic.fadeOut,
                0.2 * dashCubic.fadeOut,
                0.2 * dashCubic.fadeOut
            )
            frustum.isVisible(bbox)
        }

        val matrices = event.matrixStack

        fun renderDashVertices(
            drawMode: VertexFormat.DrawMode,
            bloom: Boolean,
            vertexConsumer: (builder: BufferBuilder, MatrixStack, DoubleArray, DashCubic, DoubleArray, Color4b) -> Unit
        ) {
            withDashRenderState({
                val buf = RenderSystem.renderThreadTesselator()
                val builder = buf.begin(drawMode, VertexFormats.POSITION_COLOR)

                filteredCubics.forEach { dashCubic ->
                    val renderDashPos = doubleArrayOf(
                        dashCubic.getRenderPosX(partialTicks),
                        dashCubic.getRenderPosY(partialTicks),
                        dashCubic.getRenderPosZ(partialTicks)
                    )

                    dashCubic.dashSparks.forEach { spark ->
                        val renderSparkPos = doubleArrayOf(
                            spark.getRenderPosX(partialTicks),
                            spark.getRenderPosY(partialTicks),
                            spark.getRenderPosZ(partialTicks)
                        )

                        val color = getDashCubicColor(dashCubic)
                        vertexConsumer(builder, matrices, renderDashPos, dashCubic, renderSparkPos, color)
                    }
                }

                BufferRenderer.drawWithGlobalProgram(builder.end())
            }, useTexture2D = false, bloom = bloom)
        }
        if (renderOptions[1]) { // DashDots
            renderDashVertices(VertexFormat.DrawMode.DEBUG_LINES, false) { builder, matrices, renderDashPos, _, renderSparkPos, color ->
                builder.vertex(
                    matrices.peek().positionMatrix,
                    (renderSparkPos[0] + renderDashPos[0] - camX).toFloat(),
                    (renderSparkPos[1] + renderDashPos[1] - camY).toFloat(),
                    (renderSparkPos[2] + renderDashPos[2] - camZ).toFloat()
                ).color(color.r, color.g, color.b, color.a)
            }
        }

        if (renderOptions[0]) { // DashSegments
            renderDashVertices(VertexFormat.DrawMode.LINES, true) { builder, matrices, renderDashPos, _, renderSparkPos, color ->
                builder.vertex(
                    matrices.peek().positionMatrix,
                    (renderSparkPos[0] + renderDashPos[0] - camX).toFloat(),
                    (renderSparkPos[1] + renderDashPos[1] - camY).toFloat(),
                    (renderSparkPos[2] + renderDashPos[2] - camZ).toFloat()
                ).color(color.r, color.g, color.b, color.a)

                builder.vertex(
                    matrices.peek().positionMatrix,
                    (-renderSparkPos[0] + renderDashPos[0] - camX).toFloat(),
                    (-renderSparkPos[1] + renderDashPos[1] - camY).toFloat(),
                    (-renderSparkPos[2] + renderDashPos[2] - camZ).toFloat()
                ).color(color.r, color.g, color.b, color.a)
            }
        }

        if (filteredCubics.isNotEmpty()) {
            renderEnvironmentForWorld(matrices) {
                withDashRenderState({
                    filteredCubics.forEach { dashCubic ->
                        dashCubic.drawDash(partialTicks, isBloomRenderer = false, matrixStack = matrices)
                    }
                    RenderSystem.setShaderTexture(0, DASH_CUBIC_BLOOM_TEXTURE)
                    filteredCubics.forEach { dashCubic ->
                        dashCubic.drawDash(partialTicks, isBloomRenderer = true, matrixStack = matrices)
                    }
                }, useTexture2D = true, bloom = true)
            }
        }
    }

    fun getEntitySpeed(entity: Entity): Double {
        val dx = entity.x - entity.prevX
        val dy = entity.y - entity.prevY
        val dz = entity.z - entity.prevZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun drawBoundTexture(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        c: Color4b,
        matrix: Matrix4f
    ) {
        val builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        builder.vertex(matrix, x, y, 0f).texture(0f, 0f).color(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f)
        builder.vertex(matrix, x, y2, 0f).texture(0f, 1f).color(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f)
        builder.vertex(matrix, x2, y2, 0f).texture(1f, 1f).color(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f)
        builder.vertex(matrix, x2, y, 0f).texture(1f, 0f).color(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f)
        BufferRenderer.drawWithGlobalProgram(builder.endNullable() ?: return)
    }

    private fun with3DDashPosition(
        renderPos: DoubleArray,
        renderPart: (Matrix4f) -> Unit,
        rotationValues: FloatArray,
        matrixStack: MatrixStack
    ) {
        matrixStack.push()
        matrixStack.translate(renderPos[0], renderPos[1], renderPos[2])
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotationValues[0]))
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationValues[1]))
        matrixStack.scale(-0.1f, -0.1f, 0.1f)
        val matrix = matrixStack.peek().positionMatrix
        renderPart(matrix)
        matrixStack.pop()
    }

    private fun addDashSpark(segment: DashCubic) {
        segment.dashSparks.add(DashSpark())
    }

    private fun removeFinishedDashSparks(segment: DashCubic) {
        if (segment.dashSparks.isNotEmpty()) {
            if (segment.addExtras) {
                segment.dashSparks.removeIf { it.finished }
            } else {
                segment.dashSparks.clear()
            }
        }
    }

    private class TextureResource(val identifier: Identifier) {
        val resolution: IntArray = getTextureResolution(identifier)
    }

    private class DashCubic(val base: DashBase, val addExtras: Boolean) {
        private val rotationAngles = floatArrayOf(0.0f, 0.0f)
        val dashSparks: MutableList<DashSpark> = ArrayList()
        private var lifeTicks = animationTime
        var fadeOut: Double = 1.0

        val isExpired: Boolean
            get() = fadeOut <= 0.0

        init {
            if (sqrt(base.motionX * base.motionX + base.motionZ * base.motionZ) < 5.0E-4) {
                rotationAngles[0] = (360.0 * Math.random()).toFloat()
                rotationAngles[1] = mc.gameRenderer.camera.pitch
            } else {
                val motionYaw = base.getMotionYaw()
                rotationAngles[0] = motionYaw - 45.0f - 15.0f - (base.entity.prevYaw - base.entity.yaw) * 3.0f
                val currentRotYaw = RotationManager.currentRotation?.yaw ?: base.entity.yaw
                val yawDiff = MathHelper.wrapDegrees((motionYaw + 26.3f) - currentRotYaw)
                rotationAngles[1] = if (yawDiff < 10.0f || yawDiff > 160.0f) -90.0f else mc.gameRenderer.camera.pitch
            }
        }

        fun getRenderPosX(partialTicks: Float): Double = base.posX
        fun getRenderPosY(partialTicks: Float): Double = base.posY
        fun getRenderPosZ(partialTicks: Float): Double = base.posZ

        fun processMotion(nextSegment: DashCubic?) {
            base.prevPosX = base.posX
            base.prevPosY = base.posY
            base.prevPosZ = base.posZ

            if (addExtras) {
                if (randomGenerator.nextInt(12) > 5) {
                    repeat(if (getDashRenderOptions()[0]) 1 else 3) { addDashSpark(this) }
                }
                dashSparks.forEach { it.processMotion() }
            }
            removeFinishedDashSparks(this)
            lifeTicks--
            if (lifeTicks <= 0) {
                fadeOut = max(fadeOut - 0.05, 0.0)
            }
        }

        fun drawDash(partialTicks: Float, isBloomRenderer: Boolean, matrixStack: MatrixStack) {
            val textureResource = base.dashTexture.getResourceWithSizes()
            val scale = 0.02f * fadeOut.toFloat()
            val extX = textureResource.resolution[0] * scale
            val extY = textureResource.resolution[1] * scale
            val renderPos = doubleArrayOf(
                getRenderPosX(partialTicks),
                getRenderPosY(partialTicks),
                getRenderPosZ(partialTicks)
            )

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR)
            if (isBloomRenderer) {
                with3DDashPosition(renderPos, { matrix ->
                    val extXY = sqrt((extX * extX + extY * extY))
                    drawBoundTexture(-extXY * 2.0f, -extXY * 2.0f, extXY * 2.0f, extXY * 2.0f, getDashCubicColor(this), matrix)
                }, floatArrayOf(mc.gameRenderer.camera.yaw, mc.gameRenderer.camera.pitch), matrixStack)
            } else {
                RenderSystem.setShaderTexture(0, textureResource.identifier)
                with3DDashPosition(renderPos, { matrix ->
                    drawBoundTexture(
                        (-extX / 2.0f),
                        (-extY / 2.0f),
                        (extX / 2.0f),
                        (extY / 2.0f),
                        getDashCubicColor(this),
                        matrix
                    )
                }, rotationAngles, matrixStack)
            }
        }
    }

    private class DashBase(
        val entity: LivingEntity,
        speedFactor: Float,
        val dashTexture: DashTexture,
        offsetTickPercentage: Float,
    ) {
        var motionX: Double = calculateMotionX()
        var motionY: Double = calculateMotionY()
        var motionZ: Double = calculateMotionZ()
        var posX: Double = entity.prevX - motionX * offsetTickPercentage + (
            -POSITION_OFFSET_BASE + POSITION_OFFSET_RANGE * Math.random() )
        var posY: Double = entity.prevY - motionY * offsetTickPercentage + (
            entity.height / 3.0 + entity.height / 4.0 * Math.random() * Y_OFFSET_MULTIPLIER)
        var posZ: Double = entity.prevZ - motionZ * offsetTickPercentage + (
            -POSITION_OFFSET_BASE + POSITION_OFFSET_RANGE * Math.random() )
        var prevPosX: Double = posX
        var prevPosY: Double = posY
        var prevPosZ: Double = posZ

        private fun calculateMotionX(): Double = -(entity.prevX - entity.x)
        private fun calculateMotionY(): Double = -(entity.prevY - entity.y)
        private fun calculateMotionZ(): Double = -(entity.prevZ - entity.z)

        init {
            motionX *= speedFactor
            motionY *= speedFactor
            motionZ *= speedFactor
        }

        fun getMotionYaw(): Float {
            var motionYaw = Math.toDegrees(atan2(motionZ, motionX) - Math.PI / 2).toFloat()
            if (motionYaw < 0) motionYaw += 360f
            return motionYaw
        }
    }

    private class DashTexture(animated: Boolean) {
        val textures: MutableList<TextureResource>
        val isAnimated: Boolean = animated && shouldUseAnimatedTexture()
        var spawnTime: Long = 0
        var animationInterval: Long = 0

        init {
            if (this.isAnimated) {
                spawnTime = System.currentTimeMillis()
                textures = getDashCubicAnimatedTextureGroupByIndex(getRandomAnimatedTextureGroupIndex()).toMutableList()
                animationInterval = getAnimationDurationTime().toLong()
            } else {
                textures = ArrayList()
                textures.add(getDashCubicTextureByIndex(getRandomDashCubicTextureIndex()))
            }
        }

        fun getResourceWithSizes(): TextureResource {
            if (isAnimated) {
                val fragCount = textures.size.toFloat()
                if (fragCount > 0f) {
                    val timeDiff = (System.currentTimeMillis() - spawnTime) % animationInterval
                    val index = MathHelper.clamp((timeDiff.toFloat() / animationInterval.toFloat()) * fragCount, 0f, fragCount - 1)
                    textures.getOrNull(index.toInt())?.let { return it }
                }
            }
            return textures[0]
        }
    }

    private class DashSpark {
        var posX: Double = 0.0
        var posY: Double = 0.0
        var posZ: Double = 0.0
        var prevPosX: Double = 0.0
        var prevPosY: Double = 0.0
        var prevPosZ: Double = 0.0
        var speed: Double = Math.random() / 50.0
        var radianYaw: Double = Math.random() * 360.0
        var radianPitch: Double = -90.0 + Math.random() * 180.0
        private var lifeTicks = animationTime
        val finished: Boolean get() = lifeTicks <= 0

        fun processMotion() {
            val radYaw = Math.toRadians(radianYaw)
            prevPosX = posX
            prevPosY = posY
            prevPosZ = posZ
            posX += sin(radYaw) * speed
            posY += cos(Math.toRadians(radianPitch - 90.0)) * speed
            posZ += cos(radYaw) * speed
            lifeTicks--
        }

        fun getRenderPosX(partialTicks: Float): Double =
            prevPosX + (posX - prevPosX) * partialTicks.toDouble()

        fun getRenderPosY(partialTicks: Float): Double =
            prevPosY + (posY - prevPosY) * partialTicks.toDouble()

        fun getRenderPosZ(partialTicks: Float): Double =
            prevPosZ + (posZ - prevPosZ) * partialTicks.toDouble()
    }

    override fun enable() {
        dashCubics.clear()
    }

    override fun disable() {
        dashCubics.clear()
    }
}
