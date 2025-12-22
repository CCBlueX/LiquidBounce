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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import static net.ccbluex.liquidbounce.utils.client.ProtocolUtilKt.getUsesViaFabricPlus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager;
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings;
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Modify window title to our client title.
 * Example: LiquidBounce v1.0.0 | 1.16.3
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftWindowTitle {

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @WrapOperation(
        method = "createTitle",
        at = @At(
            value = "NEW",
            target = "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
        )
    )
    private StringBuilder injectCreateTitle(String str, Operation<StringBuilder> original) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return original.call(str);
        }

        StringBuilder titleBuilder = new StringBuilder(LiquidBounce.CLIENT_NAME);
        titleBuilder.append(" v");
        titleBuilder.append(LiquidBounce.INSTANCE.getClientVersion());
        titleBuilder.append(LiquidBounce.IN_DEVELOPMENT ? " (dev) " : " ");
        titleBuilder.append(LiquidBounce.INSTANCE.getClientCommit());

        return titleBuilder;
    }

    @ModifyExpressionValue(
        method = "createTitle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ModCheck;shouldReportAsModified()Z"
        )
    )
    private boolean injectShouldReportAsModified(boolean original) {
        return HideAppearance.INSTANCE.isHidingNow() && original;
    }

    @ModifyArg(
        method = "createTitle",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            ordinal = 1,
            remap = false
        )
    )
    private String injectSpaceBeforeVersion(String str) {
        return HideAppearance.INSTANCE.isHidingNow() ? str : " | ";
    }

    @ModifyExpressionValue(
        method = "createTitle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/WorldVersion;name()Ljava/lang/String;"
        )
    )
    private String injectProtocolVersion(String original) {
        // ViaFabricPlus compatibility
        if (!HideAppearance.INSTANCE.isHidingNow() && getUsesViaFabricPlus()) {
            var protocolVersion = VfpCompatibility.INSTANCE.unsafeGetProtocolVersion();

            if (protocolVersion != null) {
                return protocolVersion.getName();
            } else {
                return original;
            }
        } else {
            return original;
        }
    }

    @Inject(
        method = "createTitle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getConnection()Lnet/minecraft/client/multiplayer/ClientPacketListener;"
        )
    )
    private void injectAppendExtraParts(CallbackInfoReturnable<String> cir, @Local StringBuilder titleBuilder) {
        // For debugging purposes, will be removed until we have a stable release
        if (!HideAppearance.INSTANCE.isHidingNow() && Util.getPlatform() == Util.OS.WINDOWS) {
            if (BrowserBackendManager.INSTANCE.getBrowserBackend().isInitialized() &&
                BrowserBackendManager.INSTANCE.getBrowserBackend().isAccelerationSupported()) {
                var accelerated = GlobalBrowserSettings.INSTANCE.getAccelerated();

                if (accelerated != null && accelerated.get()) {
                    titleBuilder.append(" | (UI Renderer Acceleration is ON");
                    // Hotkey only works when not in-game
                    if (this.level == null && this.player == null) {
                        titleBuilder.append(" - Toggle with F12");
                    }
                    titleBuilder.append(")");
                }
            }
        }
    }

}
