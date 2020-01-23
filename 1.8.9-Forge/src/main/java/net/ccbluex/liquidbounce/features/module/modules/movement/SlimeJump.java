/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockSlime;

@ModuleInfo(name = "SlimeJump", description = "Allows you to to jump higher on slime blocks.", category = ModuleCategory.MOVEMENT)
public class SlimeJump extends Module {

    private final FloatValue motionValue = new FloatValue("Motion", 0.42F, 0.2F, 1F);
    private final ListValue modeValue = new ListValue("Mode", new String[] {"Set", "Add"}, "Add");

    @EventTarget
    public void onJump(JumpEvent event) {
        if(mc.thePlayer != null && mc.theWorld != null && BlockUtils.getBlock(mc.thePlayer.getPosition().down()) instanceof BlockSlime) {
            event.cancelEvent();

            switch(modeValue.get().toLowerCase()) {
                case "set":
                    mc.thePlayer.motionY = motionValue.get();
                    break;
                case "add":
                    mc.thePlayer.motionY += motionValue.get();
                    break;
            }
        }
    }
}