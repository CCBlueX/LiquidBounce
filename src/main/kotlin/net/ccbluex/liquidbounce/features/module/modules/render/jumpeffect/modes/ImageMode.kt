package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect.colorMode
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.JumpEffectMode
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.collections.contains
import kotlin.math.pow

object ImageMode : JumpEffectMode("Image") {
    private val darkImprint by boolean("DarkImprint", true)
    private val onlySelf by boolean("OnlySelf", true)
    private val circleAlpha by float("Alpha", 255f, 0f..255f)
    private val circleScale by float("Scale", 1f, 0.5f..5f)
    private val rotateSpeed by float("RotateSpeed", 2f, 0.5f..5f)
    private val gradientSpeed by float("GradientSpeed", 0.25f, 0f..1f)
    private val imageType by enumChoice("ImageType", Image.ARC)
    private val circles = ArrayDeque<ObjectLongMutablePair<Vec3d>>()
    private val cache = mutableListOf<PlayerEntity>()

    init {
        Image.entries.forEach { it.texture }
    }

    override fun enable() {
        cache.clear()
        circles.clear()
    }

    val tickHandler = tickHandler {
        val world = mc.world ?: return@tickHandler
        val players = if (onlySelf) listOfNotNull(mc.player) else world.players

        players.filter { it.isOnGround && !cache.contains(it) }.forEach { cache.add(it) }

        cache.removeAll { player ->
            if (!player.isOnGround && players.contains(player)) {
                circles.add(ObjectLongMutablePair.of(player.pos, System.currentTimeMillis()))
                true
            } else {
                false
            }
        }
        circles.removeAll {
            System.currentTimeMillis() - it.rightLong() > 5000
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (circles.isEmpty()) return@handler

        val currentTime = System.currentTimeMillis()

        renderEnvironmentForWorld(event.matrixStack) {
            RenderSystem.depthMask(true)
            RenderSystem.disableDepthTest()
            RenderSystem.disableCull()
            mc.gameRenderer.lightmapTextureManager.disable()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                if (darkImprint) {
                    GlStateManager.DstFactor.ONE
                } else{
                    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
                },
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE
            )
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR)
            RenderSystem.setShaderTexture(0, imageType.texture)

            val builder = RenderBufferBuilder(
                drawMode = VertexFormat.DrawMode.QUADS,
                vertexFormat = VertexInputType.PosTexColor,
                tesselator = RenderBufferBuilder.TESSELATOR_A
            )

            circles.forEach { circle ->
                val pos = circle.left()
                val elapsed = (currentTime - circle.rightLong()).toFloat()
                val progress = elapsed / 6000f
                val sizeAnim = circleScale *  (1 - (1 - (elapsed * 2f) / 5000f).pow(4))

                val (color1Base, color2Base) = colorMode.activeChoice.getColors(null)
                val rotation = sizeAnim * rotateSpeed * 1000f
                val renderPos = Vec3d(pos.x, pos.y - 0.5, pos.z)
                withPositionRelativeToCamera(renderPos) {
                    matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f))
                    val scale = sizeAnim * 2f
                    val half = scale / 2f

                    matrixStack.translate(0.0, 0.0, half.toDouble())
                    matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation))
                    matrixStack.translate(0.0, 0.0, -half.toDouble())


                    val minX = -sizeAnim
                    val minY = -sizeAnim
                    val maxX = -sizeAnim + scale
                    val maxY = -sizeAnim + scale


                    val animationDuration = 4000f / gradientSpeed
                    val animationProgress = (currentTime % animationDuration.toLong()) / animationDuration

                    val alpha = ((1f - progress) * circleAlpha).toInt()
                    val colorTopLeft = color1Base.blend(
                        color2Base,
                        animationProgress).withAlpha(alpha)
                    val colorTopRight = color1Base.blend(
                        color2Base,
                        (animationProgress + 0.25f) % 1f).withAlpha(alpha)
                    val colorBottomRight = color1Base.blend(
                        color2Base,
                        (animationProgress + 0.5f) % 1f).withAlpha(alpha)
                    val colorBottomLeft = color1Base.blend(
                        color2Base,
                        (animationProgress + 0.75f) % 1f).withAlpha(alpha)

                    builder.drawGradientQuad(
                        this,
                        pos1 = Vec3d(minX.toDouble(), maxY.toDouble(), 0.0),
                        uv1 = UV2f(0f, 1f),
                        pos2 = Vec3d(maxX.toDouble(), maxY.toDouble(), 0.0),
                        uv2 = UV2f(1f, 1f),
                        pos3 = Vec3d(maxX.toDouble(), minY.toDouble(), 0.0),
                        uv3 = UV2f(1f, 0f),
                        pos4 = Vec3d(minX.toDouble(), minY.toDouble(), 0.0),
                        uv4 = UV2f(0f, 0f),
                        color1 = colorTopLeft,
                        color2 = colorTopRight,
                        color3 = colorBottomRight,
                        color4 = colorBottomLeft
                    )
                }
            }

            builder.draw()
        }
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.disableBlend()
        mc.gameRenderer.lightmapTextureManager.enable()
    }

    private enum class Image(
        override val choiceName: String,
        textureName: String
    ) : NamedChoice {
        ARC("Arc","arc"),
        BLADE("Blade","blade"),
        CIRCLE("Circle", "circle"),
        PORTAL("Portal", "portal"),
        KONCHAL("Konchal","konchal");
        val texture: Identifier =
            "image/jumpCircle/$textureName.png".registerAsDynamicImageFromClientResources()
    }
}
