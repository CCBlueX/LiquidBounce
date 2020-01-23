/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class MineplexGround extends SpeedMode {

    private boolean spoofSlot;
    private float speed = 0F;

    public MineplexGround() {
        super("MineplexGround");
    }

    @Override
    public void onMotion() {
        if(!MovementUtils.isMoving() || !mc.thePlayer.onGround || mc.thePlayer.inventory.getCurrentItem() == null || mc.thePlayer.isUsingItem())
            return;

        spoofSlot = false;

        for(int i = 36; i < 45; i++) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if(itemStack != null) continue;

            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(i - 36));
            spoofSlot = true;
            break;
        }
    }

    @Override
    public void onUpdate() {
        if(!MovementUtils.isMoving() || !mc.thePlayer.onGround || mc.thePlayer.isUsingItem()) {
            speed = 0F;
            return;
        }

        if(!spoofSlot && mc.thePlayer.inventory.getCurrentItem() != null) {
            ClientUtils.displayChatMessage("§8[§c§lMineplex§aSpeed§8] §cYou need one empty slot.");
            return;
        }

        final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY - 1, mc.thePlayer.posZ);
        final Vec3 vec = new Vec3(blockPos).addVector(0.4F, 0.4F, 0.4F).add(new Vec3(EnumFacing.UP.getDirectionVec()));
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, null, blockPos, EnumFacing.UP, new Vec3(vec.xCoord * 0.4F, vec.yCoord * 0.4F, vec.zCoord * 0.4F));

        final float targetSpeed = ((Speed) LiquidBounce.moduleManager.getModule(Speed.class)).mineplexGroundSpeedValue.get();

        if(targetSpeed > speed) speed += targetSpeed / 8;
        if(speed >= targetSpeed) speed = targetSpeed;

        MovementUtils.strafe(speed);

        if(!spoofSlot)
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }

    @Override
    public void onMove(final MoveEvent event) {
    }

    @Override
    public void onDisable() {
        speed = 0F;
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }
}