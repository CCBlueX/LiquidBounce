/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import java.awt.Color

object ForwardTrack : Module("ForwardTrack", Category.COMBAT) {
    val espMode by ListValue("ESP-Mode", arrayOf("Box", "Player"), "Player", subjective = true)

    private val rainbow by BoolValue("Rainbow", true, subjective = true) { espMode == "Box" }
    private val red by IntegerValue("R", 0, 0..255, subjective = true) { !rainbow && espMode == "Box" }
    private val green by IntegerValue("G", 255, 0..255, subjective = true) { !rainbow && espMode == "Box" }
    private val blue by IntegerValue("B", 0, 0..255, subjective = true) { !rainbow && espMode == "Box" }

    val color
        get() = if (rainbow) ColorUtils.rainbow() else Color(red, green, blue)

    /**
     * Any good anti-cheat will easily detect this module.
     */
    fun includeEntityTruePos(entity: Entity, action: () -> Unit) {
        if (!state || entity !is EntityLivingBase || entity is EntityPlayerSP)
            return

        val iEntity = (entity as IMixinEntity)

        if (!iEntity.truePos)
            return

        // Would be more fun if we simulated instead.
        val pos = iEntity.run { Vec3(trueX, trueY, trueZ) }

        Backtrack.runWithSimulatedPosition(entity, pos) {
            action()

            null
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (espMode != "Box")
            return

        val renderManager = mc.renderManager

        for (target in mc.theWorld.loadedEntityList) {
            if (target is EntityPlayerSP)
                continue

            target?.run {
                val targetEntity = target as IMixinEntity

                if (targetEntity.truePos) {
                    val x =
                        targetEntity.trueX - renderManager.renderPosX
                    val y =
                        targetEntity.trueY - renderManager.renderPosY
                    val z =
                        targetEntity.trueZ - renderManager.renderPosZ

                    val axisAlignedBB = entityBoundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

                    drawBacktrackBox(
                        AxisAlignedBB.fromBounds(
                            axisAlignedBB.minX,
                            axisAlignedBB.minY,
                            axisAlignedBB.minZ,
                            axisAlignedBB.maxX,
                            axisAlignedBB.maxY,
                            axisAlignedBB.maxZ
                        ), color
                    )
                }
            }
        }
    }
}