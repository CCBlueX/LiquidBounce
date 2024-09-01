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
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.entity.item.EntityExpBottle
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityEgg
import net.minecraft.entity.projectile.EntityFireball
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

    private val trailPositions = mutableMapOf<Entity, MutableList<Triple<Long, Vec3d, Float>>>()

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val theWorld = mc.world ?: return
        val renderManager = mc.entityRenderManager

        for (entity in theWorld.entities) {
            val theEntity = entity as? LivingEntity ?: continue
            val heldStack = theEntity.mainHandStack ?: continue

            val item = heldStack.item
            var isBow = false
            var motionFactor = 1.5F
            var motionSlowdown = 0.99F
            val gravity: Float
            val size: Float

            // Check items
            when (item) {
                is BowItem -> {
                    isBow = true
                    gravity = 0.05F
                    size = 0.3F

                    if (theEntity is PlayerEntity) {
                        if (!theEntity.isUsingItem) continue

                        // Calculate power of bow
                        var power = theEntity.itemUseTicks / 20f
                        power = (power * power + power * 2F) / 3F
                        if (power < 0.1F) continue
                        if (power > 1F) power = 1F
                        motionFactor = power * 3F
                    } else {
                        // Approximate bow power for other Entities (ex: Skeletons)
                        motionFactor = 3F
                    }
                }
                is ItemFishingRod -> {
                    gravity = 0.04F
                    size = 0.25F
                    motionSlowdown = 0.92F
                }
                is PotionItem -> {
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
            var x = theEntity.x - cos(yawRadians) * 0.16F
            var y = theEntity.y + theEntity.eyeHeight - 0.10000000149011612
            var z = theEntity.z - sin(yawRadians) * 0.16F

            // Motions
            var velocityX = -sin(yawRadians) * cos(pitchRadians) * if (isBow) 1.0 else 0.4
            var velocityY = -sin((pitch + if (item is PotionItem) -20 else 0).toRadians()) * if (isBow) 1.0 else 0.4
            var velocityZ = cos(yawRadians) * cos(pitchRadians) * if (isBow) 1.0 else 0.4
            val distance = sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ)

            velocityX /= distance
            velocityY /= distance
            velocityZ /= distance
            velocityX *= motionFactor
            velocityY *= motionFactor
            velocityZ *= motionFactor

            // Landing
            var landingPosition: BlockHitResult? = null
            var hasLanded = false
            var hitEntity = false

            val tessellator = Tessellator.getInstance()
            val worldRenderer = tessellator.renderer

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

            worldRenderer.begin(GL_LINE_STRIP, VertexFormats.POSITION)

            while (!hasLanded && y > 0.0) {
                // Set pos before and after
                var posBefore = Vec3d(x, y, z)
                var posAfter = Vec3d(x + velocityX, y + velocityY, z + velocityZ)

                // Get landing position
                landingPosition = theWorld.rayTrace(
                    posBefore, posAfter, false,
                    true, false
                )

                // Set pos before and after
                posBefore = Vec3d(x, y, z)
                posAfter = Vec3d(x + velocityX, y + velocityY, z + velocityZ)

                // Check if arrow is landing
                if (landingPosition != null) {
                    hasLanded = true
                    posAfter =
                        Vec3d(landingPosition.pos.x, landingPosition.pos.y, landingPosition.pos.z)
                }

                // Set arrow box
                val arrowBox = Box(
                    x - size, y - size, z - size, x + size,
                    y + size, z + size
                ).addCoord(velocityX, velocityY, velocityZ).expand(1.0, 1.0, 1.0)

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
                        val possibleEntityBoundingBox = possibleEntity.boundingBox
                            .expand(size.toDouble(), size.toDouble(), size.toDouble())

                        val possibleEntityLanding = possibleEntityBoundingBox
                            .method_585(posBefore, posAfter) ?: continue

                        hitEntity = true
                        hasLanded = true
                        landingPosition = possibleEntityLanding
                    }
                }

                // Affect motions of arrow
                x += velocityX
                y += velocityY
                z += velocityZ

                // Check is next position water
                if (getState(BlockPos(x, y, z))!!.block.material === Material.water) {
                    // Update motion
                    velocityX *= 0.6
                    velocityY *= 0.6
                    velocityZ *= 0.6
                } else { // Update motion
                    velocityX *= motionSlowdown.toDouble()
                    velocityY *= motionSlowdown.toDouble()
                    velocityZ *= motionSlowdown.toDouble()
                }

                velocityY -= gravity.toDouble()

                // Draw path
                worldRenderer.pos(
                    x - renderManager.cameraX, y - renderManager.cameraY,
                    z - renderManager.cameraZ
                ).endVertex()
            }

            // End the rendering of the path
            tessellator.draw()
            glPushMatrix()
            glTranslated(
                x - renderManager.cameraX, y - renderManager.cameraY,
                z - renderManager.cameraZ
            )

            if (landingPosition != null) {
                // Accurate landing position checking
                when (landingPosition.direction!!) {
                    Direction.DOWN -> glRotatef(90F, 0F, 1F, 0F)
                    Direction.UP -> glRotatef(-90F, 0F, 1F, 0F)
                    Direction.NORTH -> glRotatef(-90F, 1F, 0F, 0F)
                    Direction.SOUTH -> glRotatef(90F, 1F, 0F, 0F)
                    Direction.WEST -> glRotatef(-90F, 0F, 0F, 1F)
                    Direction.EAST -> glRotatef(90F, 0F, 0F, 1F)
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

        for ((entity, positions) in trailPositions) {
            if (positions.isEmpty()) continue

            glPushMatrix()
            glPushAttrib(GL_ALL_ATTRIB_BITS)

            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            glDisable(GL_LIGHTING)
            glLineWidth(2.0f)

            val tessellator = Tessellator.getInstance()
            val worldRenderer = tessellator.renderer
            worldRenderer.begin(GL_LINE_STRIP, VertexFormats.POSITION)

            for ((_, pos, alpha) in positions) {
                val interpolatePos = Vec3d(
                    pos.x - renderManager.cameraX,
                    pos.y - renderManager.cameraY,
                    pos.z - renderManager.cameraZ
                )

                val color = when (entity) {
                    is EntityArrow -> Color(255, 0, 0)
                    is EntityPotion -> Color(200, 150, 0)
                    is EntityEnderPearl -> Color(200, 0, 200)
                    is EntityFireball -> Color(255, 255, 0)
                    is EntityEgg, is EntitySnowball -> Color(200, 255, 200)
                    else -> Color(255, 255, 255)
                }

                glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, alpha)

                worldRenderer.pos(interpolatePos.x, interpolatePos.y, interpolatePos.z).endVertex()
            }

            tessellator.draw()

            glColor4f(1f, 1f, 1f, 1f)

            glPopAttrib()
            glPopMatrix()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val world = mc.world ?: return

        val currentTime = System.currentTimeMillis()

        for (entity in world.entities) {
            if (entity == null) {
                trailPositions.clear()
                continue
            }

            when (entity) {
                is EntitySnowball, is EntityEnderPearl, is EntityEgg,
                is EntityArrow, is EntityPotion, is EntityExpBottle, is EntityFireball -> {
                    val positions = trailPositions.getOrPut(entity) { mutableListOf() }

                    positions.removeIf { (timestamp, _, alpha) ->
                        currentTime - timestamp > 10000 || alpha <= 0
                    }

                    if (positions.size > maxTrailSize) {
                        positions.removeAt(0)
                    }

                    positions.add(Triple(currentTime, Vec3d(entity.x, entity.y, entity.z), 1.0f))
                }
            }
        }

        // Gradually fade out trails of entities no longer in the world
        for (positions in trailPositions.values) {
            for (i in positions.indices) {
                val (timestamp, pos, alpha) = positions[i]
                positions[i] = Triple(timestamp, pos, alpha - 0.04f)
            }
        }

        // Remove entities that are no longer in the world
        trailPositions.keys.removeIf { it !in world.entities && trailPositions[it]?.all { (_, _, alpha) -> alpha <= 0 } == true }
    }
}
