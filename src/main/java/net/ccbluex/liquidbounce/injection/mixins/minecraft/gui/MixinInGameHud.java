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
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.OverlayMessageEvent;
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Final
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE;

    @Shadow
    @Nullable
    protected abstract PlayerEntity getCameraPlayer();

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    protected abstract void renderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed);

    /**
     * Hook render hud event at the top layer
     */
    @Inject(method = "renderMainHud", at = @At("HEAD"))
    private void hookRenderEventStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        EventManager.INSTANCE.callEvent(new OverlayRenderEvent(context, tickCounter.getTickProgress(false)));

        // Draw after overlay event
        var component = HudComponentManager.getComponentWithTweak(HudComponentTweak.TWEAK_HOTBAR);
        if (component != null && component.getRunning() &&
                client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            drawHotbar(context, tickCounter, component);
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderSpyglassOverlay(DrawContext context, float scale, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.SPYGLASS_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(DrawContext context, Identifier texture, float opacity, CallbackInfo callback) {
        if (!ModuleAntiBlind.INSTANCE.getRunning()) {
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.PUMPKIN_BLUR) && ModuleAntiBlind.TEXTURE_PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.POWDER_SNOW_FOG) && POWDER_SNOW_OUTLINE.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamRenderCrosshairInThirdPerson(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if ((ModuleFreeCam.INSTANCE.getRunning() && ModuleFreeCam.INSTANCE.shouldDisableCameraInteract())
                || HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_CROSSHAIR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderPortalOverlay(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.PORTAL_OVERLAY)) {
            ci.cancel();
        }
    }


    @Inject(method = "renderScoreboardSidebar*", at = @At("HEAD"), cancellable = true)
    private void renderScoreboardSidebar(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_SCOREBOARD)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void hookRenderHotbar(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.TWEAK_HOTBAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusBars(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_STATUS_BAR)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "getCurrentBarType", at = @At("RETURN"))
    private InGameHud.BarType tweakExpBar(InGameHud.BarType original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR) && original == InGameHud.BarType.EXPERIENCE) {
            return InGameHud.BarType.EMPTY;
        }
        return original;
    }

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;hasExperienceBar()Z"))
    private boolean tweakExpLevelText(ClientPlayerInteractionManager instance, Operation<Boolean> original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR)) {
            return false;
        }
        return original.call(instance);
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void hookRenderHeldItemTooltip(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_HELD_ITEM_TOOL_TIP)) {
            ci.cancel();
        }
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void hookSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new OverlayMessageEvent(message, tinted));

        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_OVERLAY_MESSAGE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusEffectOverlay(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_STATUS_EFFECT_OVERLAY)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean hookOffhandItem(boolean original) {
        return original || ModuleSwordBlock.INSTANCE.shouldHideOffhand() && ModuleSwordBlock.INSTANCE.getHideShieldSlot();
    }

    @Unique
    private void drawHotbar(DrawContext context, RenderTickCounter tickCounter, HudComponent hudComponent) {
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
            this.renderHotbarItem(context, (int) x, (int) y, tickCounter, playerEntity,
                    playerEntity.getInventory().getMainStacks().get(m), l++);
        }

        var offHandStack = playerEntity.getOffHandStack();
        if (!hookOffhandItem(offHandStack.isEmpty())) {
            this.renderHotbarItem(context, center - offset - 32, (int) y, tickCounter, playerEntity, offHandStack, l++);
        }
    }

    @ModifyExpressionValue(method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"
            )
    )
    private Perspective hookPerspectiveEventOnCrosshair(Perspective original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyExpressionValue(method = "renderMiscOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"
            )
    )
    private Perspective hookPerspectiveEventOnMiscOverlays(Perspective original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true)
    private void hookRenderTitleAndSubtitle(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.TITLE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNauseaOverlay(DrawContext context, float distortionStrength, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.NAUSEA)) {
            ci.cancel();
        }
    }

}
