package net.ccbluex.liquidbounce.script.remapper.injection.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

/**
 * A bytecode node util
 *
 * @author CCBlueX
 */
object NodeUtils {

    /**
     * Lazy.
     */
    fun toNodes(vararg nodes : AbstractInsnNode) : InsnList {
        val insnList = InsnList()
        for(node in nodes)
            insnList.add(node)
        return insnList
    }

}