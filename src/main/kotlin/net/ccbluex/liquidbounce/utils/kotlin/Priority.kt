package net.ccbluex.liquidbounce.utils.kotlin

enum class Priority(val priority: Int) {
    IMPORTANT_FOR_USER_SAFETY(60),
    IMPORTANT_FOR_PLAYER_LIFE(40),
    IMPORTANT_FOR_USAGE_2(10),
    IMPORTANT_FOR_USAGE_1(20),
    NORMAL(0),
    NOT_IMPORTANT(-20),
}

object EventPriorityConvention {

    /**
     * The event should be called first.
     */
    const val FIRST_PRIORITY: Int = 1000

    /**
     * At the stage of modeling what the player is actually going to do after other events added their suggestions
     */
    const val MODEL_STATE: Int = -10

    /**
     * Should be the one of the last functionalities that run, because the player safety depends on it.
     * Can be objected though by handlers with [OBJECTION_AGAINST_EVERYTHING] priority
     */
    const val SAFETY_FEATURE: Int = -50

    /**
     * Used when the event handler should be able to object anything that happened previously
     */
    const val OBJECTION_AGAINST_EVERYTHING: Int = -100
    /**
     * The event should be called last. It should not only be used for events that want to read the final state of the
     * event
     */
    const val READ_FINAL_STATE: Int = -1000
}
