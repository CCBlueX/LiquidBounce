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
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura.clickedBlocks
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.ClientUtils.disableFastRender
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.entity.item.EntityMinecartChest
import net.minecraft.tileentity.*
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object StorageESP : Module("StorageESP", ModuleCategory.RENDER) {
    private val mode by
        ListValue("Mode", arrayOf("Box", "OtherBox", "Outline", "Glow", "2D", "WireFrame"), "Outline")

    private val glowRenderScale by FloatValue("Glow-Renderscale", 1f, 0.1f..2f) { mode == "Glow" }
    private val glowRadius by IntegerValue("Glow-Radius", 4, 1..5) { mode == "Glow" }
    private val glowFade by IntegerValue("Glow-Fade", 10, 0..30) { mode == "Glow" }
    private val glowTargetAlpha by FloatValue("Glow-Target-Alpha", 0f, 0f..1f) { mode == "Glow" }

    private val chest by BoolValue("Chest", true)
    private val enderChest by BoolValue("EnderChest", true)
    private val furnace by BoolValue("Furnace", true)
    private val dispenser by BoolValue("Dispenser", true)
    private val hopper by BoolValue("Hopper", true)
    private val enchantmentTable by BoolValue("EnchantmentTable", false)
    private val brewingStand by BoolValue("BrewingStand", false)
    private val sign by BoolValue("Sign", false)

    private fun getColor(tileEntity: TileEntity): Color? {
        return when {
            chest && tileEntity is TileEntityChest && tileEntity.pos !in clickedBlocks -> Color(0, 66, 255)
            enderChest && tileEntity is TileEntityEnderChest && tileEntity.pos !in clickedBlocks -> Color.MAGENTA
            furnace && tileEntity is TileEntityFurnace -> Color.BLACK
            dispenser && tileEntity is TileEntityDispenser -> Color.BLACK
            hopper && tileEntity is TileEntityHopper -> Color.GRAY
            enchantmentTable && tileEntity is TileEntityEnchantmentTable -> Color(166, 202, 240) // Light blue
            brewingStand && tileEntity is TileEntityBrewingStand -> Color.ORANGE
            sign && tileEntity is TileEntitySign -> Color.RED
            else -> null
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        try {
            val mode = mode

            if (mode == "Outline") {
                disableFastRender()
                OutlineUtils.checkSetupFBO()
            }

            val gamma = mc.gameSettings.gammaSetting

            mc.gameSettings.gammaSetting = 100000f

            for (tileEntity in mc.theWorld.loadedTileEntityList) {
                val color = getColor(tileEntity) ?: continue

                if (!(tileEntity is TileEntityChest || tileEntity is TileEntityEnderChest)) {
                    drawBlockBox(tileEntity.pos, color, mode != "OtherBox")
                    if (tileEntity !is TileEntityEnchantmentTable) {
                        continue
                    }
                }
                when (mode.lowercase()) {
                    "otherbox", "box" -> drawBlockBox(tileEntity.pos, color, mode != "OtherBox")

                    "2d" -> draw2D(tileEntity.pos, color.rgb, Color.BLACK.rgb)
                    "outline" -> {
                        glColor(color)
                        OutlineUtils.renderOne(3F)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderTwo()
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderThree()
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderFour(color)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderFive()

                        OutlineUtils.setColor(Color.WHITE)
                    }

                    "wireframe" -> {
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
                        TileEntityRendererDispatcher.instance.renderTileEntity(
                            tileEntity,
                            event.partialTicks,
                            -1
                        ) //TODO: render twice?
                        glPopAttrib()
                        glPopMatrix()
                    }
                }
            }
            for (entity in mc.theWorld.loadedEntityList) {
                if (entity is EntityMinecartChest) {
                    when (mode.lowercase()) {
                        "otherbox", "box" -> drawEntityBox(entity, Color(0, 66, 255), mode != "OtherBox")

                        "2d" -> draw2D(entity.position, Color(0, 66, 255).rgb, Color.BLACK.rgb)
                        "outline" -> {
                            val entityShadow = mc.gameSettings.entityShadows
                            mc.gameSettings.entityShadows = false
                            glColor(Color(0, 66, 255))
                            OutlineUtils.renderOne(3f)
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            OutlineUtils.renderTwo()
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            OutlineUtils.renderThree()
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            OutlineUtils.renderFour(Color(0, 66, 255))
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            OutlineUtils.renderFive()
                            OutlineUtils.setColor(Color.WHITE)
                            mc.gameSettings.entityShadows = entityShadow
                        }

                        "wireframe" -> {
                            val entityShadow = mc.gameSettings.entityShadows
                            mc.gameSettings.entityShadows = false
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
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            glColor(Color(0, 66, 255))
                            glLineWidth(1.5f)
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            glPopAttrib()
                            glPopMatrix()
                            mc.gameSettings.entityShadows = entityShadow
                        }
                    }
                }
            }
            glColor(Color(255, 255, 255, 255))
            mc.gameSettings.gammaSetting = gamma
        } catch (ignored: Exception) {
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = mode.lowercase()
        val partialTicks = event.partialTicks
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return
        val renderManager = mc.renderManager
        shader.startDraw(event.partialTicks, glowRenderScale)

        if (mc.theWorld == null) return

        try {
            val tileEntityMap = hashMapOf<Color, ArrayList<TileEntity>>()

            mc.theWorld.loadedTileEntityList.forEach { tileEntity ->
                val color = getColor(tileEntity) ?: return@forEach
                if (color !in tileEntityMap) {
                    tileEntityMap[color] = ArrayList()
                }

                tileEntityMap[color]!! += tileEntity
            }

            tileEntityMap.forEach { (color, tileEntites) ->
                shader.startDraw(partialTicks, glowRenderScale)

                for (entity in tileEntites) {
                    TileEntityRendererDispatcher.instance.renderTileEntityAt(
                        entity,
                        entity.pos.x - renderManager.renderPosX,
                        entity.pos.y - renderManager.renderPosY,
                        entity.pos.z - renderManager.renderPosZ,
                        partialTicks
                    )
                }
                shader.stopDraw(color, glowRadius, glowFade, glowTargetAlpha)
            }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all storages for shader esp", ex)
        }

        shader.stopDraw(Color(0, 66, 255), glowRadius, glowFade, glowTargetAlpha)
    }
}
