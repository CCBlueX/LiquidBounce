/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
     * @author
     * @reason
     *
     * todo: use injection instead of overwrite
     */
    @Overwrite
    private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective) {
        if (ModuleScoreboard.INSTANCE.getEnabled() && ModuleScoreboard.INSTANCE.getTurnOff()) {
            return;
        }

        int i;
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<ScoreboardPlayerScore> collection = scoreboard.getAllPlayerScores(objective);
        List list = collection.stream().filter(score -> score.getPlayerName() != null &&
                !score.getPlayerName().startsWith("#")).collect(Collectors.toList());
        collection = list.size() > 15 ? Lists.newArrayList(Iterables.skip(list, collection.size() - 15))
                : list;
        ArrayList<Pair<ScoreboardPlayerScore, MutableText>> list2 = Lists.newArrayListWithCapacity(collection.size());
        Text text = objective.getDisplayName();

        final TextRenderer textRenderer = getTextRenderer();

        // Calculate width and height of scoreboard
        int widthOfScoreboard = i = textRenderer.getWidth(text);
        int joinerWidth = textRenderer.getWidth(SCOREBOARD_JOINER);
        for (ScoreboardPlayerScore scoreboardPlayerScore : collection) {
            final Team team = scoreboard.getPlayerTeam(scoreboardPlayerScore.getPlayerName());
            final MutableText text2 = Team.decorateName(team, Text.literal(scoreboardPlayerScore.getPlayerName()));
            list2.add(Pair.of(scoreboardPlayerScore, text2));
            widthOfScoreboard = Math.max(widthOfScoreboard, textRenderer.getWidth(text2) + joinerWidth +
                    textRenderer.getWidth(Integer.toString(scoreboardPlayerScore.getScore())));
        }
        int heightOfScoreboard = collection.size() * textRenderer.fontHeight;

        // Figure out where to render scoreboard
        int scoreboardPositionY = this.scaledHeight / 2 + heightOfScoreboard / 3;
        int scoreboardPositionX = this.scaledWidth - widthOfScoreboard - 3;

        if (ModuleScoreboard.INSTANCE.getEnabled()) {
            final var alignment = ModuleScoreboard.INSTANCE.getAlignment().getBounds(widthOfScoreboard,
                    heightOfScoreboard / 3f);
            scoreboardPositionX = (int) alignment.getXMin();
            scoreboardPositionY = (int) alignment.getYMin();
        } else {
            // By default, we add 60 to the scoreboard position, this makes the arraylist more readable
            scoreboardPositionY += 60;
        }

        int count = 0;
        int bgColor = this.client.options.getTextBackgroundColor(0.3f);
        int secondBgColor = this.client.options.getTextBackgroundColor(0.4f);

        // Draw each scoreboard entry
        for (Pair pair : list2) {
            ScoreboardPlayerScore scoreboardPlayerScore2 = (ScoreboardPlayerScore) pair.getFirst();
            Text text3 = (Text) pair.getSecond();
            String string = "" + Formatting.RED + scoreboardPlayerScore2.getScore();

            int t = scoreboardPositionY - ++count * textRenderer.fontHeight;
            int u = this.scaledWidth - 3 + 2;

            // Draw scoreboard bar
            if (ModuleScoreboard.INSTANCE.getEnabled()) {
                final var alignment = ModuleScoreboard.INSTANCE.getAlignment().getBounds(widthOfScoreboard,
                        heightOfScoreboard);
                u = (int) alignment.getXMax() + 1;
            } else {
                u += 1;
            }

            context.fill(scoreboardPositionX - 2, t, u, t + textRenderer.fontHeight, bgColor);
            context.drawText(textRenderer, text3, scoreboardPositionX, t, -1, false);
            context.drawText(textRenderer, string, u - textRenderer.getWidth(string), t, -1, false);

            if (count != collection.size()) {
                continue;
            }

            context.fill(scoreboardPositionX - 2, t - textRenderer.fontHeight - 1, u, t - 1, secondBgColor);
            context.fill(scoreboardPositionX - 2, t - 1, u, t, bgColor);
            context.drawText(textRenderer, text, scoreboardPositionX + widthOfScoreboard / 2 - i / 2,
                    t - textRenderer.fontHeight, -1, false);
        }
    }

}
