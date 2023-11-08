/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiBot
import net.ccbluex.liquidbounce.render.RenderBufferBuilder
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.VertexInputType
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawQuadOutlines
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.client.render.VertexFormat
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.RotationAxis
import kotlin.math.roundToInt

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */

object ModuleNametags : Module("Nametags", Category.RENDER) {
    val fontRenderer = FontRenderer.createFontRenderer("Montserrat", 32)

    private val health by boolean("Health", true)
    private val ping by boolean("Ping", true)
    private val distance by boolean("Distance", false)
    private val armor by boolean("Armor", true)
    private val clearNames by boolean("ClearNames", false)

    val border by boolean("Border", true)
    val scale by float("Scale", 2F, 1F..4F)

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            val nametagRenderer = NametagRenderer()

            try {
                drawNametags(nametagRenderer, event.partialTicks)
            } finally {
                nametagRenderer.commit(this)
            }
        }
    }

    private fun RenderEnvironment.drawNametags(nametagRenderer: NametagRenderer, tickDelta: Float) {
        for (entity in ModuleESP.findRenderedEntities()) {
            val nametagPos = entity
                .interpolateCurrentPosition(tickDelta)
                .add(Vec3(0.0F, entity.getEyeHeight(entity.pose) + 0.55F, 0.0F))

            val nametagText = getNametagText(entity)

            nametagRenderer.drawNametag(this, nametagText, nametagPos)
        }
    }

    private fun getNametagText(entity: Entity): String {
        val bot = ModuleAntiBot.isBot(entity)
        val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
        val playerPing = if (entity is PlayerEntity) entity.ping else 0
        val playerDistance = player.distanceTo(entity)

        val distanceText = if (distance) "§7${playerDistance.roundToInt()}m " else ""
        val pingText =
            if (ping && entity is PlayerEntity) " §7[" + (if (playerPing > 200) "§c" else if (playerPing > 100) "§e" else "§a") + playerPing + "ms§7]" else ""
        val healthText = if (health && entity is LivingEntity) getHealth(entity) else ""
        val botText = if (bot) " §c§lBot" else ""

        return "$distanceText$pingText$nameColor$tag$healthText$botText"
    }

    private fun getHealth(entity: LivingEntity): Float {
        return entity.health
    }


    /**
     * Should [ModuleNametags] render nametags above this [entity]?
     */
    @JvmStatic
    fun shouldRenderNametag(entity: Entity) = entity.shouldBeShown()

}

class NametagRenderer {
    private val quadBuffers = RenderBufferBuilder(
        VertexFormat.DrawMode.QUADS,
        VertexInputType.Pos,
        RenderBufferBuilder.TESSELATOR_A
    )
    private val lineBuffers = RenderBufferBuilder(
        VertexFormat.DrawMode.DEBUG_LINES,
        VertexInputType.Pos,
        RenderBufferBuilder.TESSELATOR_B
    )

    private val fontBuffers = FontRendererBuffers()

    fun drawNametag(
        env: RenderEnvironment,
        text: String,
        pos: Vec3,
    ) {
        val camera = mc.gameRenderer.camera
        val scale = ((camera.pos.distanceTo(pos.toVec3d()) / 4F).coerceAtLeast(1.0).toFloat() / 150.0F) * 0.7F * ModuleNametags.scale

        val c = 32.0F

        env.matrixStack.push()
        env.matrixStack.translate(pos.x, pos.y, pos.z)
        env.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(camera.yaw)))
        env.matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        env.matrixStack.scale(-scale, -scale, -scale)

        val x = ModuleNametags.fontRenderer.draw(
            text,
            0.0F,
            0.0F,
            shadow = true
        )
        env.matrixStack.translate(-x * 0.5F, -ModuleNametags.fontRenderer.height * 0.5F, 0.0F)

        ModuleNametags.fontRenderer.commit(env, fontBuffers)

        val q1 = Vec3(-0.1F * c, ModuleNametags.fontRenderer.height * -0.05F, 0.0F)
        val q2 = Vec3(x + 0.1F * c, ModuleNametags.fontRenderer.height * 1.01F, 0.0F)

        quadBuffers.drawQuad(env, q1, q2)

        if (ModuleNametags.border) {
            lineBuffers.drawQuadOutlines(env, q1, q2)
        }

        env.matrixStack.pop()
    }

    fun commit(env: RenderEnvironment) {
        env.withColor(Color4b(0, 0, 0, 127)) {
            quadBuffers.draw()
        }
        env.withColor(Color4b(0, 0, 0, 255)) {
            lineBuffers.draw()
        }
        env.withColor(Color4b.WHITE) {
            fontBuffers.draw(ModuleNametags.fontRenderer)
        }
    }
}
