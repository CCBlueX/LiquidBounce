package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void hookSwordUse(World world, PlayerEntity user, Hand hand,
                              CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        // Hooks sword use - only if main hand (otherwise this makes no sense on 1.8)
        if (((Object) this) instanceof SwordItem && ModuleSwordBlock.INSTANCE.getEnabled() && hand == Hand.MAIN_HAND) {
            var itemStack = user.getStackInHand(hand);
            user.setCurrentHand(hand);
            cir.setReturnValue(TypedActionResult.consume(itemStack));
        }
    }

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void hookSwordUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        // Hooks sword use action
        if (((Object) this) instanceof SwordItem && ModuleSwordBlock.INSTANCE.getEnabled()) {
            cir.setReturnValue(UseAction.BLOCK);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void hookMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        // Hooks sword max use time
        if (((Object) this) instanceof SwordItem && ModuleSwordBlock.INSTANCE.getEnabled()) {
            cir.setReturnValue(72000);
        }
    }

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
