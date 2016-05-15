package com.pau101.cacti.gui;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import com.pau101.cacti.Cacti;
import com.pau101.cacti.config.Configurator;

public class GuiConfigCacti extends GuiConfig {
	public GuiConfigCacti(GuiScreen parentScreen) {
		super(parentScreen, getConfigElements(), Cacti.MODID, false, true, I18n.translateToLocal("cacti.config"));
	}

	private static List<IConfigElement> getConfigElements() {
		return new ConfigElement(Configurator.config().getCategory(Configuration.CATEGORY_GENERAL)).getChildElements();
	}
}
