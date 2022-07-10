/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.getPotionLiquidColor
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.entity.item.EntityExpBottle
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.*
import net.minecraft.item.*
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.Cylinder
import org.lwjgl.util.glu.GLU
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

@ModuleInfo(name = "Projectiles", description = "Allows you to see where arrows will land. (a.k.a. Trajectories)", category = ModuleCategory.RENDER)
class Projectiles : Module()
{
    private val colorGroup = ValueGroup("Color")
    private val colorModeValue = ListValue("Mode", arrayOf("Custom", "BowPower", "Rainbow"), "Custom", "Color")
    private val colorValue = RGBAColorValue("Color", 255, 255, 255, 30, listOf("R", "G", "B", "Alpha"))

    private val colorRainbowGroup = ValueGroup("Rainbow")
    private val colorRainbowEnabledValue = BoolValue("Enabled", true, "Rainbow")
    private val colorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val colorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
    private val colorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

    private val lineWidthValue = FloatValue("LineWidth", 2.0f, 1.0f, 3.0f)

    private val interpolateValue = BoolValue("Interpolate", true)

    private val allProjectilesValue = BoolValue("AllProjectiles", false)

    private var lastBowChargeDuration: Int = 0

    init
    {
        colorRainbowGroup.addAll(colorRainbowEnabledValue, colorRainbowSpeedValue, colorRainbowSaturationValue, colorRainbowBrightnessValue)
        colorGroup.addAll(colorModeValue, colorValue, colorRainbowGroup)
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return

        val renderManager = mc.renderManager
        val renderPosX = renderManager.renderPosX
        val renderPosY = renderManager.renderPosY
        val renderPosZ = renderManager.renderPosZ

        val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

        run {
            val (motionMultiplier, motionFactor, motionSlowdown, gravity, size, inaccuracy) = getProjectileInfo(thePlayer, thePlayer.heldItem ?: return@run, partialTicks) ?: return@run

            val alpha = colorValue.getAlpha()
            val color = when (colorModeValue.get().toLowerCase())
            {
                "bowpower" -> ColorUtils.applyAlphaChannel(ColorUtils.blendColors(floatArrayOf(0f, 0.5f, 1f), arrayOf(Color.RED, Color.YELLOW, Color.GREEN), (motionFactor / 30) * 10).rgb, alpha)
                "rainbow" -> ColorUtils.rainbowRGB(alpha = alpha, speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get())
                else -> colorValue.get()
            }

            val (serverYaw, serverPitch) = RotationUtils.serverRotation
            val (lastServerYaw, lastServerPitch) = RotationUtils.lastServerRotation

            val yaw = lastServerYaw + (serverYaw - lastServerYaw) * partialTicks
            val pitch = lastServerPitch + (serverPitch - lastServerPitch) * partialTicks

            val yawRadians = yaw.toRadians
            val pitchRadians = pitch.toRadians

            val yawSin = yawRadians.sin
            val yawCos = yawRadians.cos
            val pitchCos = pitchRadians.cos

            // Positions
            val posX = renderPosX - yawCos * 0.16F
            val posY = renderPosY + thePlayer.eyeHeight - 0.10000000149011612
            val posZ = renderPosZ - yawSin * 0.16F

            // Motions
            var motionX = -yawSin * pitchCos * motionMultiplier
            var motionY = -(pitch + inaccuracy).sin.toRadians * motionMultiplier
            var motionZ = yawCos * pitchCos * motionMultiplier

            val distance = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)

            motionX /= distance
            motionY /= distance
            motionZ /= distance

            motionX *= motionFactor
            motionY *= motionFactor
            motionZ *= motionFactor

            renderTrajectory(theWorld, thePlayer, renderPosX, renderPosY, renderPosZ, color, posX, posY, posZ, motionX, motionY, motionZ, motionSlowdown, gravity, size)
        }

        if (allProjectilesValue.get())
        {
            theWorld.loadedEntityList.asSequence().mapNotNull {
                it to (getProjectileInfo(it) ?: return@mapNotNull null)
            }.forEach { (proj, pair) ->
                val (info, color) = pair

                val lastPosX = proj.lastTickPosX
                val lastPosY = proj.lastTickPosY
                val lastPosZ = proj.lastTickPosZ

                renderTrajectory(theWorld, thePlayer, renderPosX, renderPosY, renderPosZ, ColorUtils.applyAlphaChannel(color, colorValue.getAlpha()), lastPosX + (proj.posX - lastPosX) * partialTicks, lastPosY + (proj.posY - lastPosY) * partialTicks, lastPosZ + (proj.posZ - lastPosZ) * partialTicks, proj.motionX, proj.motionY, proj.motionZ, info.motionSlowdown, info.gravity, info.size)
            }
        }
    }

    private fun renderTrajectory(theWorld: World, thePlayer: Entity, renderPosX: Double, renderPosY: Double, renderPosZ: Double, color: Int, defaultPosX: Double, defaultPosY: Double, defaultPosZ: Double, defaultMotionX: Double, defaultMotionY: Double, defaultMotionZ: Double, motionSlowdown: Float, gravity: Float, size: Float)
    {
        // Landing
        var landingPosition: MovingObjectPosition? = null
        var hasLanded = false
        var hitEntity = false

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        var posX = defaultPosX
        var posY = defaultPosY
        var posZ = defaultPosZ

        var motionX = defaultMotionX
        var motionY = defaultMotionY
        var motionZ = defaultMotionZ

        // Start drawing of path
        GL11.glDepthMask(false)
        RenderUtils.enableGlCap(GL11.GL_BLEND, GL11.GL_LINE_SMOOTH)
        RenderUtils.disableGlCap(GL11.GL_DEPTH_TEST, GL11.GL_ALPHA_TEST, GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

        RenderUtils.glColor(color)

        GL11.glLineWidth(lineWidthValue.get())

        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        while (!hasLanded && posY > 0.0)
        {
            // Set pos before and after
            var posBefore = Vec3(posX, posY, posZ)
            var posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

            // Get landing position
            landingPosition = theWorld.rayTraceBlocks(posBefore, posAfter, false, true, false)

            // Set pos before and after
            posBefore = Vec3(posX, posY, posZ)
            posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

            // Check if projectile is landing
            if (landingPosition != null)
            {
                hasLanded = true
                posAfter = Vec3(landingPosition.hitVec.xCoord, landingPosition.hitVec.yCoord, landingPosition.hitVec.zCoord)
            }

            // Set projectile box
            val projBox = AxisAlignedBB(posX - size, posY - size, posZ - size, posX + size, posY + size, posZ + size).addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0)

            val chunkMinX = floor((projBox.minX - 2) * 0.0625).toInt()
            val chunkMaxX = ceil((projBox.maxX + 2) * 0.0625).toInt()

            val chunkMinZ = floor((projBox.minZ - 2) * 0.0625).toInt()
            val chunkMaxZ = ceil((projBox.maxZ + 2) * 0.0625).toInt()

            // Check which entities colliding with the arrow
            val collidedEntities = mutableListOf<Entity>()

            for (x in chunkMinX..chunkMaxX) for (z in chunkMinZ..chunkMaxZ) theWorld.getChunkFromChunkCoords(x, z).getEntitiesWithinAABBForEntity(thePlayer, projBox, collidedEntities, null)

            val sizeDouble = size.toDouble()

            // Check all possible entities
            collidedEntities.filter(Entity::canBeCollidedWith).filter { it != thePlayer }.forEach { possibleEntity ->
                val possibleEntityBoundingBox = possibleEntity.entityBoundingBox.expand(sizeDouble, sizeDouble, sizeDouble)

                val possibleEntityLanding = possibleEntityBoundingBox.calculateIntercept(posBefore, posAfter) ?: return@forEach

                hitEntity = true
                hasLanded = true
                landingPosition = possibleEntityLanding
            }

            // Affect motions of projectile
            posX += motionX
            posY += motionY
            posZ += motionZ

            val blockState = theWorld.getBlockState(BlockPos(posX, posY, posZ))

            // Check is next position water
            if (blockState.block.getMaterial() == Material.water)
            {
                // Update motion in water
                motionX *= 0.6
                motionY *= 0.6
                motionZ *= 0.6
            }
            else
            {
                // Update motion
                val motionSlowdownDouble = motionSlowdown.toDouble()

                motionX *= motionSlowdownDouble
                motionY *= motionSlowdownDouble
                motionZ *= motionSlowdownDouble
            }

            motionY -= gravity.toDouble()

            // Draw path
            worldRenderer.pos(posX - renderPosX, posY - renderPosY, posZ - renderPosZ).endVertex()
        }

        // End the rendering of the path
        tessellator.draw()
        GL11.glPushMatrix()
        GL11.glTranslated(posX - renderPosX, posY - renderPosY, posZ - renderPosZ)

        if (landingPosition != null)
        {
            // Switch rotation of hit cylinder of the hit axis
            when (landingPosition?.sideHit?.ordinal ?: -1)
            {
                0 -> GL11.glRotatef(90F, 0F, 0F, 1F)
                2 -> GL11.glRotatef(90F, 1F, 0F, 0F)
            }

            // Check if hitting a entity
            if (hitEntity) RenderUtils.glColor(Color(255, 0, 0, 150))
        }

        // Rendering hit cylinder
        GL11.glRotatef(-90F, 1F, 0F, 0F)

        val cylinder = object : Cylinder()
        {
            override fun sin(r: Float): Float = r.sin
            override fun cos(r: Float): Float = r.cos
        }
        cylinder.drawStyle = GLU.GLU_LINE
        cylinder.draw(0.2F, 0F, 0F, 60, 1)

        GL11.glPopMatrix()
        GL11.glDepthMask(true)
        RenderUtils.resetCaps()
        RenderUtils.resetColor()
    }

    private fun getProjectileInfo(thePlayer: EntityPlayer, itemStack: ItemStack, partialTicks: Float): ProjectileInfo?
    {
        val item = itemStack.item

        return when
        {
            item is ItemBow ->
            {
                val fastBow = LiquidBounce.moduleManager[FastBow::class.java] as FastBow
                val fastBowEnabled = fastBow.state

                if (!fastBowEnabled && !thePlayer.isUsingItem)
                {
                    lastBowChargeDuration = 0 // Reset bow charge duration
                    return null
                }

                // Interpolate and calculate power of bow
                val bowChargeDuration = if (fastBowEnabled) fastBow.packetsValue.get() else thePlayer.itemInUseDuration
                var power = (lastBowChargeDuration + (bowChargeDuration - lastBowChargeDuration) * partialTicks) * 0.05f
                lastBowChargeDuration = bowChargeDuration

                power = ((power * power + power * 2F) / 3F).coerceAtMost(1F)

                if (power < 0.1F) return null

                ProjectileInfo(motionMultiplier = 1.0, motionFactor = power * 3F, gravity = 0.05F, size = 0.3F)
            }

            item is ItemFishingRod -> ProjectileInfo(motionSlowdown = 0.92F, gravity = 0.04F, size = 0.25F)

            item is ItemPotion && ItemPotion.isSplash(itemStack.metadata) -> ProjectileInfo(motionFactor = 0.5F, gravity = 0.05F, size = 0.25F, inaccuracy = -20.0F)

            item is ItemExpBottle -> ProjectileInfo(motionFactor = 0.7F, gravity = 0.07F, size = 0.25F, inaccuracy = -20.0F)

            else ->
            {
                if (item !is ItemSnowball && item !is ItemEnderPearl && item !is ItemEgg) return null

                ProjectileInfo(gravity = 0.03F, size = 0.25F)
            }
        }
    }

    private fun getProjectileInfo(projectile: Entity): Pair<ProjectileInfo, Int>?
    {
        if (projectile.isDead) return null

        return when
        {
            projectile is EntityArrow && !projectile.inGround -> ProjectileInfo(motionMultiplier = 1.0, gravity = 0.05F, size = 0.3F) to -65536

            projectile is EntityFishHook && !projectile.inGround && projectile.caughtEntity == null -> ProjectileInfo(motionSlowdown = 0.92F, gravity = 0.04F, size = 0.25F) to -7829368

            projectile is EntityThrowable ->
            {
                ProjectileInfo(motionFactor = projectile.velocity, gravity = projectile.gravityVelocity, size = max(projectile.width, projectile.height), inaccuracy = projectile.inaccuracy) to when
                {
                    projectile is EntityEgg -> -2109797
                    projectile is EntityEnderPearl -> -6750004
                    projectile is EntityPotion -> ColorUtils.applyAlphaChannel(getPotionLiquidColor(projectile, false), 255)
                    projectile is EntityExpBottle -> -3539055
                    else -> -1
                }
            }

            else -> return null
        }
    }
}

data class ProjectileInfo(val motionMultiplier: Double = 0.4, val motionFactor: Float = 1.5F, val motionSlowdown: Float = 0.99F, val gravity: Float, val size: Float, val inaccuracy: Float = 0F)
