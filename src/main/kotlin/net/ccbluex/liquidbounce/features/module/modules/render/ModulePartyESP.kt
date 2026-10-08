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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.WorldFeatureSubmitEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.features.chat.party.PartyManager
import net.ccbluex.liquidbounce.features.chat.party.PartyMemberStates
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.submitTextAlwaysOnTop
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

private const val NETHER_SCALE = 8.0

// Positions older than this are where the member was, not where they are
private const val STALE_AFTER = 10_000L

// Beyond the far plane the label would be clipped, so it is drawn closer and scaled up
private const val ANCHOR_DISTANCE = 48.0

/**
 * Party ESP module
 *
 * Shows party members that are out of render distance.
 */
object ModulePartyESP : ClientModule("PartyESP", ModuleCategories.RENDER) {

    private val scale by float("Scale", 1.5f, 0.25f..4f)
    private val health by boolean("Health", true)
    private val tracers by boolean("Tracers", false)
    private val color by color("Color", Color4b(255, 85, 255))

    private data class Marker(val member: PartyMember, val position: Vec3, val distance: Double)

    private fun markers(camera: Vec3): List<Marker> {
        val now = System.currentTimeMillis()
        val ownDimension = world.dimension()

        return PartyManager.others.mapNotNull { member ->
            if (member.relation != "world" && member.relation != "instance") {
                return@mapNotNull null
            }

            val loaded = member.player?.let { world.getPlayerByUUID(it.uuid) } != null
            val state = PartyMemberStates[member.user.id] ?: return@mapNotNull null
            val position = state.position?.takeIf { !loaded && now - state.positionAt < STALE_AFTER }
                ?: return@mapNotNull null

            val raw = Vec3(position.x, position.y, position.z)
            val converted = when {
                position.dimension == ownDimension.identifier().toString() -> raw
                ownDimension == Level.NETHER && position.dimension == Level.OVERWORLD.identifier().toString() ->
                    Vec3(raw.x / NETHER_SCALE, raw.y, raw.z / NETHER_SCALE)
                ownDimension == Level.OVERWORLD && position.dimension == Level.NETHER.identifier().toString() ->
                    Vec3(raw.x * NETHER_SCALE, raw.y, raw.z * NETHER_SCALE)
                else -> return@mapNotNull null
            }

            Marker(member, converted, converted.distanceTo(camera))
        }
    }

    private fun anchor(marker: Marker, camera: Vec3): Vec3 {
        val offset = marker.position.subtract(camera)
        return if (marker.distance > ANCHOR_DISTANCE) {
            camera.add(offset.scale(ANCHOR_DISTANCE / marker.distance))
        } else {
            marker.position
        }
    }

    private fun label(marker: Marker): String {
        val status = PartyMemberStates[marker.member.user.id]?.status
        val hearts = status?.get("health")?.takeIf { health && it.isJsonPrimitive }?.asFloat
        val name = marker.member.player?.name ?: marker.member.user.name

        return buildString {
            append(name)
            if (hearts != null) {
                append(" ❤ ").append("%.1f".format(hearts))
            }
            append(" (").append(marker.distance.toInt()).append("m)")
        }
    }

    @Suppress("unused")
    private val labelHandler = handler<WorldFeatureSubmitEvent> { event ->
        val cameraPos = event.camera.position()
        val font = mc.font

        for (marker in markers(cameraPos)) {
            val pos = anchor(marker, cameraPos)
            val text = label(marker).withFormat()
            val distanceScale = (pos.distanceTo(cameraPos) / 10.0).coerceAtLeast(1.0).toFloat()
            val size = EntityRenderer.NAMETAG_SCALE * scale * distanceScale

            event.poseStack.withPush {
                translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)
                rotate(event.camera.rotation())
                scale(size, -size, size)
                event.submitNodeStorage.submitTextAlwaysOnTop(
                    this,
                    -font.width(text) * 0.5f,
                    -font.lineHeight * 0.5f,
                    text,
                    true,
                    Font.DisplayMode.SEE_THROUGH,
                    LightCoordsUtil.FULL_BRIGHT,
                    color.argb,
                    0,
                    0,
                )
            }
        }
    }

    @Suppress("unused")
    private val tracerHandler = handler<WorldRenderEvent> { event ->
        if (!tracers) {
            return@handler
        }

        event.renderEnvironment {
            val eyeVector = Vec3f.eyeVector(camera)
            val cameraPos = camera.position()
            for (marker in markers(cameraPos)) {
                val target = anchor(marker, cameraPos).subtract(cameraPos).toVec3f()
                drawLines(color.argb, eyeVector, target)
            }
        }
    }

}
