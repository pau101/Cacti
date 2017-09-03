package com.pau101.cacti.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.pau101.cacti.gui.GuiContainerCactiCreative;

public final class Configurator {
	private Configurator() {}

	private static File configFile;

	private static Configuration config;

	private static DisplaySide displaySide;

	private static boolean groupSingleTabMods;

	private static boolean rememberLastTab;

	public static Configuration config() {
		return config;
	}

	public static DisplaySide displaySide() {
		return displaySide;
	}

	public static boolean groupSingleTabMods() {
		return groupSingleTabMods;
	}

	public static boolean rememberLastTab() {
		return rememberLastTab;
	}

	public static void init(FMLPreInitializationEvent event) {
		configFile = event.getSuggestedConfigurationFile();
		config = new Configuration(configFile);
		update();
	}

	public static void update() {
		displaySide = DisplaySide.fromString(config.getString("Display Side", Configuration.CATEGORY_GENERAL, DisplaySide.LEFT.toString(),
			"Either 'left' or 'right' for what side the entries will display on.", new String[] { DisplaySide.LEFT.toString(), DisplaySide.RIGHT.toString() }
		));
		int gstmChange = groupSingleTabMods ? 1 : 0;
		groupSingleTabMods = config.getBoolean("Group Single Tab Mods", Configuration.CATEGORY_GENERAL, false, 
			"When 'true' all mods which contain a single tab will be put under one tab group."
		);
		rememberLastTab = config.getBoolean("Remember Last Tab", Configuration.CATEGORY_GENERAL, true, 
			"When 'true' the current tab will be persistent between GUI opennings."
		);
		if (config.hasChanged()) {
			config.save();
			gstmChange |= groupSingleTabMods ? 2 : 0;
			if (gstmChange == 0b01) {
				GuiContainerCactiCreative.ungroupSingleTabMods();
			} else if (gstmChange == 0b10) {
				GuiContainerCactiCreative.groupSingleTabMods();
			}
		}
	}
}
