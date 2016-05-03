package com.pau101.cacti.asm;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.pau101.cacti.asm.ref.ClassReference;
import com.pau101.cacti.asm.ref.FieldReference;
import com.pau101.cacti.asm.ref.MethodReference;

public class CactiClassTransformer implements IClassTransformer {
	private static final String CACTI = "com/pau101/cacti/Cacti";

	private static final ClassReference CREATIVE_TABS = Mappings.cl("net/minecraft/creativetab/CreativeTabs");

	private static final ClassReference GUI_CONTAINER_CREATIVE = Mappings.cl("net/minecraft/client/gui/inventory/GuiContainerCreative");

	private static final ClassReference GUI_BUTTON = Mappings.cl("net/minecraft/client/gui/GuiButton");

	private static final ClassReference INVENTORY_EFFECT_RENDERER = Mappings.cl("net/minecraft/client/renderer/InventoryEffectRenderer");

	private static final FieldReference GUI_BUTTON_ID = Mappings.fd("net/minecraft/client/gui/GuiButton/id");

	private final List<ClassTransformer> classTransformers = new ArrayList<ClassTransformer>();

	{
		classTransformers.add(new ClassTransformer("net/minecraft/creativetab/CreativeTabs")
			.with(new MethodTransformer("<init>(ILjava/lang/String;)V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					InsnNode ret = find(insns, Opcodes.RETURN);
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(ret, new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "initCreativeTab",
						"(L" + CREATIVE_TABS + ";)V", false));
				}
			})
		);
		classTransformers.add(new ClassTransformer(GUI_CONTAINER_CREATIVE)
			.with(new MethodTransformer("initGui()V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					FieldInsnNode bait = find(insns, Opcodes.GETSTATIC);
					AbstractInsnNode mark;
					insns.insertBefore(bait, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(bait, mark = new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "initGui",
						"(L" + GUI_CONTAINER_CREATIVE + ";)V", false));
					// remove normal setCreativeTab
					for (AbstractInsnNode n = bait; n != null; n = mark.getNext()) {
						insns.remove(n);
						if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
							break;
						}
					}
				}
			})
			.with(new MethodTransformer("drawGuiContainerBackgroundLayer(FII)V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					FieldInsnNode bait = find(insns, Opcodes.GETSTATIC);
					insns.insertBefore(bait, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(bait, new VarInsnNode(Opcodes.FLOAD, 1));
					insns.insertBefore(bait, new VarInsnNode(Opcodes.ILOAD, 2));
					insns.insertBefore(bait, new VarInsnNode(Opcodes.ILOAD, 3));
					insns.insertBefore(bait, new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "drawBackground",
						"(L" + GUI_CONTAINER_CREATIVE + ";FII)V", false));
				}
			})
			.with(new MethodTransformer("drawScreen(IIF)V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					InsnNode ret = findLast(insns, Opcodes.RETURN);
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ILOAD, 1));
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ILOAD, 2));
					insns.insertBefore(ret, new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "drawScreen",
						"(L" + GUI_CONTAINER_CREATIVE + ";II)V", false));
				}
			})
			.with(new MethodTransformer("mouseClicked(III)V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					// add null check to make null values in CreativeTabs.creativeTabArray not cause crash
					LabelNode inc = firstTypeBefore(find(insns, Opcodes.IINC), AbstractInsnNode.LABEL);
					VarInsnNode check = firstLoadBefore(inc, 0);
					insns.insertBefore(check, new VarInsnNode(Opcodes.ALOAD, 9));
					insns.insertBefore(check, new JumpInsnNode(Opcodes.IFNULL, inc));
				}
			})
			.with(new MethodTransformer("mouseReleased(III)V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					InsnNode ret = findLast(insns, Opcodes.RETURN);
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ILOAD, 1));
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ILOAD, 2));
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ILOAD, 3));
					insns.insertBefore(ret, new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "mouseReleased",
						"(L" + GUI_CONTAINER_CREATIVE + ";III)V", false));
				}
			})
			.with(new MethodTransformer("actionPerformed(Lnet/minecraft/client/gui/GuiButton;)V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					InsnNode ret = findLast(insns, Opcodes.RETURN);
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(ret, new VarInsnNode(Opcodes.ALOAD, 1));
					insns.insertBefore(ret, new FieldInsnNode(Opcodes.GETFIELD, GUI_BUTTON.toString(), GUI_BUTTON_ID.toString(), "I"));
					insns.insertBefore(ret, new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "actionPerformed",
						"(L" + GUI_CONTAINER_CREATIVE + ";I)V", false));
				}
			})
		);
		classTransformers.add(new ClassTransformer(INVENTORY_EFFECT_RENDERER)
			.with(new MethodTransformer("drawActivePotionEffects()V") {
				@Override
				public void transform(MethodNode method, InsnList insns) {
					AbstractInsnNode first = insns.get(0);
					insns.insertBefore(first, new VarInsnNode(Opcodes.ALOAD, 0));
					insns.insertBefore(first, new MethodInsnNode(Opcodes.INVOKESTATIC, CACTI, "drawActivePotionEffects",
						"(L" + INVENTORY_EFFECT_RENDERER + ";)Z", false));
					LabelNode ret = firstTypeBefore(findLast(insns, Opcodes.RETURN), AbstractInsnNode.LABEL);
					insns.insertBefore(first, new JumpInsnNode(Opcodes.IFEQ, ret));
				}
			})
		);
	}

	@Override
	public byte[] transform(String name, String mcpName, byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		Mappings.isCurrentlyObfuscated = !name.equals(mcpName);
		String mcpRefName = mcpName.replace('.', '/');
		for (ClassTransformer transform : classTransformers) {
			if (transform.appliesTo(mcpRefName)) {
				bytes = writeClass(transform.transform(readClass(bytes)));
				break;
			}
		}
		return bytes;
	}

	private ClassNode readClass(byte[] classBytes) {
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(classBytes);
		classReader.accept(classNode, 0);
		return classNode;
	}

	private byte[] writeClass(ClassNode classNode) {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	private <T extends AbstractInsnNode> T find(InsnList insns, int opcode) {
		for (int i = 0; i < insns.size(); i++) {
			AbstractInsnNode insn = insns.get(i);
			if (insn.getOpcode() == opcode) {
				return (T) insn;
			}
		}
		throw new RuntimeException("Failed to find expected opcode: " + opcode);
	}

	private <T extends AbstractInsnNode> T findLast(InsnList insns, int opcode) {
		for (int i = insns.size() - 1; i >= 0; i--) {
			AbstractInsnNode insn = insns.get(i);
			if (insn.getOpcode() == opcode) {
				return (T) insn;
			}
		}
		throw new RuntimeException("Failed to find expected opcode: " + opcode);
	}

	private <T extends AbstractInsnNode> T findLastType(InsnList insns, int type) {
		for (int i = insns.size() - 1; i >= 0; i--) {
			AbstractInsnNode insn = insns.get(i);
			if (insn.getType() == type) {
				return (T) insn;
			}
		}
		throw new RuntimeException("Failed to find expected type: " + type);
	}

	private <T extends AbstractInsnNode> T firstBefore(AbstractInsnNode node, int opcode) {
		for (AbstractInsnNode n = node; n != null; n = n.getPrevious()) {
			if (n.getOpcode() == opcode) {
				return (T) n;
			}
		}
		throw new RuntimeException("Failed to find expected opcode: " + opcode);
	}

	private VarInsnNode firstLoadBefore(AbstractInsnNode node, int value) {
		for (AbstractInsnNode n = node; n != null; n = n.getPrevious()) {
			if (n.getOpcode() >= Opcodes.ILOAD && n.getOpcode() <= Opcodes.ALOAD && ((VarInsnNode) n).var == value) {
				return (VarInsnNode) n;
			}
		}
		throw new RuntimeException("Failed to find expected load " + value);
	}

	private <T extends AbstractInsnNode> T firstTypeBefore(AbstractInsnNode node, int type) {
		for (AbstractInsnNode n = node; n != null; n = n.getPrevious()) {
			if (n.getType() == type) {
				return (T) n;
			}
		}
		throw new RuntimeException("Failed to find expected type: " + type);
	}

	private class ClassTransformer {
		private final ClassReference clazz;

		private final List<MethodTransformer> transformers;

		public ClassTransformer(String clazz) {
			this(Mappings.cl(clazz));
		}

		public ClassTransformer(ClassReference clazz) {
			this.clazz = clazz;
			transformers = new ArrayList<MethodTransformer>();
		}

		public ClassTransformer with(MethodTransformer transformer) {
			transformer.method = Mappings.md(clazz + "/" + transformer.signature);
			transformers.add(transformer);
			return this;
		}

		public boolean appliesTo(String mcpName) {
			return clazz.mcp().equals(mcpName);
		}

		public ClassNode transform(ClassNode clazz) {
			List<MethodTransformer> remaining = new ArrayList<>(transformers);
			for (MethodNode method : clazz.methods) {
				for (MethodTransformer transformer : remaining) {
					if (transformer.appliesTo(method)) {
						try {
							transformer.transform(method, method.instructions);
						} catch (Throwable e) {
							System.out.printf("%s, during transformation of %s/%s\n", e.getMessage(), this.clazz.mcp(), transformer);
							e.printStackTrace();
						}
						remaining.remove(transformer);
						break;
					}
				}
			}
			if (remaining.size() > 0) {
				System.out.println("Failed to run " + remaining.size() + " method transformer(s)");
				for (MethodTransformer transformer : remaining) {
					System.out.println(transformer);
				}
				System.out.println("Class methods:");
				for (MethodNode method : clazz.methods) {
					System.out.println(method.name + method.desc);
				}
			}
			return clazz;
		}
	}

	private abstract class MethodTransformer {
		private MethodReference method;

		private final String signature;

		public MethodTransformer(String signature) {
			this.signature = signature;
		}

		public boolean appliesTo(MethodNode method) {
			return this.method.desc().equals(method.desc) && this.method.name().equals(method.name);
		}

		public abstract void transform(MethodNode method, InsnList insns);

		@Override
		public String toString() {
			return method.mcp() + method.mcpDesc();
		}
	}
}
