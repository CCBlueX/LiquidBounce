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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;

import static net.ccbluex.liquidbounce.utils.client.GenericPools.ARRAY_LIST;

@SuppressWarnings("rawtypes")
@Mixin(GuiRenderState.Node.class)
public abstract class MixinGuiRenderStateLayer {

    @WrapOperation(method = "submitItem", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
    private ArrayList reuseList$addItem(Operation<ArrayList> original) {
        return ARRAY_LIST.borrow();
    }

    @WrapOperation(method = "submitText", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
    private ArrayList reuseList$addText(Operation<ArrayList> original) {
        return ARRAY_LIST.borrow();
    }

    @WrapOperation(method = "submitPicturesInPictureState", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
    private ArrayList reuseList$addSpecialElement(Operation<ArrayList> original) {
        return ARRAY_LIST.borrow();
    }

    @WrapOperation(method = "submitGuiElement", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
    private ArrayList reuseList$addSimpleElement(Operation<ArrayList> original) {
        return ARRAY_LIST.borrow();
    }

    @WrapOperation(method = "submitGlyph", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
    private ArrayList reuseList$addPreparedText(Operation<ArrayList> original) {
        return ARRAY_LIST.borrow();
    }

}
