/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.IProfiler
import net.minecraft.profiler.Profiler

class ProfilerImpl(val wrapped: Profiler) : IProfiler
{
	override fun startSection(sectionName: String) = wrapped.startSection(sectionName)
	override fun endSection() = wrapped.endSection()
	override fun endStartSection(sectionName: String) = wrapped.endStartSection(sectionName)
}

fun IProfiler.unwrap(): Profiler = (this as ProfilerImpl).wrapped
fun Profiler.wrap(): IProfiler = ProfilerImpl(this)
