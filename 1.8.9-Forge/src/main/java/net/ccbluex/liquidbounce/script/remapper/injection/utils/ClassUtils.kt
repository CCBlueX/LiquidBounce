package net.ccbluex.liquidbounce.script.remapper.injection.utils

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * A bytecode class reader and writer util
 *
 * @author CCBlueX
 */
object ClassUtils {

    /**
     * Read bytes to class node
     *
     * @param bytes ByteArray of class
     */
    fun toClassNode(bytes : ByteArray) : ClassNode {
        val classReader = ClassReader(bytes)
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        return classNode
    }

    /**
     * Write class node to bytes
     *
     * @param classNode ClassNode of class
     */
    fun toBytes(classNode : ClassNode) : ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)

        return classWriter.toByteArray()
    }
}