package net.ccbluex.liquidbounce.utils.kotlin

/**
 * Returns the value of the set operation with the highest priority
 */
class PriorityField<T>(
    value: T,
    private var currentPriority: Priority
) {
    var value: T = value
        private set

    fun trySet(value: T, priority: Priority) {
        if (currentPriority < priority) {
            this.currentPriority = priority
            this.value = value
        }
    }

}
