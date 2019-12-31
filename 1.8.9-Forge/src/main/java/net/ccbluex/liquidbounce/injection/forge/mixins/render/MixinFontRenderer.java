package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.TextEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@Mixin(FontRenderer.class)
@SideOnly(Side.CLIENT)
public class MixinFontRenderer {

    @ModifyVariable(method = "renderString", at = @At("HEAD"), ordinal = 0)
    private String renderString(final String string) {
        if(string == null || LiquidBounce.CLIENT.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        LiquidBounce.CLIENT.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), ordinal = 0)
    private String getStringWidth(final String string) {
        if(string == null || LiquidBounce.CLIENT.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        LiquidBounce.CLIENT.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }
}