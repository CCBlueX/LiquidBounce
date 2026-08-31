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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.annotations.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [EventManager.registerEventHook] used to reject any class missing from [ALL_EVENT_CLASSES], which
 * caught typos at runtime. Add-ons need to register their own events, so that guard is gone. These
 * tests take its place at build time.
 */
class EventClassRegistryTest {

    @Test
    fun `every built-in event class carries a tag`() {
        val untagged = ALL_EVENT_CLASSES.filter { it.getAnnotation(Tag::class.java) == null }

        assertTrue(untagged.isEmpty(), "Event classes without @Tag: ${untagged.map { it.name }}")
    }

    @Test
    fun `built-in event tags are unique, case-insensitively`() {
        val byName = ALL_EVENT_CLASSES.groupBy { it.getAnnotation(Tag::class.java).name.lowercase() }
        val duplicates = byName.filterValues { it.size > 1 }

        val rendered = duplicates.mapValues { (_, classes) -> classes.map { it.name } }

        assertTrue(duplicates.isEmpty(), "Duplicate event tags: $rendered")
    }

    @Test
    fun `every built-in event class is known to the manager`() {
        assertTrue(EventManager.knownEventClasses.containsAll(ALL_EVENT_CLASSES.asList()))
    }

    @Test
    fun `events resolve by tag name, ignoring case`() {
        val eventClass = ALL_EVENT_CLASSES.first()
        val name = eventClass.getAnnotation(Tag::class.java).name

        assertSame(eventClass, EventManager.eventClassByName(name))
        assertSame(eventClass, EventManager.eventClassByName(name.uppercase()))
    }

    @Tag("EventClassRegistryTestAddonEvent")
    private class AddonEvent : Event()

    private class UntaggedAddonEvent : Event()

    @Test
    fun `an add-on event registers on first hook and keeps its hooks across later registrations`() {
        val listener = object : EventListener {}
        val hook = EventHook<AddonEvent>(listener) { }

        EventManager.registerEventHook(AddonEvent::class.java, hook)

        assertTrue(AddonEvent::class.java in EventManager.knownEventClasses)
        assertSame(AddonEvent::class.java, EventManager.eventClassByName("eventclassregistrytestaddonevent"))
        assertNotNull(EventManager.eventFlow(AddonEvent::class.java))

        // Registering a second class rebuilds the tables; the first one's hook must survive it.
        EventManager.registerEventHook(UntaggedAddonEvent::class.java, EventHook(listener) { })

        var received = 0
        val counting = EventHook<AddonEvent>(listener) { received++ }
        EventManager.registerEventHook(AddonEvent::class.java, counting)
        EventManager.callEvent(AddonEvent())

        assertEquals(1, received)

        EventManager.unregisterEventHandler(listener)
    }

    @Test
    fun `an add-on event without a tag falls back to its simple name`() {
        EventManager.registerEventClass(UntaggedAddonEvent::class.java)

        assertEquals("UntaggedAddonEvent", UntaggedAddonEvent::class.java.eventName)
    }
}
