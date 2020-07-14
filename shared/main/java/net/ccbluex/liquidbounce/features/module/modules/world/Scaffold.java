/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.enums.BlockType;
import net.ccbluex.liquidbounce.api.enums.EnumFacingType;
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketHeldItemChange;
import net.ccbluex.liquidbounce.api.minecraft.util.*;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.*;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.block.PlaceInfo;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_I)
public class Scaffold extends Module {

    /**
     * OPTIONS
     */

    // Mode
    public final ListValue modeValue = new ListValue("Mode", new String[] {"Normal", "Rewinside", "Expand"}, "Normal");

    // Delay
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 0, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = minDelayValue.get();

            if(i > newValue)
                set(i);
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 0, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = maxDelayValue.get();

            if(i < newValue)
                set(i);
        }
    };
    private final BoolValue placeableDelay = new BoolValue("PlaceableDelay", false);

    // AutoBlock
    private final BoolValue autoBlockValue = new BoolValue("AutoBlock", true);
    private final BoolValue stayAutoBlock = new BoolValue("StayAutoBlock", false);

    // Basic stuff
    public final BoolValue sprintValue = new BoolValue("Sprint", true);
    private final BoolValue swingValue = new BoolValue("Swing", true);
    private final BoolValue searchValue = new BoolValue("Search", true);
    private final BoolValue downValue = new BoolValue("Down", true);
    private final ListValue placeModeValue = new ListValue("PlaceTiming", new String[]{"Pre", "Post"}, "Post");

    // Eagle
    private final BoolValue eagleValue = new BoolValue("Eagle", false);
    private final BoolValue eagleSilentValue = new BoolValue("EagleSilent", false);
    private final IntegerValue blocksToEagleValue = new IntegerValue("BlocksToEagle", 0, 0, 10);

    // Expand
    private final IntegerValue expandLengthValue = new IntegerValue("ExpandLength", 5, 1, 6);

    // Rotations
    private final BoolValue rotationsValue = new BoolValue("Rotations", true);
    private final IntegerValue keepLengthValue = new IntegerValue("KeepRotationLength", 0, 0, 20);
    private final BoolValue keepRotationValue = new BoolValue("KeepRotation", false);

    // Zitter
    private final BoolValue zitterValue = new BoolValue("Zitter", false);
    private final ListValue zitterModeValue = new ListValue("ZitterMode", new String[] {"Teleport", "Smooth"}, "Teleport");
    private final FloatValue zitterSpeed = new FloatValue("ZitterSpeed", 0.13F, 0.1F, 0.3F);
    private final FloatValue zitterStrength = new FloatValue("ZitterStrength", 0.072F, 0.05F, 0.2F);

    // Game
    private final FloatValue timerValue = new FloatValue("Timer", 1F, 0.1F, 10F);
    private final FloatValue speedModifierValue = new FloatValue("SpeedModifier", 1F, 0, 2F);

    // Safety
    private final BoolValue sameYValue = new BoolValue("SameY", false);
    private final BoolValue safeWalkValue = new BoolValue("SafeWalk", true);
    private final BoolValue airSafeValue = new BoolValue("AirSafe", false);

    // Visuals
    private final BoolValue counterDisplayValue = new BoolValue("Counter", true);
    private final BoolValue markValue = new BoolValue("Mark", false);

    /**
     * MODULE
     */

    // Target block
    private PlaceInfo targetPlace;


    // Launch position
    private int launchY;

    // Rotation lock
    private Rotation lockRotation;

    // Auto block slot
    private int slot;

    // Zitter Smooth
    private boolean zitterDirection;

    // Delay
    private final MSTimer delayTimer = new MSTimer();
    private final MSTimer zitterTimer = new MSTimer();
    private long delay;

    // Eagle
    private int placedBlocksWithoutEagle = 0;
    private boolean eagleSneaking;

    // Down
    private boolean shouldGoDown = false;

    /**
     * Enable module
     */
    @Override
    public void onEnable() {
        if (mc.getThePlayer() == null) return;

        launchY = (int) mc.getThePlayer().getPosY();
    }

    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        mc.getTimer().setTimerSpeed(timerValue.get());

        shouldGoDown = downValue.get() && mc.getGameSettings().isKeyDown(mc.getGameSettings().getKeyBindSneak()) && getBlocksAmount() > 1;
        if (shouldGoDown)
            mc.getGameSettings().getKeyBindSneak().setPressed(false);

        if (mc.getThePlayer().getOnGround()) {
            final String mode = modeValue.get();

            // Rewinside scaffold mode
            if (mode.equalsIgnoreCase("Rewinside")) {
                MovementUtils.strafe(0.2F);
                mc.getThePlayer().setMotionY(0D);
            }

            // Smooth Zitter
            if (zitterValue.get() && zitterModeValue.get().equalsIgnoreCase("smooth")) {
                if (!mc.getGameSettings().isKeyDown(mc.getGameSettings().getKeyBindRight()))
                    mc.getGameSettings().getKeyBindRight().setPressed(false);

                if (!mc.getGameSettings().isKeyDown(mc.getGameSettings().getKeyBindLeft()))
                    mc.getGameSettings().getKeyBindLeft().setPressed(false);

                if (zitterTimer.hasTimePassed(100)) {
                    zitterDirection = !zitterDirection;
                    zitterTimer.reset();
                }

                if (zitterDirection) {
                    mc.getGameSettings().getKeyBindRight().setPressed(true);
                    mc.getGameSettings().getKeyBindLeft().setPressed(false);
                } else {
                    mc.getGameSettings().getKeyBindRight().setPressed(false);
                    mc.getGameSettings().getKeyBindLeft().setPressed(true);
                }
            }

            // Eagle
            if (eagleValue.get() && !shouldGoDown) {
                if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                    final boolean shouldEagle = mc.getTheWorld().getBlockState(new WBlockPos(mc.getThePlayer().getPosX(),
                            mc.getThePlayer().getPosY() - 1D, mc.getThePlayer().getPosZ())).getBlock().equals(classProvider.getBlockEnum(BlockType.AIR));

                    if (eagleSilentValue.get()) {
                        if (eagleSneaking != shouldEagle) {
                            mc.getNetHandler().addToSendQueue(
                                    classProvider.createCPacketEntityAction(mc.getThePlayer(), shouldEagle ?
                                            ICPacketEntityAction.WAction.START_SNEAKING :
                                            ICPacketEntityAction.WAction.STOP_SNEAKING)
                            );
                        }

                        eagleSneaking = shouldEagle;
                    } else
                        mc.getGameSettings().getKeyBindSneak().setPressed(shouldEagle);

                    placedBlocksWithoutEagle = 0;
                } else
                    placedBlocksWithoutEagle++;
            }

            // Zitter
            if (zitterValue.get() && zitterModeValue.get().equalsIgnoreCase("teleport")) {
                MovementUtils.strafe(zitterSpeed.get());

                final double yaw = Math.toRadians(mc.getThePlayer().getRotationYaw() + (zitterDirection ? 90D : -90D));
                mc.getThePlayer().setMotionX(mc.getThePlayer().getMotionX() - Math.sin(yaw) * zitterStrength.get());
                mc.getThePlayer().setMotionZ(mc.getThePlayer().getMotionZ() + Math.cos(yaw) * zitterStrength.get());

                zitterDirection = !zitterDirection;
            }
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if (mc.getThePlayer() == null)
            return;

        final IPacket packet = event.getPacket();

        // AutoBlock
        if (classProvider.isCPacketHeldItemChange(packet)) {
            final ICPacketHeldItemChange packetHeldItemChange = packet.asCPacketHeldItemChange();

            slot = packetHeldItemChange.getSlotId();
        }
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        final EventState eventState = event.getEventState();

        // Lock Rotation
        if (rotationsValue.get() && keepRotationValue.get() && lockRotation != null)
            RotationUtils.setTargetRotation(lockRotation);

        // Place block
        if (placeModeValue.get().equalsIgnoreCase(eventState.getStateName()))
            place();

        // Update and search for new block
        if (eventState == EventState.PRE)
            update();

        // Reset placeable delay
        if (targetPlace == null && placeableDelay.get())
            delayTimer.reset();
    }

    private void update() {
        final boolean isHeldItemBlock = mc.getThePlayer().getHeldItem() != null && classProvider.isItemBlock(mc.getThePlayer().getHeldItem().getItem());
        if (autoBlockValue.get() ? InventoryUtils.findAutoBlockBlock() == -1 && !isHeldItemBlock : !isHeldItemBlock)
            return;

        findBlock(modeValue.get().equalsIgnoreCase("expand"));
    }

    /**
     * Search for new target block
     */
    private void findBlock(final boolean expand) {
        final WBlockPos blockPosition = shouldGoDown ? (mc.getThePlayer().getPosY() == (int) mc.getThePlayer().getPosY() + 0.5D ?
                new WBlockPos(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY() - 0.6D, mc.getThePlayer().getPosZ())
                : new WBlockPos(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY() - 0.6, mc.getThePlayer().getPosZ()).down()) :
                (mc.getThePlayer().getPosY() == (int) mc.getThePlayer().getPosY() + 0.5D ? new WBlockPos(mc.getThePlayer())
                        : new WBlockPos(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY(), mc.getThePlayer().getPosZ()).down());

        if (!expand && (!BlockUtils.isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown)))
            return;

        if (expand) {
            for (int i = 0; i < expandLengthValue.get(); i++) {
                if (search(blockPosition.add(
                        mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.WEST)) ? -i : mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.EAST)) ? i : 0,
                        0,
                        mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.NORTH)) ? -i : mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.SOUTH)) ? i : 0
                ), false))

                    return;
            }
        } else if (searchValue.get()) {
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown))
                        return;
        }
    }

    /**
     * Place target block
     */
    private void place() {
        if (targetPlace == null) {
            if (placeableDelay.get())
                delayTimer.reset();
            return;
        }

        if (!delayTimer.hasTimePassed(delay) || (sameYValue.get() && launchY - 1 != (int) targetPlace.getVec3().getYCoord()))
            return;

        int blockSlot = -1;
        IItemStack itemStack = mc.getThePlayer().getHeldItem();

        if (mc.getThePlayer().getHeldItem() == null || !(classProvider.isItemBlock(mc.getThePlayer().getHeldItem().getItem())) || mc.getThePlayer().getHeldItem().getStackSize() <= 0) {
            if (!autoBlockValue.get())
                return;

            blockSlot = InventoryUtils.findAutoBlockBlock();

            if (blockSlot == -1)
                return;

            mc.getNetHandler().addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36));
            itemStack = mc.getThePlayer().getInventory().getStackInSlot(blockSlot);
        }


        if (mc.getPlayerController().onPlayerRightClick(mc.getThePlayer(), mc.getTheWorld(), itemStack, targetPlace.getBlockPos(), targetPlace.getEnumFacing(), targetPlace.getVec3())) {
            delayTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (mc.getThePlayer().getOnGround()) {
                final float modifier = speedModifierValue.get();

                mc.getThePlayer().setMotionX(mc.getThePlayer().getMotionX() * modifier);
                mc.getThePlayer().setMotionZ(mc.getThePlayer().getMotionZ() * modifier);
            }

            if (swingValue.get())
                mc.getThePlayer().swingItem();
            else
                mc.getNetHandler().addToSendQueue(classProvider.createCPacketAnimation());
        }

        if (!stayAutoBlock.get() && blockSlot >= 0)
            mc.getNetHandler().addToSendQueue(classProvider.createCPacketHeldItemChange(mc.getThePlayer().getInventory().getCurrentItem()));

        // Reset
        this.targetPlace = null;
    }

    /**
     * Disable scaffold module
     */
    @Override
    public void onDisable() {
        if (mc.getThePlayer() == null) return;

        if (!mc.getGameSettings().isKeyDown(mc.getGameSettings().getKeyBindSneak())) {
            mc.getGameSettings().getKeyBindSneak().setPressed(false);

            if (eagleSneaking)
                mc.getNetHandler().addToSendQueue(classProvider.createCPacketEntityAction(mc.getThePlayer(), ICPacketEntityAction.WAction.STOP_SNEAKING));
        }

        if (!mc.getGameSettings().isKeyDown(mc.getGameSettings().getKeyBindRight()))
            mc.getGameSettings().getKeyBindRight().setPressed(false);

        if (!mc.getGameSettings().isKeyDown(mc.getGameSettings().getKeyBindLeft()))
            mc.getGameSettings().getKeyBindLeft().setPressed(false);

        lockRotation = null;
        mc.getTimer().setTimerSpeed(1F);
        shouldGoDown = false;

        if (slot != mc.getThePlayer().getInventory().getCurrentItem())
            mc.getNetHandler().addToSendQueue(classProvider.createCPacketHeldItemChange(mc.getThePlayer().getInventory().getCurrentItem()));
    }

    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    public void onMove(final MoveEvent event) {
        if (!safeWalkValue.get() || shouldGoDown)
            return;

        if (airSafeValue.get() || mc.getThePlayer().getOnGround())
            event.setSafeWalk(true);
    }

    /**
     * Scaffold visuals
     *
     * @param event
     */
    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        if (counterDisplayValue.get()) {
            GL11.glPushMatrix();

            final BlockOverlay blockOverlay = (BlockOverlay) LiquidBounce.moduleManager.getModule(BlockOverlay.class);
            if (blockOverlay.getState() && blockOverlay.getInfoValue().get() && blockOverlay.getCurrentBlock() != null)
                GL11.glTranslatef(0, 15F, 0);

            final String info = "Blocks: §7" + getBlocksAmount();
            final IScaledResolution scaledResolution = classProvider.createScaledResolution(mc);

            RenderUtils.drawBorderedRect((scaledResolution.getScaledWidth() / 2.0f) - 2,
                    (scaledResolution.getScaledHeight() / 2.0f) + 5,
                    (scaledResolution.getScaledWidth() / 2.0f) + Fonts.font40.getStringWidth(info) + 2,
                    (scaledResolution.getScaledHeight() / 2.0f) + 16, 3, Color.BLACK.getRGB(), Color.BLACK.getRGB());

            classProvider.getGlStateManager().resetColor();

            Fonts.font40.drawString(info, scaledResolution.getScaledWidth() / 2.0f,
                    scaledResolution.getScaledHeight() / 2.0f + 7, Color.WHITE.getRGB());

            GL11.glPopMatrix();
        }
    }

    /**
     * Scaffold visuals
     *
     * @param event
     */
    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if (!markValue.get())
            return;

        for (int i = 0; i < (modeValue.get().equalsIgnoreCase("Expand") ? expandLengthValue.get() + 1 : 2); i++) {
            final WBlockPos blockPos = new WBlockPos(mc.getThePlayer().getPosX() + (mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.WEST)) ? -i : mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.EAST)) ? i : 0), mc.getThePlayer().getPosY() - (mc.getThePlayer().getPosY() == (int) mc.getThePlayer().getPosY() + 0.5D ? 0D : 1.0D) - (shouldGoDown ? 1D : 0), mc.getThePlayer().getPosZ() + (mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.NORTH)) ? -i : mc.getThePlayer().getHorizontalFacing().equals(classProvider.getEnumFacing(EnumFacingType.SOUTH)) ? i : 0));
            final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

            if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, new Color(68, 117, 255, 100), false);
                break;
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */
    private boolean search(final WBlockPos blockPosition, final boolean checks) {
        if (!BlockUtils.isReplaceable(blockPosition))
            return false;

        final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() + mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());

        PlaceRotation placeRotation = null;

        for (final EnumFacingType facingType : EnumFacingType.values()) {
            IEnumFacing side = classProvider.getEnumFacing(facingType);
            final WBlockPos neighbor = blockPosition.offset(side);

            if (!BlockUtils.canBeClicked(neighbor))
                continue;

            final WVec3 dirVec = new WVec3(side.getDirectionVec());

            for (double xSearch = 0.1D; xSearch < 0.9D; xSearch += 0.1D) {
                for (double ySearch = 0.1D; ySearch < 0.9D; ySearch += 0.1D) {
                    for (double zSearch = 0.1D; zSearch < 0.9D; zSearch += 0.1D) {
                        final WVec3 posVec = new WVec3(blockPosition).addVector(xSearch, ySearch, zSearch);
                        final double distanceSqPosVec = eyesPos.squareDistanceTo(posVec);
                        final WVec3 hitVec = posVec.add(new WVec3(dirVec.getXCoord() * 0.5, dirVec.getYCoord() * 0.5, dirVec.getZCoord() * 0.5));

                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18D || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.getTheWorld().rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
                            continue;

                        // face block
                        final double diffX = hitVec.getXCoord() - eyesPos.getXCoord();
                        final double diffY = hitVec.getYCoord() - eyesPos.getYCoord();
                        final double diffZ = hitVec.getZCoord() - eyesPos.getZCoord();

                        final double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

                        final Rotation rotation = new Rotation(
                                WMathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F),
                                WMathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))
                        );

                        final WVec3 rotationVector = RotationUtils.getVectorForRotation(rotation);
                        final WVec3 vector = eyesPos.addVector(rotationVector.getXCoord() * 4, rotationVector.getYCoord() * 4, rotationVector.getZCoord() * 4);
                        final IMovingObjectPosition obj = mc.getTheWorld().rayTraceBlocks(eyesPos, vector, false, false, true);

                        if (obj.getTypeOfHit() != IMovingObjectPosition.WMovingObjectType.BLOCK || !obj.getBlockPos().equals(neighbor))
                            continue;

                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.getRotation()))
                            placeRotation = new PlaceRotation(new PlaceInfo(neighbor, side.getOpposite(), hitVec), rotation);
                    }
                }
            }
        }

        if (placeRotation == null) return false;

        if (rotationsValue.get()) {
            RotationUtils.setTargetRotation(placeRotation.getRotation(), keepLengthValue.get());
            lockRotation = placeRotation.getRotation();
        }
        targetPlace = placeRotation.getPlaceInfo();
        return true;
    }

    /**
     * @return hotbar blocks amount
     */
    private int getBlocksAmount() {
        int amount = 0;

        for (int i = 36; i < 45; i++) {
            final IItemStack itemStack = mc.getThePlayer().getInventoryContainer().getSlot(i).getStack();

            if (itemStack != null && classProvider.isItemBlock(itemStack.getItem())) {
                final IBlock block = (itemStack.getItem().asItemBlock()).getBlock();

                if (mc.getThePlayer().getHeldItem().equals(itemStack) || !InventoryUtils.BLOCK_BLACKLIST.contains(block))
                    amount += itemStack.getStackSize();
            }
        }

        return amount;
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
