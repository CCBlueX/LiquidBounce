/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.util

import net.ccbluex.liquidbounce.api.minecraft.util.IFoodStats
import net.minecraft.util.FoodStats

class FoodStatsImpl(val wrapped: FoodStats) : IFoodStats
{
    override val foodLevel: Int
        get() = wrapped.foodLevel

    override fun equals(other: Any?): Boolean = other is FoodStatsImpl && other.wrapped == wrapped
}

fun IFoodStats.unwrap(): FoodStats = (this as FoodStatsImpl).wrapped
fun FoodStats.wrap(): IFoodStats = FoodStatsImpl(this)
