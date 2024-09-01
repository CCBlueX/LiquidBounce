/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import co.uk.hexeption.utils.OutlineUtils
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura.clickedTileEntities
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.ClientUtils.disableFastRender
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.entity.item.EntityMinecartChest
import net.minecraft.tileentity.*
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.pow

object StorageESP : Module("StorageESP", Category.RENDER) {
    private val mode by
        ListValue("Mode", arrayOf("Box", "OtherBox", "Outline", "Glow", "2D", "WireFrame"), "Outline")

        private val glowRenderScale by FloatValue("Glow-Renderscale", 1f, 0.5f..2f) { mode == "Glow" }
        private val glowRadius by IntegerValue("Glow-Radius", 4, 1..5) { mode == "Glow" }
        private val glowFade by IntegerValue("Glow-Fade", 10, 0..30) { mode == "Glow" }
        private val glowTargetAlpha by FloatValue("Glow-Target-Alpha", 0f, 0f..1f) { mode == "Glow" }

    private val customColor by BoolValue("CustomColor", false)
        private val colorRed by IntegerValue("R", 255, 0..255) { customColor }
        private val colorGreen by IntegerValue("G", 179, 0..255) { customColor }
        private val colorBlue by IntegerValue("B", 72, 0..255) { customColor }

    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..500) {
        override fun onInit(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }

    private val onLook by BoolValue("OnLook", false)
    private val maxAngleDifference by FloatValue("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val thruBlocks by BoolValue("ThruBlocks", true)

    private var maxRenderDistanceSq = 0.0

    private val chest by BoolValue("Chest", true)
    private val enderChest by BoolValue("EnderChest", true)
    private val furnace by BoolValue("Furnace", true)
    private val dispenser by BoolValue("Dispenser", true)
    private val hopper by BoolValue("Hopper", true)
    private val enchantmentTable by BoolValue("EnchantmentTable", false)
    private val brewingStand by BoolValue("BrewingStand", false)
    private val sign by BoolValue("Sign", false)

    private fun getColor(tileEntity: TileEntity): Color? {
        return if (customColor) {
            when {
                chest && tileEntity is TileEntityChest && tileEntity !in clickedTileEntities -> Color(colorRed, colorGreen, colorBlue)
                enderChest && tileEntity is TileEntityEnderChest && tileEntity !in clickedTileEntities -> Color(colorRed, colorGreen, colorBlue)
                furnace && tileEntity is TileEntityFurnace -> Color(colorRed, colorGreen, colorBlue)
                dispenser && tileEntity is TileEntityDispenser -> Color(colorRed, colorGreen, colorBlue)
                hopper && tileEntity is TileEntityHopper -> Color(colorRed, colorGreen, colorBlue)
                enchantmentTable && tileEntity is TileEntityEnchantmentTable -> Color(colorRed, colorGreen, colorBlue)
                brewingStand && tileEntity is TileEntityBrewingStand -> Color(colorRed, colorGreen, colorBlue)
                sign && tileEntity is TileEntitySign -> Color(colorRed, colorGreen, colorBlue)
                else -> null
            }
        } else {
            when {
                chest && tileEntity is TileEntityChest && tileEntity !in clickedTileEntities -> Color(0, 66, 255)
                enderChest && tileEntity is TileEntityEnderChest && tileEntity !in clickedTileEntities -> Color.MAGENTA
                furnace && tileEntity is TileEntityFurnace -> Color.BLACK
                dispenser && tileEntity is TileEntityDispenser -> Color.BLACK
                hopper && tileEntity is TileEntityHopper -> Color.GRAY
                enchantmentTable && tileEntity is TileEntityEnchantmentTable -> Color(166, 202, 240) // Light blue
                brewingStand && tileEntity is TileEntityBrewingStand -> Color.ORANGE
                sign && tileEntity is TileEntitySign -> Color.RED
                else -> null
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        try {
            if (mode == "Outline") {
                disableFastRender()
                OutlineUtils.checkSetupFBO()
            }

            val gamma = mc.options.gammaSetting

            mc.options.gammaSetting = 100000f

            for (tileEntity in mc.world.loadedTileEntityList) {
                val color = getColor(tileEntity) ?: continue

                val tileEntityPos = tileEntity.pos

                val distanceSquared = mc.player.squaredDistanceTo(
                    tileEntityPos.x.toDouble(),
                    tileEntityPos.y.toDouble(),
                    tileEntityPos.z.toDouble()
                )

                if (distanceSquared <= maxRenderDistanceSq) {
                    if (!(tileEntity is TileEntityChest || tileEntity is TileEntityEnderChest)) {
                        drawBlockBox(tileEntity.pos, color, mode != "OtherBox")

                        if (tileEntity !is TileEntityEnchantmentTable)
                            continue
                    }

                    if (onLook && !isLookingOnEntities(tileEntity, maxAngleDifference.toDouble()))
                        continue

                    if (!thruBlocks && !RotationUtils.isVisible(Vec3d(tileEntityPos.x.toDouble(), tileEntityPos.y.toDouble(), tileEntityPos.z.toDouble())))
                        continue

                    when (mode) {
                        "OtherBox", "Box" -> drawBlockBox(tileEntity.pos, color, mode != "OtherBox")

                        "2D" -> draw2D(tileEntity.pos, color.rgb, Color.BLACK.rgb)
                        "Outline" -> {
                            glColor(color)
                            OutlineUtils.renderOne(3F)
                            BlockEntityRenderDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                            OutlineUtils.renderTwo()
                            BlockEntityRenderDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                            OutlineUtils.renderThree()
                            BlockEntityRenderDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                            OutlineUtils.renderFour(color)
                            BlockEntityRenderDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                            OutlineUtils.renderFive()

                            OutlineUtils.setColor(Color.WHITE)
                        }

                        "WireFrame" -> {
                            glPushMatrix()
                            glPushAttrib(GL_ALL_ATTRIB_BITS)
                            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                            glDisable(GL_TEXTURE_2D)
                            glDisable(GL_LIGHTING)
                            glDisable(GL_DEPTH_TEST)
                            glEnable(GL_LINE_SMOOTH)
                            glEnable(GL_BLEND)
                            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                            glColor(color)
                            glLineWidth(1.5f)

                            // Render tiles the first time
                            BlockEntityRenderDispatcher.instance.renderTileEntity(
                                tileEntity,
                                event.partialTicks,
                                -1
                            )

                            glPopAttrib()
                            glPopMatrix()

                            // Render tiles the second time
                            BlockEntityRenderDispatcher.instance.renderTileEntity(
                                tileEntity,
                                event.partialTicks,
                                -1
                            )
                        }
                    }
                }
            }
            for (entity in mc.world.entities) {
                val entityPos = entity.position

                val distanceSquared = mc.player.squaredDistanceTo(
                    entityPos.x.toDouble(),
                    entityPos.y.toDouble(),
                    entityPos.z.toDouble()
                )

                if (distanceSquared <= maxRenderDistanceSq) {
                    if (entity is EntityMinecartChest) {
                        if (onLook && !isLookingOnEntities(entity, maxAngleDifference.toDouble()))
                            continue

                        if (!thruBlocks && !RotationUtils.isVisible(Vec3d(entity.posX, entity.posY, entity.posZ)))
                            continue

                        when (mode) {
                            "OtherBox", "Box" -> drawEntityBox(entity, Color(0, 66, 255), mode != "OtherBox")

                            "2d" -> draw2D(entity.position, Color(0, 66, 255).rgb, Color.BLACK.rgb)
                            "Outline" -> {
                                val entityShadow = mc.options.entityShadows
                                mc.options.entityShadows = false
                                glColor(Color(0, 66, 255))
                                OutlineUtils.renderOne(3f)
                                mc.entityRenderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                                OutlineUtils.renderTwo()
                                mc.entityRenderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                                OutlineUtils.renderThree()
                                mc.entityRenderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                                OutlineUtils.renderFour(Color(0, 66, 255))
                                mc.entityRenderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                                OutlineUtils.renderFive()
                                OutlineUtils.setColor(Color.WHITE)
                                mc.options.entityShadows = entityShadow
                            }

                            "WireFrame" -> {
                                val entityShadow = mc.options.entityShadows
                                mc.options.entityShadows = false
                                glPushMatrix()
                                glPushAttrib(GL_ALL_ATTRIB_BITS)
                                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                                glDisable(GL_TEXTURE_2D)
                                glDisable(GL_LIGHTING)
                                glDisable(GL_DEPTH_TEST)
                                glEnable(GL_LINE_SMOOTH)
                                glEnable(GL_BLEND)
                                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                                glColor(Color(0, 66, 255))
                                mc.entityRenderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                                glColor(Color(0, 66, 255))
                                glLineWidth(1.5f)
                                mc.entityRenderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                                glPopAttrib()
                                glPopMatrix()
                                mc.options.entityShadows = entityShadow
                            }
                        }
                    }
                }
            }

            glColor(Color(255, 255, 255, 255))
            mc.options.gammaSetting = gamma
            } catch (ignored: Exception) {
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.world == null || mode != "Glow")
            return

        val renderManager = mc.entityRenderManager
        GlowShader.startDraw(event.partialTicks, glowRenderScale)

        try {
            mc.world.loadedTileEntityList
                .groupBy { getColor(it) }
                .forEach { (color, tileEntities) ->
                    color ?: return@forEach

                    GlowShader.startDraw(event.partialTicks, glowRenderScale)

                    for (entity in tileEntities) {
                        val entityPos = entity.pos
                        val distanceSquared = mc.player.squaredDistanceTo(
                            entityPos.x.toDouble(),
                            entityPos.y.toDouble(),
                            entityPos.z.toDouble()
                        )

                        if (distanceSquared <= maxRenderDistanceSq) {
                            if (onLook && !isLookingOnEntities(entity, maxAngleDifference.toDouble()))
                                continue

                            if (!thruBlocks && !RotationUtils.isVisible(Vec3d(entityPos.x.toDouble(), entityPos.y.toDouble(), entityPos.z.toDouble())))
                                continue

                            BlockEntityRenderDispatcher.instance.renderTileEntityAt(
                                entity,
                                entityPos.x - renderManager.renderPosX,
                                entityPos.y - renderManager.renderPosY,
                                entityPos.z - renderManager.renderPosZ,
                                event.partialTicks
                            )
                        }
                    }

                    GlowShader.stopDraw(color, glowRadius, glowFade, glowTargetAlpha)
                }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all storages for shader esp", ex)
        }

       GlowShader.stopDraw(Color(0, 66, 255), glowRadius, glowFade, glowTargetAlpha)
    }
}
