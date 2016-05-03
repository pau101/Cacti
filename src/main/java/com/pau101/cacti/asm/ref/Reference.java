package com.pau101.cacti.asm.ref;

import com.pau101.cacti.asm.Mappings;

public abstract class Reference {
	protected final String mcp;

	protected final String notch;

	public Reference(String mcp, String notch) {
		this.mcp = mcp;
		this.notch = notch;
	}

	public String name() {
		return toString();
	}

	public String mcp() {
		return mcp;
	}

	public String notch() {
		return notch;
	}

	@Override
	public String toString() {
		return Mappings.isCurrentlyObfuscated ? notch : mcp;
	}

	@Override
	public int hashCode() {
		return 31 * (31 + mcp.hashCode()) + notch.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Reference) {
			Reference other = (Reference) obj;
			return mcp.equals(other.mcp) && notch.equals(other.notch);
		}
		return false;
	}

	public  static String stripClass(String ref) {
		return ref.substring(ref.lastIndexOf('/') + 1);
	}

	public static String isolateClass(String ref) {
		return ref.substring(0, ref.lastIndexOf('/'));
	}
}
