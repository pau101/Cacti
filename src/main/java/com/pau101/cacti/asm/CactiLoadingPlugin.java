package com.pau101.cacti.asm;

import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import com.pau101.cacti.Cacti;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name(Cacti.NAME)
@IFMLLoadingPlugin.MCVersion(MinecraftForge.MC_VERSION)
@IFMLLoadingPlugin.TransformerExclusions({ "com.pau101.cacti.asm." })
public class CactiLoadingPlugin implements IFMLLoadingPlugin {
	@Override
	public String[] getASMTransformerClass() {
		return new String[] { CactiClassTransformer.class.getCanonicalName() };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
