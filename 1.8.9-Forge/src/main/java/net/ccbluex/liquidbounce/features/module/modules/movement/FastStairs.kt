/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockStairs;
import net.minecraft.util.BlockPos;

@ModuleInfo(name = "FastStairs", description = "Allows you to climb up stairs faster.", category = ModuleCategory.MOVEMENT)
public class FastStairs extends Module {
    private final ListValue modeValue = new ListValue("Mode", new String[] {"NCP", "AAC", "LAAC"}, "NCP");
    private final BoolValue longJumpValue = new BoolValue("LongJump", false);

    private boolean canJump;

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if (mc.thePlayer == null || LiquidBounce.moduleManager.getModule(Speed.class).getState())
            return;

        final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ);

        if(mc.thePlayer.onGround && mc.thePlayer.movementInput.moveForward > 0D) {
            final String mode = modeValue.get();

            if(BlockUtils.getBlock(blockPos) instanceof BlockStairs) {
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.5D, mc.thePlayer.posZ);

                final double motion = mode.equalsIgnoreCase("NCP") ? 1.4D : mode.equalsIgnoreCase("AAC") ? 1.5D : mode.equalsIgnoreCase("AAC") ? 1.499D : 1D;
                mc.thePlayer.motionX *= motion;
                mc.thePlayer.motionZ *= motion;
            }

            if(BlockUtils.getBlock(blockPos.down()) instanceof BlockStairs) {
                mc.thePlayer.motionX *= 1.3D;
                mc.thePlayer.motionZ *= 1.3D;

                if(mode.equalsIgnoreCase("LAAC")) {
                    mc.thePlayer.motionX *= 1.18D;
                    mc.thePlayer.motionZ *= 1.18D;
                }

                canJump = true;
            }else if((mode.equalsIgnoreCase("LAAC") || mode.equalsIgnoreCase("AAC")) && mc.thePlayer.onGround && canJump) {
                if(longJumpValue.get()) {
                    mc.thePlayer.jump();
                    mc.thePlayer.motionX *= 1.35D;
                    mc.thePlayer.motionZ *= 1.35D;
                }

                canJump = false;
            }
        }
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
