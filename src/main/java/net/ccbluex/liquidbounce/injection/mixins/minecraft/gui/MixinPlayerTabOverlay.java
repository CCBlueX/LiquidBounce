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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.ccbluex.liquidbounce.features.misc.FriendManager;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiStaff;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterTab;
import net.ccbluex.liquidbounce.features.module.modules.misc.Visibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    @Shadow
    protected abstract List<PlayerInfo> getPlayerInfos();

    @WrapOperation(method = "getPlayerInfos", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;"))
    private Stream<PlayerInfo> injectTabSize(Stream<PlayerInfo> instance, long l, Operation<Stream<PlayerInfo>> original) {
        long size = ModuleBetterTab.INSTANCE.getRunning() ? ModuleBetterTab.Limits.INSTANCE.getTabSize() : l;
        return original.call(instance, size);
    }

    @WrapOperation(method = "getPlayerInfos", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"))
    private Stream<PlayerInfo> hookSort(Stream<PlayerInfo> instance, Comparator<PlayerInfo> defaultComparator, Operation<Stream<PlayerInfo>> original) {
        var betterTab = ModuleBetterTab.INSTANCE;

        var running = betterTab.getRunning();
        var customComparator = betterTab.getSorting().getComparator();

        var comparator = running ? (customComparator != null ? customComparator : defaultComparator) : defaultComparator;

        var playerHider = ModuleBetterTab.PlayerHider.INSTANCE;
        var hidden = running && playerHider.getRunning() ? instance.filter(player -> !playerHider.getFilter().isInFilter(player)) : instance;

        return original.call(hidden, comparator);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;header:Lnet/minecraft/network/chat/Component;", ordinal = 0))
    private Component hookHeader(Component original) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.HEADER) ? original : null;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;footer:Lnet/minecraft/network/chat/Component;", ordinal = 0))
    private Component hookFooter(Component original) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.FOOTER) ? original : null;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay$ScoreDisplayEntry;name:Lnet/minecraft/network/chat/Component;"))
    private Component hookVisibilityName(Component original, @Local(ordinal = 0) PlayerInfo entry) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.NAME_ONLY) ? Component.nullToEmpty(entry.getProfile().name()) : original;

    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;"))
    private Component hookWidthVisibilityName(Component original, @Local(ordinal = 0) PlayerInfo entry) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.NAME_ONLY) ? Component.nullToEmpty(entry.getProfile().name()) : original;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void hookTabColumnHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return;
        }

        int playerCount = getPlayerInfos().size();
        int height = Math.max(1, ModuleBetterTab.Limits.INSTANCE.getHeight());
        int columns = Math.max(1, Mth.ceil((double) playerCount / height));
        int rows = Mth.ceil((double) playerCount / columns);
        o.set(rows);
        p.set(columns);
    }

    /**
     * @source <a href="https://github.com/MeteorDevelopment/meteor-client/blob/2025789457e5b4c0671f04f0d3c7e0d91a31765c/src/main/java/meteordevelopment/meteorclient/mixin/PlayerListHudMixin.java#L46-L51">code section</a>
     * @contributor sqlerrorthing (<a href="https://github.com/CCBlueX/LiquidBounce/pull/5077">pull request</a>)
     * @author Paul1365972 (on Meteor Client)
     */
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int hookWidth(int width) {
        return ModuleBetterTab.INSTANCE.getRunning() && ModuleBetterTab.AccurateLatency.INSTANCE.getRunning() ? width + 30 : width;
    }

    /**
     * @source <a href="https://github.com/MeteorDevelopment/meteor-client/blob/2025789457e5b4c0671f04f0d3c7e0d91a31765c/src/main/java/meteordevelopment/meteorclient/mixin/PlayerListHudMixin.java#L28">code section</a>
     * @contributor sqlerrorthing (<a href="https://github.com/CCBlueX/LiquidBounce/pull/5077">pull request</a>)
     * @author Paul1365972 (on Meteor Client)
     */
    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void hookOnRenderLatencyIcon(GuiGraphics context, int width, int x, int y, PlayerInfo entry, CallbackInfo ci) {
        var accurateLatency = ModuleBetterTab.AccurateLatency.INSTANCE;
        if (ModuleBetterTab.INSTANCE.getRunning() && accurateLatency.getRunning()) {
            var textRenderer = Minecraft.getInstance().font;

            var latency = Mth.clamp(entry.getLatency(), 0, 9999);
            var color = latency < 150 ? 0xFF00E970 : latency < 300 ? 0xFFE7D020 : 0xFFD74238;
            var text = latency + (accurateLatency.getSuffix() ? "ms" : "");
            context.drawString(textRenderer, text, x + width - textRenderer.width(text), y, color);
            ci.cancel();
        }
    }

    // ModifyArg breaks lunar compatibility as of 17.1.2025 (minecraft 1.21.4); that's why WrapOperation is used
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 2))
    private void hookRenderPlayerBackground(GuiGraphics instance, int x1, int y1, int x2, int y2, int color, Operation<Void> original, @Local(ordinal = 13) int w, @Local(ordinal = 0) List<PlayerInfo> entries) {
        var drawColor = color;

        var highlight = ModuleBetterTab.Highlight.INSTANCE;
        if (ModuleBetterTab.INSTANCE.getRunning() && highlight.getRunning() && w < entries.size()) {
            var entry = entries.get(w);
            var others = highlight.getOthers();

            //noinspection DataFlowIssue
            if (highlight.getSelf().getRunning() && Objects.equals(entry.getProfile().name(), Minecraft.getInstance().player.getGameProfile().name())) {
                drawColor = highlight.getSelf().getColor().argb();
            } else if (highlight.getFriends().getRunning() && FriendManager.INSTANCE.isFriend(entry.getProfile().name())) {
                drawColor = highlight.getFriends().getColor().argb();
            } else if (others.getRunning() && others.getFilter().isInFilter(entry)) {
                drawColor = others.getColor().argb();
            }
        }

        original.call(instance, x1, y1, x2, y2, drawColor);
    }

    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    private Component modifyPlayerName(Component original, PlayerInfo entry) {
        if (ModuleAntiStaff.INSTANCE.shouldShowAsStaffOnTab(entry.getProfile().name())) {
            return original.copy().append(Component.literal(" - (Staff)").withColor(CommonColors.SOFT_RED));
        }

        return original;
    }

}
