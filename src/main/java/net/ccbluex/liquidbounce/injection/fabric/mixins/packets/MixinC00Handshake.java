/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.packets;

import net.ccbluex.liquidbounce.features.special.ClientFixes;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@SideOnly(Side.CLIENT)
@Mixin(C00Handshake.class)
public class MixinC00Handshake {

    /**
     * @author CCBlueX
     */
    @ModifyConstant(method = "writePacketData", constant = @Constant(stringValue = "\u0000FML\u0000"))
    private String injectAntiForge(String constant) {
        return ClientFixes.INSTANCE.getFmlFixesEnabled() && ClientFixes.INSTANCE.getBlockFML() && !mc.isIntegratedServerRunning() ? "" : "\u0000FML\u0000";
    }
}