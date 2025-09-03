package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.canSeeEntity
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper

object EspAvatarMode : EspMode("Avatar", requiresTrueSight = true) {

    private val size by float("ImageSize", 1f, 0.25f..1f)
    private val opacity by float("Opacity", 1f, 0.5f..1f)
    private val samples by int("Samples",2,2..5)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val entities = RenderedEntities.filter { it is PlayerEntity && it != mc.player }

        if (entities.isEmpty()) return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            entities.forEach { entity ->
                if (entity !is PlayerEntity) return@forEach
                if (mc.player!!.canSeeEntity(entity, samples)) return@forEach
                renderAvatarAtEntity(entity, event.partialTicks)
            }

        }
    }
    private fun WorldRenderEnvironment.renderAvatarAtEntity(entity: PlayerEntity, partialTicks: Float) {
        val pos = entity.interpolateCurrentPosition(partialTicks)
        val renderPos = pos.add(0.0, 1.0, 0.0)

        val dist = mc.player!!.distanceTo(entity).toDouble()
        val scaleFactor = (0.5f + dist.toFloat() * 0.05f).coerceIn(0.25f, 3.0f)

        val skin = mc.skinProvider.getSkinTextures(entity.gameProfile).texture()
        RenderSystem.setShaderTexture(0, skin)

        val s = (size * scaleFactor)
        withPositionRelativeToCamera(renderPos) {
            matrixStack.apply {
                multiply(mc.gameRenderer.camera.rotation)
                scale(s, s, s)
                translate(-0.5f, -0.5f, 0f)
            }

            val alpha = MathHelper.clamp((255 * opacity).toInt(), 0, 255)
            val color = Color4b.WHITE.withAlpha(alpha)

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { matrix ->
                vertex(matrix, 0f, 0f, 0f).texture(8f/64f, 16f/64f).color(color.toARGB())
                vertex(matrix, 1f, 0f, 0f).texture(16f/64f, 16f/64f).color(color.toARGB())
                vertex(matrix, 1f, 1f, 0f).texture(16f/64f, 8f/64f).color(color.toARGB())
                vertex(matrix, 0f, 1f, 0f).texture(8f/64f, 8f/64f).color(color.toARGB())
            }

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { matrix ->
                vertex(matrix, 0f, 0f, 0.001f).texture(40f/64f, 16f/64f).color(color.toARGB())
                vertex(matrix, 1f, 0f, 0.001f).texture(48f/64f, 16f/64f).color(color.toARGB())
                vertex(matrix, 1f, 1f, 0.001f).texture(48f/64f, 8f/64f).color(color.toARGB())
                vertex(matrix, 0f, 1f, 0.001f).texture(40f/64f, 8f/64f).color(color.toARGB())
            }
        }
    }


}
