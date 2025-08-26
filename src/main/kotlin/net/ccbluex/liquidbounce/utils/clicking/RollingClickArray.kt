package net.ccbluex.liquidbounce.utils.clicking

/**
 * A circular buffer that maintains double the cycle length and regenerates the second half
 * when reaching the midpoint
 */
class RollingClickArray(
    private val cycleLength: Int,
    val iterations: Int,
) {

    internal val array = IntArray(cycleLength * iterations)
    var head = 0
        private set
    private val size get() = array.size

    /**
     * Gets value at relative index from current head
     */
    fun get(relativeIndex: Int): Int {
        val actualIndex = (head + relativeIndex) % size
        return array[actualIndex]
    }

    /**
     * Sets value at relative index from current head
     */
    fun set(relativeIndex: Int, value: Int) {
        val actualIndex = (head + relativeIndex) % size
        array[actualIndex] = value
    }

    /**
     * Advances the head position and returns true if halfway point reached
     */
    fun advance(amount: Int = 1): Boolean {
        head = (head + amount) % size
        return head % cycleLength == 0
    }

    /**
     * Clears the array
     */
    fun clear() {
        array.fill(0)
        head = 0
    }

    fun push(cycleArray: IntArray) {
        require(cycleArray.size == cycleLength) { "Array size must match cycle length" }

        when (head) {
            0 -> System.arraycopy(cycleArray, 0, array, cycleLength, cycleLength)
            cycleLength -> System.arraycopy(cycleArray, 0, array, 0, cycleLength)
            else -> error("Head must be at 0 or cycle length")
        }
    }

}
