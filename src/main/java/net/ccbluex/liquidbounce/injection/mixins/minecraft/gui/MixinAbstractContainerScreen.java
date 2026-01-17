/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleItemScroller;
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinMouseHandlerAccessor;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen<T extends AbstractContainerMenu> extends MixinScreen {

    @Shadow
    @Final
    protected T menu;

    @Shadow
    @Nullable
    protected abstract Slot getHoveredSlot(double mouseX, double mouseY);

    @Shadow
    private ItemStack lastQuickMoved;

    @Shadow
    protected abstract void slotClicked(Slot slot, int id, int button, ClickType actionType);

    @Shadow
    private boolean skipNextRelease;

    @Shadow
    private @Nullable Slot lastClickSlot;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", at = @At("HEAD"), cancellable = true)
    private void cancelMouseClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo ci) {
        var inventoryMove = ModuleInventoryMove.INSTANCE;
        if ((AbstractContainerScreen<?>) (Object) this instanceof InventoryScreen && inventoryMove.getRunning() && inventoryMove.getDoNotAllowClicking()) {
            ci.cancel();
        }

        if (FeatureSilentScreen.INSTANCE.getShouldHide()) {
            ci.cancel();
        }
    }

    // Before `if (itemStack.isEmpty() && slot.isEnabled()) {`
    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 5))
    private void drawSlotOutline(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ModuleBetterInventory.INSTANCE.drawHighlightSlot(context, slot);
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlots(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.AFTER))
    private void hookDrawSlot(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var cursorStack = this.menu.getCarried();
        var slot = getHoveredSlot(mouseX, mouseY);

        if (!cursorStack.isEmpty() || slot == null) {
            return;
        }

        var stack = slot.getItem();
        if (!ModuleBetterInventory.INSTANCE.drawContainerItemView(context, cursorStack, this.leftPos, this.topPos, mouseX, mouseY)) {
            ModuleBetterInventory.INSTANCE.drawContainerItemView(context, stack, this.leftPos, this.topPos, mouseX, mouseY);
        }

        if (matchingItemScrollerMoveConditions(mouseX, mouseY)) {
            this.lastQuickMoved = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();

            ModuleItemScroller.getClickMode().getAction().invoke(this.menu, slot, this::slotClicked);

            this.skipNextRelease = true;

            this.lastClickSlot = slot;
            // See Mouse.onMouseButton
            /*
            <pre>
             long l = Util.getMeasuringTimeMs();
             boolean bl2 = this.lastMouseClick != null
             && l - this.lastMouseClick.time() < 250L
             && this.lastMouseClick.screen() == screen
             && this.lastMouseButton == click.button();
             if (screen.mouseClicked(click, bl2)) {
             this.lastMouseClick = new Mouse.MouseClickTime(l, screen);
             this.lastMouseButton = mouseInput.button();
             return;
             }
             </pre>
             */
            var mouse = (MixinMouseHandlerAccessor) this.minecraft.mouseHandler;
            mouse.setLastClick(new MouseHandler.LastClick(Util.getMillis(), (Screen) (Object) this));
            mouse.setLastClickButton(GLFW.GLFW_MOUSE_BUTTON_1);

            ModuleItemScroller.INSTANCE.resetChronometer();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void hookMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        /*
         * We move the item by itself, we don't need this action by Minecraft
         */
        if (matchingItemScrollerMoveConditions(click.x(), click.y())) {
            cir.cancel();
        }
    }

    @Unique
    private boolean matchingItemScrollerMoveConditions(double mouseX, double mouseY) {
        return getHoveredSlot(mouseX, mouseY) != null
            && ModuleItemScroller.INSTANCE.canPerformScroll(this.minecraft.getWindow());
    }

}
