package net.ccbluex.liquidbounce.injection.fabric.mixins.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SliderWidget.class)
public class MixinSliderWidget {

    @Redirect(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/SliderWidget;drawTexturedModalRect(IIIIII)V"), require = 2)
    public void redirectedDrawRect(SliderWidget guiSlider, int x, int y, int textureX, int textureY, int width, int height) {
        Gui.drawRect(x, y - 2, x + width, y + height + 2, 0xFF4751C0);
    }

}
