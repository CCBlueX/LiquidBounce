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

import com.mojang.blaze3d.audio.ListenerTransform;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.SoundSubtitleEntry;
import net.ccbluex.liquidbounce.event.events.SoundSubtitlesEvent;
import net.ccbluex.liquidbounce.event.events.SubtitleDirection;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(SubtitleOverlay.class)
public abstract class MixinSubtitleOverlay {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Final
    @Shadow
    private List<SubtitleOverlay.Subtitle> audibleSubtitles;

    @Inject(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;nextStratum()V"
        ),
        cancellable = true
    )
    private void hookBeforeRendering(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (this.audibleSubtitles.isEmpty()) {
            return;
        }

        SoundManager soundManager = this.minecraft.getSoundManager();
        ListenerTransform listener = soundManager.getListenerTransform();
        Vec3 position = listener.position();
        Vec3 forward = listener.forward();
        Vec3 right = listener.right();
        double displayTimeMultiplier = this.minecraft.options.notificationDisplayTime().get();

        List<SoundSubtitleEntry> entries = new ArrayList<>();
        for (var subtitle : this.audibleSubtitles) {
            var closest = subtitle.getClosest(position);
            if (closest == null) {
                continue;
            }

            Vec3 delta = closest.location().subtract(position).normalize();
            double forwardness = forward.dot(delta);
            double rightness = right.dot(delta);

            SubtitleDirection direction;
            if (forwardness > 0.5) {
                direction = SubtitleDirection.CENTER;
            } else {
                direction = rightness > 0.0 ? SubtitleDirection.RIGHT : SubtitleDirection.LEFT;
            }

            float brightness = Mth.clampedLerp(
                (float) (Util.getMillis() - closest.time()) / (float) (3000.0 * displayTimeMultiplier),
                255.0F, 75.0F
            );
            float opacity = brightness / 255.0F;

            entries.add(new SoundSubtitleEntry(subtitle.getText(), direction, opacity));
        }

        EventManager.INSTANCE.callEvent(new SoundSubtitlesEvent(entries));

        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_SUBTITLE_OVERLAY)) {
            ci.cancel();
        }
    }

}
