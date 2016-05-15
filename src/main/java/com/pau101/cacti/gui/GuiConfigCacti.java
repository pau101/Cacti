package com.pau101.cacti.gui;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import com.pau101.cacti.Cacti;
import com.pau101.cacti.config.Configurator;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class GuiConfigCacti extends GuiConfig {
	public GuiConfigCacti(GuiScreen parentScreen) {
		super(parentScreen, getConfigElements(), Cacti.MODID, false, true, StatCollector.translateToLocal("cacti.config"));
	}

	private static List<IConfigElement> getConfigElements() {
		return new ConfigElement(Configurator.config().getCategory(Configuration.CATEGORY_GENERAL)).getChildElements();
	}
}
