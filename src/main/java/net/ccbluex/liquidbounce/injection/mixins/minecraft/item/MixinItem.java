package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public class MixinItem {

    @Redirect(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private static float hookFixRotationA(PlayerEntity instance) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (instance != MinecraftClient.getInstance().player || rotation == null) {
            return instance.getYaw();
        }

        return rotation.getYaw();
    }

    @Redirect(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getPitch()F"))
    private static float hookFixRotationB(PlayerEntity instance) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (instance != MinecraftClient.getInstance().player || rotation == null) {
            return instance.getPitch();
        }

        return rotation.getPitch();
    }
}
