/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.extensions.MathExtensionsKt;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.minecraft.block.BlockAir;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket;
import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets;
import static net.ccbluex.liquidbounce.utils.extensions.BlockExtensionKt.*;
import static net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;

public class Ignite extends Module {
    public Ignite() {
        super("Ignite", ModuleCategory.COMBAT);
    }

   private final BoolValue lighterValue = new BoolValue("Lighter", true);
   private final BoolValue lavaBucketValue = new BoolValue("Lava", true);

   private final MSTimer msTimer = new MSTimer();

   @EventTarget
   public void onUpdate(final UpdateEvent event) {
       if (!msTimer.hasTimePassed(500))
           return;

       EntityPlayerSP thePlayer = mc.thePlayer;
       WorldClient theWorld = mc.theWorld;

       if (thePlayer == null || theWorld == null)
           return;

       final int lighterInHotbar =
               lighterValue.get() ? InventoryUtils.INSTANCE.findItem(36, 44, Items.flint_and_steel) : -1;
       final int lavaInHotbar =
               lavaBucketValue.get() ? InventoryUtils.INSTANCE.findItem(26, 44, Items.lava_bucket) : -1;

       if (lighterInHotbar == -1 && lavaInHotbar == -1)
           return;

       final int fireInHotbar = lighterInHotbar != -1 ? lighterInHotbar : lavaInHotbar;

       for (final Entity entity : theWorld.getLoadedEntityList()) {
           if (EntityUtils.INSTANCE.isSelected(entity, true) && !entity.isBurning()) {
               BlockPos blockPos = entity.getPosition();

               if (mc.thePlayer.getDistanceSq(blockPos) >= 22.3 ||
                       !isReplaceable(blockPos) ||
                       !(getBlock(blockPos) instanceof BlockAir))
                   continue;

               RotationUtils.INSTANCE.setKeepCurrentRotation(true);

               sendPacket(new C09PacketHeldItemChange(fireInHotbar - 36));

               final ItemStack itemStack =
                       mc.thePlayer.inventoryContainer.getSlot(fireInHotbar).getStack();

               if (itemStack.getItem() instanceof ItemBucket) {
                   final double diffX = blockPos.getX() + 0.5 - mc.thePlayer.posX;
                   final double diffY = blockPos.getY() + 0.5 -
                           (thePlayer.getEntityBoundingBox().minY +
                                   thePlayer.getEyeHeight());
                   final double diffZ = blockPos.getZ() + 0.5 - thePlayer.posZ;
                   final double sqrt = Math.sqrt(diffX * diffX + diffZ * diffZ);
                   final float yaw = MathExtensionsKt.toDegreesF(Math.atan2(diffZ, diffX)) - 90F;
                   final float pitch = -MathExtensionsKt.toDegreesF(Math.atan2(diffY, sqrt));

                   sendPacket(new C05PacketPlayerLook(
                           mc.thePlayer.rotationYaw +
                                   MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                           mc.thePlayer.rotationPitch +
                                   MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch),
                           mc.thePlayer.onGround));

                   mc.playerController.sendUseItem(thePlayer, theWorld, itemStack);
               } else {
                   for (final EnumFacing side : EnumFacing.values()) {
                       final BlockPos neighbor = blockPos.offset(side);

                       if (!canBeClicked(neighbor)) continue;

                       final double diffX = neighbor.getX() + 0.5 - thePlayer.posX;
                       final double diffY = neighbor.getY() + 0.5 -
                               (thePlayer.getEntityBoundingBox().minY +
                                       thePlayer.getEyeHeight());
                       final double diffZ = neighbor.getZ() + 0.5 - thePlayer.posZ;
                       final double sqrt = Math.sqrt(diffX * diffX + diffZ * diffZ);
                       final float yaw = MathExtensionsKt.toDegreesF(Math.atan2(diffZ, diffX)) - 90F;
                       final float pitch = -MathExtensionsKt.toDegreesF(Math.atan2(diffY, sqrt));

                       sendPacket(new C05PacketPlayerLook(
                               mc.thePlayer.rotationYaw +
                                       MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                               mc.thePlayer.rotationPitch +
                                       MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch),
                               mc.thePlayer.onGround));

                       if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, itemStack, neighbor,
                               side.getOpposite(), new Vec3(side.getDirectionVec()))) {
                           thePlayer.swingItem();
                           break;
                       }
                   }
               }

               RotationUtils.INSTANCE.setKeepCurrentRotation(false);
               sendPackets(
                       new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem),
                       new C05PacketPlayerLook(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround)
               );

               msTimer.reset();
               break;
           }
      }
   }
}
