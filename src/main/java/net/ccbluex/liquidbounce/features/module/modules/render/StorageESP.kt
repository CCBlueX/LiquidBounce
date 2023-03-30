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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
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

@ModuleInfo(
    name = "StorageESP",
    description = "Allows you to see chests, dispensers, etc. through walls.",
    category = ModuleCategory.RENDER
)
class StorageESP : Module() {
    private val modeValue =
        ListValue("Mode", arrayOf("Box", "OtherBox", "Outline", "Glow", "2D", "WireFrame"), "Outline")

    private val glowRenderScale = object : FloatValue("Glow-Renderscale", 1f, 0.1f, 2f) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowRadius = object : IntegerValue("Glow-Radius", 4, 1, 5) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowFade = object : IntegerValue("Glow-Fade", 10, 0, 30) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowTargetAlpha = object : FloatValue("Glow-Target-Alpha", 0f, 0f, 1f) {
        override fun isSupported() = modeValue.get() == "Glow"
    }

    private val chestValue = BoolValue("Chest", true)
    private val enderChestValue = BoolValue("EnderChest", true)
    private val furnaceValue = BoolValue("Furnace", true)
    private val dispenserValue = BoolValue("Dispenser", true)
    private val hopperValue = BoolValue("Hopper", true)
    private val enchantmentTableValue = BoolValue("EnchantmentTable", false)
    private val brewingStandValue = BoolValue("BrewingStand", false)
    private val signValue = BoolValue("Sign", false)

    private fun getColor(tileEntity: TileEntity): Color? {
        return when {
            chestValue.get() && tileEntity is TileEntityChest && tileEntity.pos !in clickedBlocks -> Color(0, 66, 255)
            enderChestValue.get() && tileEntity is TileEntityEnderChest && tileEntity.pos !in clickedBlocks -> Color.MAGENTA
            furnaceValue.get() && tileEntity is TileEntityFurnace -> Color.BLACK
            dispenserValue.get() && tileEntity is TileEntityDispenser -> Color.BLACK
            hopperValue.get() && tileEntity is TileEntityHopper -> Color.GRAY
            enchantmentTableValue.get() && tileEntity is TileEntityEnchantmentTable -> Color(166, 202, 240) // Light blue
            brewingStandValue.get() && tileEntity is TileEntityBrewingStand -> Color.ORANGE
            signValue.get() && tileEntity is TileEntitySign -> Color.RED
            else -> null
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        try {
            val mode = modeValue.get()

            if (mode == "Outline") {
                disableFastRender()
                OutlineUtils.checkSetupFBO()
            }

            val gamma = mc.gameSettings.gammaSetting

            mc.gameSettings.gammaSetting = 100000f

            for (tileEntity in mc.theWorld.loadedTileEntityList) {
                val color: Color = getColor(tileEntity) ?: continue

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
                            val entityShadow: Boolean = mc.gameSettings.entityShadows
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
                            val entityShadow: Boolean = mc.gameSettings.entityShadows
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
        val mode = modeValue.get().lowercase()
        val partialTicks = event.partialTicks
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return
        val renderManager = mc.renderManager
        shader.startDraw(event.partialTicks, glowRenderScale.get())

        if (mc.theWorld == null) return

        try {
            val tileEntityMap = hashMapOf<Color, ArrayList<TileEntity>>()

            mc.theWorld.loadedTileEntityList.forEach { tileEntity ->
                val color: Color = getColor(tileEntity) ?: return@forEach
                if (color !in tileEntityMap) {
                    tileEntityMap[color] = ArrayList()
                }

                tileEntityMap[color]!!.add(tileEntity)
            }

            tileEntityMap.forEach { (color, tileEntites) ->
                shader.startDraw(partialTicks, glowRenderScale.get())

                for (entity in tileEntites) {
                    TileEntityRendererDispatcher.instance.renderTileEntityAt(
                        entity,
                        entity.pos.x - renderManager.renderPosX,
                        entity.pos.y - renderManager.renderPosY,
                        entity.pos.z - renderManager.renderPosZ,
                        partialTicks
                    )
                }
                shader.stopDraw(color, glowRadius.get(), glowFade.get(), glowTargetAlpha.get())
            }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all storages for shader esp", ex)
        }

        shader.stopDraw(Color(0, 66, 255), glowRadius.get(), glowFade.get(), glowTargetAlpha.get())
    }
}
