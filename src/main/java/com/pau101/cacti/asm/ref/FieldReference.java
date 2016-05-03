package com.pau101.cacti.asm.ref;

public final class FieldReference extends Reference {
	private final ClassReference clazz;

	public FieldReference(String mcp, String notch) {
		super(stripClass(mcp), stripClass(notch));
		clazz = new ClassReference(isolateClass(mcp), isolateClass(notch));
	}

	public ClassReference getClassRef() {
		return clazz;
	}
}
