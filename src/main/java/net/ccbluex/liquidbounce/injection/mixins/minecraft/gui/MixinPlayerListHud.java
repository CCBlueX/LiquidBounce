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
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.ccbluex.liquidbounce.features.misc.FriendManager;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiStaff;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterTab;
import net.ccbluex.liquidbounce.features.module.modules.misc.Visibility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.MathHelper;
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

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @WrapOperation(method = "collectPlayerEntries", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;"))
    private Stream<PlayerListEntry> injectTabSize(Stream<PlayerListEntry> instance, long l, Operation<Stream<PlayerListEntry>> original) {
        long size = ModuleBetterTab.INSTANCE.getRunning() ? ModuleBetterTab.Limits.INSTANCE.getTabSize() : l;
        return original.call(instance, size);
    }

    @WrapOperation(method = "collectPlayerEntries", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"))
    private Stream<PlayerListEntry> hookSort(Stream<PlayerListEntry> instance, Comparator<PlayerListEntry> defaultComparator, Operation<Stream<PlayerListEntry>> original) {
        var betterTab = ModuleBetterTab.INSTANCE;

        var running = betterTab.getRunning();
        var customComparator = betterTab.getSorting().getComparator();

        var comparator = running ? (customComparator != null ? customComparator : defaultComparator) : defaultComparator;

        var playerHider = ModuleBetterTab.PlayerHider.INSTANCE;
        var hidden = running && playerHider.getRunning() ? instance.filter(player -> !playerHider.getFilter().isInFilter(player)) : instance;

        return original.call(hidden, comparator);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;header:Lnet/minecraft/text/Text;", ordinal = 0))
    private Text hookHeader(Text original) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.HEADER) ? original : null;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;footer:Lnet/minecraft/text/Text;", ordinal = 0))
    private Text hookFooter(Text original) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.FOOTER) ? original : null;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/PlayerListHud$ScoreDisplayEntry;name:Lnet/minecraft/text/Text;"))
    private Text hookVisibilityName(Text original, @Local(ordinal = 0) PlayerListEntry entry) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.NAME_ONLY) ? Text.of(entry.getProfile().getName()) : original;

    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;"))
    private Text hookWidthVisibilityName(Text original, @Local(ordinal = 0) PlayerListEntry entry) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.isVisible(Visibility.NAME_ONLY) ? Text.of(entry.getProfile().getName()) : original;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void hookTabColumnHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return;
        }

        int playerCount = collectPlayerEntries().size();
        int height = Math.max(1, ModuleBetterTab.Limits.INSTANCE.getHeight());
        int columns = Math.max(1, MathHelper.ceil((double) playerCount / height));
        int rows = MathHelper.ceil((double) playerCount / columns);
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
    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void hookOnRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        var accurateLatency = ModuleBetterTab.AccurateLatency.INSTANCE;
        if (ModuleBetterTab.INSTANCE.getRunning() && accurateLatency.getRunning()) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            int latency = MathHelper.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0x00E970 : latency < 300 ? 0xE7D020 : 0xD74238;
            String text = latency + (accurateLatency.getSuffix() ? "ms" : "");
            context.drawTextWithShadow(textRenderer, text, x + width - textRenderer.getWidth(text), y, color);
            ci.cancel();
        }
    }

    // ModifyArg breaks lunar compatibility as of 17.1.2025 (minecraft 1.21.4); that's why WrapOperation is used
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 2))
    private void hookRenderPlayerBackground(DrawContext instance, int x1, int y1, int x2, int y2, int color, Operation<Void> original, @Local(ordinal = 13) int w, @Local(ordinal = 0) List<PlayerListEntry> entries) {
        var drawColor = color;

        var highlight = ModuleBetterTab.Highlight.INSTANCE;
        if (ModuleBetterTab.INSTANCE.getRunning() && highlight.getRunning() && w < entries.size()) {
            var entry = entries.get(w);
            var others = highlight.getOthers();

            //noinspection DataFlowIssue
            if (highlight.getSelf().getRunning() && Objects.equals(entry.getProfile().getName(), MinecraftClient.getInstance().player.getGameProfile().getName())) {
                drawColor = highlight.getSelf().getColor().toARGB();
            } else if (highlight.getFriends().getRunning() && FriendManager.INSTANCE.isFriend(entry.getProfile().getName())) {
                drawColor = highlight.getFriends().getColor().toARGB();
            } else if (others.getRunning() && others.getFilter().isInFilter(entry)) {
                drawColor = others.getColor().toARGB();
            }
        }

        original.call(instance, x1, y1, x2, y2, drawColor);
    }

    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text modifyPlayerName(Text original, PlayerListEntry entry) {
        if (ModuleAntiStaff.INSTANCE.shouldShowAsStaffOnTab(entry.getProfile().getName())) {
            return original.copy().append(Text.literal(" - (Staff)").withColor(Colors.LIGHT_RED));
        }

        return original;
    }

}
