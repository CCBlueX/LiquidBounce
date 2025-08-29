package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleItemScroller;
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory;
import net.minecraft.client.gui.DrawContext;
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
    private int lastClickedButton;

    @Shadow
    private long lastButtonClickTime;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void cancelMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        var inventoryMove = ModuleInventoryMove.INSTANCE;
        if ((Object) this instanceof InventoryScreen && inventoryMove.getRunning() && inventoryMove.getDoNotAllowClicking()) {
            ci.cancel();
        }

        if (FeatureSilentScreen.getShouldHide()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelRenderByChestStealer(CallbackInfo ci) {
        if (FeatureSilentScreen.getShouldHide()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", shift = At.Shift.AFTER))
    private void drawSlotOutline(DrawContext context, Slot slot, CallbackInfo ci) {
        ModuleBetterInventory.INSTANCE.drawHighlightSlot(context, slot);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;)V", shift = At.Shift.AFTER))
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
            this.lastButtonClickTime = Util.getMeasuringTimeMs();
            this.lastClickedButton = GLFW.GLFW_MOUSE_BUTTON_1;

            ModuleItemScroller.INSTANCE.resetChronometer();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void hookMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        /*
         * We move the item by itself, we don't need this action by Minecraft
         */
        if (matchingItemScrollerMoveConditions((int) mouseX, (int) mouseY)) {
            cir.cancel();
        }
    }

    @Unique
    private boolean matchingItemScrollerMoveConditions(int mouseX, int mouseY) {
        long handle = this.client.getWindow().getHandle();
        return getSlotAt(mouseX, mouseY) != null && ModuleItemScroller.INSTANCE.canPerformScroll(handle);
    }

}
