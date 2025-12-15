/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleEntityControl;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPush;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoPushBy;
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoEntityInteract;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSwing;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleLiquidPlace;
import net.ccbluex.liquidbounce.integration.BrowserScreen;
import net.ccbluex.liquidbounce.integration.VirtualDisplayScreen;
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData;
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerInventoryData;
import net.ccbluex.liquidbounce.interfaces.ClientPlayerEntityAddition;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.utils.RaytracingKt;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends MixinPlayerEntity implements ClientPlayerEntityAddition {

    @Shadow
    public Input input;

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    public abstract boolean isSubmergedInWater();

    @Unique
    private PlayerData lastKnownStatistics = null;

    @Unique
    private PlayerInventoryData lastKnownInventory = null;

    @Unique
    private PlayerNetworkMovementTickEvent eventMotion;

    @Unique
    private int onGroundTicks = 0;
    @Unique
    private int airTicks = 0;

    /**
     * Hook entity tick event
     */
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
            shift = At.Shift.BEFORE,
            ordinal = 0),
            cancellable = true)
    private void hookTickEvent(CallbackInfo ci) {
        var tickEvent = new PlayerTickEvent();
        EventManager.INSTANCE.callEvent(tickEvent);

        if (tickEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
            shift = At.Shift.AFTER,
            ordinal = 0))
    private void hookPostTickEvent(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(PlayerPostTickEvent.INSTANCE);

        // Call player statistics change event when statistics change
        var statistics = PlayerData.Companion.fromPlayer((ClientPlayerEntity) (Object) this);
        if (lastKnownStatistics == null || !lastKnownStatistics.equals(statistics)) {
            EventManager.INSTANCE.callEvent(new ClientPlayerDataEvent(statistics));
        }
        this.lastKnownStatistics = statistics;

        // Call player inventory event when inventory changes
        var playerInventory = PlayerInventoryData.Companion.fromPlayer((ClientPlayerEntity) (Object) this);
        if (lastKnownInventory == null || !lastKnownInventory.equals(playerInventory)) {
            EventManager.INSTANCE.callEvent(new ClientPlayerInventoryEvent(playerInventory));
        }
        this.lastKnownInventory = playerInventory;
    }

    /**
     * Hook entity movement tick event
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookMovementTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(PlayerMovementTickEvent.INSTANCE);
    }

    /**
     * Hook entity movement tick event at HEAD and call out PRE tick movement event
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void hookMovementPre(CallbackInfo callbackInfo) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        eventMotion = new PlayerNetworkMovementTickEvent(EventState.PRE, player.getX(), player.getY(), player.getZ(), player.isOnGround());
        EventManager.INSTANCE.callEvent(eventMotion);

        if (eventMotion.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double modifyXPosition(double original) {
        return eventMotion.getX();
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double modifyYPosition(double original) {
        return eventMotion.getY();
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double modifyZPosition(double original) {
        return eventMotion.getZ();
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean modifyOnGround(boolean original) {
        return eventMotion.getGround();
    }

    /**
     * Hook entity movement tick event at RETURN and call out POST tick movement event
     */
    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void hookMovementPost(CallbackInfo callbackInfo) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.POST, player.getX(), player.getY(), player.getZ(), player.isOnGround()));
    }

    /**
     * Hook push out function tick at HEAD and call out push out event, which is able to stop the cancel the execution.
     */
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void hookPushOut(double x, double z, CallbackInfo ci) {
        if (!ModuleNoPush.canPush(NoPushBy.BLOCKS)) {
            ci.cancel();
            return;
        }

        final PlayerPushOutEvent pushOutEvent = new PlayerPushOutEvent();
        EventManager.INSTANCE.callEvent(pushOutEvent);
        if (pushOutEvent.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Hook move function to modify movement
     */
    @ModifyVariable(method = "move", at = @At("HEAD"), name = "arg2", ordinal = 0, index = 2, argsOnly = true)
    private Vec3d hookMove(Vec3d movement, MovementType type) {
        return EventManager.INSTANCE.callEvent(new PlayerMoveEvent(type, movement)).getMovement();
    }

    /**
     * Hook counter for on ground and air ticks
     */
    @Inject(method = "move", at = @At("RETURN"))
    private void hookGroundAirTimeCounters(CallbackInfo ci) {
        if (this.isOnGround()) {
            onGroundTicks++;
            airTicks = 0;
        } else {
            airTicks++;
            onGroundTicks = 0;
        }
    }

    @Override
    public int liquid_bounce$getOnGroundTicks() {
        return onGroundTicks;
    }

    @Override
    public int liquid_bounce$getAirTicks() {
        return airTicks;
    }

    /**
     * Hook portal menu module to make opening menus in portals possible
     */
    @ModifyExpressionValue(method = "tickNausea", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;keepOpenThroughPortal()Z"))
    private boolean hookNetherClosingScreen(boolean original) {
        if (ModulePortalMenu.INSTANCE.getRunning()) {
            return true;
        }

        return original;
    }

    /**
     * We change crossHairTarget according to server side rotations
     */
    @ModifyExpressionValue(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private static HitResult hookRaycast(HitResult original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != MinecraftClient.getInstance().player) {
            return original;
        }

        var cameraRotation = new Rotation(camera.getYaw(tickDelta), camera.getPitch(tickDelta), true);

        Rotation rotation;
        if (RotationManager.INSTANCE.getCurrentRotation() != null) {
            rotation = RotationManager.INSTANCE.getCurrentRotation();
        } else if (ModuleFreeCam.INSTANCE.getRunning()) {
            var serverRotation = RotationManager.INSTANCE.getServerRotation();
            rotation = ModuleFreeCam.INSTANCE.shouldDisableCameraInteract() ? serverRotation : cameraRotation;
        } else {
            rotation = cameraRotation;
        }

        return RaytracingKt.raycast(rotation, Math.max(blockInteractionRange, entityInteractionRange),
            ModuleLiquidPlace.INSTANCE.getRunning(), tickDelta);
    }

    @ModifyExpressionValue(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d hookRotationVector(Vec3d original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? rotation.getDirectionVector() : original;
    }

    @ModifyExpressionValue(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"))
    private static @Nullable EntityHitResult hookEntityHitResult(@Nullable EntityHitResult original) {
        return original == null || !ModuleNoEntityInteract.INSTANCE.test(original) ? null : original;
    }

    /**
     * Hook custom sneaking multiplier
     */
    @ModifyExpressionValue(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"))
    private double hookCustomSneakingMultiplier(double original) {
        var playerSneakMultiplier = new PlayerSneakMultiplier(original);
        EventManager.INSTANCE.callEvent(playerSneakMultiplier);
        return playerSneakMultiplier.getMultiplier();
    }

    /**
     * Hook custom multiplier
     *
     * <pre>
     * if (this.isUsingItem() && !this.hasVehicle()) {
     *     vec2f = vec2f.multiply(this.getActiveItemSpeedMultiplier());
     * }
     * </pre>
     */
    @WrapOperation(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;", ordinal = 1))
    private Vec2f hookCustomMultiplier(Vec2f instance, float value, Operation<Vec2f> original) {
        var playerUseMultiplier = new PlayerUseMultiplier(value, value);
        EventManager.INSTANCE.callEvent(playerUseMultiplier);
        return new Vec2f(
            instance.x * playerUseMultiplier.getSideways(),
            instance.y * playerUseMultiplier.getForward()
        );
    }

    /**
     * Hook sprint effect from NoSlow module
     */
    @ModifyExpressionValue(method = "isBlockedFromSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean hookSprintAffectStart(boolean original) {
        if (ModuleNoSlow.INSTANCE.getRunning()) {
            return false;
        }

        return original;
    }

    // Silent rotations (Rotation Manager)

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float hookSilentRotationYaw(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.getYaw();
    }

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float hookSilentRotationPitch(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.getPitch();
    }

    @ModifyReturnValue(method = "isAutoJumpEnabled", at = @At("RETURN"))
    private boolean injectAutoJumpAllowed(boolean original) {
        return EventManager.INSTANCE.callEvent(new AllowAutoJumpEvent(original)).isAllowed();
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void swingHand(Hand hand, CallbackInfo ci) {
        if (ModuleNoSwing.INSTANCE.getRunning()) {
            if (!ModuleNoSwing.INSTANCE.shouldHideForServer()) {
                networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            }
            if (!ModuleNoSwing.INSTANCE.shouldHideForClient()) {
                swingHand(hand, false);
            }

            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "getMountJumpStrength", at = @At("RETURN"))
    private float hookMountJumpStrength(float original) {
        if (ModuleEntityControl.getEnforceJumpStrength()) {
            return 1f;
        }

        return original;
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z"))
    private boolean hookFreeCamPreventCreativeFly(boolean original) {
        return !ModuleFreeCam.INSTANCE.getRunning() && original;
    }

    @ModifyVariable(method = "sendMovementPackets", at = @At("STORE"), ordinal = 1)
    private boolean hookFreeCamPreventRotations(boolean bl4) {
        // Prevent rotation changes when free cam is active, unless a rotation is being set via the rotation manager
        return (!ModuleFreeCam.INSTANCE.getRunning() ||
                RotationManager.INSTANCE.getCurrentRotation() != null) && bl4;
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canStartSprinting()Z"))
    private boolean hookSprint0(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.MOVEMENT_TICK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PlayerInput;sprint()Z"))
    private boolean hookSprint1(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.MOVEMENT_TICK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    // canStartSprinting calls canSprint(boolean) which then checks for blindness
    @ModifyExpressionValue(method = "canSprint(Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasBlindnessEffect()Z"))
    private boolean hookSprintIgnoreBlindness(boolean original) {
        return !ModuleSprint.INSTANCE.getShouldIgnoreBlindness() && original;
    }

    @ModifyExpressionValue(method = "shouldStopSprinting", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;horizontalCollision:Z"))
    private boolean hookSprintIgnoreCollision(boolean original) {
        return !ModuleSprint.INSTANCE.getShouldIgnoreCollision() && original;
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;hasForwardMovement()Z"))
    private boolean hookIsWalking(boolean original) {
        if (!ModuleSprint.INSTANCE.getShouldSprintOmnidirectional()) {
            return original;
        }

        float movementForward = input.getMovementInput().y;
        float movementSideways = input.getMovementInput().x;
        var hasMovement = Math.abs(movementForward) > 1.0E-5F ||
                Math.abs(movementSideways) > 1.0E-5F;
        var isWalking = (double) Math.abs(movementForward) >= 0.8 ||
                (double) Math.abs(movementSideways) >= 0.8;
        return this.isSubmergedInWater() ? hasMovement : isWalking;
    }

    @ModifyExpressionValue(method = "sendSprintingPacket", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z")
    )
    private boolean hookNetworkSprint(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.NETWORK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    @WrapWithCondition(method = "closeScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    private boolean preventCloseScreen(MinecraftClient instance, Screen screen) {
        // Prevent closing screen if the current screen is a client screen
        return !(instance.currentScreen instanceof BrowserScreen || instance.currentScreen instanceof VirtualDisplayScreen ||
                instance.currentScreen instanceof ModuleClickGui.ClickScreen);
    }

}
