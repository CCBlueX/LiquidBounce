package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.VecRotation;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.block.PlaceInfo;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.TickTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

import java.awt.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "Turm", description = "Automatically builds a tower beneath you.", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_O)
public class Tower extends Module {

    /**
     * OPTIONS
     */

    private final ListValue modeValue = new ListValue("Mode", new String[] {"Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9"}, "Motion");
    private final BoolValue autoBlockValue = new BoolValue("AutoBlock", true);
    private final BoolValue stayAutoBlock = new BoolValue("StayAutoBlock", false);
    private final BoolValue swingValue = new BoolValue("Swing", true);
    private final BoolValue stopWhenBlockAbove = new BoolValue("StopWhenBlockAbove", false);
    private final BoolValue rotationsValue = new BoolValue("Rotations", true);
    private final BoolValue keepRotationValue = new BoolValue("KeepRotation", false);
    private final BoolValue onJumpValue = new BoolValue("OnJump", false);
    private final ListValue placeModeValue = new ListValue("PlaceTiming", new String[]{"Pre", "Post"}, "Post");

    private final FloatValue timerValue = new FloatValue("Timer", 1F, 0F, 10F);

    // Jump mode
    private final FloatValue jumpMotionValue = new FloatValue("JumpMotion", 0.42F, 0.3681289F, 0.79F);
    private final IntegerValue jumpDelayValue = new IntegerValue("JumpDelay", 0, 0, 20);

    // ConstantMotion
    private final FloatValue constantMotionValue = new FloatValue("ConstantMotion", 0.42F, 0.1F, 1F);
    private final FloatValue constantMotionJumpGroundValue = new FloatValue("ConstantMotionJumpGround", 0.79F, 0.76F, 1F);

    // Teleport
    private final FloatValue teleportHeightValue = new FloatValue("TeleportHeight", 1.15F, 0.1F, 5F);
    private final IntegerValue teleportDelayValue = new IntegerValue("TeleportDelay", 0, 0, 20);
    private final BoolValue teleportGroundValue = new BoolValue("TeleportGround", true);
    private final BoolValue teleportNoMotionValue = new BoolValue("TeleportNoMotion", false);

    // Render
    private final BoolValue counterDisplayValue = new BoolValue("Counter", true);

    /**
     * MODULE
     */

    private PlaceInfo placeInfo;
    private final TickTimer packetTimer = new TickTimer();
    private final TickTimer teleportTimer = new TickTimer();
    private final TickTimer jumpTimer = new TickTimer();
    private double jumpGround = 0;

    private int slot;

    @EventTarget
    public void onMotion(final MotionEvent event) {
        if (onJumpValue.get() && !mc.gameSettings.keyBindJump.isKeyDown())
            return;

        if(rotationsValue.get() && keepRotationValue.get())
            RotationUtils.setToServerRotation();

        mc.timer.timerSpeed = timerValue.get();

        final EventState eventState = event.getEventState();

        if (placeModeValue.get().equalsIgnoreCase(eventState.getStateName()))
            place();

        if (eventState == EventState.PRE) {
            packetTimer.update();
            teleportTimer.update();
            jumpTimer.update();

            if (autoBlockValue.get() ? InventoryUtils.findAutoBlockBlock() != -1 : mc.thePlayer.getHeldItem() != null
                    && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
                if (!stopWhenBlockAbove.get() || BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX,
                        mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir)
                    move();

                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.5D, mc.thePlayer.posZ);
                if (mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockAir) {
                    if ((placeInfo = PlaceInfo.get(blockPos)) != null && rotationsValue.get()) {
                        final VecRotation vecRotation = RotationUtils.faceBlock(blockPos);

                        if (vecRotation != null)
                            placeInfo.setVec3(vecRotation.getVec());
                    }
                }
            }
        }
    }

    private void move() {
        switch (modeValue.get().toLowerCase()) {
            case "jump":
                if (mc.thePlayer.onGround && jumpTimer.hasTimePassed(jumpDelayValue.get())) {
                    mc.thePlayer.motionY = jumpMotionValue.get();
                    jumpTimer.reset();
                }
                break;
            case "motion":
                if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.42D;
                else if (mc.thePlayer.motionY < 0.1D) mc.thePlayer.motionY = -0.3D;
                break;
            case "motiontp":
                if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.42D;
                else if (mc.thePlayer.motionY < 0.23D)
                    mc.thePlayer.setPosition(mc.thePlayer.posX, (int) mc.thePlayer.posY, mc.thePlayer.posZ);
                break;
            case "packet":
                if (mc.thePlayer.onGround && packetTimer.hasTimePassed(2)) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY + 0.42D, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY + 0.753D, mc.thePlayer.posZ, false));
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1D, mc.thePlayer.posZ);
                    packetTimer.reset();
                }
                break;
            case "teleport":
                if (teleportNoMotionValue.get())
                    mc.thePlayer.motionY = 0;

                if ((mc.thePlayer.onGround || !teleportGroundValue.get()) && teleportTimer.hasTimePassed(teleportDelayValue.get())) {
                    mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + teleportHeightValue.get(), mc.thePlayer.posZ);
                    teleportTimer.reset();
                }
                break;
            case "constantmotion":
                if (mc.thePlayer.onGround) {
                    jumpGround = mc.thePlayer.posY;
                    mc.thePlayer.motionY = constantMotionValue.get();
                }

                if (mc.thePlayer.posY > jumpGround + constantMotionJumpGroundValue.get()) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, (int) mc.thePlayer.posY, mc.thePlayer.posZ);
                    mc.thePlayer.motionY = constantMotionValue.get();
                    jumpGround = mc.thePlayer.posY;
                }
                break;
            case "aac3.3.9":
                if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.4001;

                mc.timer.timerSpeed = 1F;

                if (mc.thePlayer.motionY < 0) {
                    mc.thePlayer.motionY -= 0.00000945;
                    mc.timer.timerSpeed = 1.6F;
                }
                break;
        }
    }

    /**
     * Place target block
     */
    private void place() {
        if(placeInfo == null)
            return;

        int blockSlot = Integer.MAX_VALUE;
        ItemStack itemStack = mc.thePlayer.getHeldItem();

        if(mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            if(!autoBlockValue.get())
                return;

            blockSlot = InventoryUtils.findAutoBlockBlock();

            if(blockSlot == -1)
                return;

            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot - 36));
            itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).getStack();
        }

        if(mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, this.placeInfo.getBlockPos(), placeInfo.getEnumFacing(), placeInfo.getVec3())) {
            if(swingValue.get())
                mc.thePlayer.swingItem();
            else
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
        }
        this.placeInfo = null;

        if(!stayAutoBlock.get() && blockSlot != Integer.MAX_VALUE)
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if(mc.thePlayer == null)
            return;

        final Packet packet = event.getPacket();

        if(packet instanceof C09PacketHeldItemChange) {
            final C09PacketHeldItemChange packetHeldItemChange = (C09PacketHeldItemChange) packet;

            slot = packetHeldItemChange.getSlotId();
        }
    }

    /**
     * Tower visuals
     *
     * @param event
     */
    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        if(counterDisplayValue.get()) {
            GlStateManager.pushMatrix();

            final BlockOverlay blockOverlay = (BlockOverlay) ModuleManager.getModule(BlockOverlay.class);
            if(blockOverlay.getState() && blockOverlay.infoValue.get() && mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null && mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() != null && BlockUtils.canBeClicked(mc.objectMouseOver.getBlockPos()) && mc.theWorld.getWorldBorder().contains(mc.objectMouseOver.getBlockPos()))
                GlStateManager.translate(0, 15F, 0);

            final String info = "Blocks: §7" + getBlocksAmount();
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            RenderUtils.drawBorderedRect((scaledResolution.getScaledWidth() / 2) - 2, (scaledResolution.getScaledHeight() / 2) + 5, (scaledResolution.getScaledWidth() / 2) + Fonts.font40.getStringWidth(info) + 2, (scaledResolution.getScaledHeight() / 2) + 16, 3, Color.BLACK.getRGB(), Color.BLACK.getRGB());
            GlStateManager.resetColor();
            Fonts.font40.drawString(info, scaledResolution.getScaledWidth() / 2, scaledResolution.getScaledHeight() / 2 + 7, Color.WHITE.getRGB());

            GlStateManager.popMatrix();
        }
    }

    @EventTarget
    public void onJump(final JumpEvent event) {
        if (onJumpValue.get())
            event.cancelEvent();
    }

    private int getBlocksAmount() {
        int amount = 0;

        for(int i = 36; i < 45; i++) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if(itemStack != null && itemStack.getItem() instanceof ItemBlock)
                amount += itemStack.stackSize;
        }

        return amount;
    }

    @Override
    public void onDisable() {
        if(mc.thePlayer == null)
            return;

        mc.timer.timerSpeed = 1F;

        if(slot != mc.thePlayer.inventory.currentItem)
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
