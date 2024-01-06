/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.common.SidebarEntry;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleScoreboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Final
    @Shadow
    private static Identifier PUMPKIN_BLUR;

    @Final
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Shadow
    @Final
    private static String SCOREBOARD_JOINER;

    @Shadow
    private int scaledHeight;

    @Shadow
    private int scaledWidth;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private static Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR;

    /**
     * Hook render hud event at the top layer
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;)V", shift = At.Shift.AFTER))
    private void hookRenderEvent(DrawContext context, float tickDelta, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new OverlayRenderEvent(context, tickDelta));
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(DrawContext context, Identifier texture, float opacity, CallbackInfo callback) {
        ModuleAntiBlind module = ModuleAntiBlind.INSTANCE;
        if (!module.getEnabled()) {
            return;
        }

        if (module.getPumpkinBlur() && PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
            return;
        }

        if (module.getPowerSnowFog() && POWDER_SNOW_OUTLINE.equals(texture)) {
            callback.cancel();
        }
    }

    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"))
    private boolean hookFreeCamRenderCrosshairInThirdPerson(Perspective instance) {
        return ModuleFreeCam.INSTANCE.shouldRenderCrosshair(instance.isFirstPerson());
    }


    /**
     * Renders the scoreboard sidebar on the screen.
     *
     * @param context The draw context.
     * @param objective The scoreboard objective.
     *
     * @author 1zuna
     * @reason Scoreboard module
     */
    @Overwrite
    private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective) {
        if (ModuleScoreboard.INSTANCE.getEnabled() && ModuleScoreboard.INSTANCE.getTurnOff()) {
            return;
        }

        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);

        SidebarEntry[] sidebarEntrys = scoreboard.getScoreboardEntries(objective)
                .stream()
                .filter(score -> !score.hidden())
                .sorted(SCOREBOARD_ENTRY_COMPARATOR)
                .limit(15L)
                .map(scoreboardEntry -> {
                    Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
                    Text textxx = scoreboardEntry.name();
                    Text text2 = Team.decorateName(team, textxx);
                    Text text3 = scoreboardEntry.formatted(numberFormat);
                    int ixx = this.getTextRenderer().getWidth(text3);
                    return new SidebarEntry(text2, text3, ixx);
                })
                .toArray(SidebarEntry[]::new);

        Text text = objective.getDisplayName();
        int i = this.getTextRenderer().getWidth(text);
        int j = i;
        int k = this.getTextRenderer().getWidth(": ");

        for(SidebarEntry sidebarEntry : sidebarEntrys) {
            j = Math.max(j, this.getTextRenderer().getWidth(sidebarEntry.name()) + (sidebarEntry.scoreWidth() > 0 ? k + sidebarEntry.scoreWidth() : 0));
        }

        int widthOfScoreboard = j;
        context.draw(() -> {
            int length = sidebarEntrys.length;
            int heightOfScoreboard = length * 9;
            int scoreboardPositionY = this.scaledHeight / 2 + heightOfScoreboard / 3;
            int scoreboardPositionX = this.scaledWidth - widthOfScoreboard - 3;

            int p = this.scaledWidth - 3 + 2;
            if (ModuleScoreboard.INSTANCE.getEnabled()) {
                final var alignment = ModuleScoreboard.INSTANCE.getAlignment().getBounds(widthOfScoreboard,
                        heightOfScoreboard / 3f);
                scoreboardPositionX = (int) alignment.getXMin();
                scoreboardPositionY = (int) alignment.getYMin();
                p = (int) alignment.getXMax() + 1;
            } else {
                // By default, we add 60 to the scoreboard position, this makes the arraylist more readable
                scoreboardPositionY += 60;
            }

            int bgColor = this.client.options.getTextBackgroundColor(0.3F);
            int secondBgColor = this.client.options.getTextBackgroundColor(0.4F);
            int s = scoreboardPositionY - length * 9;

            context.fill(scoreboardPositionX - 2, s - 9 - 1, p, s - 1, secondBgColor);
            context.fill(scoreboardPositionX - 2, s - 1, p, scoreboardPositionY, bgColor);
            context.drawText(this.getTextRenderer(), text, scoreboardPositionX + widthOfScoreboard / 2 - i / 2, s - 9,
                    Colors.WHITE, false);

            for(int t = 0; t < length; ++t) {
                SidebarEntry sidebarEntryxx = sidebarEntrys[t];
                int u = scoreboardPositionY - (length - t) * 9;
                context.drawText(this.getTextRenderer(), sidebarEntryxx.name(), scoreboardPositionX, u, Colors.WHITE, false);
                context.drawText(this.getTextRenderer(), sidebarEntryxx.score(), p - sidebarEntryxx.scoreWidth(), u,
                        Colors.WHITE, false);
            }
        });
    }

}
