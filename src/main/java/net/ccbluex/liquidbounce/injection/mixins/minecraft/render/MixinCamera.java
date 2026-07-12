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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleDroneControl;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAspect;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoFov;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleQuickPerspectiveSwap;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSmoothCamera;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private boolean detached;
    @Shadow
    private float yRot;
    @Shadow
    private float xRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract float getMaxZoom(float maxZoom);

    @Shadow
    protected abstract void move(float zoom, float dy, float dx);

    @Shadow
    public abstract void setPosition(Vec3 pos);

    @Shadow
    private @Nullable Entity entity;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void hookFreeCamModifiedPosition(float partialTicks, CallbackInfo ci) {
        ModuleFreeCam.INSTANCE.applyCameraPosition(this.entity, partialTicks);
    }

    @Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void modifyCameraOrientation(float partialTicks, CallbackInfo ci) {
        var freeLook = ModuleFreeLook.INSTANCE.getRunning();
        var freeLockInvertedView = ModuleFreeLook.INSTANCE.isInvertedView();
        var qps = ModuleQuickPerspectiveSwap.INSTANCE.getRunning();
        var rearView = qps && ModuleQuickPerspectiveSwap.INSTANCE.getRearView() && !freeLook && !this.detached;

        if (freeLook || qps) {
            if (!rearView) this.detached = true;

            if (freeLook) {
                var cameraYaw = ModuleFreeLook.INSTANCE.getCameraYaw();
                var cameraPitch = ModuleFreeLook.INSTANCE.getCameraPitch();

                if (freeLockInvertedView) {
                    setRotation(cameraYaw + 180, -cameraPitch);
                } else {
                    setRotation(cameraYaw, cameraPitch);
                }
            }

            if (qps) {
                setRotation(yRot + 180.0f, freeLook && !freeLockInvertedView ? xRot : -xRot);
            }

            float scale = this.entity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
            float desiredCameraDistance = PerspectiveEvent.INSTANCE.getDistance();

            if (!rearView) {
                move(-getMaxZoom(desiredCameraDistance * scale), 0.0f, 0.0f);
            }

            ci.cancel();
            return;
        }
        var screen = ModuleDroneControl.INSTANCE.getScreen();

        if (screen != null) {
            this.setPosition(screen.getCameraPos());
            this.setRotation(screen.getCameraRotation().yRot(), screen.getCameraRotation().xRot());
        }
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void applyFreeCamPlayerSelfRendering(float partialTicks, CallbackInfo ci) {
        if (ModuleFreeCam.INSTANCE.getRunning()) {
            this.detached = true;
        }
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 modifyPositionVehicle(Vec3 instance, Vec3 vec) {
        if (ModuleFreeLook.INSTANCE.getRunning()) {
            return vec;
        }

        return ModuleSmoothCamera.shouldApplyChanges() ? vec.add(0, 1, 0) : vec;
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void modifyPosition(Args args) {
        if (ModuleFreeLook.INSTANCE.getRunning()) {
            return;
        }

        Vec3 original = new Vec3(args.get(0), args.get(1), args.get(2));
        ModuleSmoothCamera.cameraUpdate(original);
        if (ModuleSmoothCamera.shouldApplyChanges()) {
            Vec3 smoothPos = ModuleSmoothCamera.INSTANCE.getSmoothPos();
            args.set(0, smoothPos.x);
            args.set(1, smoothPos.y);
            args.set(2, smoothPos.z);
        }
    }


    /**
     * <pre>
     *     minecraft.options.fov.get().intValue();
     * </pre>
     */
    @ModifyExpressionValue(method = {
        "createProjectionMatrixForCulling",
        "getFluidInCamera",
        "calculateFov",
    }, at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        int result;

        if (ModuleZoom.INSTANCE.getRunning()) {
            return ModuleZoom.INSTANCE.getFov(true, 0);
        } else {
            result = ModuleZoom.INSTANCE.getFov(false, original);
        }

        if (ModuleNoFov.INSTANCE.getRunning() && result == original) {
            return ModuleNoFov.INSTANCE.getFov(result);
        }

        return result;
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float injectShit(float original) {
        var screen = ModuleDroneControl.INSTANCE.getScreen();

        if (screen != null) {
            return Math.min(120f, original / screen.getZoomFactor());
        }

        return original;
    }

    @ModifyArgs(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;", remap = false))
    private void hookBasicProjectionMatrix(Args args) {
        if (ModuleAspect.INSTANCE.getRunning()) {
            args.set(1, (float) args.get(1) / ModuleAspect.getRatioMultiplier());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        final PerspectiveEvent event = PerspectiveEvent.INSTANCE;
        event.update(minecraft, entity);

        EventManager.INSTANCE.callEvent(event);
    }

    /**
     * Set as spectator to disable smart culling
     */
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"))
    private boolean hookFreeCamDisableSmartCullInBlocks(boolean original) {
        return original || ModuleFreeCam.INSTANCE.getRunning();
    }

    @ModifyExpressionValue(method = "alignWithEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
        )
    )
    private CameraType hookPerspectiveEventOnCamera(CameraType original) {
        return PerspectiveEvent.INSTANCE.getPerspective();
    }

    @ModifyConstant(method = "getMaxZoom", constant = @Constant(intValue = 8))
    private int hookCameraClip(int constant) {
        return (PerspectiveEvent.INSTANCE.getNoClip()) ? 0 : constant;
    }

    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float hookCameraDistance(float original, float partialTicks) {
        if (!PerspectiveEvent.INSTANCE.getNoClip()) {
            return original;
        }

        final float lastDistance = PerspectiveEvent.INSTANCE.getLastDistance();
        final float distance = PerspectiveEvent.INSTANCE.getDistance();
        return distance != lastDistance ? Mth.lerp(partialTicks, lastDistance, distance) : distance;
    }

}
