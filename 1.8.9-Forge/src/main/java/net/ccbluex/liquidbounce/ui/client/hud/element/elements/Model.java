package net.ccbluex.liquidbounce.ui.client.hud.element.elements;

import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Model")
public class Model extends Element {

    private float rotate;
    private boolean rotateDirection;

    @Override
    public void drawElement() {
        final int delta = RenderUtils.deltaTime;

        if(rotateDirection) {
            if(rotate <= 70) {
                rotate += 0.12F * delta;
            }else{
                rotateDirection = false;
                rotate = 70;
            }
        }else{
            if(rotate >= -70) {
                rotate -= 0.12F * delta;
            }else{
                rotateDirection = true;
                rotate = -70;
            }
        }

        final int[] location = getLocationFromFacing();

        final EntityPlayerSP entityPlayerSP = mc.thePlayer;

        float pitch = entityPlayerSP.rotationPitch;

        if(pitch > 0)
            pitch = -entityPlayerSP.rotationPitch;
        else
            pitch = Math.abs(entityPlayerSP.rotationPitch);

        drawEntityOnScreen(location[0], location[1], rotate, pitch, entityPlayerSP);

        if (mc.currentScreen instanceof GuiHudDesigner)
            RenderUtils.drawBorderedRect(location[0] + 30, location[1] + 10, location[0] - 30, location[1] - 100, 3, Integer.MIN_VALUE, 0);
    }

    @Override
    public void destroyElement() {

    }

    @Override
    public void updateElement() {

    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int mouseButton) {

    }

    @Override
    public void handleKey(char c, int keyCode) {

    }

    @Override
    public boolean isMouseOverElement(int mouseX, int mouseY) {
        final int[] location = getLocationFromFacing();

        return mouseX >= location[0] - 30 && mouseY >= location[1] - 100 && mouseX <= location[0] + 30 && mouseY <= location[1] + 10;
    }

    private void drawEntityOnScreen(int x, int y, float yaw, float pitch, EntityLivingBase entityLivingBase) {
        GlStateManager.resetColor();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 50.0F);
        GlStateManager.scale((float) (-50), (float) 50, (float) 50);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float var6 = entityLivingBase.renderYawOffset;
        float var7 = entityLivingBase.rotationYaw;
        float var8 = entityLivingBase.rotationPitch;
        float var9 = entityLivingBase.prevRotationYawHead;
        float var10 = entityLivingBase.rotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan(pitch / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        entityLivingBase.renderYawOffset = (float) Math.atan(yaw / 40.0F) * 20.0F;
        entityLivingBase.rotationYaw = (float) Math.atan(yaw / 40.0F) * 40.0F;
        entityLivingBase.rotationPitch = -((float) Math.atan(pitch / 40.0F)) * 20.0F;
        entityLivingBase.rotationYawHead = entityLivingBase.rotationYaw;
        entityLivingBase.prevRotationYawHead = entityLivingBase.rotationYaw;
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager var11 = mc.getRenderManager();
        var11.setPlayerViewY(180.0F);
        var11.setRenderShadow(false);
        var11.renderEntityWithPosYaw(entityLivingBase, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        var11.setRenderShadow(true);
        entityLivingBase.renderYawOffset = var6;
        entityLivingBase.rotationYaw = var7;
        entityLivingBase.rotationPitch = var8;
        entityLivingBase.prevRotationYawHead = var9;
        entityLivingBase.rotationYawHead = var10;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.resetColor();
    }
}
