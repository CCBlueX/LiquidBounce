package net.ccbluex.liquidbounce.injection.fabric.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.NoAchievement;
import net.minecraft.client.gui.achievement.AchievementNotification;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AchievementNotification.class)
public class MixinAchievementNotification {

    @Inject(method = "displayAchievement", at = @At("HEAD"), cancellable = true)
    private void injectAchievements(CallbackInfo ci) {
        final NoAchievement noachievement = (NoAchievement) LiquidBounce.INSTANCE.getModuleManager().getModule(NoAchievement.class);

        if (noachievement.getState()) {
            // Cancel Achievement Display Packet
            ci.cancel();
        }
    }

    @Inject(method = "updateAchievementWindow", at = @At("HEAD"), cancellable = true)
    private void injectAchievementWindows(CallbackInfo ci) {
        final NoAchievement noachievement = (NoAchievement) LiquidBounce.INSTANCE.getModuleManager().getModule(NoAchievement.class);

        if (noachievement.getState()) {
            // Cancel Achievement Window Packet
            ci.cancel();
        }
    }
}
