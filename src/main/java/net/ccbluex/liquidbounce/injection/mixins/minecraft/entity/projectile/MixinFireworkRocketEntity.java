package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleExtendedFirework;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity shooter;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d getRotationVector(Vec3d original) {
        if (shooter != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (rotation == null || rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.getDirectionVector();
    }

    @ModifyArgs(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal = 0))
    private void hookExtendedFirework(Args args, @Local(ordinal = 0) Vec3d rotation, @Local(ordinal = 1) Vec3d velocity) {
        if (shooter != MinecraftClient.getInstance().player
                || !ModuleExtendedFirework.INSTANCE.getRunning()
        ) return;

        var multiplier = ModuleExtendedFirework.getVelocityMultiplier();
        args.set(0, rotation.x * multiplier.x + (rotation.x * multiplier.y - velocity.x) * multiplier.z);
        args.set(1, rotation.y * multiplier.x + (rotation.y * multiplier.y - velocity.y) * multiplier.z);
        args.set(2, rotation.z * multiplier.x + (rotation.z * multiplier.y - velocity.z) * multiplier.z);
    }
}
