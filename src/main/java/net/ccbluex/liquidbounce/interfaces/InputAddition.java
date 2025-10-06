package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

public interface InputAddition {
    /**
     * @param movementVector x -> movementSideways; y -> movementForward
     */
    void liquid_bounce$setMovementInput(Vec2f movementVector);

    PlayerInput liquid_bounce$getInitial();
    PlayerInput liquid_bounce$getUntransformed();
}
