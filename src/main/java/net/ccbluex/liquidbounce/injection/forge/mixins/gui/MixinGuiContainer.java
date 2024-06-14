package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiContainer extends MixinGuiScreen {

    @Inject(method = "initGui", at = @At("RETURN"), cancellable = true)
    private void init(CallbackInfo ci) {
        if (ChestStealer.INSTANCE.handleEvents() && ChestStealer.INSTANCE.getSilentGUI()) {
            if (mc.currentScreen instanceof GuiChest) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ChestStealer.INSTANCE.handleEvents() && ChestStealer.INSTANCE.getSilentGUI()) {
            if (mc.currentScreen instanceof GuiChest) {
                ci.cancel();
            }
        }
    }
}
