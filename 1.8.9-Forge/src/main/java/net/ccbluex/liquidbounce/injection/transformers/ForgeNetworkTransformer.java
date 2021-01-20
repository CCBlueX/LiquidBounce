/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.ccbluex.liquidbounce.script.remapper.injection.utils.ClassUtils;
import net.ccbluex.liquidbounce.script.remapper.injection.utils.NodeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.tree.*;

/**
 * Transform bytecode of classes
 */
public class ForgeNetworkTransformer implements IClassTransformer
{

	/**
	 * Transform a class
	 *
	 * @param  name
	 *                         of target class
	 * @param  transformedName
	 *                         of target class
	 * @param  basicClass
	 *                         bytecode of target class
	 * @return                 new bytecode
	 */
	@Override
	public byte[] transform(final String name, final String transformedName, final byte[] basicClass)
	{
		if (name.equals("net.minecraftforge.fml.common.network.handshake.NetworkDispatcher"))
			try {
				final ClassNode classNode = ClassUtils.INSTANCE.toClassNode(basicClass);

				classNode.methods.stream().filter(methodNode -> methodNode.name.equals("handleVanilla")).forEach(methodNode ->
				{
					final LabelNode labelNode = new LabelNode();

					methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/injection/transformers/ForgeNetworkTransformer", "returnMethod", "()Z", false), new JumpInsnNode(IFEQ, labelNode), new InsnNode(ICONST_0), new InsnNode(IRETURN), labelNode));
				});

				return ClassUtils.INSTANCE.toBytes(classNode);
			} catch (final Throwable throwable) {
				throwable.printStackTrace();
			}

		if (name.equals("net.minecraftforge.fml.common.network.handshake.HandshakeMessageHandler"))
			try {
				final ClassNode classNode = ClassUtils.INSTANCE.toClassNode(basicClass);

				classNode.methods.stream().filter(method -> method.name.equals("channelRead0")).forEach(methodNode ->
				{
					final LabelNode labelNode = new LabelNode();

					methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/injection/transformers/ForgeNetworkTransformer", "returnMethod", "()Z", false), new JumpInsnNode(IFEQ, labelNode), new InsnNode(RETURN), labelNode));
				});

				return ClassUtils.INSTANCE.toBytes(classNode);
			} catch (final Throwable throwable) {
				throwable.printStackTrace();
			}

		return basicClass;
	}

	public static boolean returnMethod()
	{
		return AntiModDisable.enabled && AntiModDisable.blockFMLPackets && !Minecraft.getMinecraft().isIntegratedServerRunning();
	}
}
