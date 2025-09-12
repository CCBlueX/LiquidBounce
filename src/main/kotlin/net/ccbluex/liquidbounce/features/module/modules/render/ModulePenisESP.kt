@file:Suppress("LongParameterList", "LongMethod")

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ModulePenisESP : ClientModule("PenisESP", Category.RENDER) {

    val penisSize by float("PenisSize", 0.5f, 0.1f..2f)
    private val segments by int("Segments", 32, 8..64)
    private val penisLength by float("PenisLength", 0.8f, 0.1f..1.8f)
    private val glansRadius by float("GlansRadius", 0.15f, 0.05f..0.5f)
    private val ejaculationTime by float("EjaculationTime", 2f, 0.5f..5f)
    private val ejaculationParticles by int("EjaculationParticles", 2, 1..5)

    private object Offset : ToggleableConfigurable(this, "Offset", true) {
        val offsetX by float("OffsetX", 0f, -1f..1f)
        val offsetY by float("OffsetY", -0.2f, -1f..1f)
        val offsetZ by float("OffsetZ", 0f, -1f..1f)
    }

    private object Rotation : ToggleableConfigurable(this, "Rotation", false) {
        val offsetX by float("OffsetX", 50f, -180f..180f)
        val offsetY by float("OffsetY", 0f, -180f..180f)
        val offsetZ by float("OffsetZ", 0f, -180f..180f)
    }

    private val penis by color("Penis", Color4b.LIQUID_BOUNCE)
    private val glans by color("Glans", Color4b.LIQUID_BOUNCE)
    init {
        tree(Offset)
        tree(Rotation)

        ClientTickEvents.END_CLIENT_TICK.register {
            val player = mc.player ?: return@register
            val entities = RenderedEntities.filter { it.isPlayer }

            for (entity in entities) {
                if (entity == player || !entity.isAlive) continue

                val dx = entity.x - player.x
                val dz = entity.z - player.z
                val yawRad = Math.toRadians(entity.bodyYaw.toDouble())
                val lookX = -sin(yawRad)
                val lookZ = cos(yawRad)

                val isBehind = dx * lookX + dz * lookZ > 0.5
                val isSneaking = player.isSneaking && isBehind && player.squaredDistanceTo(entity) < 2.5

                val sneakTime = if (isSneaking) (sneakStateMap[entity] ?: 0) + 1 else 0
                sneakStateMap[entity] = sneakTime

                val cooldown = (ejaculationCooldownMap[entity] ?: 0) - 1
                ejaculationCooldownMap[entity] = cooldown.coerceAtLeast(0)


                if (sneakTime >= 60 && cooldown <= 0) {
                    ejaculationCooldownMap[entity] = 40
                    spawnEjaculationParticles(
                        entity,
                    )
                }
            }
        }
    }

    private val attackedTrigger by boolean("AttackedTrigger", true)
    private val manualTrigger by boolean("Manual", false)

    //Because there’s no decent texture,this is just a placeholder texture now.
    private val penisTexture: Identifier =
        "image/penisEsp/penis.png".registerAsDynamicImageFromClientResources()
    private val glansTexture: Identifier =
        "image/penisEsp/glans.png".registerAsDynamicImageFromClientResources()

    private val sneakingBehindTimes = mutableMapOf<LivingEntity, Long>()
    private val sneakStateMap = mutableMapOf<LivingEntity, Int>()
    private val ejaculationCooldownMap = mutableMapOf<LivingEntity, Int>()

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (!event.entity.isLiving || !attackedTrigger) {
            return@handler
        }

        val target = event.entity as LivingEntity

        if (target.hurtTime < 10) {
            spawnEjaculationParticles(target)
        }
    }
    private fun spawnEjaculationParticles(entity: LivingEntity, partialTicks: Float = 1.0f) {
        val world = entity.world ?: return

        val yaw = entity.prevYaw + (entity.yaw - entity.prevYaw) * partialTicks
        val pitch = entity.prevPitch + (entity.pitch - entity.prevPitch) * partialTicks


        val penisMatrix = Matrix4f()
            .rotateY(Math.toRadians(-yaw.toDouble()).toFloat())
            .rotateX(Math.toRadians(pitch.toDouble()).toFloat())

        if (Rotation.enabled) {
            penisMatrix
                .rotateX(Math.toRadians(Rotation.offsetX.toDouble()).toFloat())
                .rotateY(Math.toRadians(Rotation.offsetY.toDouble()).toFloat())
                .rotateZ(Math.toRadians(Rotation.offsetZ.toDouble()).toFloat())
        }


        val direction = Vector4f(0f, 1f, 0f, 0f)
        direction.mul(penisMatrix)

        var finalDirX = direction.x.toDouble()
        var finalDirY = direction.y.toDouble()
        var finalDirZ = direction.z.toDouble()

        val length = sqrt(finalDirX * finalDirX + finalDirY * finalDirY + finalDirZ * finalDirZ)

        if (length > 0) {
            finalDirX /= length
            finalDirY /= length
            finalDirZ /= length
        }

        val pos = entity.interpolateCurrentPosition(partialTicks)
        val dims = entity.getDimensions(entity.pose)
        val centerY = dims.height / 2.0
        val size = penisSize

        val baseX = pos.x + (if (Offset.enabled) Offset.offsetX.toDouble() else 0.0)
        val baseY = pos.y + centerY + (if (Offset.enabled) Offset.offsetY.toDouble() else 0.0)
        val baseZ = pos.z + (if (Offset.enabled) Offset.offsetZ.toDouble() else 0.0)

        val tipOffsetX = finalDirX * (penisLength * size)
        val tipOffsetY = finalDirY * (penisLength * size)
        val tipOffsetZ = finalDirZ * (penisLength * size)

        val particleX = baseX + tipOffsetX
        val particleY = baseY + tipOffsetY
        val particleZ = baseZ + tipOffsetZ

        repeat(ejaculationParticles) {
            val offset = it * 0.05
            world.addParticle(
                ParticleTypes.END_ROD,
                particleX + finalDirX * offset,
                particleY + finalDirY * offset,
                particleZ + finalDirZ * offset,
                finalDirX * 0.2,
                finalDirY * 0.2,
                finalDirZ * 0.2
            )
        }
    }
    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val entities = RenderedEntities.filter { it.isPlayer }
        if (manualTrigger) {
            entities.forEach { spawnEjaculationParticles(it) }
        }
        renderEnvironmentForWorld(matrixStack) {
            entities.forEach { entity ->
                renderPenisAtEntity(entity, event.partialTicks)
                if (sneakingBehindTimes[entity]?.
                    let { System.currentTimeMillis() - it > ejaculationTime * 1000 } == true) {
                    spawnEjaculationParticles(entity)
                }
            }
        }
    }

    private fun WorldRenderEnvironment.renderPenisAtEntity(entity: LivingEntity, partialTicks: Float) {
        val pos = entity.interpolateCurrentPosition(partialTicks)
        val dims = entity.getDimensions(entity.pose)
        val centerY = dims.height / 2.0f
        val size = penisSize

        withPositionRelativeToCamera(
            pos.add(
                if (Offset.enabled) Offset.offsetX.toDouble() else 0.0,
                centerY + if (Offset.enabled) Offset.offsetY.toDouble() else 0.0,
                if (Offset.enabled) Offset.offsetZ.toDouble() else 0.0
            )
        ) {
            matrixStack.push()
            matrixStack.scale(size, size, size)

            val yaw = entity.prevYaw + (entity.yaw - entity.prevYaw) * partialTicks
            val pitch = entity.prevPitch + (entity.pitch - entity.prevPitch) * partialTicks

            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw))
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch))

            if (Rotation.enabled) {
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Rotation.offsetX))
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(Rotation.offsetY))
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Rotation.offsetZ))
            }
            RenderSystem.disableCull()
            RenderSystem.setShaderTexture(0, penisTexture)
            drawCylinder(matrixStack, penisLength)


            matrixStack.translate(0.0, penisLength.toDouble(), 0.0)
            RenderSystem.setShaderTexture(0, glansTexture)
            drawGlans(matrixStack)

            matrixStack.pop()

            RenderSystem.enableCull()
        }
    }

    private fun WorldRenderEnvironment.drawCylinder(matrixStack: MatrixStack, length: Float) {
        val radius = glansRadius
        val matrix = matrixStack.peek().positionMatrix
        val segmentCount = segments

        drawCustomMesh(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_TEXTURE_COLOR,
            ShaderProgramKeys.POSITION_TEX_COLOR
        ) { vertex ->
            for (i in 0 until segmentCount) {
                val angle0 = (2 * Math.PI * i / segmentCount).toFloat()
                val angle1 = (2 * Math.PI * ((i + 1) % segmentCount) / segmentCount).toFloat()

                val x0 = cos(angle0) * radius
                val z0 = sin(angle0) * radius
                val x1 = cos(angle1) * radius
                val z1 = sin(angle1) * radius

                val u0 = i.toFloat() / segmentCount
                val u1 = (i + 1).toFloat() / segmentCount

                vertex(matrix, x0, 0f, z0)
                    .texture(u0, 1f).color(penis.toARGB())
                vertex(matrix, x1, 0f, z1)
                    .texture(u1, 1f).color(penis.toARGB())
                vertex(matrix, x1, length, z1)
                    .texture(u1, 0f).color(penis.toARGB())
                vertex(matrix, x0, length, z0)
                    .texture(u0, 0f).color(penis.toARGB())
            }
        }
    }

    private fun WorldRenderEnvironment.drawGlans(matrixStack: MatrixStack) {
        val matrix = matrixStack.peek().positionMatrix
        val radius = glansRadius
        val segmentCount = segments

        drawCustomMesh(
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            VertexFormats.POSITION_TEXTURE_COLOR,
            ShaderProgramKeys.POSITION_TEX_COLOR
        ) { vertex ->
            for (i in 0 until segmentCount) {
                val theta0 = Math.PI * i / segmentCount
                val theta1 = Math.PI * (i + 1) / segmentCount
                val y0 = cos(theta0).toFloat() * radius
                val y1 = cos(theta1).toFloat() * radius
                val r0 = sin(theta0).toFloat() * radius
                val r1 = sin(theta1).toFloat() * radius

                for (j in 0..segmentCount) {
                    val phi = 2 * Math.PI * j / segmentCount
                    val cosPhi = cos(phi).toFloat()
                    val sinPhi = sin(phi).toFloat()

                    val x0 = r0 * cosPhi
                    val z0 = r0 * sinPhi
                    val x1 = r1 * cosPhi
                    val z1 = r1 * sinPhi

                    val u = j.toFloat() / segmentCount
                    val v0 = i.toFloat() / segmentCount
                    val v1 = (i + 1).toFloat() / segmentCount

                    vertex(matrix, x0, y0, z0)
                        .texture(u, v0)
                        .color(glans.toARGB())
                    vertex(matrix, x1, y1, z1)
                        .texture(u, v1)
                        .color(glans.toARGB())
                }
            }
        }
    }
}
