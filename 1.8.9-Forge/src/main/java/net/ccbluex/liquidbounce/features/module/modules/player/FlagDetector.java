/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

@ModuleInfo(name = "FlagDetector", description = "Allows you to see flags.", category = ModuleCategory.PLAYER)
public class FlagDetector extends Module {

    @EventTarget
    public void onPacket(PacketEvent event) {
            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                ClientUtils.displayChatMessage("§7[§9VIO§7] You §ffailed §bIllegalMove");
          }
        }



    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}