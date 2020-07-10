/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.remapper.injection.transformers;

import net.ccbluex.liquidbounce.script.remapper.injection.utils.ClassUtils;
import net.ccbluex.liquidbounce.script.remapper.injection.utils.NodeUtils;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.*;

/**
 * Transform bytecode of classes
 *
 * @author CCBlueX
 * @class jdk/internal/dynalink/beans/AbstractJavaLinker
 */
public class AbstractJavaLinkerTransformer implements IClassTransformer {

    /**
     * Transform a class
     *
     * @param name            of target class
     * @param transformedName of target class
     * @param basicClass      bytecode of target class
     * @return new bytecode
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if(name.equals("jdk.internal.dynalink.beans.AbstractJavaLinker")) {
            try {
                final ClassNode classNode = ClassUtils.INSTANCE.toClassNode(basicClass);

                classNode.methods.forEach(methodNode -> {
                    switch(methodNode.name + methodNode.desc) {
                        case "addMember(Ljava/lang/String;Ljava/lang/reflect/AccessibleObject;Ljava/util/Map;)V":
                            methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(
                                    new VarInsnNode(ALOAD, 0),
                                    new FieldInsnNode(GETFIELD, "jdk/internal/dynalink/beans/AbstractJavaLinker", "clazz", "Ljava/lang/Class;"),
                                    new VarInsnNode(ALOAD, 1),
                                    new VarInsnNode(ALOAD, 2),
                                    new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/script/remapper/injection/transformers/handlers/AbstractJavaLinkerHandler", "addMember", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/reflect/AccessibleObject;)Ljava/lang/String;", false),
                                    new VarInsnNode(ASTORE, 1)
                            ));
                            break;
                        case "addMember(Ljava/lang/String;Ljdk/internal/dynalink/beans/SingleDynamicMethod;Ljava/util/Map;)V":
                            methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(
                                    new VarInsnNode(ALOAD, 0),
                                    new FieldInsnNode(GETFIELD, "jdk/internal/dynalink/beans/AbstractJavaLinker", "clazz", "Ljava/lang/Class;"),
                                    new VarInsnNode(ALOAD, 1),
                                    new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/script/remapper/injection/transformers/handlers/AbstractJavaLinkerHandler", "addMember", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;", false),
                                    new VarInsnNode(ASTORE, 1)
                            ));
                            break;
                        case "setPropertyGetter(Ljava/lang/String;Ljdk/internal/dynalink/beans/SingleDynamicMethod;Ljdk/internal/dynalink/beans/GuardedInvocationComponent$ValidationType;)V":
                            methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(
                                    new VarInsnNode(ALOAD, 0),
                                    new FieldInsnNode(GETFIELD, "jdk/internal/dynalink/beans/AbstractJavaLinker", "clazz", "Ljava/lang/Class;"),
                                    new VarInsnNode(ALOAD, 1),
                                    new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/script/remapper/injection/transformers/handlers/AbstractJavaLinkerHandler", "setPropertyGetter", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;", false),
                                    new VarInsnNode(ASTORE, 1)
                            ));
                            break;
                    }
                });

                return ClassUtils.INSTANCE.toBytes(classNode);
            }catch(final Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return basicClass;
    }

}