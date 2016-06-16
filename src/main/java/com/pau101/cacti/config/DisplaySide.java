package com.pau101.cacti.config;

import net.minecraft.util.StringUtils;

public enum DisplaySide {
	LEFT, RIGHT;

	private String configValue;

	private DisplaySide() {
		configValue = name().toLowerCase();
	}

	@Override
	public String toString() {
		return configValue;
	}

	public static DisplaySide fromString(String str) {
		if (StringUtils.isNullOrEmpty(str) || !str.toLowerCase().equals(RIGHT.toString())) {
			return LEFT;
		}
		return RIGHT;
	}
}
