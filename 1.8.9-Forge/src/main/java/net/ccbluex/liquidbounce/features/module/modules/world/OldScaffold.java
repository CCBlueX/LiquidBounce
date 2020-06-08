/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay;
import net.ccbluex.liquidbounce.script.api.Chat;
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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import java.awt.*;

@ModuleInfo(name = "OldScaffold", description = "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD)
public class OldScaffold extends Module {

    /**
     * OPTIONS
     */

    // Mode
    public final ListValue modeValue = new ListValue("Mode", new String[]{"Normal", "Rewinside", "Expand"}, "Normal");

    // Delay
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 0, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = minDelayValue.get();

            if (i > newValue)
                set(i);
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 0, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = maxDelayValue.get();

            if (i < newValue)
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
    private final ListValue placeModeValue = new ListValue("PlaceTiming", new String[]{"Pre", "Post"}, "Post");

    // Eagle
    private final ListValue eagleValue = new ListValue("Eagle", new String[]{"Normal", "EdgeDistance", "Silent", "Off"}, "Off");
    private final IntegerValue blocksToEagleValue = new IntegerValue("BlocksToEagle", 0, 0, 10);
    private final FloatValue edgeDistanceValue = new FloatValue("EdgeDistance", 0.2F, 0F, 0.5F);

    // Expand
    private final IntegerValue expandLengthValue = new IntegerValue("ExpandLength", 5, 1, 6);

    // Rotations
    public final ListValue rotationModeValue = new ListValue("RotationMode", new String[]{"Normal", "Static", "StaticPitch", "StaticYaw", "Off"}, "Normal");

    public final BoolValue rotationStrafeValue = new BoolValue("RotationStrafe", false) {
        @Override
        protected void onChanged(final Boolean oldValue, final Boolean newValue) {
            if (newValue)
                keepRotationValue.set(true);
        }
    };
    private final BoolValue keepRotationValue = new BoolValue("KeepRotation", false);

    public final BoolValue clientSideRotationsValue = new BoolValue("ClientSideRotations", false);
    private final FloatValue xzRangeValue = new FloatValue("xzRange", 0.8F, 0.1F, 1.0F);
    private final FloatValue yRangeValue = new FloatValue("yRange", 0.8F, 0.1F, 1.0F);
    private final IntegerValue searchAccuracyValue = new IntegerValue("SearchAccuracy", 8, 1, 16) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            if (getMaximum() < newValue) {
                set(getMaximum());
            } else if (getMinimum() > newValue) {
                set(getMinimum());
            }
        }
    };
    // Turn Speed
    private final FloatValue maxTurnSpeedValue = new FloatValue("MaxTurnSpeed", 180, 1, 180) {
        @Override
        protected void onChanged(final Float oldValue, final Float newValue) {
            float v = minTurnSpeedValue.get();
            if (v > newValue) set(v);
            if (getMaximum() < newValue) {
                set(getMaximum());
            } else if (getMinimum() > newValue) {
                set(getMinimum());
            }
        }
    };
    private final FloatValue minTurnSpeedValue = new FloatValue("MinTurnSpeed", 180, 1, 180) {
        @Override
        protected void onChanged(final Float oldValue, final Float newValue) {
            float v = maxTurnSpeedValue.get();
            if (v < newValue) set(v);
            if (getMaximum() < newValue) {
                set(getMaximum());
            } else if (getMinimum() > newValue) {
                set(getMinimum());
            }
        }
    };
    private final FloatValue staticPitchValue = new FloatValue("StaticPitch", 86F, 70F, 90F);
    private final FloatValue staticYawOffsetValue = new FloatValue("StaticYawOffset", 0F, 0F, 90F);
    private final IntegerValue keepLengthValue = new IntegerValue("KeepRotationLength", 0, 0, 20);


    // Zitter
    private final BoolValue zitterValue = new BoolValue("Zitter", false);
    private final ListValue zitterModeValue = new ListValue("ZitterMode", new String[]{"Teleport", "Smooth"}, "Teleport");
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

    // Rotation + lock
    private Rotation lockRotation;
    private Rotation limitedRotation;
    private boolean facesBlock = false;

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

    /**
     * Enable module
     */
    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        launchY = (int) mc.thePlayer.posY;
    }

    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        mc.timer.timerSpeed = timerValue.get();

        if (mc.thePlayer.onGround) {
            final String mode = modeValue.get();

            // Rewinside scaffold mode
            if (mode.equalsIgnoreCase("Rewinside")) {
                MovementUtils.strafe(0.2F);
                mc.thePlayer.motionY = 0D;
            }

            // Smooth Zitter
            if (zitterValue.get() && zitterModeValue.get().equalsIgnoreCase("smooth")) {
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight))
                    mc.gameSettings.keyBindRight.pressed = false;

                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
                    mc.gameSettings.keyBindLeft.pressed = false;

                if (zitterTimer.hasTimePassed(100)) {
                    zitterDirection = !zitterDirection;
                    zitterTimer.reset();
                }

                if (zitterDirection) {
                    mc.gameSettings.keyBindRight.pressed = true;
                    mc.gameSettings.keyBindLeft.pressed = false;
                } else {
                    mc.gameSettings.keyBindRight.pressed = false;
                    mc.gameSettings.keyBindLeft.pressed = true;
                }
            }

            // Eagle
            if (!eagleValue.get().equalsIgnoreCase("Off")) {
                double dif = 0.5D;
                if (eagleValue.get().equalsIgnoreCase("EdgeDistance")) {
                    for (int i = 0; i < 4; i++) {
                        switch (i) {
                            case 0: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX - 1, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posX - blockPos.getX();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                            case 1: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + 1, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posX - blockPos.getX();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                            case 2: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ - 1);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posZ - blockPos.getZ();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                            case 3: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ + 1);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posZ - blockPos.getZ();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                        }
                    }
                }

                if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                    final boolean shouldEagle = mc.theWorld.getBlockState(
                            new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1D, mc.thePlayer.posZ)).getBlock() == Blocks.air || (dif < edgeDistanceValue.get() && eagleValue.get().equalsIgnoreCase("EdgeDistance"));

                    if (eagleValue.get().equalsIgnoreCase("Silent")) {
                        if (eagleSneaking != shouldEagle) {
                            mc.getNetHandler().addToSendQueue(
                                    new C0BPacketEntityAction(mc.thePlayer, shouldEagle ?
                                            C0BPacketEntityAction.Action.START_SNEAKING :
                                            C0BPacketEntityAction.Action.STOP_SNEAKING)
                            );
                        }

                        eagleSneaking = shouldEagle;
                    } else
                        mc.gameSettings.keyBindSneak.pressed = shouldEagle;

                    placedBlocksWithoutEagle = 0;
                } else
                    placedBlocksWithoutEagle++;
            }

            // Zitter
            if (zitterValue.get() && zitterModeValue.get().equalsIgnoreCase("teleport")) {
                MovementUtils.strafe(zitterSpeed.get());


                final double yaw = Math.toRadians(mc.thePlayer.rotationYaw + (zitterDirection ? 90D : -90D));
                mc.thePlayer.motionX -= Math.sin(yaw) * zitterStrength.get();
                mc.thePlayer.motionZ += Math.cos(yaw) * zitterStrength.get();
                zitterDirection = !zitterDirection;
            }
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if (mc.thePlayer == null)
            return;

        final Packet<?> packet = event.getPacket();

        // AutoBlock
        if (packet instanceof C09PacketHeldItemChange) {
            final C09PacketHeldItemChange packetHeldItemChange = (C09PacketHeldItemChange) packet;

            slot = packetHeldItemChange.getSlotId();
        }
    }

    /**
     * Strafe event
     */
    @EventTarget
    private void onStrafe(StrafeEvent event) {

        if (!rotationStrafeValue.get())
            return;

        if (lockRotation != null && keepRotationValue.get()) {
            final int dif = (int) ((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - (minTurnSpeedValue.get() < 180 ? limitedRotation : lockRotation).getYaw()
                    - 23.5F - 135)
                    + 180) / 45);

            final float yaw = (minTurnSpeedValue.get() < 180 ? limitedRotation : lockRotation).getYaw();
            final float strafe = event.getStrafe();
            final float forward = event.getForward();
            final float friction = event.getFriction();
            float calcForward = 0F;
            float calcStrafe = 0F;
            /*
            Rotation Dif

            7 \ 0 / 1     +  +  +      +  |  -
            6   +   2     -- F --      +  S  -
            5 / 4 \ 3     -  -  -      +  |  -
            */
            switch (dif) {
                case 0: {
                    calcForward = forward;
                    calcStrafe = strafe;
                    break;
                }
                case 1: {
                    calcForward += forward;
                    calcStrafe -= forward;
                    calcForward += strafe;
                    calcStrafe += strafe;
                    break;
                }
                case 2: {
                    calcForward = strafe;
                    calcStrafe = -forward;
                    break;
                }
                case 3: {
                    calcForward -= forward;
                    calcStrafe -= forward;
                    calcForward += strafe;
                    calcStrafe -= strafe;
                    break;
                }
                case 4: {
                    calcForward = -forward;
                    calcStrafe = -strafe;
                    break;
                }
                case 5: {
                    calcForward -= forward;
                    calcStrafe += forward;
                    calcForward -= strafe;
                    calcStrafe -= strafe;
                    break;
                }
                case 6: {
                    calcForward = -strafe;
                    calcStrafe = forward;
                    break;
                }
                case 7: {
                    calcForward += forward;
                    calcStrafe += forward;
                    calcForward -= strafe;
                    calcStrafe += strafe;
                    break;
                }
            }

            if (calcForward > 1F) {
                calcForward *= 0.5F;
            } else if (calcForward < 0.9F && calcForward > 0.3F) {
                calcForward *= 0.5F;
            }
            if (calcForward < -1F) {
                calcForward *= 0.5F;
            } else if (calcForward > -0.9F && calcForward < -0.3F) {
                calcForward *= 0.5F;
            }

            if (calcStrafe > 1F) {
                calcStrafe *= 0.5F;
            } else if (calcStrafe < 0.9F && calcStrafe > 0.3F) {
                calcStrafe *= 0.5F;
            }
            if (calcStrafe < -1F) {
                calcStrafe *= 0.5F;
            } else if (calcStrafe > -0.9F && calcStrafe < -0.3F) {
                calcStrafe *= 0.5F;
            }

            float f = calcStrafe * calcStrafe + calcForward * calcForward;

            if (f >= 1.0E-4F) {
                f = MathHelper.sqrt_float(f);

                if (f < 1.0F)
                    f = 1.0F;

                f = friction / f;
                calcStrafe *= f;
                calcForward *= f;

                final float yawSin = MathHelper.sin((float) (yaw * Math.PI / 180F));
                final float yawCos = MathHelper.cos((float) (yaw * Math.PI / 180F));

                mc.thePlayer.motionX += calcStrafe * yawCos - calcForward * yawSin;
                mc.thePlayer.motionZ += calcForward * yawCos + calcStrafe * yawSin;
            }
            event.cancelEvent();
        }
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        // Lock Rotation
        if (!rotationModeValue.get().equalsIgnoreCase("Off") && keepRotationValue.get() && lockRotation != null)
            setRotation(lockRotation);

        final String mode = modeValue.get();
        final EventState eventState = event.getEventState();

        if ((facesBlock || rotationModeValue.get().equalsIgnoreCase("Off")) && placeModeValue.get().equalsIgnoreCase(eventState.getStateName()))
            place();

        if (eventState == EventState.PRE) {
            if (autoBlockValue.get() ? InventoryUtils.findAutoBlockBlock() == -1 : mc.thePlayer.getHeldItem() == null ||
                    !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock))
                return;

            findBlock(mode.equalsIgnoreCase("expand"));
        }

        if (targetPlace == null) {
            if (placeableDelay.get())
                delayTimer.reset();
        }
    }

    private void setRotation(Rotation r, int keepR) {
        if (clientSideRotationsValue.get()) {
            mc.thePlayer.rotationYaw = r.getYaw();
            mc.thePlayer.rotationPitch = r.getPitch();
        } else {
            RotationUtils.setTargetRotation(r, keepR);
        }
    }

    private void setRotation(Rotation r) {
        setRotation(r, 0);
    }

    /**
     * Search for new target block
     */
    private void findBlock(final boolean expand) {
        final BlockPos blockPosition = mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? new BlockPos(mc.thePlayer)
                : new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down();

        if (!expand && (!BlockUtils.isReplaceable(blockPosition) || search(blockPosition, true)))
            return;

        if (expand) {
            for (int i = 0; i < expandLengthValue.get(); i++) {
                if (search(blockPosition.add(
                        mc.thePlayer.getHorizontalFacing() == EnumFacing.WEST ? -i : mc.thePlayer.getHorizontalFacing() == EnumFacing.EAST ? i : 0,
                        0,
                        mc.thePlayer.getHorizontalFacing() == EnumFacing.NORTH ? -i : mc.thePlayer.getHorizontalFacing() == EnumFacing.SOUTH ? i : 0
                ), false))

                    return;
            }
        } else if (searchValue.get()) {
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    if (search(blockPosition.add(x, 0, z), true))
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

        if (!delayTimer.hasTimePassed(delay) || (sameYValue.get() && launchY != (int) mc.thePlayer.posY))
            return;

        int blockSlot = -1;
        ItemStack itemStack = mc.thePlayer.getHeldItem();

        if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            if (!autoBlockValue.get())
                return;

            blockSlot = InventoryUtils.findAutoBlockBlock();

            if (blockSlot == -1)
                return;

            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot - 36));
            itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).getStack();
        }

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, targetPlace.getBlockPos(),
                targetPlace.getEnumFacing(), targetPlace.getVec3())) {
            delayTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (mc.thePlayer.onGround) {
                final float modifier = speedModifierValue.get();

                mc.thePlayer.motionX *= modifier;
                mc.thePlayer.motionZ *= modifier;
            }

            if (swingValue.get())
                mc.thePlayer.swingItem();
            else
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
        }

        if (!stayAutoBlock.get() && blockSlot >= 0)
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));

        // Reset
        this.targetPlace = null;
    }

    /**
     * Disable scaffold module
     */
    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false;

            if (eagleSneaking)
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight))
            mc.gameSettings.keyBindRight.pressed = false;

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
            mc.gameSettings.keyBindLeft.pressed = false;

        lockRotation = null;
        limitedRotation = null;
        facesBlock = false;
        mc.timer.timerSpeed = 1F;

        if (slot != mc.thePlayer.inventory.currentItem)
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }

    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    public void onMove(final MoveEvent event) {
        if (!safeWalkValue.get())
            return;

        if (airSafeValue.get() || mc.thePlayer.onGround)
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
            GlStateManager.pushMatrix();

            final BlockOverlay blockOverlay = (BlockOverlay) LiquidBounce.moduleManager.getModule(BlockOverlay.class);
            if (blockOverlay.getState() && blockOverlay.getInfoValue().get() && blockOverlay.getCurrentBlock() != null)
                GlStateManager.translate(0, 15F, 0);

            final String info = "Blocks: §7" + getBlocksAmount();
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            RenderUtils.drawBorderedRect((scaledResolution.getScaledWidth() / 2) - 2, (scaledResolution.getScaledHeight() / 2) + 5, (scaledResolution.getScaledWidth() / 2) + Fonts.font40.getStringWidth(info) + 2, (scaledResolution.getScaledHeight() / 2) + 16, 3, Color.BLACK.getRGB(), Color.BLACK.getRGB());
            GlStateManager.resetColor();
            Fonts.font40.drawString(info, scaledResolution.getScaledWidth() / 2, scaledResolution.getScaledHeight() / 2 + 7, Color.WHITE.getRGB());

            GlStateManager.popMatrix();
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
            final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + (mc.thePlayer.getHorizontalFacing() == EnumFacing.WEST ? -i : mc.thePlayer.getHorizontalFacing() == EnumFacing.EAST ? i : 0), mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ + (mc.thePlayer.getHorizontalFacing() == EnumFacing.NORTH ? -i : mc.thePlayer.getHorizontalFacing() == EnumFacing.SOUTH ? i : 0));
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
    private boolean search(final BlockPos blockPosition, final boolean checks) {
        if (!BlockUtils.isReplaceable(blockPosition))
            return false;
// StaticModes
        final boolean staticMode = rotationModeValue.get().equalsIgnoreCase("Static");
        final boolean staticPitchMode = staticMode || rotationModeValue.get().equalsIgnoreCase("StaticPitch");
        final boolean staticYawMode = staticMode || rotationModeValue.get().equalsIgnoreCase("StaticYaw");
        final float staticPitch = staticPitchValue.get();
        final float staticYawOffset = staticYawOffsetValue.get();

        // SearchRanges
        final double xzRV = xzRangeValue.get();
        final double xzSSV = calcStepSize(xzRV);
        final double yRV = yRangeValue.get();
        final double ySSV = calcStepSize(yRV);

        double xSearchFace = 0;
        double ySearchFace = 0;
        double zSearchFace = 0;


        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        PlaceRotation placeRotation = null;

        for (final EnumFacing side : EnumFacing.values()) {
            final BlockPos neighbor = blockPosition.offset(side);

            if (!BlockUtils.canBeClicked(neighbor))
                continue;

            final Vec3 dirVec = new Vec3(side.getDirectionVec());

            for (double xSearch = 0.5D - (xzRV / 2); xSearch <= 0.5D + (xzRV / 2); xSearch += xzSSV) {
                for (double ySearch = 0.5D - (yRV / 2); ySearch <= 0.5D + (yRV / 2); ySearch += ySSV) {
                    for (double zSearch = 0.5D - (xzRV / 2); zSearch <= 0.5D + (xzRV / 2); zSearch += xzSSV) {
                        final Vec3 posVec = new Vec3(blockPosition).addVector(xSearch, ySearch, zSearch);
                        final double distanceSqPosVec = eyesPos.squareDistanceTo(posVec);
                        final Vec3 hitVec = posVec.add(new Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5));

                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18D || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
                            continue;

                        // face block
                        for (int i = 0; i < (staticYawMode ? 2 : 1); i++) {
                            final double diffX = staticYawMode && i == 0 ? 0 : hitVec.xCoord - eyesPos.xCoord;
                            final double diffY = hitVec.yCoord - eyesPos.yCoord;
                            final double diffZ = staticYawMode && i == 1 ? 0 : hitVec.zCoord - eyesPos.zCoord;

                            final double diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

                            final float pitch = staticPitchMode ? staticPitch : MathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));
                            final Rotation rotation = new Rotation(
                                    MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F +
                                            (staticYawMode ? staticYawOffset : 0)), pitch);

                            final Vec3 rotationVector = RotationUtils.getVectorForRotation(rotation);
                            final Vec3 vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4);
                            final MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true);

                            if (!(obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.getBlockPos().equals(neighbor)))
                                continue;

                            if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.getRotation())) {
                                placeRotation = new PlaceRotation(new PlaceInfo(neighbor, side.getOpposite(), hitVec), rotation);
                            }
                            xSearchFace = xSearch;
                            ySearchFace = ySearch;
                            zSearchFace = zSearch;
                        }
                    }
                }
            }
        }

        if (placeRotation == null) return false;

        if (!rotationModeValue.get().equalsIgnoreCase("Off")) {
            if (minTurnSpeedValue.get() < 180) {
                limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, placeRotation.getRotation(), (float) (Math.random() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()));
                setRotation(limitedRotation, keepLengthValue.get());
                lockRotation = limitedRotation;

                facesBlock = false;
                for (final EnumFacing side : EnumFacing.values()) {
                    final BlockPos neighbor = blockPosition.offset(side);

                    if (!BlockUtils.canBeClicked(neighbor))
                        continue;

                    final Vec3 dirVec = new Vec3(side.getDirectionVec());

                    final Vec3 posVec = new Vec3(blockPosition).addVector(xSearchFace, ySearchFace, zSearchFace);
                    final double distanceSqPosVec = eyesPos.squareDistanceTo(posVec);
                    final Vec3 hitVec = posVec.add(new Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5));

                    if (checks && (eyesPos.squareDistanceTo(hitVec) > 18D || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
                        continue;

                    final Vec3 rotationVector = RotationUtils.getVectorForRotation(limitedRotation);
                    final Vec3 vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4);
                    final MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true);

                    if (!(obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.getBlockPos().equals(neighbor)))
                        continue;
                    facesBlock = true;
                    break;
                }
            } else {
                setRotation(placeRotation.getRotation(), keepLengthValue.get());
                lockRotation = placeRotation.getRotation();
                facesBlock = true;
            }
        }
        targetPlace = placeRotation.getPlaceInfo();
        return true;
    }

    private double calcStepSize(double range) {
        double accuracy = searchAccuracyValue.get();
        accuracy += accuracy % 2; // If it is set to uneven it changes it to even. Fixes a bug
        if (range / accuracy < 0.01D)
            return 0.01D;
        return range / accuracy;
    }

    /**
     * @return hotbar blocks amount
     */
    private int getBlocksAmount() {
        int amount = 0;

        for (int i = 36; i < 45; i++) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (itemStack != null && itemStack.getItem() instanceof ItemBlock)
                amount += itemStack.stackSize;
        }

        return amount;
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
