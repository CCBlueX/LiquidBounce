/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.inventory.isSplashPotion
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.interpolateHSB
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.entity.item.EntityExpBottle
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityEgg
import net.minecraft.entity.projectile.EntityPotion
import net.minecraft.entity.projectile.EntitySnowball
import net.minecraft.item.*
import net.minecraft.util.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.glu.Cylinder
import org.lwjgl.util.glu.GLU
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Projectiles : Module("Projectiles", Category.RENDER, gameDetecting = false, hideModule = false) {
    private val maxTrailSize by IntegerValue("MaxTrailSize", 20, 1..100)

    private val colorMode by ListValue("Color", arrayOf("Custom", "BowPower", "Rainbow"), "Custom")
        private val colorRed by IntegerValue("R", 0, 0..255) { colorMode == "Custom" }
        private val colorGreen by IntegerValue("G", 160, 0..255) { colorMode == "Custom" }
        private val colorBlue by IntegerValue("B", 255, 0..255) { colorMode == "Custom" }

    private val trailPositions = mutableMapOf<Entity, MutableList<Pair<Long, Vec3>>>()

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val theWorld = mc.theWorld ?: return
        val renderManager = mc.renderManager

        for (entity in theWorld.loadedEntityList) {
            val theEntity = entity as? EntityPlayer ?: continue
            val heldStack = theEntity.heldItem ?: continue

            val item = heldStack.item
            var isBow = false
            var motionFactor = 1.5F
            var motionSlowdown = 0.99F
            val gravity: Float
            val size: Float

            // Check items
            when (item) {
                is ItemBow -> {
                    if (!theEntity.isUsingItem) continue
                    isBow = true
                    gravity = 0.05F
                    size = 0.3F
                    // Calculate power of bow
                    var power = theEntity.itemInUseDuration / 20f
                    power = (power * power + power * 2F) / 3F
                    if (power < 0.1F) continue
                    if (power > 1F) power = 1F
                    motionFactor = power * 3F
                }
                is ItemFishingRod -> {
                    gravity = 0.04F
                    size = 0.25F
                    motionSlowdown = 0.92F
                }
                is ItemPotion -> {
                    if (!heldStack.isSplashPotion()) continue
                    gravity = 0.05F
                    size = 0.25F
                    motionFactor = 0.5F
                }
                is ItemSnowball, is ItemEnderPearl, is ItemEgg -> {
                    gravity = 0.03F
                    size = 0.25F
                }
                else -> continue
            }

            // Yaw and pitch of player
            val (yaw, pitch) = theEntity.rotation

            val yawRadians = yaw.toRadiansD()
            val pitchRadians = pitch.toRadiansD()

            // Positions
            var posX = theEntity.posX - cos(yawRadians) * 0.16F
            var posY = theEntity.posY + theEntity.eyeHeight - 0.10000000149011612
            var posZ = theEntity.posZ - sin(yawRadians) * 0.16F

            // Motions
            var motionX = -sin(yawRadians) * cos(pitchRadians) * if (isBow) 1.0 else 0.4
            var motionY = -sin((pitch + if (item is ItemPotion) -20 else 0).toRadians()) * if (isBow) 1.0 else 0.4
            var motionZ = cos(yawRadians) * cos(pitchRadians) * if (isBow) 1.0 else 0.4
            val distance = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)

            motionX /= distance
            motionY /= distance
            motionZ /= distance
            motionX *= motionFactor
            motionY *= motionFactor
            motionZ *= motionFactor

            // Landing
            var landingPosition: MovingObjectPosition? = null
            var hasLanded = false
            var hitEntity = false

            val tessellator = Tessellator.getInstance()
            val worldRenderer = tessellator.worldRenderer

            // Start drawing of path
            glDepthMask(false)
            enableGlCap(GL_BLEND, GL_LINE_SMOOTH)
            disableGlCap(GL_DEPTH_TEST, GL_ALPHA_TEST, GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
            glColor(
                when (colorMode.lowercase()) {
                    "bowpower" -> interpolateHSB(Color.RED, Color.GREEN, (motionFactor / 30) * 10)
                    "rainbow" -> ColorUtils.rainbow()
                    else -> Color(colorRed, colorGreen, colorBlue, 255)
                }
            )
            glLineWidth(2f)

            worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)

            while (!hasLanded && posY > 0.0) {
                // Set pos before and after
                var posBefore = Vec3(posX, posY, posZ)
                var posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

                // Get landing position
                landingPosition = theWorld.rayTraceBlocks(
                    posBefore, posAfter, false,
                    true, false
                )

                // Set pos before and after
                posBefore = Vec3(posX, posY, posZ)
                posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

                // Check if arrow is landing
                if (landingPosition != null) {
                    hasLanded = true
                    posAfter =
                        Vec3(landingPosition.hitVec.xCoord, landingPosition.hitVec.yCoord, landingPosition.hitVec.zCoord)
                }

                // Set arrow box
                val arrowBox = AxisAlignedBB(
                    posX - size, posY - size, posZ - size, posX + size,
                    posY + size, posZ + size
                ).addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0)

                val chunkMinX = ((arrowBox.minX - 2) / 16).toInt()
                val chunkMaxX = ((arrowBox.maxX + 2.0) / 16.0).toInt()
                val chunkMinZ = ((arrowBox.minZ - 2.0) / 16.0).toInt()
                val chunkMaxZ = ((arrowBox.maxZ + 2.0) / 16.0).toInt()

                // Check which entities colliding with the arrow
                val collidedEntities = mutableListOf<Entity>()

                for (x in chunkMinX..chunkMaxX)
                    for (z in chunkMinZ..chunkMaxZ)
                        theWorld.getChunkFromChunkCoords(x, z)
                            .getEntitiesWithinAABBForEntity(theEntity, arrowBox, collidedEntities, null)

                // Check all possible entities
                for (possibleEntity in collidedEntities) {
                    if (possibleEntity.canBeCollidedWith() && possibleEntity != theEntity) {
                        val possibleEntityBoundingBox = possibleEntity.entityBoundingBox
                            .expand(size.toDouble(), size.toDouble(), size.toDouble())

                        val possibleEntityLanding = possibleEntityBoundingBox
                            .calculateIntercept(posBefore, posAfter) ?: continue

                        hitEntity = true
                        hasLanded = true
                        landingPosition = possibleEntityLanding
                    }
                }

                // Affect motions of arrow
                posX += motionX
                posY += motionY
                posZ += motionZ

                // Check is next position water
                if (getState(BlockPos(posX, posY, posZ))!!.block.material === Material.water) {
                    // Update motion
                    motionX *= 0.6
                    motionY *= 0.6
                    motionZ *= 0.6
                } else { // Update motion
                    motionX *= motionSlowdown.toDouble()
                    motionY *= motionSlowdown.toDouble()
                    motionZ *= motionSlowdown.toDouble()
                }

                motionY -= gravity.toDouble()

                // Draw path
                worldRenderer.pos(
                    posX - renderManager.renderPosX, posY - renderManager.renderPosY,
                    posZ - renderManager.renderPosZ
                ).endVertex()
            }

            // End the rendering of the path
            tessellator.draw()
            glPushMatrix()
            glTranslated(
                posX - renderManager.renderPosX, posY - renderManager.renderPosY,
                posZ - renderManager.renderPosZ
            )

            if (landingPosition != null) {
                // Accurate landing position checking
                when (landingPosition.sideHit) {
                    EnumFacing.DOWN -> glRotatef(90F, 0F, 1F, 0F)
                    EnumFacing.UP -> glRotatef(-90F, 0F, 1F, 0F)
                    EnumFacing.NORTH -> glRotatef(-90F, 1F, 0F, 0F)
                    EnumFacing.SOUTH -> glRotatef(90F, 1F, 0F, 0F)
                    EnumFacing.WEST -> glRotatef(-90F, 0F, 0F, 1F)
                    EnumFacing.EAST -> glRotatef(90F, 0F, 0F, 1F)
                    else -> glRotatef(90F, 0F, 0F, 1F)
                }

                // Check if hitting an entity
                if (hitEntity)
                    glColor(Color(255, 0, 0, 150))
            }

            // Rendering hit cylinder
            glRotatef(-90F, 1F, 0F, 0F)

            val cylinder = Cylinder()
            cylinder.drawStyle = GLU.GLU_LINE
            cylinder.draw(0.2F, 0F, 0F, 60, 1)

            glPopMatrix()
            glDepthMask(true)
            resetCaps()
            glColor4f(1F, 1F, 1F, 1F)
        }

        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(2.0f)

        for ((entity, positions) in trailPositions) {
            if (positions.size < 2) continue

            val tessellator = Tessellator.getInstance()
            val worldRenderer = tessellator.worldRenderer
            worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)

            val color = when (entity) {
                is EntityArrow -> Color(255, 0, 0, 125)
                is EntityPotion -> Color(250, 200, 0, 255)
                is EntityEnderPearl -> Color(200, 0, 200, 125)
                is EntityEgg, is EntitySnowball -> Color(200, 255, 200, 200)
                else -> Color(255, 255, 255, 255)
            }

            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

            for ((_, pos) in positions) {
                val interpolatePos = Vec3(
                    pos.xCoord - renderManager.renderPosX,
                    pos.yCoord - renderManager.renderPosY,
                    pos.zCoord - renderManager.renderPosZ
                )
                worldRenderer.pos(interpolatePos.xCoord, interpolatePos.yCoord, interpolatePos.zCoord).endVertex()
            }

            tessellator.draw()
        }

        glPopMatrix()
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glDisable(GL_BLEND)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val world = mc.theWorld ?: return

        val currentTime = System.currentTimeMillis()

        for (entity in world.loadedEntityList) {
            if (entity == null) {
                trailPositions.clear()
                continue
            }

            when (entity) {
                is EntitySnowball, is EntityEnderPearl, is EntityEgg,
                is EntityArrow, is EntityPotion, is EntityExpBottle -> {
                    val positions = trailPositions.getOrPut(entity) { mutableListOf() }

                    positions.removeIf { (timestamp, _) -> currentTime - timestamp > 10000 }

                    if (positions.size > maxTrailSize) {
                        positions.removeAt(0)
                    }

                    positions.add(Pair(currentTime, Vec3(entity.posX, entity.posY, entity.posZ)))
                }
                else -> trailPositions.remove(entity)
            }
        }

        // Remove entities that are no longer in the world
        trailPositions.keys.removeIf { it !in world.loadedEntityList }
    }
}
