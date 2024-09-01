/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.pow

object Tracers : Module("Tracers", Category.RENDER, hideModule = false) {

    private val colorMode by ListValue("Color", arrayOf("Custom", "DistanceColor", "Rainbow"), "Custom")
    private val colorRed by IntegerValue("R", 0, 0..255) { colorMode == "Custom" }
    private val colorGreen by IntegerValue("G", 160, 0..255) { colorMode == "Custom" }
    private val colorBlue by IntegerValue("B", 255, 0..255) { colorMode == "Custom" }

    private val thickness by FloatValue("Thickness", 2F, 1F..5F)

    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..200) {
        override fun onInit(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }

    private var maxRenderDistanceSq = 0.0

    private val bot by BoolValue("Bots", true)
    private val teams by BoolValue("Teams", false)

    private val onLook by BoolValue("OnLook", false)
    private val maxAngleDifference by FloatValue("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val thruBlocks by BoolValue("ThruBlocks", true)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.player ?: return

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(thickness)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        glBegin(GL_LINES)

        for (entity in mc.world.entities) {
            val distanceSquared = player.squaredDistanceToToEntity(entity)

            if (distanceSquared <= maxRenderDistanceSq) {
                if (onLook && !isLookingOnEntities(entity, maxAngleDifference.toDouble())) continue
                if (entity !is LivingEntity || !bot && isBot(entity)) continue
                if (!thruBlocks && !RotationUtils.isVisible(Vec3d(entity.posX, entity.posY, entity.posZ))) continue

                if (entity != player && isSelected(entity, false)) {
                    val dist = (player.distanceTo(entity) * 2).toInt().coerceAtMost(255)

                    val colorMode = colorMode.lowercase()
                    val color = when {
                        entity is PlayerEntity && entity.isClientFriend() -> Color(0, 0, 255, 150)
                        teams && Teams.state && Teams.isInYourTeam(entity) -> Color(0, 162, 232)
                        colorMode == "custom" -> Color(colorRed, colorGreen, colorBlue, 150)
                        colorMode == "distancecolor" -> Color(255 - dist, dist, 0, 150)
                        colorMode == "rainbow" -> ColorUtils.rainbow()
                        else -> Color(255, 255, 255, 150)
                    }

                    drawTraces(entity, color)
                }
            }
        }

        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)
        glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawTraces(entity: Entity, color: Color) {
        val player = mc.player ?: return

        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
            - mc.entityRenderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
            - mc.entityRenderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
            - mc.entityRenderManager.renderPosZ)

        val yaw = player.prevRotationYaw + (player.yaw - player.prevRotationYaw) * mc.timer.renderPartialTicks
        val pitch = player.prevRotationPitch + (player.pitch - player.prevRotationPitch) * mc.timer.renderPartialTicks

        val eyeVector = Vec3d(0.0, 0.0, 1.0).rotatePitch(-pitch.toRadians()).rotateYaw(-yaw.toRadians())

        glColor(color)

        glVertex3d(eyeVector.x, player.getEyeHeight() + eyeVector.y, eyeVector.z)
        glVertex3d(x, y, z)
        glVertex3d(x, y, z)
        glVertex3d(x, y + entity.height, z)
    }
}
