/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;

import java.awt.*;
import java.util.LinkedList;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "Breadcrumbs", description = "Leaves a trail behind you.", category = ModuleCategory.RENDER)
public class Breadcrumbs extends Module {
    public final IntegerValue colorRedValue = new IntegerValue("R", 255, 0, 255);
    public final IntegerValue colorGreenValue = new IntegerValue("G", 179, 0, 255);
    public final IntegerValue colorBlueValue = new IntegerValue("B", 72, 0, 255);
    public final BoolValue colorRainbow = new BoolValue("Rainbow", false);
    private final LinkedList<double[]> positions = new LinkedList<>();

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        final Color color = colorRainbow.get() ? ColorUtils.rainbow() : new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get());

        synchronized (positions) {
            glPushMatrix();

            glDisable(GL_TEXTURE_2D);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_LINE_SMOOTH);
            glEnable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            mc.entityRenderer.disableLightmap();
            glBegin(GL_LINE_STRIP);
            RenderUtils.glColor(color);
            final double renderPosX = mc.getRenderManager().viewerPosX;
            final double renderPosY = mc.getRenderManager().viewerPosY;
            final double renderPosZ = mc.getRenderManager().viewerPosZ;

            for (final double[] pos : positions)
                glVertex3d(pos[0] - renderPosX, pos[1] - renderPosY, pos[2] - renderPosZ);

            glColor4d(1, 1, 1, 1);
            glEnd();
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_LINE_SMOOTH);
            glDisable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            glPopMatrix();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        synchronized (positions) {
            positions.add(new double[]{mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ});
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null)
            return;

        synchronized (positions) {
            positions.add(new double[]{mc.thePlayer.posX,
                    mc.thePlayer.getEntityBoundingBox().minY + (mc.thePlayer.getEyeHeight() * 0.5f),
                    mc.thePlayer.posZ});
            positions.add(new double[]{mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ});
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        synchronized (positions) {
            positions.clear();
        }
        super.onDisable();
    }
}
