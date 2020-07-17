package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.injection.implementations.IMixinGuiContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer implements IMixinGuiContainer {
    @Shadow
    protected abstract void handleMouseClick(Slot p_handleMouseClick_1_, int p_handleMouseClick_2_, int p_handleMouseClick_3_, ClickType p_handleMouseClick_4_);

    @Override
    public void publicHandleMouseClick(Slot slot, int slotNumber, int clickedButton, ClickType clickType) {
        this.handleMouseClick(slot, slotNumber, clickedButton, clickType);
    }
}
