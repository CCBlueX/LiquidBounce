/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.minecraft.block.BlockLiquid;

@ModuleInfo(name = "WaterSpeed", description = "Allows you to swim faster.", category = ModuleCategory.MOVEMENT)
public class WaterSpeed extends Module {

    private final FloatValue speedValue = new FloatValue("Speed", 1.2F, 1.1F, 1.5F);

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if(mc.thePlayer.isInWater() && BlockUtils.getBlock(mc.thePlayer.getPosition()) instanceof BlockLiquid) {
            final float speed = speedValue.get();

            mc.thePlayer.motionX *= speed;
            mc.thePlayer.motionZ *= speed;
        }
    }
}