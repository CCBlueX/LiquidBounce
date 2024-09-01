package net.ccbluex.liquidbounce.injection.fabric.mixins.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.render.block.entity.MobSpawnerBlockEntityRenderer.class)
public class MixinMobSpawnerBlockEntityRenderer {

    @Inject(method = "renderMob", cancellable = true, at = @At("HEAD"))
    private static void injectPaintingSpawnerFix(MobSpawnerBaseLogic mobSpawnerLogic, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        Entity entity = mobSpawnerLogic.func_180612_a(mobSpawnerLogic.getSpawnerWorld());

        if (entity == null || entity instanceof EntityPainting)
            ci.cancel();
    }

}
