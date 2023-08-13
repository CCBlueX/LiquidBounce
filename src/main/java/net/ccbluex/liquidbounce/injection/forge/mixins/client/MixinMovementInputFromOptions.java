/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.features.module.modules.combat.SuperKnockback;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions extends MixinMovementInput {

    @Inject(method = "updatePlayerMoveState", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInputFromOptions;jump:Z"))
    private void hookSuperKnockbackInputBlock(CallbackInfo ci) {
        SuperKnockback module = SuperKnockback.INSTANCE;

        if (module.shouldBlockInput()) {
            if (module.getOnlyMove()) {
                this.moveForward = 0f;

                if (!module.getOnlyMoveForward()) {
                    this.moveStrafe = 0f;
                }
            }
        }
    }
}
