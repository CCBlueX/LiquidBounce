package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiSlider.class)
public class MixinGuiSlider {

    @Redirect(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiSlider;drawTexturedModalRect(IIIIII)V"), require = 2)
    public void redirectedDrawRect(GuiSlider guiSlider, int x, int y, int textureX, int textureY, int width, int height) {
        Gui.drawRect(x, y - 2, x + width, y + height + 2, 0xFF4751C0);
    }

}
