/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSwing;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleLiquidPlace;
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData;
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerInventoryData;
import net.ccbluex.liquidbounce.integration.screen.ScreenManager;
import net.ccbluex.liquidbounce.interfaces.LocalPlayerAddition;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.ccbluex.liquidbounce.utils.raytracing.EntityRaytracingKt;
import net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends MixinPlayer implements LocalPlayerAddition {

    @Shadow
    public ClientInput input;

    @Shadow
    @Final
    public ClientPacketListener connection;

    @Shadow
    public abstract boolean isUnderWater();

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
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
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
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
            shift = At.Shift.AFTER,
            ordinal = 0))
    private void hookPostTickEvent(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(PlayerPostTickEvent.INSTANCE);

        // Call player statistics change event when statistics change
        var statistics = PlayerData.Companion.fromPlayer((LocalPlayer) (Object) this);
        if (lastKnownStatistics == null || !lastKnownStatistics.equals(statistics)) {
            EventManager.INSTANCE.callEvent(new ClientPlayerDataEvent(statistics));
        }
        this.lastKnownStatistics = statistics;

        // Call player inventory event when inventory changes
        var playerInventory = PlayerInventoryData.Companion.fromPlayer((LocalPlayer) (Object) this);
        if (lastKnownInventory == null || !lastKnownInventory.equals(playerInventory)) {
            EventManager.INSTANCE.callEvent(new ClientPlayerInventoryEvent(playerInventory));
        }
        this.lastKnownInventory = playerInventory;
    }

    /**
     * Hook entity movement tick event
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void hookMovementTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(PlayerMovementTickEvent.INSTANCE);
    }

    /**
     * Hook entity movement tick event at HEAD and call out PRE tick movement event
     */
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void hookMovementPre(CallbackInfo callbackInfo) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        eventMotion = new PlayerNetworkMovementTickEvent(EventState.PRE, player.getX(), player.getY(), player.getZ(), player.onGround());
        EventManager.INSTANCE.callEvent(eventMotion);

        if (eventMotion.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double modifyXPosition(double original) {
        return eventMotion.getX();
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double modifyYPosition(double original) {
        return eventMotion.getY();
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double modifyZPosition(double original) {
        return eventMotion.getZ();
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean modifyOnGround(boolean original) {
        return eventMotion.getGround();
    }

    /**
     * Hook entity movement tick event at RETURN and call out POST tick movement event
     */
    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void hookMovementPost(CallbackInfo callbackInfo) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.POST, player.getX(), player.getY(), player.getZ(), player.onGround()));
    }

    /**
     * Hook moveTowardsClosestSpace at HEAD and call PlayerPushoutEvent
     */
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
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
    private Vec3 hookMove(Vec3 movement, MoverType type) {
        return EventManager.INSTANCE.callEvent(new PlayerMoveEvent(type, movement)).getMovement();
    }

    /**
     * Hook counter for on ground and air ticks
     */
    @Inject(method = "move", at = @At("RETURN"))
    private void hookGroundAirTimeCounters(CallbackInfo ci) {
        if (this.onGround()) {
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
    @ModifyExpressionValue(method = "handlePortalTransitionEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isAllowedInPortal()Z"))
    private boolean hookNetherClosingScreen(boolean original) {
        if (ModulePortalMenu.INSTANCE.getRunning()) {
            return true;
        }

        return original;
    }

    /**
     * We change crossHairTarget according to server side rotations
     */
    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult hookRaycast(HitResult original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != Minecraft.getInstance().player) {
            return original;
        }

        var cameraRotation = new Rotation(camera.getViewYRot(tickDelta), camera.getViewXRot(tickDelta), true);

        Rotation rotation;
        if (RotationManager.INSTANCE.getCurrentRotation() != null) {
            rotation = RotationManager.INSTANCE.getCurrentRotation();
        } else if (ModuleFreeCam.INSTANCE.getRunning()) {
            var serverRotation = RotationManager.INSTANCE.getServerRotation();
            rotation = ModuleFreeCam.INSTANCE.shouldDisableCameraInteract() ? serverRotation : cameraRotation;
        } else {
            rotation = cameraRotation;
        }

        // Through Walls Reach
        if (ModuleReach.INSTANCE.getRunning()) {
            var throughWallsRange = ModuleReach.INSTANCE.getEntity().getInteractionThroughWallsRange$liquidbounce();

            if (throughWallsRange > 0.0) {
                var hitEntityResult = EntityRaytracingKt.findEntityInCrosshair(throughWallsRange, rotation, null);

                if (hitEntityResult != null && hitEntityResult.getType() == HitResult.Type.ENTITY) {
                    return hitEntityResult;
                }
            }
        }


        return RaytracingKt.traceFromPlayer(
            rotation,
            Math.max(blockInteractionRange, entityInteractionRange),
            ClipContext.Block.OUTLINE,
            ModuleLiquidPlace.INSTANCE.getRunning(),
            tickDelta
        );
    }

    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 hookRotationVector(Vec3 original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != Minecraft.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? rotation.directionVector() : original;
    }

    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static @Nullable EntityHitResult hookEntityHitResult(@Nullable EntityHitResult original) {
        return original == null || !ModuleNoEntityInteract.INSTANCE.test(original) ? null : original;
    }

    /**
     * Hook custom sneaking multiplier
     */
    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
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
    @WrapOperation(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;", ordinal = 1))
    private Vec2 hookCustomMultiplier(Vec2 instance, float value, Operation<Vec2> original) {
        var playerUseMultiplier = new PlayerUseMultiplier(value, value);
        EventManager.INSTANCE.callEvent(playerUseMultiplier);
        return new Vec2(
            instance.x * playerUseMultiplier.getSideways(),
            instance.y * playerUseMultiplier.getForward()
        );
    }

    /**
     * Hook sprint effect from NoSlow module
     */
    @ModifyExpressionValue(method = "isSlowDueToUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean hookSprintAffectStart(boolean original) {
        if (ModuleNoSlow.INSTANCE.getRunning()) {
            return false;
        }

        return original;
    }

    // Silent rotations (Rotation Manager)

    @ModifyExpressionValue(method = {"sendPosition",
        "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float hookSilentRotationYaw(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.yRot();
    }

    @ModifyExpressionValue(method = {"sendPosition",
        "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float hookSilentRotationPitch(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.xRot();
    }

    @ModifyReturnValue(method = "isAutoJumpEnabled", at = @At("RETURN"))
    private boolean injectAutoJumpAllowed(boolean original) {
        return EventManager.INSTANCE.callEvent(new AllowAutoJumpEvent(original)).isAllowed();
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void swingHand(InteractionHand hand, CallbackInfo ci) {
        if (ModuleNoSwing.INSTANCE.getRunning()) {
            if (!ModuleNoSwing.INSTANCE.shouldHideForServer()) {
                connection.send(new ServerboundSwingPacket(hand));
            }
            if (!ModuleNoSwing.INSTANCE.shouldHideForClient()) {
                swing(hand, false);
            }

            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "getJumpRidingScale", at = @At("RETURN"))
    private float hookMountJumpStrength(float original) {
        if (ModuleEntityControl.getEnforceJumpStrength()) {
            return 1f;
        }

        return original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z"))
    private boolean hookFreeCamPreventCreativeFly(boolean original) {
        return !ModuleFreeCam.INSTANCE.getRunning() && original;
    }

    @ModifyVariable(method = "sendPosition", at = @At("STORE"), ordinal = 1)
    private boolean hookFreeCamPreventRotations(boolean bl4) {
        // Prevent rotation changes when free cam is active, unless a rotation is being set via the rotation manager
        return (!ModuleFreeCam.INSTANCE.getRunning() ||
                RotationManager.INSTANCE.getCurrentRotation() != null) && bl4;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean hookSprint0(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.MOVEMENT_TICK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean hookSprint1(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.MOVEMENT_TICK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    // canStartSprinting calls canSprint(boolean) which then checks for blindness
    @ModifyExpressionValue(method = "isSprintingPossible(Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isMobilityRestricted()Z"))
    private boolean hookSprintIgnoreBlindness(boolean original) {
        return !ModuleSprint.INSTANCE.getShouldIgnoreBlindness() && original;
    }

    @ModifyExpressionValue(method = "shouldStopRunSprinting", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
    private boolean hookSprintIgnoreCollision(boolean original) {
        return !ModuleSprint.INSTANCE.getShouldIgnoreCollision() && original;
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean hookIsWalking(boolean original) {
        if (!ModuleSprint.INSTANCE.getShouldSprintOmnidirectional()) {
            return original;
        }

        float movementForward = input.getMoveVector().y;
        float movementSideways = input.getMoveVector().x;
        var hasMovement = Math.abs(movementForward) > 1.0E-5F ||
                Math.abs(movementSideways) > 1.0E-5F;
        var isWalking = (double) Math.abs(movementForward) >= 0.8 ||
                (double) Math.abs(movementSideways) >= 0.8;
        return this.isUnderWater() ? hasMovement : isWalking;
    }

    @ModifyExpressionValue(method = "sendIsSprintingIfNeeded", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z")
    )
    private boolean hookNetworkSprint(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.NETWORK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    @WrapWithCondition(method = "clientSideCloseContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private boolean preventCloseScreen(Minecraft instance, Screen screen) {
        // Prevent closing screen if the current screen is a client screen
        return !ScreenManager.isClientScreen(screen);
    }

}
