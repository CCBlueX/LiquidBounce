package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "HUD", description = "Toggles visibility of the HUD.", category = ModuleCategory.RENDER)
@SideOnly(Side.CLIENT)
public class HUD extends Module {

    public final BoolValue blackHotbarValue = new BoolValue("BlackHotbar", true);
    public final BoolValue inventoryParticle = new BoolValue("InventoryParticle", false);
    private final BoolValue blurValue = new BoolValue("Blur", false);
    public final BoolValue fontChatValue = new BoolValue("FontChat", false);

    public HUD() {
        setState(true);
        setArray(false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if(LiquidBounce.CLIENT.hud == null || mc.currentScreen instanceof GuiHudDesigner)
            return;

        LiquidBounce.CLIENT.hud.render(false);
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        LiquidBounce.CLIENT.hud.update();
    }

    @EventTarget
    public void onKey(final KeyEvent event) {
        LiquidBounce.CLIENT.hud.handleKey('a', event.getKey());
    }

    @EventTarget(ignoreCondition = true)
    public void onScreenChange(ScreenEvent event) {
        if(mc.theWorld == null || mc.thePlayer == null)
            return;

        if(getState() && event.getGuiScreen() != null && !(event.getGuiScreen() instanceof GuiChat || event.getGuiScreen() instanceof GuiHudDesigner) && blurValue.get())
            mc.entityRenderer.loadShader(new ResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/blur.json"));
        else
            mc.entityRenderer.stopUseShader();
    }
}
