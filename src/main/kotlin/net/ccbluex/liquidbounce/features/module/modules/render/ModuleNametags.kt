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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.engine.memory.IndexBuffer
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.VertexFormatComponentDataType
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.math.Mat4
import net.ccbluex.liquidbounce.utils.render.rect
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import kotlin.math.roundToInt

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */

object ModuleNametags : Module("Nametags", Category.RENDER) {

    private val healthValue by boolean("Health", true)
    private val pingValue by boolean("Ping", true)
    private val distanceValue by boolean("Distance", false)
    private val armorValue by boolean("Armor", true)
    private val clearNamesValue by boolean("ClearNames", false)

    private val borderValue by boolean("Border", true)
    private val scaleValue by float("Scale", 2F, 1F..4F)

    val renderHandler = handler<EngineRenderEvent> { event ->
        val filteredEntities = world.entities.filter(ModuleNametags::shouldRenderNametag)

        if (filteredEntities.isEmpty()) {
            return@handler
        }

        val fontRenderer = Fonts.bodyFont

        fontRenderer.begin()

        // Two triangles per rect
        val vertexFormat = PositionColorVertexFormat()

        vertexFormat.initBuffer(filteredEntities.size * 4)

        val indexBuffer = IndexBuffer(filteredEntities.size * 4 * 3, VertexFormatComponentDataType.GlUnsignedShort)

        val aspectRatio = mc.window.width.toFloat() / mc.window.height.toFloat()

        val (borderVertexFormat, borderIndexBuffer) = if (this.borderValue) {
            val vf = PositionColorVertexFormat()

            vf.initBuffer(filteredEntities.size * 4)

            Pair(vf, IndexBuffer(filteredEntities.size * 4 * 2, VertexFormatComponentDataType.GlUnsignedShort))
        } else {
            Pair(null, null)
        }

        val total = filteredEntities.size.toFloat()
        val delta = 0.5f / filteredEntities.size.toFloat()
        var currIdx = 0

        for (entity in filteredEntities) {
            val tag = if (clearNamesValue) {
                entity.displayName.asTruncatedString(100)?.stripMinecraftColorCodes() ?: continue
            } else {
                entity.displayName.asTruncatedString(100) ?: continue
            }

            // Scale
            var distance = player.distanceTo(entity) * 0.25f

            if (distance < 1.0f) {
                distance = 1.0f
            } else if (distance > 3.0f) {
                distance = 3.0f
            }

            val scale = 1.0f / (25f * fontRenderer.size * distance) * scaleValue

            // Modify tag
            val bot = false
            val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
            val ping = if (entity is PlayerEntity) entity.ping else 0

            val distanceText = if (distanceValue) "§7${player.distanceTo(entity).roundToInt()}m " else ""
            val pingText =
                if (pingValue && entity is PlayerEntity) (if (ping > 200) "§c" else if (ping > 100) "§e" else "§a") + ping + "ms §7" else ""
            val healthText = if (healthValue && entity is LivingEntity) "§7§c ${entity.health.toInt()} HP" else ""
            val botText = if (bot) " §c§lBot" else ""

            val text = "$distanceText$pingText$nameColor$tag$healthText$botText"

            // Precalculate width
            val width = fontRenderer.getStringWidth(text) / 2
            val height = fontRenderer.height + 2F

            val boundingBox = entity.box
            val boundingBoxCenter = boundingBox.center

            val offset = Vec3(
                boundingBoxCenter.x - entity.x + entity.lastRenderX + (entity.x - entity.lastRenderX) * event.tickDelta,
                boundingBox.minY - entity.y + entity.lastRenderY + entity.standingEyeHeight + 0.55 + height * scale + (entity.y - entity.lastRenderY) * event.tickDelta,
                boundingBoxCenter.z - entity.z + entity.lastRenderZ + (entity.z - entity.lastRenderZ) * event.tickDelta
            )

            val p1 = Vec3(-width - 4F, -fontRenderer.height / 2.0f - 2f, 0.0f)
            val p2 = Vec3(width + 6f, fontRenderer.height / 2.0f + 2F, 0.0f)

            val vec = RenderEngine.cameraMvp * Vec4(offset, 1.0f)

            if (vec.w < 0.0f) {
                continue
            }

            val factor = 1.0f / vec.w

            val xWithoutAspectRatio = vec.x * factor

            val currZ = -(currIdx.toFloat() / total)

            val screenSpaceVec = Vec3(xWithoutAspectRatio * aspectRatio, -vec.y * factor, currZ)

            vertexFormat.rect(
                indexBuffer, screenSpaceVec + p1 * scale, screenSpaceVec + p2 * scale, Color4b(0, 0, 0, 127)
            )
            borderVertexFormat?.rect(
                borderIndexBuffer!!,
                screenSpaceVec + p1 * scale,
                screenSpaceVec + p2 * scale,
                Color4b(0, 0, 0, 255),
                true
            )

            Fonts.bodyFont.draw(
                text,
                screenSpaceVec.x + (1F - width) * scale,
                screenSpaceVec.y - fontRenderer.height / 2.0f * scale,
                Color4b.WHITE,
                true,
                scale = scale,
                z = currZ - delta
            )

            if (armorValue && entity is PlayerEntity) {
                val armorScale = 1.0f / distance * scaleValue

                val pixelX = (xWithoutAspectRatio + 1.0f) / 2.0f * mc.window.scaledWidth
                val pixelY = (screenSpaceVec.y + 1.0f) / 2.0f * mc.window.scaledHeight

                val slotTypes = arrayOf(
                    EquipmentSlot.MAINHAND,
                    EquipmentSlot.OFFHAND,
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET
                )

                val renderTasks = slotTypes.withIndex().mapNotNull { (index, slot) ->
                    val equipmentInSlot = entity.getEquippedStack(slot) ?: return@mapNotNull null

                    ItemModelRenderTask(equipmentInSlot, -40 + index * 20, -23)
                }

                val mvpMatrix = Mat4.translate(pixelX, pixelY, 0.0f)

                mvpMatrix.multiply(Mat4.scale(armorScale, armorScale, armorScale))

                RenderEngine.enqueueForRendering(
                    RenderEngine.MINECRAFT_INTERNAL_RENDER_TASK, MVPRenderTask(renderTasks.toTypedArray(), mvpMatrix)
                )
            }

            currIdx++
        }

        RenderEngine.enqueueForRendering(
            RenderEngine.SCREEN_SPACE_LAYER, VertexFormatRenderTask(
                vertexFormat,
                PrimitiveType.Triangles,
                ColoredPrimitiveShader,
                indexBuffer = indexBuffer,
                state = GlRenderState(lineWidth = 2.0f, lineSmooth = true, depthTest = true)
            )
        )
        borderVertexFormat?.let {
            RenderEngine.enqueueForRendering(
                RenderEngine.SCREEN_SPACE_LAYER, VertexFormatRenderTask(
                    it,
                    PrimitiveType.Lines,
                    ColoredPrimitiveShader,
                    indexBuffer = borderIndexBuffer!!,
                    state = GlRenderState(lineWidth = 1.0f, lineSmooth = true, depthTest = true)
                )
            )
        }
        RenderEngine.enqueueForRendering(RenderEngine.SCREEN_SPACE_LAYER, fontRenderer.commit())
    }

    /**
     * Should [ModuleNametags] render nametags above this [entity]?
     */
    @JvmStatic
    fun shouldRenderNametag(entity: Entity) = entity.shouldBeShown()

}
