/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "BufferSpeed", description = "Allows you to walk faster on slabs and stairs.", category = ModuleCategory.MOVEMENT)
public class BufferSpeed extends Module {
    private final BoolValue speedLimitValue = new BoolValue("SpeedLimit", true);
    private final FloatValue maxSpeedValue = new FloatValue("MaxSpeed", 2.0F, 1.0F, 5F);
    private final BoolValue bufferValue = new BoolValue("Buffer", true);

    private final BoolValue stairsValue = new BoolValue("Stairs", true);
    private final FloatValue stairsBoostValue = new FloatValue("StairsBoost", 1.87F, 1F, 2F);
    private final ListValue stairsModeValue = new ListValue("StairsMode", new String[] {"Old", "New"}, "New");
    private final BoolValue slabsValue = new BoolValue("Slabs", true);
    private final FloatValue slabsBoostValue = new FloatValue("SlabsBoost", 1.87F, 1F, 2F);
    private final ListValue slabsModeValue = new ListValue("SlabsMode", new String[] {"Old", "New"}, "New");
    private final BoolValue iceValue = new BoolValue("Ice", false);
    private final FloatValue iceBoostValue = new FloatValue("IceBoost", 1.342F, 1F, 2F);
    private final BoolValue snowValue = new BoolValue("Snow", true);
    private final FloatValue snowBoostValue = new FloatValue("SnowBoost", 1.87F, 1F, 2F);
    private final BoolValue snowPortValue = new BoolValue("SnowPort", true);
    private final BoolValue wallValue = new BoolValue("Wall", true);
    private final FloatValue wallBoostValue = new FloatValue("WallBoost", 1.87F, 1F, 2F);
    private final ListValue wallModeValue = new ListValue("WallMode", new String[] {"Old", "New"}, "New");
    private final BoolValue headBlockValue = new BoolValue("HeadBlock", true);
    private final FloatValue headBlockBoostValue = new FloatValue("HeadBlockBoost", 1.87F, 1F, 2F);
    private final BoolValue slimeValue = new BoolValue("Slime", true);
    private final BoolValue airStrafeValue = new BoolValue("AirStrafe", false);
    private final BoolValue noHurtValue = new BoolValue("NoHurt", true);

    private double speed = 0;
    private boolean down;
    private boolean forceDown;
    private boolean fastHop;
    private boolean hadFastHop;
    private boolean legitHop;

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if (LiquidBounce.moduleManager.getModule(Speed.class).getState() || (noHurtValue.get() && mc.thePlayer.hurtTime > 0)) {
            reset();
            return;
        }

        final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY,
                mc.thePlayer.posZ);

        if(forceDown || (down && mc.thePlayer.motionY == 0)) {
            mc.thePlayer.motionY = -1;
            down = false;
            forceDown = false;
        }

        if(fastHop) {
            mc.thePlayer.speedInAir = 0.0211F;
            hadFastHop = true;
        }else if(hadFastHop) {
            mc.thePlayer.speedInAir = 0.02F;
            hadFastHop = false;
        }

        if(!MovementUtils.isMoving() || mc.thePlayer.isSneaking() || mc.thePlayer.isInWater() ||
                mc.gameSettings.keyBindJump.isKeyDown()) {
            reset();
            return;
        }

        if(mc.thePlayer.onGround) {
            fastHop = false;

            if(slimeValue.get() && (BlockUtils.getBlock(blockPos.down()) instanceof BlockSlime ||
                    BlockUtils.getBlock(blockPos) instanceof BlockSlime)) {
                mc.thePlayer.jump();
                mc.thePlayer.motionY = 0.08;
                mc.thePlayer.motionX *= 1.132;
                mc.thePlayer.motionZ *= 1.132;
                down = true;
                return;
            }

            if(slabsValue.get() && BlockUtils.getBlock(blockPos) instanceof BlockSlab) {
                switch(slabsModeValue.get().toLowerCase()) {
                    case "old":
                        boost(slabsBoostValue.get());
                        return;
                    case "new":
                        fastHop = true;

                        if(legitHop) {
                            mc.thePlayer.jump();
                            mc.thePlayer.onGround = false;
                            legitHop = false;
                            return;
                        }

                        mc.thePlayer.onGround = false;
                        MovementUtils.strafe(0.375F);
                        mc.thePlayer.jump();
                        mc.thePlayer.motionY = 0.41;
                        return;
                }
            }

            if(stairsValue.get() && (BlockUtils.getBlock(blockPos.down()) instanceof BlockStairs ||
                    BlockUtils.getBlock(blockPos) instanceof BlockStairs)) {
                switch(stairsModeValue.get().toLowerCase()) {
                    case "old":
                        boost(stairsBoostValue.get());
                        return;
                    case "new":
                        fastHop = true;

                        if(legitHop) {
                            mc.thePlayer.jump();
                            mc.thePlayer.onGround = false;
                            legitHop = false;
                            return;
                        }

                        mc.thePlayer.onGround = false;
                        MovementUtils.strafe(0.375F);
                        mc.thePlayer.jump();
                        mc.thePlayer.motionY = 0.41;
                        return;
                }
            }

            legitHop = true;

            if(headBlockValue.get() && BlockUtils.getBlock(blockPos.up(2)) != Blocks.air) {
                boost(headBlockBoostValue.get());
                return;
            }

            if(iceValue.get() && (BlockUtils.getBlock(blockPos.down()) == Blocks.ice ||
                    BlockUtils.getBlock(blockPos.down()) == Blocks.packed_ice)) {
                boost(iceBoostValue.get());
                return;
            }

            if((snowValue.get() && BlockUtils.getBlock(blockPos) == Blocks.snow_layer) &&
                    (snowPortValue.get() || mc.thePlayer.posY - (int) mc.thePlayer.posY >= 0.12500)) {
                if(mc.thePlayer.posY - (int) mc.thePlayer.posY >= 0.12500)
                    boost(snowBoostValue.get());
                else{
                    mc.thePlayer.jump();
                    forceDown = true;
                }
                return;
            }

            if(wallValue.get()) {
                switch(wallModeValue.get().toLowerCase()) {
                    case "old":
                        if((mc.thePlayer.isCollidedHorizontally && isNearBlock()) || !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2D, mc.thePlayer.posZ)) instanceof BlockAir)) {
                            boost(wallBoostValue.get());
                            return;
                        }
                        break;
                    case "new":
                        if(isNearBlock() && !mc.thePlayer.movementInput.jump) {
                            mc.thePlayer.jump();
                            mc.thePlayer.motionY = 0.08;
                            mc.thePlayer.motionX *= 0.99;
                            mc.thePlayer.motionZ *= 0.99;
                            down = true;
                            return;
                        }
                        break;
                }
            }

            final float currentSpeed = MovementUtils.getSpeed();

            if(speed < currentSpeed)
                speed = currentSpeed;

            if(bufferValue.get() && speed > 0.2F) {
                speed /= 1.0199999809265137D;

                MovementUtils.strafe((float) speed);
            }
        }else{
            speed = 0F;

            if(airStrafeValue.get())
                MovementUtils.strafe();
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if(packet instanceof S08PacketPlayerPosLook)
            speed = 0F;
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        if(mc.thePlayer == null)
            return;

        legitHop = true;
        speed = 0;
        if(hadFastHop) {
            mc.thePlayer.speedInAir = 0.02F;
            hadFastHop = false;
        }
    }

    private void boost(final float boost) {
        mc.thePlayer.motionX *= boost;
        mc.thePlayer.motionZ *= boost;
        speed = MovementUtils.getSpeed();

        if(speedLimitValue.get() && speed > maxSpeedValue.get())
            speed = maxSpeedValue.get();
    }

    private boolean isNearBlock() {
        final EntityPlayerSP thePlayer = mc.thePlayer;
        final WorldClient theWorld = mc.theWorld;

        final List<BlockPos> blocks = new ArrayList<>();

        blocks.add(new BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ - 0.7));
        blocks.add(new BlockPos(thePlayer.posX + 0.7, thePlayer.posY + 1, thePlayer.posZ));
        blocks.add(new BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ + 0.7));
        blocks.add(new BlockPos(thePlayer.posX - 0.7, thePlayer.posY + 1, thePlayer.posZ));

        for(final BlockPos blockPos : blocks)
            if((theWorld.getBlockState(blockPos).getBlock().getBlockBoundsMaxY() ==
                    theWorld.getBlockState(blockPos).getBlock().getBlockBoundsMinY() + 1 &&
                    !theWorld.getBlockState(blockPos).getBlock().isTranslucent() &&
                    theWorld.getBlockState(blockPos).getBlock() != Blocks.water &&
                    !(theWorld.getBlockState(blockPos).getBlock() instanceof BlockSlab)) ||
                    theWorld.getBlockState(blockPos).getBlock() == Blocks.barrier)
                return true;

        return false;
    }
}