package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleFPSBoost;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class MixinDebugHud {

    @Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true)
    private void modifyLeftText(CallbackInfoReturnable<List<String>> cir) {
        if (!ModuleFPSBoost.INSTANCE.getRunning()) return;

        List<String> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;

        int realFPS = MinecraftClient.getInstance().getCurrentFps();
        int spoofedFPS = ModuleFPSBoost.INSTANCE.getModifiedFPS(realFPS);

        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (s == null) continue;
            if (s.contains("fps")) {
                String replaced = s.replaceFirst("\\d+", Integer.toString(spoofedFPS));
                list.set(i, replaced);
                break;
            }
        }

        cir.setReturnValue(list);
    }
}
