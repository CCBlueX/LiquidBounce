/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.block;

import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow;
import net.minecraft.block.SoulSandBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoulSandBlock.class)
@SideOnly(Side.CLIENT)
public class MixinSoulSandBlock {

    @Inject(method = "onEntityCollidedWithBlock", at = @At("HEAD"), cancellable = true)
    private void onEntityCollidedWithBlock(CallbackInfo callbackInfo) {
        final NoSlow noSlow = NoSlow.INSTANCE;

        if (noSlow.handleEvents() && noSlow.getSoulsand())
            callbackInfo.cancel();
    }
}