package com.pau101.cacti.asm;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pau101.cacti.asm.ref.ClassReference;
import com.pau101.cacti.asm.ref.FieldReference;
import com.pau101.cacti.asm.ref.MethodReference;
import com.pau101.cacti.asm.ref.PackageReference;
import com.pau101.cacti.asm.ref.Reference;

public final class Mappings {
	private Mappings() {}

	public static boolean isCurrentlyObfuscated;

	private static final String RESOURCE = "/assets/cacti/texts/mcp-notch.srg";

	private static final Map<String, ClassReference> CLASSES = new HashMap<>();

	private static final Map<String, FieldReference> FIELDS = new HashMap<>();

	private static final Map<String, MethodReference> METHODS = new HashMap<>();

	private static final Map<String, PackageReference> PACKAGES = new HashMap<>();

	static {
		InputStream mappingsStream = Mappings.class.getResourceAsStream(RESOURCE);
		if (mappingsStream == null) {
			throw new RuntimeException("Failed to locate Cacti mcp-notch mappings!");
		}
		Scanner mappings = new Scanner(mappingsStream);
		while (mappings.hasNextLine()) {
			String type = mappings.next();	
			String token1 = mappings.next();
			String token2 = mappings.next();
			switch (type) {
				case "CL:":
					CLASSES.put(token1, new ClassReference(token1, token2));
					break;
				case "FD:":
					FIELDS.put(token1, new FieldReference(token1, token2));
					break;
				case "MD:": {
					String notch = mappings.next();
					String notchDesc = mappings.next();
					METHODS.put(token1 + token2, new MethodReference(token1, notch, token2, notchDesc));
					break;
				}
				case "PK:":
					PACKAGES.put(token1, new PackageReference(token1, token2));
			}
			mappings.nextLine();
		}
		mappings.close();
	}

	public static ClassReference cl(String ref) {
		return get(CLASSES, ref, "class");
	}

	public static FieldReference fd(String ref) {
		return get(FIELDS, ref, "field");
	}

	private static final Pattern CLASS_REFERENCE = Pattern.compile("(?<=L).+?(?=;)");

	public static MethodReference md(String ref) {
		if (METHODS.containsKey(ref)) {
			return METHODS.get(ref);
		}
		int ms = ref.indexOf('(');
		String mcpName = ref.substring(0, ms);
		String name = mcpName.substring(mcpName.lastIndexOf('/') + 1);
		if (name.equals("<init>")) {
			Matcher m = CLASS_REFERENCE.matcher(ref);
			StringBuffer mcpSig = new StringBuffer();
			while (m.find()) {
				String cls = m.group();
				if (CLASSES.containsKey(cls)) {
					cls = CLASSES.get(cls).notch();
				}
				m.appendReplacement(mcpSig, cls);
			}
			String notchRef = m.appendTail(mcpSig).toString();
			int ns = notchRef.indexOf('(');
			String notchName = notchRef.substring(0, ns);
			String mcpDesc = ref.substring(ms);
			String notchDesc = notchRef.substring(ns);
			MethodReference method = new MethodReference(mcpName, notchName, mcpDesc, notchDesc);
			METHODS.put(ref, method);
			return method;
		}
		return get(METHODS, ref, "method");
	}

	public static PackageReference pk(String ref) {
		return get(PACKAGES, ref, "package");
	}

	private static <T extends Reference> T get(Map<String, T> map, String ref, String type) {
		T value = map.get(ref);
		if (value == null) {
			throw new RuntimeException("Failed to find " + type + ": " + ref);
		}
		return value;
	}
}
