package com.pau101.cacti.asm.ref;

import com.pau101.cacti.asm.Mappings;

public final class MethodReference extends Reference {
	private final ClassReference clazz;

	private final String mcpDesc;

	private final String notchDesc;

	public MethodReference(String mcp, String notch, String mcpDesc, String notchDesc) {
		super(stripClass(mcp), stripClass(notch));
		clazz = new ClassReference(isolateClass(mcp), isolateClass(notch));
		this.mcpDesc = mcpDesc;
		this.notchDesc = notchDesc;
	}

	public ClassReference getClassRef() {
		return clazz;
	}

	public String desc() {
		return Mappings.isCurrentlyObfuscated ? notchDesc : mcpDesc;
	}

	public String mcpDesc() {
		return mcpDesc;
	}

	public String notchDesc() {
		return notchDesc;
	}
}
