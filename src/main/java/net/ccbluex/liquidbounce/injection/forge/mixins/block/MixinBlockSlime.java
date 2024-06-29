/*
 * SkidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge, Forked from LiquidBounce.
 * https://github.com/ManInMyVan/SkidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.features.module.modules.movement.AntiBounce;
import net.minecraft.block.BlockSlime;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockSlime.class)
@SideOnly(Side.CLIENT)
public class MixinBlockSlime {
    @Inject(method = "onLanded", at = @At("HEAD"), cancellable = true)
    private void AntiBounce(CallbackInfo callbackInfo) {
        if (AntiBounce.INSTANCE.handleEvents()) {
            callbackInfo.cancel();
        }
    }
}
