package com.pau101.cacti;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.pau101.cacti.api.CactiAPI;
import com.pau101.cacti.api.CactiEntry;
import com.pau101.cacti.api.CactiEntryCategory;
import com.pau101.cacti.api.CactiEntryTabGroup;
import com.pau101.cacti.config.Configurator;
import com.pau101.cacti.config.DisplaySide;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

@Mod(
	modid = Cacti.MODID,
	name = Cacti.NAME,
	version = Cacti.VERSION,
	dependencies = "required-after:Forge@[10.13.4.1614,);",
	guiFactory = "com.pau101.cacti.gui.CactiGuiFactory"
)
public class Cacti {
	public static final String MODID = "cacti";

	public static final String NAME = "Cacti";

	public static final String VERSION = "1.0.0";

	private static final ResourceLocation WIDGETS_TEX = new ResourceLocation(MODID, "textures/widgets.png");

	private static final ResourceLocation TABS_TEX = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

	private static final ResourceLocation BUTTON_TEX = new ResourceLocation("textures/gui/widgets.png");

	private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");

	private static final String MINECRAFT_TAB_GROUP = "minecraft";

	private static final String MISC_TAB_GROUP = "misc";

	private static final String MODS_TAB_GROUP = "mods";

	private static final int ENTRIES_PER_PAGE = 8;

	private static final int RIBBON_TILE_SIZE = 17;

	private static final int RIBBON_HEIGHT = 16;

	private static final int RIBBON_START_Y_OFFSET = 3;

	private static final int RIBBON_X_OFFSET = 17;

	private static final int MAX_NAME_WIDTH = 109;

	private static final int BUTTON_ID_PARENT_CATEGORY = 0x534D57;

	private static final int BUTTON_ID_PAGE_PREVIOUS = 0xC6E9A5EE;

	private static final int BUTTON_ID_PAGE_NEXT = 0x8F087F60;

	private static final int LEFT_BUTTON_WIDTH = 106;

	private static boolean shouldUseButtonRibbonRender;

	private static Map<ModContainer, List<CreativeTabs>> capturedCreativeTabs = new HashMap<>();

	private static Map<ModContainer, List<CreativeTabs>> creativeTabs = new HashMap<>();

	private static CactiEntryCategory currentCategory = CactiAPI.categories();

	private static CactiEntryTabGroup currentTabGroup;

	private static int categoryPage;

	private static int[] ribbonWidths = new int[ENTRIES_PER_PAGE];

	private static List<CactiEntry> currentPageEntries;

	private static int entryCount;

	private static GuiButton parentCategory;

	private static GuiButton pagePrevious;

	private static GuiButton pageNext;

	@EventHandler
	public void init(FMLPreInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(this);
		Configurator.init(event);
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (Cacti.MODID.equals(event.modID)) {
			Configurator.update();
		}
	}

	@HookInvoked(callerClass = CreativeTabs.class, callerMethods = "<init>(ILjava/lang/String;)V")
	public static void initCreativeTab(CreativeTabs tab) {
		ModContainer mod = null;
		String name = tab.getClass().getName();
		int pkgIdx = name.lastIndexOf('.');
		if (pkgIdx > -1) {
			String pkg = name.substring(0, pkgIdx);
			List<ModContainer> owners = new ArrayList<>();
			for (ModContainer container : Loader.instance().getModList()) {
				if (container.getOwnedPackages().contains(pkg)) { 
					owners.add(container);
				}
			}
			if (owners.size() == 1) {
				mod = owners.get(0);
			} else if (owners.size() > 1) {
				String[] unfavorable = { "api", "lib", "util" };
				owners.sort((m1, m2) -> {
					String name1 = m1.getName().toLowerCase();
					String name2 = m2.getName().toLowerCase();
					for (String k : unfavorable) {
						if (name1.contains(k)) {
							return 1;
						}
						if (name2.contains(k)) {
							return -1;
						}
					}
					return 0;
				});
				mod = owners.get(0);
			}
		}
		if (mod == null) {
			mod = Loader.instance().activeModContainer();
		}
		List<CreativeTabs> tabs = capturedCreativeTabs.get(mod);
		if (tabs == null) {
			tabs = new ArrayList<CreativeTabs>();
			capturedCreativeTabs.put(mod, tabs);
		}
		tabs.add(tab);
	}

	private static void initializeCapturedCreativeTabs() {
		Iterator<ModContainer> mods = capturedCreativeTabs.keySet().iterator();
		while (mods.hasNext()) {
			ModContainer mod = mods.next();
			List<CreativeTabs> tabs = capturedCreativeTabs.get(mod);
			mods.remove();
			List<CreativeTabs> existingTabs = creativeTabs.get(mod);
			if (existingTabs == null) {
				existingTabs = new ArrayList<CreativeTabs>();
				creativeTabs.put(mod, existingTabs);
			}
			existingTabs.addAll(tabs);
			boolean addToGroupedMods = tabs.size() == 1 && Configurator.groupSingleTabMods();
			for (CreativeTabs tab : tabs) {
				if (CactiAPI.categories().contains(tab)) {
					continue;
				}
				CactiEntryTabGroup tabGroup;
				boolean isMod = mod != null;
				String id = isMod ? mod.getModId() : MINECRAFT_TAB_GROUP;
				if (CactiAPI.categories().hasCategory(id)) {
					CactiEntryCategory category = CactiAPI.categories().getCategory(id);
					tabGroup = category.getTabGroup(MISC_TAB_GROUP);
					if (tabGroup == null) {
						tabGroup = category.withTabGroup(MISC_TAB_GROUP);
						tabGroup.setUnlocalizedNameKey("itemGroup.misc");
					}
				} else {
					String groupId = addToGroupedMods ? MODS_TAB_GROUP : id;
					tabGroup = CactiAPI.categories().getTabGroup(groupId);
					if (tabGroup == null) {
						tabGroup = CactiAPI.categories().withTabGroup(groupId);
						if (isMod && !addToGroupedMods) {
							tabGroup.setCustomName(mod.getName());
						}
					}
				}
				tabGroup.withTab(tab);
			}
		}
		sortRootEntries();
	}

	public static void ungroupSingleTabMods() {
		boolean checkAdjustTab = currentTabGroup != null && currentTabGroup.getId().equals(MODS_TAB_GROUP);
		String modid = null;
		if (CactiAPI.categories().hasTabGroup(MODS_TAB_GROUP)) {
			CactiAPI.categories().removeEntry(MODS_TAB_GROUP);
			Iterator<ModContainer> mods = creativeTabs.keySet().iterator();
			while (mods.hasNext()) {
				ModContainer mod = mods.next();
				List<CreativeTabs> tabs = creativeTabs.get(mod);
				if (tabs.size() == 1) {
					CreativeTabs tab = tabs.get(0);
					CactiAPI.categories().removeEntry(mod.getModId());
					CactiEntryTabGroup modTab = CactiAPI.categories()
						.withTabGroup(mod.getModId())
						.withTab(tab);
					modTab.setCustomName(mod.getName());
					if (checkAdjustTab && tab.tabIndex == GuiContainerCreative.selectedTabIndex) {
						currentTabGroup = modTab;
						currentCategory = CactiAPI.categories();
						GuiContainerCreative.selectedTabIndex = 0;
						checkAdjustTab = false;
					}
				}
			}
		}
		sortRootEntries();
		reinitialize();
	}

	public static void groupSingleTabMods() {
		boolean checkAdjustTab = true;
		Iterator<ModContainer> mods = creativeTabs.keySet().iterator();
		while (mods.hasNext()) {
			ModContainer mod = mods.next();
			List<CreativeTabs> tabs = creativeTabs.get(mod);
			if (tabs.size() == 1) {
				CactiAPI.categories().removeEntry(mod.getModId());
				CactiEntryTabGroup groupedMods = CactiAPI.categories().getTabGroup(MODS_TAB_GROUP);
				if (groupedMods == null) {
					groupedMods = CactiAPI.categories().withTabGroup(MODS_TAB_GROUP);
				}
				if (checkAdjustTab && currentTabGroup.getId().equals(mod.getModId())) {
					currentTabGroup = groupedMods;
					currentCategory = CactiAPI.categories();
					int tabId = groupedMods.size();
					// skip over search
					if (groupedMods.size() > 4) {
						tabId++;
					}
					// skip over inventory
					if (groupedMods.size() > 10) {
						tabId++;
					}
					GuiContainerCreative.selectedTabIndex = tabId;
					checkAdjustTab = false;
				}
				groupedMods.withTab(tabs.get(0));
			}
		}
		sortRootEntries();
		reinitialize();
	}

	private static void sortRootEntries() {
		List<CactiEntry> entries = new ArrayList<>(CactiAPI.categories().getEntries());
		CactiEntry minecraft = CactiAPI.categories().get(MINECRAFT_TAB_GROUP);
		CactiEntry groupedMods = CactiAPI.categories().get(MODS_TAB_GROUP);
		entries.remove(minecraft);
		if (groupedMods != null) {
			entries.remove(groupedMods);
		}
		Collections.sort(entries);
		entries.add(0, minecraft);
		if (groupedMods != null) {
			entries.add(1, groupedMods);
		}
		CactiAPI.categories().clear();
		for (CactiEntry entry : entries) {
			CactiAPI.categories().addEntry(entry);
		}
	}

	private static void reinitialize() {
		categoryPage = currentCategory.getEntries().indexOf(currentTabGroup) / ENTRIES_PER_PAGE;
		if (Minecraft.getMinecraft().currentScreen instanceof GuiContainerCreative) {
			initCurrent((GuiContainerCreative) Minecraft.getMinecraft().currentScreen);
		}
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "initGui()V")
	public static void initGui(GuiContainerCreative gui) {
		if (gui.mc.thePlayer.getActivePotionEffects().size() > 0) {
			gui.guiLeft = (gui.width - gui.xSize) / 2;
		}
		int parentCategoryX, pagePreviousX, pageNextX;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			int left = gui.guiLeft - 5;
			pagePreviousX = parentCategoryX = left - LEFT_BUTTON_WIDTH;
			pageNextX = left - 20;
		} else {
			int right = gui.guiLeft + gui.xSize + 5;
			pagePreviousX = parentCategoryX = right;
			pageNextX = right + LEFT_BUTTON_WIDTH - 20;
		}
		parentCategory = new GuiButton(BUTTON_ID_PARENT_CATEGORY, parentCategoryX, gui.guiTop - 22, LEFT_BUTTON_WIDTH, 20, "");
		pagePrevious = new GuiButton(BUTTON_ID_PAGE_PREVIOUS, pagePreviousX, gui.guiTop + gui.ySize + 1, 20, 20, "<");
		pageNext = new GuiButton(BUTTON_ID_PAGE_NEXT, pageNextX, gui.guiTop + gui.ySize + 1, 20, 20, ">");
		gui.buttonList.add(parentCategory);
		gui.buttonList.add(pagePrevious);
		gui.buttonList.add(pageNext);
		initializeCapturedCreativeTabs();
		if (!Configurator.rememberLastTab()) {
			GuiContainerCreative.selectedTabIndex = 0;
			currentCategory = CactiAPI.categories();
			currentTabGroup = null;
			categoryPage = 0;
		}
		initCurrent(gui);
		String widgetsOwner = getCurrentResourceOwner(WIDGETS_TEX);
		String tabsOwner = getCurrentResourceOwner(TABS_TEX);
		String widgetsOwnerName = getName(widgetsOwner);
		String tabsOwnerName = getName(tabsOwner);
		String defaultPack = Minecraft.getMinecraft().mcDefaultResourcePack.getPackName();
		String devDefaultPack = "Minecraft Forge";
		if (widgetsOwner.isEmpty() || tabsOwner.isEmpty()) {
			shouldUseButtonRibbonRender = true;
		} else if (widgetsOwnerName.equals(Cacti.NAME) && (tabsOwnerName.equals(defaultPack) || tabsOwnerName.equals(devDefaultPack))) {
			shouldUseButtonRibbonRender = false;
		} else {
			shouldUseButtonRibbonRender = !widgetsOwner.equals(tabsOwner);
		}
	}

	private static void initCurrent(GuiContainerCreative gui) {
		int index = GuiContainerCreative.selectedTabIndex;
		GuiContainerCreative.selectedTabIndex = -1;
		CactiEntryTabGroup group = currentTabGroup;
		setCurrentCategory(gui, currentCategory, categoryPage);
		if (group != null) {
			setCurrentTabGroup(gui, group);
		}
		if (index >= 0 && index < CreativeTabs.creativeTabArray.length) {
			CreativeTabs tab = CreativeTabs.creativeTabArray[index];
			if (tab != null) {
				gui.setCurrentCreativeTab(tab);
				setTabPage((index - 2) / 10);
			}
		}
	}

	private static String getCurrentResourceOwner(ResourceLocation location) {
		IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
		if (manager instanceof SimpleReloadableResourceManager) {
			SimpleReloadableResourceManager simpleMgr = (SimpleReloadableResourceManager) manager;
			FallbackResourceManager fallbackMgr = (FallbackResourceManager) simpleMgr.domainResourceManagers.get(location.getResourceDomain());
			List<IResourcePack> packs = fallbackMgr.resourcePacks;
			for (int i = packs.size(); i --> 0;) {
				IResourcePack pack = packs.get(i);
				if (pack.resourceExists(location)) {
					return pack.getPackName();
				}
			}
		}
		return "";
	}

	private static String getName(String str) {
		return str.substring(str.indexOf(':') + 1);
	}

	private static void setCurrentEntry(GuiContainerCreative gui, CactiEntry entry) {
		if (entry instanceof CactiEntryCategory) {
			setCurrentCategory(gui, (CactiEntryCategory) entry, 0);
		} else if (entry instanceof CactiEntryTabGroup) {
			setCurrentTabGroup(gui, (CactiEntryTabGroup) entry);
		}
	}

	private static void setCurrentCategory(GuiContainerCreative gui, CactiEntryCategory category, int page) {
		currentCategory = category;
		categoryPage = page;
		ImmutableList<CactiEntry> entries = category.getEntries();
		entryCount = entries.size();
		updatePageEntries(gui);
		updatePageButtons(gui);
		updateParentCategoryButton(gui);
	}

	private static void setCurrentTabGroup(GuiContainerCreative gui, CactiEntryTabGroup group) {
		currentTabGroup = group;
		if (group == null) {
			updateTabs(gui, ImmutableList.<CreativeTabs>of());
		} else {
			updateTabs(gui, group.getTabs());
		}
	}

	private static void updateTabs(GuiContainerCreative gui, ImmutableList<CreativeTabs> tabs) {
		CreativeTabs[] tabsArray = new CreativeTabs[tabs.size()];
		int len = 0;
		for (CreativeTabs tab : tabs) {
			if (tab == CreativeTabs.tabInventory || tab == CreativeTabs.tabAllSearch) {
				continue;
			}
			tabsArray[len++] = tab;
		}
		CreativeTabs[] arr = CreativeTabs.creativeTabArray = new CreativeTabs[Math.max(len + 2, 12)];
		arr[CreativeTabs.tabAllSearch.tabIndex] = CreativeTabs.tabAllSearch;
		arr[CreativeTabs.tabInventory.tabIndex] = CreativeTabs.tabInventory;
		for (int i = 0, idx = 0; i < len; idx++) {
			if (arr[idx] == null) {
				(arr[idx] = tabsArray[i++]).tabIndex = idx;
			}
		}
		int maxPages;
		final int prevId = 101, nextId = 102;
		if (arr.length > 12) {
			boolean needPrev = true, needNext = true;
			for (GuiButton button : (List<GuiButton>) gui.buttonList) {
				if (button.id == prevId) {
					needPrev = false;
				} else if (button.id == nextId) {
					needNext = false;
				}
			}
			if (needPrev) {
				gui.buttonList.add(new GuiButton(prevId, gui.guiLeft, gui.guiTop - 50, 20, 20, "<"));
			}
			if (needNext) {
				gui.buttonList.add(new GuiButton(nextId, gui.guiLeft + gui.xSize - 20, gui.guiTop - 50, 20, 20, ">"));
			}
			maxPages = (arr.length - 3) / 10;
		} else {
			Iterator<GuiButton> buttonIter = gui.buttonList.iterator();
			while (buttonIter.hasNext()) {
				GuiButton button = buttonIter.next();
				if (button.id == prevId || button.id == nextId) {
					buttonIter.remove();
				}
			}
			maxPages = 0;
		}
		// Can't AT a Forge patched member
		ReflectionHelper.setPrivateValue(GuiContainerCreative.class, gui, maxPages, "maxPages");
		setTabPage(0);
		if (tabs.isEmpty()) {
			gui.setCurrentCreativeTab(CreativeTabs.tabInventory);
		} else {
			gui.setCurrentCreativeTab(tabs.get(0));
		}
	}

	private static void setTabPage(int tabPage) {
		ReflectionHelper.setPrivateValue(GuiContainerCreative.class, null, tabPage, "tabPage");
	}

	private static void updatePageEntries(GuiContainerCreative gui) {
		ImmutableList<CactiEntry> entries = currentCategory.getEntries();
		// Hide the Minecraft entry if there are no other entries
		if (currentCategory == CactiAPI.categories() && entries.size() == 1) {
			currentPageEntries = Collections.EMPTY_LIST;
		} else {
			int start = categoryPage * ENTRIES_PER_PAGE;
			int end = Math.min(entries.size(), (categoryPage + 1) * ENTRIES_PER_PAGE);
			currentPageEntries = entries = entries.subList(start, end);
		}
		initTabGroup(gui, entries);
		updateRibbonWidths();
	}

	private static void initTabGroup(GuiContainerCreative gui, List<CactiEntry> entries) {
		CactiEntryTabGroup group = null;
		if (entries.size() > 0) {
			CactiEntry entry = entries.get(0);
			if (entry instanceof CactiEntryTabGroup) {
				group = (CactiEntryTabGroup) entry;
			}
		}
		setCurrentTabGroup(gui, group);
	}

	private static void updateRibbonWidths() {
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		for (int i = 0; i < ribbonWidths.length; i++) {
			if (i < currentPageEntries.size()) {
				ribbonWidths[i] = font.getStringWidth(currentPageEntries.get(i).getDisplayName());
			} else {
				ribbonWidths[i] = 0;
			}
		}
	}

	private static void updatePageButtons(GuiContainerCreative gui) {
		pagePrevious.visible = pageNext.visible = entryCount > ENTRIES_PER_PAGE;
		updatePageButtonEnabledStates(gui);
	}

	private static void updateParentCategoryButton(GuiContainerCreative gui) {
		CactiEntryCategory owner = currentCategory.getOwner();
		if (parentCategory.visible = owner != null) {
			String name = owner.getDisplayName();
			parentCategory.displayString = name;
		}
	}

	private static void updatePageButtonEnabledStates(GuiContainerCreative gui) {
		pagePrevious.enabled = categoryPage > 0;
		pageNext.enabled = categoryPage < (entryCount - 1) / ENTRIES_PER_PAGE;
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "mouseReleased(III)V")
	public static void mouseReleased(GuiContainerCreative gui, int mouseX, int mouseY, int state) {
		if (state == 0) {
			CactiEntry entry = getCategoryAtCursor(gui, mouseX, mouseY);
			if (entry != null) {
				setCurrentEntry(gui, entry);
			}
		}
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "actionPerformed(Lnet/minecraft/client/gui/GuiButton;)V")
	public static void actionPerformed(GuiContainerCreative gui, int id) {
		switch (id) {
			case BUTTON_ID_PAGE_PREVIOUS:
				categoryPage -= 2;
			case BUTTON_ID_PAGE_NEXT:
				categoryPage++;
				updatePageButtonEnabledStates(gui);
				updatePageEntries(gui);
				break;
			case BUTTON_ID_PARENT_CATEGORY:
				setCurrentCategory(gui, currentCategory.getOwner(), 0);
		}
	}

	private static CactiEntry getCategoryAtCursor(GuiContainerCreative gui, int mouseX, int mouseY) {
		int xLeft, xRight, xAlong;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			xLeft = 0;
			xRight = gui.guiLeft;
			xAlong = gui.guiLeft - mouseX - 1;
		} else {
			xLeft = gui.guiLeft + gui.xSize;
			xRight = gui.width;
			xAlong = mouseX - xLeft;
		}
		if (mouseX >= xLeft && mouseX < xRight && mouseY >= gui.guiTop + RIBBON_START_Y_OFFSET) {
			int index = (mouseY - gui.guiTop - RIBBON_START_Y_OFFSET - 1) / (RIBBON_TILE_SIZE - 1);
			if (index < currentPageEntries.size() && xAlong < ribbonWidths[index] + 6) {
				return currentPageEntries.get(index);
			}
		}
		return null;
	}

	private enum Pass {
		BACKGROUND, TEXT;
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "drawGuiContainerBackgroundLayer(FII)V")
	public static void drawBackground(GuiContainerCreative gui, float delta, int mouseX, int mouseY) {
		if (currentPageEntries.size() == 0) {
			return;
		}
		GL11.glDisable(GL11.GL_LIGHTING);
		gui.mc.getTextureManager().bindTexture(shouldUseButtonRibbonRender ? BUTTON_TEX : WIDGETS_TEX);
		renderEntryRibbonTiles(gui, currentPageEntries, currentTabGroup, Pass.BACKGROUND);
		renderEntryRibbonTiles(gui, currentPageEntries, currentTabGroup, Pass.TEXT);
		int pages = (entryCount - 1) / ENTRIES_PER_PAGE;
		boolean parentCategoryButtonVisible = parentCategory.visible;
		boolean renderPages = pages > 0;
		if (parentCategoryButtonVisible || renderPages) {
			int leftX = gui.guiLeft - 5;
			int shift = Math.max(0, LEFT_BUTTON_WIDTH - leftX + 2);
			int leftWidth = LEFT_BUTTON_WIDTH - shift;
			leftX += shift;
			if (parentCategoryButtonVisible) {
				parentCategory.width = leftWidth;
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					parentCategory.xPosition = leftX - LEFT_BUTTON_WIDTH;
				}
				parentCategory.displayString = fitString(gui.mc.fontRenderer, currentCategory.getDisplayName(), parentCategory.width - 6);
			}
			if (renderPages) {
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					pagePrevious.xPosition = leftX - LEFT_BUTTON_WIDTH;
				} else {
					pageNext.xPosition = parentCategory.xPosition + leftWidth - pageNext.width;
				}
				FontRenderer font = gui.mc.fontRenderer;
				String str = String.format("%d / %d", categoryPage + 1, pages + 1);
				if (pageNext.xPosition - pagePrevious.xPosition - pagePrevious.width - 5 < font.getStringWidth(str)) {
					str = Integer.toString(categoryPage + 1);
				}
				int w = font.getStringWidth(str);
				font.drawString(str, (pagePrevious.xPosition + pagePrevious.width + pageNext.xPosition) / 2 - w / 2, gui.guiTop + gui.ySize + 7, 0xFFFFFF);
			}
		}
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "drawScreen(IIF)V")
	public static void drawScreen(GuiContainerCreative gui, int mouseX, int mouseY) {
		CactiEntry entry = getCategoryAtCursor(gui, mouseX, mouseY);
		if (entry != null) {
			FontRenderer font = gui.mc.fontRenderer;
			String name = entry.getDisplayName();
			String shown = fitString(font, name, getAvailableEntryWidth(font, gui.guiLeft - 3, name) - 4);
			if (!name.equals(shown)) {
				gui.drawCreativeTabHoveringText(entry.getDisplayName(), mouseX, mouseY);
			}
		}
		GL11.glDisable(GL11.GL_LIGHTING);
	}

	private static void renderEntryRibbonTiles(GuiContainer gui, List<CactiEntry> entries, CactiEntry selected, Pass pass) {
		FontRenderer font = gui.mc.fontRenderer;
		int rightSpace = gui.guiLeft - 3;
		for (int i = 0, size = entries.size(); i < size; i++) {
			CactiEntry entry = entries.get(i);
			String name = entry.getDisplayName();
			int width = getAvailableEntryWidth(font, rightSpace, name);
			name = fitString(font, name, width - 4);
			// ceil tiles
			int tiles = (width - RIBBON_TILE_SIZE + (RIBBON_TILE_SIZE + 1)) / RIBBON_TILE_SIZE;
			int offset = (width + 1) % RIBBON_TILE_SIZE;
			int x;
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				x = gui.guiLeft - RIBBON_TILE_SIZE - offset + RIBBON_X_OFFSET;
			} else {
				x = gui.guiLeft + gui.xSize - RIBBON_TILE_SIZE * 2 + offset + RIBBON_X_OFFSET;
			}
			int y = gui.guiTop + RIBBON_START_Y_OFFSET + RIBBON_HEIGHT * i;
			boolean isSelected = entry == selected;
			if (shouldUseButtonRibbonRender) {
				renderButtonRibbon(gui, isSelected, pass, font, name, width, x, y);
			} else {
				renderRegularRibbon(gui, isSelected, pass, font, name, width, tiles, offset, x, y);
			}
		}
	}

	private static int getAvailableEntryWidth(FontRenderer font, int rightSpace, String name) {
		int width = font.getStringWidth(name) + 5;
		return Math.max(10, width - Math.max(0, width - rightSpace));
	}

	private static void renderButtonRibbon(GuiContainer gui, boolean isSelected, Pass pass, FontRenderer font, String name, int width, int x, int y) {
		width -= 1;
		if (pass == Pass.TEXT) {
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				font.drawStringWithShadow(name, gui.guiLeft - width - (width + 1) % 2, y + 3, 0xFFFFFF);
			} else {
				font.drawStringWithShadow(name, gui.guiLeft + gui.xSize + 5, y + 3, 0xFFFFFF);
			}
			return;
		}
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			x = gui.guiLeft - width - 4 + width % 2;
		} else {
			x = gui.guiLeft + gui.xSize + 2;
		}
		int vOffset = isSelected ? 20 : 0;
		width = width / 2 + 1;
		gui.drawTexturedModalRect(x, y, 0, 46 + vOffset, width, 8);
		gui.drawTexturedModalRect(x, y + 8, 0, 46 + vOffset + 13, width, 7);
		gui.drawTexturedModalRect(x + width, y, 200 - width, 46 + vOffset, width, 8);
		gui.drawTexturedModalRect(x + width, y + 8, 200 - width, 46 + vOffset + 13, width, 7);
	}

	private static void renderRegularRibbon(GuiContainer gui, boolean isSelected, Pass pass, FontRenderer font, String name, int width, int tiles, int offset, int x, int y) {
		if (pass == Pass.TEXT) {
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				font.drawString(name, gui.guiLeft - width + 4, y + 5, 0x404040);
			} else {
				font.drawString(name, gui.guiLeft + gui.xSize + 2, y + 5, 0x404040);
			}
			return;
		}
		int v;
		if (isSelected) {
			v = 0;
			GL11.glTranslatef(0, 0, 1);
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				gui.drawTexturedModalRect(gui.guiLeft, y, 51, 0, 4, RIBBON_TILE_SIZE);
			} else {
				gui.drawTexturedModalRect(gui.guiLeft + gui.xSize - 4, y, 81, 0, 4, RIBBON_TILE_SIZE);
			}
			GL11.glTranslatef(0, 0, -1);
		} else {
			v = 17;
		}
		if (tiles > 0) {
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				gui.drawTexturedModalRect(gui.guiLeft - RIBBON_TILE_SIZE, y, 34, v, RIBBON_TILE_SIZE, RIBBON_TILE_SIZE);
			} else {
				gui.drawTexturedModalRect(gui.guiLeft + gui.xSize, y, 34, v, RIBBON_TILE_SIZE, RIBBON_TILE_SIZE);
			}
		}
		for (int t = 1, lastTile = tiles - 1;; t++) {
			int w = t == 1 ? offset : RIBBON_TILE_SIZE;
			if (lastTile > 0) {
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					gui.drawTexturedModalRect(x - t * RIBBON_TILE_SIZE, y, 17, v, w, RIBBON_TILE_SIZE);
				} else {
					gui.drawTexturedModalRect(x - 0 + t * RIBBON_TILE_SIZE + (t == 1 ? RIBBON_TILE_SIZE - offset : 0), y, 102, v, w, RIBBON_TILE_SIZE);
				}
			}
			if (t >= lastTile) {
				w = lastTile == 0 ? w : RIBBON_TILE_SIZE;
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					gui.drawTexturedModalRect(x - tiles * RIBBON_TILE_SIZE, y, 0, v, w, RIBBON_TILE_SIZE);
				} else {
					gui.drawTexturedModalRect(x - 0 + tiles * RIBBON_TILE_SIZE, y, 119, v, w, RIBBON_TILE_SIZE);
				}	
				break;
			}
		}
	}

	@HookInvoked(callerClass = InventoryEffectRenderer.class, callerMethods = "drawActivePotionEffects()V")
	public static boolean drawActivePotionEffects(InventoryEffectRenderer gui) {
		if (!(gui instanceof GuiContainerCreative)) {
			return true;
		}
		final int texV = 166;
		final int texWidth = 120;
		int leftSpace = gui.guiLeft - 4;
		int widthReduceAmount = Math.max(0, texWidth - leftSpace);
		int texFullWidth = texWidth - widthReduceAmount;
		int texSplitWidth = texFullWidth / 2;
		int x, y = gui.guiTop;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			x = gui.guiLeft + gui.xSize + 2;
		} else {
			x = gui.guiLeft - texFullWidth - 2;
		}
		Collection<PotionEffect> collection = gui.mc.thePlayer.getActivePotionEffects();
		FontRenderer font = gui.mc.fontRenderer;
		if (!collection.isEmpty()) {
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			int yStride = 33;
			if (collection.size() > 5) {
				yStride = 132 / (collection.size() - 1);
			}
			for (PotionEffect effect : collection) {
				Potion potion = Potion.potionTypes[effect.getPotionID()];
				if (!potion.shouldRenderInvText(effect)) {
					continue;
				}
				GL11.glColor3f(1, 1, 1);
				gui.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
				gui.drawTexturedModalRect(x, y, 0, texV, texSplitWidth, 32);
				gui.drawTexturedModalRect(x + texSplitWidth, y, texWidth - texSplitWidth, texV, texSplitWidth, 32);
				if (potion.hasStatusIcon()) {
					int icon = potion.getStatusIconIndex();
					gui.drawTexturedModalRect(x + 6, y + 7, 0 + icon % 8 * 18, 198 + icon / 8 * 18, 18, 18);
				}
				potion.renderInventoryEffect(x, y, effect, gui.mc);
				if (!potion.shouldRenderInvText(effect)) {
					y += yStride;
					continue;
				}
				String name = I18n.format(potion.getName());
				int amplifier = effect.getAmplifier();
				String level;
				if (amplifier > 0 && amplifier < 4) {
					level = " " + I18n.format("enchantment.level." + (amplifier + 1));
				} else {
					level = "";
				}
				int levelWidth = font.getStringWidth(level);
				name = fitString(font, name, texWidth - widthReduceAmount - 32 - levelWidth) + level;
				font.drawStringWithShadow(name, x + 28, y + 6, 0xFFFFFF);
				String remainingTime = Potion.getDurationString(effect);
				font.drawStringWithShadow(remainingTime, x + 28, y + 16, 0x7F7F7F);
				y += yStride;
			}
		}
		return false;
	}

	private static String fitString(FontRenderer font, String text, int maxWidth) {
		if (font.getStringWidth(text) < maxWidth) {
			return text;
		}
		int width = 0;
		boolean isBold = false;
		String ellipses = "...";
		int boldEllipsesAddition = 3;
		maxWidth -= font.getStringWidth(ellipses);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < text.length(); ++i) {
			char chr = text.charAt(i);
			int charWidth = font.getCharWidth(chr);
			if (charWidth < 0 && i < text.length() - 1) {
				chr = text.charAt(++i);
				if (!isBold && (chr == 'l' || chr == 'L')) {
					isBold = true;
					maxWidth -= boldEllipsesAddition;
				} else if (isBold && (chr == 'r' || chr == 'R')) {
					isBold = false;
					maxWidth += boldEllipsesAddition;
				}
				charWidth = 0;
			}
			width += charWidth;
			if (isBold && charWidth > 0) {
				width++;
			}
			if (width > maxWidth) {
				result.append(ellipses);
				break;
			}
			result.append(chr);
		}
		return result.toString();
	}
}
