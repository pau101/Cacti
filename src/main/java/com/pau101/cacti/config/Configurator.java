package com.pau101.cacti.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.pau101.cacti.Cacti;

public final class Configurator {
	private Configurator() {}

	private static File configFile;

	private static Configuration config;

	private static boolean displayOnLeftSide;

	private static boolean groupSingleTabMods;

	public static Configuration config() {
		return config;
	}

	public static boolean displayOnLeftSide() {
		return displayOnLeftSide;
	}

	public static boolean groupSingleTabMods() {
		return groupSingleTabMods;
	}

	public static void init(FMLPreInitializationEvent event) {
		configFile = event.getSuggestedConfigurationFile();
		config = new Configuration(configFile);
		update();
	}

	public static void update() {
		displayOnLeftSide = config.getBoolean("Display On Left Side", Configuration.CATEGORY_GENERAL, true,
			"When 'true' the entries will be displayed on the left of the creative inventory, otherwise the right."
		);
		int gstmChange = groupSingleTabMods ? 1 : 0;
		groupSingleTabMods = config.getBoolean("Group Single Tab Mods", Configuration.CATEGORY_GENERAL, false, 
			"When 'true' all mods which contain a single tab will be put under one tab group."
		);
		if (config.hasChanged()) {
			config.save();
			gstmChange |= groupSingleTabMods ? 2 : 0;
			if (gstmChange == 0b01) {
				Cacti.ungroupSingleTabMods();
			} else if (gstmChange == 0b10) {
				Cacti.groupSingleTabMods();
			}
		}
	}
}
