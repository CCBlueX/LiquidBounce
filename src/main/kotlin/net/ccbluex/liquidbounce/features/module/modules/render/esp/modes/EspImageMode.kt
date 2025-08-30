@file:Suppress("unused")
package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspImageMode.RotationOption.maxRotationAngle
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspImageMode.RotationOption.rotationDuration
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspImageMode.RotationOption.rotationSpeed
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf
import java.io.FileInputStream
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object EspImageMode : EspMode("Image", requiresTrueSight = true) {

    internal val imageMode = choices(
        "Mode",
        ImageMode.FaceImageMode,
        arrayOf(ImageMode.FaceImageMode, ImageMode.FileImageMode)
    )

    private val imageSize by float("ImageSize", 0.5f, 0.5f..1f)
    private val opacity by float("Opacity", 0.8f, 0f..1f)
    private val attackColor by color("Attack", Color4b.PINK)
    private val onlyPlayer by boolean("OnlyPlayer", true)

    private object RotationOption : ToggleableConfigurable(this, "AttackRotation", true) {
        val rotationSpeed by float("RotationSpeed", 180f, 0f..360f)
        val rotationDuration by int("RotationDuration", 1000, 0..3000, "ms")
        val maxRotationAngle by float("MaxRotation", 360f, 0f..720f)
    }

    private data class HitData(var startTime: Long, var startAngle: Float, var isRotatingBack: Boolean = false)

    private val hitDataMap = mutableMapOf<UUID, HitData>()
    private val chronometer = Chronometer()

    init {
        tree(RotationOption)
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (!RotationOption.enabled || !event.entity.isLiving || !chronometer.hasElapsed(230)) {
            return@handler
        }
        chronometer.reset()

        val target = event.entity as LivingEntity
        val uuid = target.uuid
        val now = System.currentTimeMillis()

        val prev = hitDataMap[uuid]
        if (prev != null) {
            val elapsedPrev = now - prev.startTime
            if (elapsedPrev < rotationDuration) {
                val progressPrev = MathHelper.clamp(elapsedPrev.toFloat() / rotationDuration, 0f, 1f)
                val easedPrev = MathHelper.sin(progressPrev * MathHelper.PI / 2f)
                val deltaPrev = easedPrev * maxRotationAngle * (rotationSpeed / 360f)
                prev.startAngle += deltaPrev
            }
            prev.isRotatingBack = false
        }

        hitDataMap[uuid] = HitData(now, prev?.startAngle ?: 0f)
    }
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val entities = if(onlyPlayer) RenderedEntities.filter { it.isPlayer } else RenderedEntities
        if (entities.isEmpty()) return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            val activeTex = imageMode.activeChoice.getTexture()
            RenderSystem.setShaderTexture(0, activeTex)

            matrixStack.push()
            entities.forEach { entity ->
                renderImageAtEntity(entity, event.partialTicks)
            }
            matrixStack.pop()
        }
    }

    private fun WorldRenderEnvironment.renderImageAtEntity(entity: LivingEntity, partialTicks: Float) {
        val pos = entity.interpolateCurrentPosition(partialTicks)
        val eyeHeight = entity.getEyeHeight(entity.pose)
        var renderPos = pos.add(0.0, eyeHeight.toDouble() + 0.03, 0.0)
        val size = imageSize
        if (entity.isSwimming) {
            val pitchRad = Math.toRadians(entity.pitch.toDouble())
            val yawRad = Math.toRadians(entity.yaw.toDouble())
            val forward = Vec3d(
                -sin(yawRad) * cos(pitchRad),
                -sin(pitchRad),
                cos(yawRad) * cos(pitchRad)
            )
            renderPos = renderPos.add(forward.multiply(0.64))
        }
        withPositionRelativeToCamera(renderPos) {
            matrixStack.apply {
                multiply(mc.gameRenderer.camera.rotation)
                if (RotationOption.enabled) {
                    calculateRotationAngle(entity)?.let { angle -> rotateZ(angle) }
                }
                scale(size, size, size)
                translate(-0.5f, -0.5f, 0.0f)
            }

            val alpha = MathHelper.clamp((255 * opacity).toInt(), 0, 255)
            val color = attackColor.with(a = alpha).takeIf { entity.hurtTime > 0 }
                ?: getColor(entity).with(a = alpha)

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { matrix ->
                vertex(matrix, 0.0f, 0.0f, 0.0f).texture(0.0f, 1.0f).color(color.toARGB())
                vertex(matrix, 1.0f, 0.0f, 0.0f).texture(1.0f, 1.0f).color(color.toARGB())
                vertex(matrix, 1.0f, 1.0f, 0.0f).texture(1.0f, 0.0f).color(color.toARGB())
                vertex(matrix, 0.0f, 1.0f, 0.0f).texture(0.0f, 0.0f).color(color.toARGB())
            }
        }
    }

    private fun calculateRotationAngle(entity: LivingEntity): Float? {
        if (!RotationOption.enabled) {
            hitDataMap.remove(entity.uuid)
            return null
        }

        val data = hitDataMap[entity.uuid] ?: return null
        val now = System.currentTimeMillis()
        val elapsed = now - data.startTime

        if (data.isRotatingBack) {
            if (elapsed > rotationDuration) {
                hitDataMap.remove(entity.uuid)
                return 0f
            }

            val progress = MathHelper.clamp(elapsed.toFloat() / rotationDuration, 0f, 1f)
            val eased = MathHelper.sin(progress * MathHelper.PI / 2f)
            return data.startAngle * (1 - eased)
        } else {
            if (elapsed > rotationDuration) {
                data.startTime = now
                data.startAngle = data.startAngle + maxRotationAngle * (rotationSpeed / 360f)
                data.isRotatingBack = true
                return data.startAngle
            }

            val progress = MathHelper.clamp(elapsed.toFloat() / rotationDuration, 0f, 1f)
            val eased = MathHelper.sin(progress * MathHelper.PI / 2f)
            val delta = eased * maxRotationAngle * (rotationSpeed / 360f)
            return data.startAngle + delta
        }
    }

    private fun MatrixStack.rotateZ(degrees: Float) {
        multiply(Quaternionf().rotateZ(Math.toRadians(degrees.toDouble()).toFloat()))
    }

    internal sealed class ImageMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*> get() = imageMode

        abstract fun getTexture(): Identifier

        object FaceImageMode : ImageMode("Face") {
            private val image by enumChoice("Image", FaceImage.ALAN34)

            override fun getTexture(): Identifier = image.texture

            private enum class FaceImage(
                override val choiceName: String,
                textureName: String
            ) : NamedChoice {
                XINXIN("Xinxin", "xinxin"),
                BAIZHIJUN("SuChen", "suchen"),
                ALAN34("Alan34", "alan");

                val texture: Identifier =
                    "image/esp2D/$textureName.png".registerAsDynamicImageFromClientResources()
            }
        }
        object FileImageMode : ImageMode("File") {
            private val customImage by file("CustomImage")
            private var cachedTexture: Identifier? = null
            private var cachedNativeImage: NativeImage? = null
            private var cachedPath: String? = null

            override fun getTexture(): Identifier {
                val file = customImage.absoluteFile.takeIf { it.exists() && it.isFile && it.canRead() }
                if (file == null) {
                    cachedTexture?.let { return it }
                    cachedNativeImage?.close()
                    val defaultId = Identifier.of("liquidbounce", "esp-default")
                    cachedNativeImage = NativeImage.read(
                        LiquidBounce.javaClass.getResourceAsStream("/resources/liquidbounce/image/esp2D/alan.png")!!
                    )
                    mc.textureManager.registerTexture(defaultId, NativeImageBackedTexture(cachedNativeImage))
                    cachedTexture = defaultId
                    cachedPath = null
                    return defaultId
                }

                val path = file.absolutePath
                if (cachedPath != path) {
                    cachedNativeImage?.close()
                    FileInputStream(file).use { fis ->
                        cachedNativeImage = NativeImage.read(fis)
                    }
                    val id = Identifier.of("liquidbounce", "esp-file-${System.currentTimeMillis().toString(36)}")
                    mc.textureManager.registerTexture(id, NativeImageBackedTexture(cachedNativeImage))
                    cachedTexture = id
                    cachedPath = path
                }
                return cachedTexture!!
            }
        }

    }
}
