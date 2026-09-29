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
package net.ccbluex.liquidbounce.injection.mixins.sodium;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class MixinChunkBuilderMeshingTask {

    /**
     * Captures XRay's state for the whole section build, so the per face and per block checks of the
     * block renderer below read it instead of resolving it again.
     * <p>
     * The descriptor is spelled out because the class also carries the synthetic bridge
     * {@code execute(ChunkBuildContext, CancellationToken)BuilderTaskOutput} of the covariant
     * override, which a name only selector would match as well.
     *
     * @see ModuleXRay#renderActive()
     */
    @WrapMethod(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
    private ChunkBuildOutput liquidbounce$xrayScope(ChunkBuildContext buildContext, CancellationToken cancellationToken,
            Operation<ChunkBuildOutput> original) {
        return ScopedValue.where(ModuleXRay.RENDER_ACTIVE, ModuleXRay.INSTANCE.getRunning())
            .call(() -> original.call(buildContext, cancellationToken));
    }

}
