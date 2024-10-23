package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.AutoBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_LEFT_HAND;
import static net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;

@Mixin(ItemRenderer.class)
public class MixinItemRender {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("HEAD"), cancellable = true)
    private void hookRenderItem(LivingEntity entity, ItemStack item, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
        if (renderMode == (leftHanded ? THIRD_PERSON_LEFT_HAND : THIRD_PERSON_RIGHT_HAND)
                && entity instanceof PlayerEntity player
                && (ModuleSwordBlock.INSTANCE.handleEvents() || AutoBlock.INSTANCE.getBlockVisual())
                && (player.getMainHandStack().getItem() instanceof SwordItem || player == this.client.player
                && ModuleSwordBlock.INSTANCE.handleEvents() && ModuleSwordBlock.INSTANCE.getAlwaysHideShield())
                && item.getItem() instanceof ShieldItem
        ) ci.cancel();
    }
}
