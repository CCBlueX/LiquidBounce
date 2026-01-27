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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.OverlayMessageEvent;
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.ModuleCrosshair;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Final
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

    @Shadow
    @Nullable
    protected abstract Player getCameraPlayer();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void renderSlot(GuiGraphics context, int x, int y, DeltaTracker tickCounter, Player player, ItemStack stack, int seed);

    /**
     * Hook render hud event at the top layer
     */
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
    private void hookRenderEventStart(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        EventManager.INSTANCE.callEvent(new OverlayRenderEvent(context, tickCounter.getGameTimeDeltaPartialTick(false)));

        // Draw after overlay event
        var component = HudComponentManager.getComponentWithTweak(HudComponentTweak.TWEAK_HOTBAR);
        if (component != null && component.getRunning() &&
                minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            drawHotbar(context, tickCounter, component);
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderSpyglassOverlay(GuiGraphics context, float scale, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.SPYGLASS_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(GuiGraphics context, Identifier texture, float opacity, CallbackInfo callback) {
        if (!ModuleAntiBlind.INSTANCE.getRunning()) {
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.PUMPKIN_BLUR) && ModuleAntiBlind.TEXTURE_PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.POWDER_SNOW_FOG) && POWDER_SNOW_OUTLINE_LOCATION.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamRenderCrosshairInThirdPerson(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if ((ModuleFreeCam.INSTANCE.getRunning() && ModuleFreeCam.INSTANCE.shouldDisableCameraInteract())
                || HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_CROSSHAIR) || ModuleCrosshair.INSTANCE.getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderPortalOverlay(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.PORTAL_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void renderScoreboardSidebar(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_SCOREBOARD)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void hookRenderHotbar(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.TWEAK_HOTBAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusBars(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_STATUS_BAR)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "nextContextualInfoState", at = @At("RETURN"))
    private Gui.ContextualInfo tweakExpBar(Gui.ContextualInfo original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR) && original == Gui.ContextualInfo.EXPERIENCE) {
            return Gui.ContextualInfo.EMPTY;
        }
        return original;
    }

    @WrapOperation(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"))
    private boolean tweakExpLevelText(MultiPlayerGameMode instance, Operation<Boolean> original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR)) {
            return false;
        }
        return original.call(instance);
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void hookRenderHeldItemTooltip(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_HELD_ITEM_TOOL_TIP)) {
            ci.cancel();
        }
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void hookSetOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new OverlayMessageEvent(message, tinted));

        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_OVERLAY_MESSAGE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusEffectOverlay(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_STATUS_EFFECT_OVERLAY)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean hookOffhandItem(boolean original) {
        return original || ModuleSwordBlock.INSTANCE.shouldHideOffhand() && ModuleSwordBlock.INSTANCE.getHideShieldSlot();
    }

    @Unique
    private void drawHotbar(GuiGraphics context, DeltaTracker tickCounter, HudComponent hudComponent) {
        var playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }

        var itemWidth = 22.5;
        var offset = 98;
        var bounds = hudComponent.getAlignment().getBounds(0, 0);

        int center = (int) bounds.xMin();
        var y = bounds.yMin() - 12;

        int l = 1;
        for (int m = 0; m < 9; ++m) {
            var x = center - offset + m * itemWidth;
            this.renderSlot(context, (int) x, (int) y, tickCounter, playerEntity,
                    playerEntity.getInventory().getNonEquipmentItems().get(m), l++);
        }

        var offHandStack = playerEntity.getOffhandItem();
        if (!hookOffhandItem(offHandStack.isEmpty())) {
            this.renderSlot(context, center - offset - 32, (int) y, tickCounter, playerEntity, offHandStack, l);
        }
    }

    @ModifyExpressionValue(method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnCrosshair(CameraType original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyExpressionValue(method = "renderCameraOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnMiscOverlays(CameraType original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void hookRenderTitleAndSubtitle(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.TITLE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNauseaOverlay(GuiGraphics context, float distortionStrength, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.NAUSEA)) {
            ci.cancel();
        }
    }

    @ModifyReceiver(
        method = "renderCrosshair",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/AttackRange;isInRange(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Z"
        )
    )
    private AttackRange injectReachAttackRange(AttackRange instance, LivingEntity entity, Vec3 pos) {
        if (ModuleReach.INSTANCE.getRunning()) {
            return ModuleReach.INSTANCE.getEntity().adjustAttackRange(instance);
        }

        return instance;
    }

}
