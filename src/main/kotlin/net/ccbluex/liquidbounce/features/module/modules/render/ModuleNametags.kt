/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.float
import net.ccbluex.liquidbounce.event.LiquidBounceRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.renderer.Fonts
import net.ccbluex.liquidbounce.renderer.engine.*
import net.ccbluex.liquidbounce.renderer.engine.utils.rect
import net.ccbluex.liquidbounce.utils.Mat4
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.extensions.stripMinecraftColorCodes
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Quaternion
import kotlin.math.roundToInt

object ModuleNametags : Module("Nametags", Category.RENDER) {
    private val healthValue = boolean("Health", true)
    private val pingValue = boolean("Ping", true)
    private val distanceValue = boolean("Distance", false)
    private val armorValue = boolean("Armor", true)
    private val clearNamesValue = boolean("ClearNames", false)

    //    private val fontValue = font("Font", Fonts.font40) // TODO Please add this option after marco adds the fkin system
    private val borderValue = boolean("Border", true)
    private val scaleValue = float("Scale", 2F, 1F..4F)
    private val botValue = boolean("Bots", true)

    val realRenderHandler = handler<LiquidBounceRenderEvent> {
        val filteredEntities = mc.world!!.entities


        val rotMat = Mat4()

        rotMat.multiply(Quaternion(0.0f, -mc.player!!.yaw, 0.0f, true))
        rotMat.multiply(Quaternion(mc.player!!.pitch, 0.0f, 0.0f, true))

        for (entity in filteredEntities) {
//            if (!EntityUtils.isSelected(entity, false)) continue // TODO Fix targeting
//            if (AntiBot.isBot(entity.asEntityLivingBase()) && !botValue.get()) continue

            // Two triangles per rect
            val rectRenderTask = ColoredPrimitiveRenderTask(2, PrimitiveType.Triangles)
            val borderRenderTask = if (this.borderValue.value) {
                ColoredPrimitiveRenderTask(4, PrimitiveType.LineLoop)
            } else {
                null
            }

            val tag = if (clearNamesValue.value)
                entity.displayName.asTruncatedString(100)?.stripMinecraftColorCodes() ?: continue
            else
                entity.displayName.asTruncatedString(100) ?: continue

            val player = mc.player ?: continue

            // Scale
            var distance = player.distanceTo(entity) * 0.25f

            if (distance < 1F)
                distance = 1F

            val fontRenderer = Fonts.font40

            val scale = distance / (10f * fontRenderer.size) * scaleValue.value

            fontRenderer.begin()

            // Modify tag
//            val bot = AntiBot.isBot(entity) // TODO Fix AntiBot
            val bot = false
            val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
            val ping = if (entity is PlayerEntity) entity.ping else 0

            val distanceText = if (distanceValue.value) "§7${player.distanceTo(entity).roundToInt()}m " else ""
            val pingText =
                if (pingValue.value && entity is PlayerEntity) (if (ping > 200) "§c" else if (ping > 100) "§e" else "§a") + ping + "ms §7" else ""
            val healthText =
                if (healthValue.value && entity is LivingEntity) "§7§c " + entity.health.toInt() + " HP" else ""
            val botText = if (bot) " §c§lBot" else ""

            val text = "$distanceText$pingText$nameColor$tag$healthText$botText"

            // Precalculate width
            val width = fontRenderer.getStringWidth(text) / 2
            val height = fontRenderer.height + 2F

            val boundingBox = entity.boundingBox
            val boundingBoxCenter = boundingBox.center


            val offset = Vec3(
                boundingBoxCenter.x - entity.x + entity.lastRenderX + (entity.x - entity.lastRenderX) * it.tickDelta,
                boundingBox.minY - entity.y + entity.lastRenderY + entity.standingEyeHeight + 0.55 + height * scale + (entity.y - entity.lastRenderY) * it.tickDelta,
                boundingBoxCenter.z - entity.z + entity.lastRenderZ + (entity.z - entity.lastRenderZ) * it.tickDelta
            )

            val p1 = Vec3(-width - 2F, -2f, 0.0f)
            val p2 = Vec3(width + 4f, fontRenderer.height + 2F, 0.0f)

            rectRenderTask.rect(
                p1,
                p2,
                Color4b(0, 0, 0, 127)
            )

            borderRenderTask?.rect(
                p1,
                p2,
                Color4b(0, 0, 0, 127),
                true
            )

            Fonts.font40.draw(
                text, (1F - width), (if (fontRenderer == Fonts.minecraftFont) 1F else 0.0F),
                Color4b.WHITE, true, z = 0.0f
            )


            val matrix = Mat4()

            matrix.multiply(Mat4.translate(offset.x, offset.y, offset.z))
            matrix.multiply(rotMat)
            matrix.multiply(Mat4.scale(-scale, -scale, -scale))

            RenderEngine.enqueueForRendering(
                RenderEngine.PSEUDO_2D_LAYER,
                MVPRenderTask(
                    if (borderRenderTask == null) arrayOf(rectRenderTask) else arrayOf<RenderTask>(
                        rectRenderTask,
                        borderRenderTask
                    ) + Fonts.font40.commit(), matrix
                )
            )
        }

    }

}
