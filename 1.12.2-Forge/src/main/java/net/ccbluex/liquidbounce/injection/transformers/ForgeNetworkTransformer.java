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

	public static boolean returnMethod()
	{
		return AntiModDisable.Companion.getEnabled() && AntiModDisable.Companion.getBlockFMLPackets() && !Minecraft.getMinecraft().isIntegratedServerRunning();
	}

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
		if ("net.minecraftforge.fml.common.network.handshake.NetworkDispatcher".equals(name))
			try
			{
				final ClassNode classNode = ClassUtils.INSTANCE.toClassNode(basicClass);

				classNode.methods.stream().filter(methodNode -> "handleVanilla".equals(methodNode.name)).forEach(methodNode ->
				{
					final LabelNode labelNode = new LabelNode();

					methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/injection/transformers/ForgeNetworkTransformer", "returnMethod", "()Z", false), new JumpInsnNode(IFEQ, labelNode), new InsnNode(ICONST_0), new InsnNode(IRETURN), labelNode));
				});

				return ClassUtils.INSTANCE.toBytes(classNode);
			}
			catch (final Throwable throwable)
			{
				throwable.printStackTrace();
			}

		if ("net.minecraftforge.fml.common.network.handshake.HandshakeMessageHandler".equals(name))
			try
			{
				final ClassNode classNode = ClassUtils.INSTANCE.toClassNode(basicClass);

				classNode.methods.stream().filter(method -> "channelRead0".equals(method.name)).forEach(methodNode ->
				{
					final LabelNode labelNode = new LabelNode();

					methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), NodeUtils.INSTANCE.toNodes(new MethodInsnNode(INVOKESTATIC, "net/ccbluex/liquidbounce/injection/transformers/ForgeNetworkTransformer", "returnMethod", "()Z", false), new JumpInsnNode(IFEQ, labelNode), new InsnNode(RETURN), labelNode));
				});

				return ClassUtils.INSTANCE.toBytes(classNode);
			}
			catch (final Throwable throwable)
			{
				throwable.printStackTrace();
			}

		return basicClass;
	}
}
