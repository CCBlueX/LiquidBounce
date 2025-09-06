package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.RenderBufferBuilder
import net.ccbluex.liquidbounce.render.VertexInputType
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.RotationAxis
import net.minecraft.client.render.VertexFormat
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.option.Perspective
import kotlin.math.sin
import kotlin.math.cos

object ModuleHalo : ClientModule("Halo", Category.RENDER) {

    private val character by enumChoice("Character", HaloImage.ALICE)
    private val size by float("Scale", 0.8f, 0.75f..1f)
    private val onlySelf by boolean("OnlySelf", true)
    private val firstPerson by boolean("InTheFirstPerson", false)

    private val offsetX by float("OffsetX", 90f, -180f..180f)
    private val offsetY by float("OffsetY", 5f, -180f..180f)

    private object DynamicOffset : ToggleableConfigurable(this, "DynamicFloat", true) {
        val range by float("FloatRange", 0.05f, 0.01f..0.1f)
        val speed by float("FloatSpeed", 0.1f, 0.1f..2f)
    }
    private object TrackCamera : ToggleableConfigurable(this, "TrackCamera", true) {
        val radius by float("TrackRange", 0.4f, 0.35f..0.5f)
        val positivePitchLimit by floatRange("PositivePitchLimit", 30f..30f, 0f..30f)
        val negativePitchLimit by floatRange("NegativePitchLimit", -30f..-30f, -30f..0f)
    }

    init {
        tree(DynamicOffset)
        tree(TrackCamera)
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val mc = MinecraftClient.getInstance()
        val world = mc.world ?: return@handler
        var players = if (!onlySelf) world.players else listOfNotNull(mc.player)

        val time = world.time.toDouble() + event.partialTicks.toDouble()
        if (!firstPerson && mc.player != null) {
            if (mc.options.perspective == Perspective.FIRST_PERSON && players.contains(mc.player)) {
                players -= mc.player
            }
        }
        val freq = DynamicOffset.speed.toDouble()
        val amp = DynamicOffset.range.toDouble()
        val floatY = sin(time * freq) * amp

        RenderSystem.disableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        )
        RenderSystem.setShader(VertexInputType.PosTexColor.shaderProgram)
        RenderSystem.setShaderTexture(0, character.texture)
        renderEnvironmentForWorld(event.matrixStack) {
            players.forEach { player ->
                if (player.isSpectator || !player.isAlive) return@forEach
                val interp = player.interpolateCurrentPosition(event.partialTicks)
                val baseY = player.standingEyeHeight

                // Calculate circular offset based on pitch
                val pitch = if (TrackCamera.enabled) {
                    player.pitch.coerceIn(
                        TrackCamera.negativePitchLimit.start,
                        TrackCamera.positivePitchLimit.endInclusive
                    )
                } else {
                    0f
                }
                val pitchRad = Math.toRadians(pitch.toDouble())
                val yawRad = Math.toRadians(player.yaw.toDouble())


                val pitchOffset = Vec3d(
                    -sin(yawRad) * sin(pitchRad),
                    cos(pitchRad),
                    cos(yawRad) * sin(pitchRad)
                ).multiply(TrackCamera.radius.toDouble())

                val haloPos = interp.add(
                    pitchOffset.x, baseY + floatY + pitchOffset.y, pitchOffset.z)

                withPositionRelativeToCamera(haloPos) {
                    val ms = event.matrixStack
                    ms.push()

                    ms.scale(size, size, size)
                    if (TrackCamera.enabled) {
                        val yaw = player.yaw
                        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw)) // Only rotate with yaw
                    }
                    if (enabled) {
                        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(offsetX))
                        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(offsetY))
                    }

                    val builder = RenderBufferBuilder(
                        VertexFormat.DrawMode.QUADS,
                        VertexInputType.PosTexColor,
                        RenderBufferBuilder.TESSELATOR_A
                    )
                    val half = 0.5
                    builder.drawQuad(
                        this,
                        pos1 = Vec3d(-half, -half, 0.0),
                        uv1 = UV2f(0f, 1f),
                        pos2 = Vec3d(half, half, 0.0),
                        uv2 = UV2f(1f, 0f),
                        color = Color4b(255, 255, 255, 255)
                    )
                    builder.draw()
                    ms.pop()
                }
            }
        }

        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.disableBlend()
    }
}
@Suppress("UNUSED")
private enum class HaloImage(
    override val choiceName: String,
    private val textureName: String
) : NamedChoice {
    ALICE("Alice", "alice"),
    SHIROKO("Shiroko", "shiroko"),
    REISA("Reisa", "reisa"),
    HOSHINO("Hoshino", "hoshino"),
    AZUSA("Azusa", "azusa"),
    IORI("Iori", "iori"),
    IZUNA("Izuna", "izuna"),
    KAYOKO("Kayoko", "kayoko");

    val texture: Identifier by lazy {
        "image/halo/$textureName.png".registerAsDynamicImageFromClientResources()
    }
}
