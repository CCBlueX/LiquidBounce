/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render;

import co.uk.hexeption.utils.OutlineUtils;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader;
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader;
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.tileentity.*;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "StorageESP", description = "Allows you to see chests, dispensers, etc. through walls.", category = ModuleCategory.RENDER)
public class StorageESP extends Module {
    private final ListValue modeValue = new ListValue("Mode", new String[]{"Box", "OtherBox", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"}, "Outline");

    private final BoolValue chestValue = new BoolValue("Chest", true);
    private final BoolValue enderChestValue = new BoolValue("EnderChest", true);
    private final BoolValue furnaceValue = new BoolValue("Furnace", true);
    private final BoolValue dispenserValue = new BoolValue("Dispenser", true);
    private final BoolValue hopperValue = new BoolValue("Hopper", true);

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        try {
            final String mode = modeValue.get();

            if (mode.equalsIgnoreCase("outline")) {
                ClientUtils.disableFastRender();
                OutlineUtils.checkSetupFBO();
            }

            float gamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100000.0F;

            for (final TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
                Color color = null;

                if (chestValue.get() && tileEntity instanceof TileEntityChest && !ChestAura.INSTANCE.getClickedBlocks().contains(tileEntity.getPos()))
                    color = new Color(0, 66, 255);

                if (enderChestValue.get() && tileEntity instanceof TileEntityEnderChest && !ChestAura.INSTANCE.getClickedBlocks().contains(tileEntity.getPos()))
                    color = Color.MAGENTA;

                if (furnaceValue.get() && tileEntity instanceof TileEntityFurnace)
                    color = Color.BLACK;

                if (dispenserValue.get() && tileEntity instanceof TileEntityDispenser)
                    color = Color.BLACK;

                if (hopperValue.get() && tileEntity instanceof TileEntityHopper)
                    color = Color.GRAY;

                if (color == null)
                    continue;

                if (!(tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest)) {
                    RenderUtils.drawBlockBox(tileEntity.getPos(), color, !mode.equalsIgnoreCase("otherbox"));
                    continue;
                }

                switch (mode.toLowerCase()) {
                    case "otherbox":
                    case "box":
                        RenderUtils.drawBlockBox(tileEntity.getPos(), color, !mode.equalsIgnoreCase("otherbox"));
                        break;
                    case "2d":
                        RenderUtils.draw2D(tileEntity.getPos(), color.getRGB(), Color.BLACK.getRGB());
                        break;
                    case "outline":
                        RenderUtils.glColor(color);
                        OutlineUtils.renderOne(3F);
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.getPartialTicks(), -1);
                        OutlineUtils.renderTwo();
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.getPartialTicks(), -1);
                        OutlineUtils.renderThree();
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.getPartialTicks(), -1);
                        OutlineUtils.renderFour(color);
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.getPartialTicks(), -1);
                        OutlineUtils.renderFive();

                        OutlineUtils.setColor(Color.WHITE);
                        break;
                    case "wireframe":
                        glPushMatrix();
                        glPushAttrib(GL_ALL_ATTRIB_BITS);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        glDisable(GL_TEXTURE_2D);
                        glDisable(GL_LIGHTING);
                        glDisable(GL_DEPTH_TEST);
                        glEnable(GL_LINE_SMOOTH);
                        glEnable(GL_BLEND);
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.getPartialTicks(), -1);
                        RenderUtils.glColor(color);
                        glLineWidth(1.5F);
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.getPartialTicks(), -1);
                        glPopAttrib();
                        glPopMatrix();
                        break;
                }
            }

            for (final Entity entity : mc.theWorld.loadedEntityList)
                if (entity instanceof EntityMinecartChest) {
                    switch (mode.toLowerCase()) {
                        case "otherbox":
                        case "box":
                            RenderUtils.drawEntityBox(entity, new Color(0, 66, 255), !mode.equalsIgnoreCase("otherbox"));
                            break;
                        case "2d":
                            RenderUtils.draw2D(entity.getPosition(), new Color(0, 66, 255).getRGB(), Color.BLACK.getRGB());
                            break;
                        case "outline": {
                            final boolean entityShadow = mc.gameSettings.entityShadows;
                            mc.gameSettings.entityShadows = false;

                            RenderUtils.glColor(new Color(0, 66, 255));
                            OutlineUtils.renderOne(3F);
                            mc.getRenderManager().renderEntityStatic(entity, mc.timer.renderPartialTicks, true);
                            OutlineUtils.renderTwo();
                            mc.getRenderManager().renderEntityStatic(entity, mc.timer.renderPartialTicks, true);
                            OutlineUtils.renderThree();
                            mc.getRenderManager().renderEntityStatic(entity, mc.timer.renderPartialTicks, true);
                            OutlineUtils.renderFour(new Color(0, 66, 255));
                            mc.getRenderManager().renderEntityStatic(entity, mc.timer.renderPartialTicks, true);
                            OutlineUtils.renderFive();

                            OutlineUtils.setColor(Color.WHITE);

                            mc.gameSettings.entityShadows = entityShadow;
                            break;
                        }
                        case "wireframe": {
                            final boolean entityShadow = mc.gameSettings.entityShadows;
                            mc.gameSettings.entityShadows = false;

                            glPushMatrix();
                            glPushAttrib(GL_ALL_ATTRIB_BITS);
                            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                            glDisable(GL_TEXTURE_2D);
                            glDisable(GL_LIGHTING);
                            glDisable(GL_DEPTH_TEST);
                            glEnable(GL_LINE_SMOOTH);
                            glEnable(GL_BLEND);
                            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                            RenderUtils.glColor(new Color(0, 66, 255));
                            mc.getRenderManager().renderEntityStatic(entity, mc.timer.renderPartialTicks, true);
                            RenderUtils.glColor(new Color(0, 66, 255));
                            glLineWidth(1.5F);
                            mc.getRenderManager().renderEntityStatic(entity, mc.timer.renderPartialTicks, true);
                            glPopAttrib();
                            glPopMatrix();

                            mc.gameSettings.entityShadows = entityShadow;
                            break;
                        }
                    }
                }

            RenderUtils.glColor(new Color(255, 255, 255, 255));
            mc.gameSettings.gammaSetting = gamma;
        } catch (Exception ignored) {
        }
    }

    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        final String mode = modeValue.get();

        final FramebufferShader shader = mode.equalsIgnoreCase("shaderoutline")
                ? OutlineShader.OUTLINE_SHADER : mode.equalsIgnoreCase("shaderglow")
                ? GlowShader.GLOW_SHADER : null;

        if (shader == null) return;

        shader.startDraw(event.getPartialTicks());

        try {
            final RenderManager renderManager = mc.getRenderManager();

            for (final TileEntity entity : mc.theWorld.loadedTileEntityList) {
                if (!(entity instanceof TileEntityChest))
                    continue;

                TileEntityRendererDispatcher.instance.renderTileEntityAt(
                        entity,
                        entity.getPos().getX() - renderManager.renderPosX,
                        entity.getPos().getY() - renderManager.renderPosY,
                        entity.getPos().getZ() - renderManager.renderPosZ,
                        event.getPartialTicks()
                );
            }

            for (final Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityMinecartChest))
                    continue;

                renderManager.renderEntityStatic(entity, event.getPartialTicks(), true);
            }
        } catch (final Exception ex) {
            ClientUtils.getLogger().error("An error occurred while rendering all storages for shader esp", ex);
        }

        shader.stopDraw(new Color(0, 66, 255), mode.equalsIgnoreCase("shaderglow") ? 2.5F : 1.5F, 1F);
    }
}

