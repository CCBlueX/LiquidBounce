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
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.entity.item.EntityMinecartChest
import net.minecraft.tileentity.*
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "StorageESP", description = "Allows you to see chests, dispensers, etc. through walls.", category = ModuleCategory.RENDER)
class StorageESP : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"), "Outline")
    private val chestValue = BoolValue("Chest", true)
    private val enderChestValue = BoolValue("EnderChest", true)
    private val furnaceValue = BoolValue("Furnace", true)
    private val dispenserValue = BoolValue("Dispenser", true)
    private val hopperValue = BoolValue("Hopper", true)
    private val enchantmentTableValue = BoolValue("EnchantmentTable", false)
    private val brewingStandValue = BoolValue("BrewingStand", false)
    private val signValue = BoolValue("Sign", false)

    private fun getColor(tileEntity: TileEntity):Color?{
        return when {
            chestValue.get() && tileEntity is TileEntityChest && !clickedBlocks.contains(tileEntity.pos) -> Color(0, 66, 255)
            enderChestValue.get() && tileEntity is TileEntityEnderChest && !clickedBlocks.contains(tileEntity.pos) -> Color.MAGENTA
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

            if (mode.equals("outline", ignoreCase = true)) {
                ClientUtils.disableFastRender()
                OutlineUtils.checkSetupFBO()
            }

            val gamma = mc.gameSettings.gammaSetting

            mc.gameSettings.gammaSetting = 100000.0f

            for (tileEntity in mc.theWorld!!.loadedTileEntityList) {
                val color: Color = getColor(tileEntity)?: continue

                if (!(tileEntity is TileEntityChest || tileEntity is TileEntityEnderChest)) {
                    RenderUtils.drawBlockBox(tileEntity.pos, color, !mode.equals("otherbox", ignoreCase = true))
                    if (tileEntity !is TileEntityEnchantmentTable) {
                        continue
                    }
                }
                when (mode.lowercase()) {
                    "otherbox", "box" -> RenderUtils.drawBlockBox(tileEntity.pos, color, !mode.equals("otherbox", ignoreCase = true))
                    "2d" -> RenderUtils.draw2D(tileEntity.pos, color.rgb, Color.BLACK.rgb)
                    "outline" -> {
                        RenderUtils.glColor(color);
                        OutlineUtils.renderOne(3F);
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1);
                        OutlineUtils.renderTwo();
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1);
                        OutlineUtils.renderThree();
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1);
                        OutlineUtils.renderFour(color);
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1);
                        OutlineUtils.renderFive();

                        OutlineUtils.setColor(Color.WHITE);
                    }
                    "wireframe" -> {
                        GL11.glPushMatrix()
                        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
                        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
                        GL11.glDisable(GL11.GL_TEXTURE_2D)
                        GL11.glDisable(GL11.GL_LIGHTING)
                        GL11.glDisable(GL11.GL_DEPTH_TEST)
                        GL11.glEnable(GL11.GL_LINE_SMOOTH)
                        GL11.glEnable(GL11.GL_BLEND)
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                        RenderUtils.glColor(color)
                        GL11.glLineWidth(1.5f)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1) //TODO: render twice?
                        GL11.glPopAttrib()
                        GL11.glPopMatrix()
                    }
                }
            }
            for (entity in mc.theWorld!!.loadedEntityList) {
                if (entity is EntityMinecartChest) {
                    when (mode.lowercase()) {
                        "otherbox", "box" -> RenderUtils.drawEntityBox(entity, Color(0, 66, 255), !mode.equals("otherbox", ignoreCase = true))
                        "2d" -> RenderUtils.draw2D(entity.position, Color(0, 66, 255).rgb, Color.BLACK.rgb)
                        "outline" -> {
                            val entityShadow: Boolean = mc.gameSettings.entityShadows
                            mc.gameSettings.entityShadows = false
                            RenderUtils.glColor(Color(0, 66, 255))
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
                            GL11.glPushMatrix()
                            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
                            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
                            GL11.glDisable(GL11.GL_TEXTURE_2D)
                            GL11.glDisable(GL11.GL_LIGHTING)
                            GL11.glDisable(GL11.GL_DEPTH_TEST)
                            GL11.glEnable(GL11.GL_LINE_SMOOTH)
                            GL11.glEnable(GL11.GL_BLEND)
                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                            RenderUtils.glColor(Color(0, 66, 255))
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            RenderUtils.glColor(Color(0, 66, 255))
                            GL11.glLineWidth(1.5f)
                            mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                            GL11.glPopAttrib()
                            GL11.glPopMatrix()
                            mc.gameSettings.entityShadows = entityShadow
                        }
                    }
                }
            }
            RenderUtils.glColor(Color(255, 255, 255, 255))
            mc.gameSettings.gammaSetting = gamma
        } catch (ignored: Exception) {
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = modeValue.get()
        val shader = (if (mode.equals("shaderoutline", ignoreCase = true)) OutlineShader.OUTLINE_SHADER else if (mode.equals("shaderglow", ignoreCase = true)) GlowShader.GLOW_SHADER else null)
                ?: return
        val partialTicks = event.partialTicks
        val renderManager = mc.renderManager
        shader.startDraw(event.partialTicks)

        try {
            val entityMap = HashMap<Color, ArrayList<TileEntity>>()
            for (tileEntity in mc.theWorld!!.loadedTileEntityList) {
                val color: Color = getColor(tileEntity) ?: continue

                if (!entityMap.containsKey(color)) {
                    entityMap.put(color, ArrayList())
                }

                entityMap[color]!!.add(tileEntity)
            }

            for ((color, arr) in entityMap) {
                shader.startDraw(partialTicks)
                for (entity in arr) {
                    TileEntityRendererDispatcher.instance.renderTileEntityAt(
                            entity,
                            entity.pos.x - renderManager.renderPosX,
                            entity.pos.y - renderManager.renderPosY,
                            entity.pos.z - renderManager.renderPosZ,
                            partialTicks
                    )
                }
                shader.stopDraw(color, if (mode.equals("shaderglow", ignoreCase = true)) 2.5f else 1.5f, 1f)
            }
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("An error occurred while rendering all storages for shader esp", ex)
        }

        shader.stopDraw(Color(0, 66, 255), if (mode.equals("shaderglow", ignoreCase = true)) 2.5f else 1.5f, 1f)
    }
}
