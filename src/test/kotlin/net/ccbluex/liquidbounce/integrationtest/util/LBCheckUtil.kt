package net.ccbluex.liquidbounce.integrationtest.util

import net.ccbluex.tenacc.api.common.TACCTestAdapter
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos

fun PlayerEntity.isStandingOn(pos: BlockPos): Boolean {
    return this.isOnGround && blockPos == pos
}

fun tenaccAssert(cond: Boolean, s: String) {
    check(cond) { "Assert failed: $s" }
}
