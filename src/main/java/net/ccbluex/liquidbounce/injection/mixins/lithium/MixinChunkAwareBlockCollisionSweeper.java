package net.ccbluex.liquidbounce.injection.mixins.lithium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import net.ccbluex.liquidbounce.common.ShapeFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = ChunkAwareBlockCollisionSweeper.class)
public abstract class MixinChunkAwareBlockCollisionSweeper {

    @Shadow
    @Final
    private BlockPos.MutableBlockPos pos;

    /**
     * Hook collision shape event
     *
     * @param original voxel shape
     * @return possibly modified voxel shape
     */
    @ModifyExpressionValue(method = "computeNext()Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
    ))
    private VoxelShape hookCollisionShape(VoxelShape original, @Local BlockState blockState) {
        if (this.pos == null || ShapeFlag.noShapeChange) {
            return original;
        }

        final BlockShapeEvent shapeEvent = EventManager.INSTANCE.callEvent(new BlockShapeEvent(blockState, this.pos, original));
        return shapeEvent.getShape();
    }

}
