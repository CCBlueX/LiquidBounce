/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleItemScroller;
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinMouseAccessor;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Util;
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

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends MixinScreen {

    @Shadow
    @Final
    protected T handler;

    @Shadow
    @Nullable
    protected abstract Slot getSlotAt(double mouseX, double mouseY);

    @Shadow
    private ItemStack quickMovingStack;

    @Shadow
    protected abstract void onMouseClick(Slot slot, int id, int button, SlotActionType actionType);

    @Shadow
    private boolean cancelNextRelease;

    @Shadow
    private @Nullable Slot lastClickedSlot;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void cancelMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        var inventoryMove = ModuleInventoryMove.INSTANCE;
        if ((HandledScreen<?>) (Object) this instanceof InventoryScreen && inventoryMove.getRunning() && inventoryMove.getDoNotAllowClicking()) {
            ci.cancel();
        }

        if (FeatureSilentScreen.getShouldHide()) {
            ci.cancel();
        }
    }

    // Before `if (itemStack.isEmpty() && slot.isEnabled()) {`
    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 5))
    private void drawSlotOutline(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ModuleBetterInventory.INSTANCE.drawHighlightSlot(context, slot);
    }

    @Inject(method = "renderMain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.AFTER))
    private void hookDrawSlot(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var cursorStack = this.handler.getCursorStack();
        var slot = getSlotAt(mouseX, mouseY);

        if (!cursorStack.isEmpty() || slot == null) {
            return;
        }

        var stack = slot.getStack();
        if (!ModuleBetterInventory.INSTANCE.drawContainerItemView(context, cursorStack, this.x, this.y, mouseX, mouseY)) {
            ModuleBetterInventory.INSTANCE.drawContainerItemView(context, stack, this.x, this.y, mouseX, mouseY);
        }

        if (matchingItemScrollerMoveConditions(mouseX, mouseY)) {
            this.quickMovingStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();

            ModuleItemScroller.getClickMode().getAction().invoke(this.handler, slot, this::onMouseClick);

            this.cancelNextRelease = true;

            this.lastClickedSlot = slot;
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
            var mouse = (MixinMouseAccessor) this.client.mouse;
            mouse.setLastMouseClick(new Mouse.MouseClickTime(Util.getMeasuringTimeMs(), (Screen) (Object) this));
            mouse.setLastMouseButton(GLFW.GLFW_MOUSE_BUTTON_1);

            ModuleItemScroller.INSTANCE.resetChronometer();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void hookMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        /*
         * We move the item by itself, we don't need this action by Minecraft
         */
        if (matchingItemScrollerMoveConditions(click.x(), click.y())) {
            cir.cancel();
        }
    }

    @Unique
    private boolean matchingItemScrollerMoveConditions(double mouseX, double mouseY) {
        return getSlotAt(mouseX, mouseY) != null
            && ModuleItemScroller.INSTANCE.canPerformScroll(this.client.getWindow());
    }

}
