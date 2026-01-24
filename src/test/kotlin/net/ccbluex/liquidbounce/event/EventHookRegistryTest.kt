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

import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EventHookRegistryTest {

    // Mock Event class for testing
    private class TestEvent : Event()

    // Mock EventListener for testing
    private object TestEventListener : EventListener {
        override val running: Boolean get() = true
    }

    @Test
    fun testAddAndIterate() {
        val registry = EventHookRegistry<TestEvent>()
        val list = CopyOnWriteArrayList<EventHook<TestEvent>>()

        // Create test event hooks with different priorities
        val hook1 = TestEventListener.newEventHook<TestEvent>(10) { }
        val hook2 = TestEventListener.newEventHook<TestEvent>(5) { }
        val hook3 = TestEventListener.newEventHook<TestEvent>(15) { }

        // Add to both collections
        registry.addIfAbsent(hook1)
        registry.addIfAbsent(hook2)
        registry.addIfAbsent(hook3)

        list.addIfAbsent(hook1)
        list.addIfAbsent(hook2)
        list.addIfAbsent(hook3)

        // Verify sizes are equal
        assertEquals(list.size, registry.count())

        list.sortBy { -it.priority }

        // Verify iteration order (should be sorted by priority descending)
        val registryIterator = registry.iterator()
        val listIterator = list.iterator()

        while (registryIterator.hasNext() && listIterator.hasNext()) {
            assertEquals(listIterator.next().priority, registryIterator.next().priority)
        }

        assertFalse(registryIterator.hasNext())
        assertFalse(listIterator.hasNext())
    }

    @Test
    fun testRemoveByEventHook() {
        val registry = EventHookRegistry<TestEvent>()
        val list = CopyOnWriteArrayList<EventHook<TestEvent>>()

        // Create test event hooks
        val hook1 = TestEventListener.newEventHook<TestEvent>(10) { }
        val hook2 = TestEventListener.newEventHook<TestEvent>(5) { }

        // Add to both collections
        registry.addIfAbsent(hook1)
        registry.addIfAbsent(hook2)

        list.addIfAbsent(hook1)
        list.addIfAbsent(hook2)

        // Remove one element
        registry.remove(hook1)
        list.remove(hook1)

        // Verify sizes are equal
        assertEquals(list.size, registry.count())

        // Verify remaining elements
        val registryIterator = registry.iterator()
        val listIterator = list.iterator()

        while (registryIterator.hasNext() && listIterator.hasNext()) {
            assertEquals(listIterator.next().priority, registryIterator.next().priority)
        }

        assertFalse(registryIterator.hasNext())
        assertFalse(listIterator.hasNext())
    }

    @Test
    fun testRemoveByEventListener() {
        val registry = EventHookRegistry<TestEvent>()
        val list = CopyOnWriteArrayList<EventHook<TestEvent>>()

        // Create test event hooks
        val hook1 = TestEventListener.newEventHook<TestEvent>(10) { }
        val hook2 = TestEventListener.newEventHook<TestEvent>(5) { }

        // Add to both collections
        registry.addIfAbsent(hook1)
        registry.addIfAbsent(hook2)

        list.addIfAbsent(hook1)
        list.addIfAbsent(hook2)

        // Remove by event listener
        registry.remove(TestEventListener)
        list.removeIf { it.handlerClass == TestEventListener }

        // Verify sizes are equal
        assertEquals(list.size, registry.count())
        assertEquals(0, registry.count())
        assertEquals(0, list.size)
    }

    @Test
    fun testClear() {
        val registry = EventHookRegistry<TestEvent>()
        val list = CopyOnWriteArrayList<EventHook<TestEvent>>()

        // Create test event hooks
        val hook1 = TestEventListener.newEventHook<TestEvent>(10) { }
        val hook2 = TestEventListener.newEventHook<TestEvent>(5) { }

        // Add to both collections
        registry.addIfAbsent(hook1)
        registry.addIfAbsent(hook2)

        list.addIfAbsent(hook1)
        list.addIfAbsent(hook2)

        // Clear both collections
        registry.clear()
        list.clear()

        // Verify both are empty
        assertEquals(0, registry.count())
        assertEquals(0, list.size)
    }

    @Test
    fun testConcurrentModificationDuringIteration() {
        val registry = EventHookRegistry<TestEvent>()

        // Create test event hooks
        val hook1 = TestEventListener.newEventHook<TestEvent>(10) { }
        val hook2 = TestEventListener.newEventHook<TestEvent>(5) { }
        val hook3 = TestEventListener.newEventHook<TestEvent>(15) { }

        // Add to registry
        registry.addIfAbsent(hook1)
        registry.addIfAbsent(hook2)
        registry.addIfAbsent(hook3)

        // Test concurrent modification during iteration
        assertDoesNotThrow {
            val iterator = registry.iterator()
            while (iterator.hasNext()) {
                val hook = iterator.next()
                // Try to modify while iterating
                if (hook.priority.toInt() == 10) {
                    registry.remove(hook1)
                    registry.addIfAbsent(TestEventListener.newEventHook(20) { })
                }
            }
        }
    }

    @Test
    fun testAddDuplicate() {
        val registry = EventHookRegistry<TestEvent>()
        val list = CopyOnWriteArrayList<EventHook<TestEvent>>()

        // Create test event hook
        val hook = TestEventListener.newEventHook<TestEvent>(10) { }

        // Add the same hook twice
        registry.addIfAbsent(hook)
        registry.addIfAbsent(hook)

        list.addIfAbsent(hook)
        list.addIfAbsent(hook)

        // Should only contain one instance
        assertEquals(1, registry.count())
        assertEquals(1, list.size)
    }

    @Test
    fun testPriorityOrdering() {
        val registry = EventHookRegistry<TestEvent>()

        // Create test event hooks with various priorities
        val hooks = listOf(
            TestEventListener.newEventHook<TestEvent>(1) { },
            TestEventListener.newEventHook<TestEvent>(10) { },
            TestEventListener.newEventHook<TestEvent>(5) { },
            TestEventListener.newEventHook<TestEvent>(100) { },
            TestEventListener.newEventHook<TestEvent>(-5) { }
        )

        // Add all hooks
        hooks.forEach { registry.addIfAbsent(it) }

        // Verify they are sorted by priority descending
        val priorities = registry.map { it.priority.toInt() }.toList()
        val expectedPriorities = listOf(100, 10, 5, 1, -5)

        assertEquals(expectedPriorities, priorities)
    }

    @Test
    fun testEmptyRegistry() {
        val registry = EventHookRegistry<TestEvent>()
        val list = CopyOnWriteArrayList<EventHook<TestEvent>>()

        // Both should be empty
        assertEquals(0, registry.count())
        assertEquals(0, list.size)

        // Iteration should work on empty collection
        assertFalse(registry.iterator().hasNext())
        assertFalse(list.iterator().hasNext())
    }

    @Test
    fun testMultipleEventListeners() {
        val otherEventListener = object : EventListener {
            override val running: Boolean get() = true
        }

        val registry = EventHookRegistry<TestEvent>()

        // Create test event hooks with different event listeners
        val hook1 = TestEventListener.newEventHook<TestEvent>(10) { }
        val hook2 = otherEventListener.newEventHook<TestEvent>(5) { }

        // Add to registry
        registry.addIfAbsent(hook1)
        registry.addIfAbsent(hook2)

        // Remove by one event listener
        registry.remove(TestEventListener)

        // Should only have the hook with OtherEventListener left
        assertEquals(1, registry.count())
        assertEquals(otherEventListener, registry.first().handlerClass)
    }

    @Test
    fun testSamePriorityDifferentHooks() {
        val registry = EventHookRegistry<TestEvent>()

        // Two different hooks with same priority
        val hookA = TestEventListener.newEventHook<TestEvent>(10) { }
        val hookB = TestEventListener.newEventHook<TestEvent>(10) { }

        registry.addIfAbsent(hookA)
        registry.addIfAbsent(hookB)

        // Both should be present
        assertEquals(2, registry.count())
        // Ensure both references present (identity)
        assertTrue(registry.any { it === hookA })
        assertTrue(registry.any { it === hookB })

        // Because we insert after equal-range, order should be hookA then hookB
        val list = registry.toList()
        assertSame(hookA, list[0])
        assertSame(hookB, list[1])
    }

    @Test
    fun testRemoveSpecificHookWithSamePriority() {
        val registry = EventHookRegistry<TestEvent>()

        // Two different hooks with same priority
        val hookA = TestEventListener.newEventHook<TestEvent>(10) { }
        val hookB = TestEventListener.newEventHook<TestEvent>(10) { }

        registry.addIfAbsent(hookA)
        registry.addIfAbsent(hookB)

        // Remove only hookA
        registry.remove(hookA)

        // hookB should remain
        assertEquals(1, registry.count())
        val remaining = registry.first()
        assertSame(hookB, remaining)
    }

    @Test
    fun testRemoveListenerNoopWhenNotPresent() {
        val registry = EventHookRegistry<TestEvent>()

        // Create one hook for TestEventListener
        val hook = TestEventListener.newEventHook<TestEvent>(10) { }
        registry.addIfAbsent(hook)

        // Some other listener not registered
        val otherListener = object : EventListener {
            override val running: Boolean get() = true
        }

        // Should be a no-op and not throw
        assertDoesNotThrow {
            registry.remove(otherListener)
        }

        // Registry unchanged
        assertEquals(1, registry.count())
        assertSame(hook, registry.first())
    }

}
