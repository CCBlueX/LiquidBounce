package net.ccbluex.liquidbounce.utils.block

import net.minecraft.util.math.BlockPos

/**
 * Prevents bugs where a mutable block pos is put in a position where an immutable is expected.
 */
class ImmutableBlockPos(pos: BlockPos): BlockPos(pos)
