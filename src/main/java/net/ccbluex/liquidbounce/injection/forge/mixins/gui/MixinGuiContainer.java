package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor;
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner;
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer;
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timing.TickTimer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GuiContainer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiContainer extends MixinGuiScreen {

    // Separate TickTimer instances to avoid timing conflicts
    @Unique
    final TickTimer tick0 = new TickTimer();
    @Unique
    final TickTimer tick1 = new TickTimer();
    @Unique
    final TickTimer tick2 = new TickTimer();

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

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawSlot(Slot slot, CallbackInfo ci) {
        // Instances
        final InventoryManager inventoryManager = InventoryManager.INSTANCE;
        final ChestStealer chestStealer = ChestStealer.INSTANCE;
        final InventoryCleaner inventoryCleaner = InventoryCleaner.INSTANCE;
        final AutoArmor autoArmor = AutoArmor.INSTANCE;
        final RenderUtils renderUtils = RenderUtils.INSTANCE;

        // Slot X/Y
        int x = slot.xDisplayPosition;
        int y = slot.yDisplayPosition;

        // ChestStealer Highlight Values
        int red0 = chestStealer.getBackgroundRed();
        int green0 = chestStealer.getBackgroundGreen();
        int blue0 = chestStealer.getBackgroundBlue();
        int alpha0 = chestStealer.getBackgroundAlpha();

        int borderRed0 = chestStealer.getBorderRed();
        int borderGreen0 = chestStealer.getBorderGreen();
        int borderBlue0 = chestStealer.getBorderBlue();
        int borderAlpha0 = chestStealer.getBorderAlpha();

        // InvCleaner & AutoArmor Highlight Values
        int red1 = inventoryManager.getBackgroundRedValue().get();
        int green1 = inventoryManager.getBackgroundGreenValue().get();
        int blue1 = inventoryManager.getBackgroundBlueValue().get();
        int alpha1 = inventoryManager.getBackgroundAlphaValue().get();

        int borderRed1 = inventoryManager.getBorderRed().get();
        int borderGreen1 = inventoryManager.getBorderGreen().get();
        int borderBlue1 = inventoryManager.getBorderBlue().get();
        int borderAlpha1 = inventoryManager.getBorderAlpha().get();

        // ChestStealer Highlight Colors
        int color0 = new Color(red0, green0, blue0, alpha0).getRGB();
        int border0 = new Color(borderRed0, borderGreen0, borderBlue0, borderAlpha0).getRGB();

        // InvCleaner & AutoArmor Highlight Colors
        int color1 = new Color(red1, green1, blue1, alpha1).getRGB();
        int border1 = new Color(borderRed1, borderGreen1, borderBlue1, borderAlpha1).getRGB();

        // Get the current slot being stolen
        int currentSlotChestStealer = inventoryManager.getChestStealerCurrentSlot();
        int currentSlotInvCleaner = inventoryManager.getInvCleanerCurrentSlot();
        int currentSlotAutoArmor = inventoryManager.getAutoArmorCurrentSlot();

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);

        if (mc.currentScreen instanceof GuiChest) {
            if (chestStealer.handleEvents() && !chestStealer.getSilentGUI() && chestStealer.getHighlightSlot()) {
                if (slot.slotNumber == currentSlotChestStealer && currentSlotChestStealer != -1 && currentSlotChestStealer != inventoryManager.getChestStealerLastSlot()) {
                    renderUtils.drawBorderedRect(x, y, x + 16, y + 16, chestStealer.getBorderStrength(), border0, color0);

                    // Prevent rendering the highlighted rectangle twice
                    if (!slot.getHasStack() && tick0.hasTimePassed(100)) {
                        inventoryManager.setChestStealerLastSlot(currentSlotChestStealer);
                        tick0.reset();
                    } else {
                        tick0.update();
                    }
                }
            }
        }

        if (mc.currentScreen instanceof GuiInventory) {
            if (inventoryManager.getHighlightSlotValue().get()) {
                if (inventoryCleaner.handleEvents()) {
                    if (slot.slotNumber == currentSlotInvCleaner && currentSlotInvCleaner != -1 && currentSlotInvCleaner != inventoryManager.getInvCleanerLastSlot()) {
                        renderUtils.drawBorderedRect(x, y, x + 16, y + 16, inventoryManager.getBorderStrength().get(), border1, color1);

                        // Prevent rendering the highlighted rectangle twice
                        if (!slot.getHasStack() && tick1.hasTimePassed(100)) {
                            inventoryManager.setInvCleanerLastSlot(currentSlotInvCleaner);
                            tick1.reset();
                        } else {
                            tick1.update();
                        }
                    }
                }

                if (autoArmor.handleEvents()) {
                    if (slot.slotNumber == currentSlotAutoArmor && currentSlotAutoArmor != -1 && currentSlotAutoArmor != inventoryManager.getAutoArmorLastSlot()) {
                        renderUtils.drawBorderedRect(x, y, x + 16, y + 16, inventoryManager.getBorderStrength().get(), border1, color1);

                        // Prevent rendering the highlighted rectangle twice
                        if (!slot.getHasStack() && tick2.hasTimePassed(100)) {
                            inventoryManager.setAutoArmorLastSlot(currentSlotAutoArmor);
                            tick2.reset();
                        } else {
                            tick2.update();
                        }
                    }
                }
            }
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
