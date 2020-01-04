package net.ccbluex.liquidbounce.ui.client.hud.element.elements;

import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Armor")
public class Armor extends Element {

    @Override
    public void drawElement() {
        final int[] location = getLocationFromFacing();

        if(mc.playerController.isNotCreative()) {
            int x = location[0];
            GL11.glPushMatrix();

            for(int index = 3; index >= 0; --index) {
                final ItemStack stack = mc.thePlayer.inventory.armorInventory[index];

                if(stack != null) {
                    mc.getRenderItem().renderItemIntoGUI(stack, x, location[1] - (mc.thePlayer.isInsideOfMaterial(Material.water) ? 10 : 0));
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x, location[1] - (mc.thePlayer.isInsideOfMaterial(Material.water) ? 10 : 0));
                    x += 18;
                }
            }

            GlStateManager.disableCull();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GL11.glPopMatrix();
        }

        if (mc.currentScreen instanceof GuiHudDesigner)
            RenderUtils.drawBorderedRect(location[0], location[1], location[0] + 72, location[1] + 17, 3, Integer.MIN_VALUE, 0);
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

        return mouseX >= location[0] && mouseY >= location[1] && mouseX <= location[0] + 72 && mouseY <= location[1] + 10;
    }
}