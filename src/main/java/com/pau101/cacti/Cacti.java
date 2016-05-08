package com.pau101.cacti;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.pau101.cacti.api.CactiAPI;
import com.pau101.cacti.api.CactiEntry;
import com.pau101.cacti.api.CactiEntryCategory;
import com.pau101.cacti.api.CactiEntryTabGroup;

@Mod(
	modid = Cacti.MODID,
	name = Cacti.NAME,
	version = Cacti.VERSION,
	clientSideOnly = true,
	dependencies = "required-after:Forge@[11.14.4.1563,);"
)
public class Cacti {
	public static final String MODID = "cacti";

	public static final String NAME = "Cacti";

	public static final String VERSION = "1.0.0";

	private static final ResourceLocation WIDGETS_TEX = new ResourceLocation(MODID, "textures/widgets.png");

	private static final ResourceLocation TABS_TEX = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

	private static final ResourceLocation BUTTON_TEX = new ResourceLocation("textures/gui/widgets.png");

	private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");

	private static final String MISC_TAB_GROUP = "misc";

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

	private static List<Pair<CreativeTabs, ModContainer>> capturedCreativeTabs = new ArrayList<>(12);

	private static CactiEntryCategory currentCategory = CactiAPI.categories();

	private static CactiEntryTabGroup currentTabGroup;

	private static int categoryPage;

	private static int[] ribbonWidths = new int[ENTRIES_PER_PAGE];

	private static List<CactiEntry> currentPageEntries;

	private static int entryCount;

	private static GuiButton parentCategory;

	private static GuiButton pagePrevious;

	private static GuiButton pageNext;

	public static int onPotionShift(InventoryEffectRenderer gui, int shift) {
		if (Minecraft.getMinecraft().currentScreen instanceof GuiContainerCreative) {
			return (gui.width - gui.xSize) / 2;
		}
		return shift;
	}

	@HookInvoked(callerClass = CreativeTabs.class, callerMethods = "<init>(ILjava/lang/String;)V")
	public static void initCreativeTab(CreativeTabs tab) {
		ModContainer mod = null;
		try {
			// Just a bit of FML hackery to make our lives easier
			Field modControllerField = Loader.class.getDeclaredField("modController");
			Field packageOwnersField = LoadController.class.getDeclaredField("packageOwners");
			modControllerField.setAccessible(true);
			packageOwnersField.setAccessible(true);
			LoadController controller = (LoadController) modControllerField.get(Loader.instance());
			// Null if we're in vanilla bootstrap
			if (controller != null) {
				ListMultimap<String, ModContainer> packageOwners = (ListMultimap<String, ModContainer>) packageOwnersField.get(controller);
				// If empty the CreativeTabs was probably created during FMLConstruction in <init> or <cinit>
				if (packageOwners.size() > 0) {
					String name = tab.getClass().getName();
					int pkgIdx = name.lastIndexOf('.');
					if (pkgIdx > -1) {
						String pkg = name.substring(0, pkgIdx);
						// I'm pretty sure it is guaranteed to have it at this point, just in case
						if (packageOwners.containsKey(pkg)) {
							mod = packageOwners.get(pkg).get(0);
						}
					}
					// else the CreativeTabs is in the default package
				}
				// Either packageOwners is empty or the package is in default or unknown
				if (mod == null) {
					mod = Loader.instance().activeModContainer();
				}
			}
		} catch (Exception e) { /* Shhh... */ }
		capturedCreativeTabs.add(Pair.of(tab, mod));
	}

	private static void initializeCapturedCreativeTabs() {
		while (capturedCreativeTabs.size() > 0) {
			Pair<CreativeTabs, ModContainer> tabCapture = capturedCreativeTabs.remove(0);
			CreativeTabs tab = tabCapture.getLeft();
			if (CactiAPI.categories().contains(tab)) {
				continue;
			}
			ModContainer mod = tabCapture.getRight();
			CactiEntryTabGroup tabGroup;
			boolean isMod = mod != null;
			String id = isMod ? mod.getModId() : "minecraft";
			if (CactiAPI.categories().hasCategory(id)) {
				CactiEntryCategory category = CactiAPI.categories().getCategory(id);
				tabGroup = category.getTabGroup(MISC_TAB_GROUP);
				if (tabGroup == null) {
					tabGroup = category.withTabGroup(MISC_TAB_GROUP);
					tabGroup.setUnlocalizedNameKey("itemGroup.misc");
				}
			} else {
				tabGroup = CactiAPI.categories().getTabGroup(id);
				if (tabGroup == null) {
					tabGroup = CactiAPI.categories().withTabGroup(id);
					if (isMod) {
						tabGroup.setCustomName(mod.getName());
					}
				}
			}
			tabGroup.withTab(tab);
		}
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "initGui()V")
	public static void initGui(GuiContainerCreative gui) {
		int left = gui.guiLeft - 5;
		parentCategory = new GuiButton(BUTTON_ID_PARENT_CATEGORY, left - LEFT_BUTTON_WIDTH, gui.guiTop - 22, LEFT_BUTTON_WIDTH, 20, "");
		pagePrevious = new GuiButton(BUTTON_ID_PAGE_PREVIOUS, left - LEFT_BUTTON_WIDTH, gui.guiTop + gui.ySize + 1, 20, 20, "<");
		pageNext = new GuiButton(BUTTON_ID_PAGE_NEXT, left - 20, gui.guiTop + gui.ySize + 1, 20, 20, ">");
		gui.buttonList.add(parentCategory);
		gui.buttonList.add(pagePrevious);
		gui.buttonList.add(pageNext);
		initializeCapturedCreativeTabs();
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
			}
		}
		String widgetsOwner = getCurrentResourceOwner(WIDGETS_TEX);
		String tabsOwner = getCurrentResourceOwner(TABS_TEX);
		String widgetsOwnerName = getName(widgetsOwner);
		String tabsOwnerName = getName(tabsOwner);
		String defaultPack = Minecraft.getMinecraft().mcDefaultResourcePack.getPackName();
		String devDefaultPack = ForgeModContainer.getInstance().getName();
		if (widgetsOwner.isEmpty() || tabsOwner.isEmpty()) {
			shouldUseButtonRibbonRender = true;
		} else if (widgetsOwnerName.equals(Cacti.NAME) && (tabsOwnerName.equals(defaultPack) || tabsOwnerName.equals(devDefaultPack))) {
			shouldUseButtonRibbonRender = false;
		} else {
			shouldUseButtonRibbonRender = !widgetsOwner.equals(tabsOwner);
		}
	}

	private static String getCurrentResourceOwner(ResourceLocation location) {
		try {
			IResource res = Minecraft.getMinecraft().getResourceManager().getResource(location);
			if (res instanceof SimpleResource) {
				return ((SimpleResource) res).getResourcePackName();
			}
		} catch (IOException e) {}
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
		CreativeTabs[] arr = CreativeTabs.creativeTabArray = new CreativeTabs[Math.max(tabs.size(), 12)];
		arr[CreativeTabs.tabAllSearch.tabIndex] = CreativeTabs.tabAllSearch;
		arr[CreativeTabs.tabInventory.tabIndex] = CreativeTabs.tabInventory;
		for (int i = 0, idx = 0; idx < arr.length;) {
			if (arr[idx] == null) {
				CreativeTabs tab = tabs.get(i++);
				if (tab == CreativeTabs.tabInventory || tab == CreativeTabs.tabAllSearch) {
					continue;
				} else {
					arr[idx] = tab;
					tab.tabIndex = idx;
				}
				if (i == tabs.size()) {
					break;
				}
			}
			idx++;
		}
		int maxPages;
		final int prevId = 101, nextId = 102;
		if (arr.length > 12) {
			boolean needPrev = true, needNext = true;
			for (GuiButton button : gui.buttonList) {
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
			maxPages = ((arr.length - 12) / 10) + 1;
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
		ReflectionHelper.setPrivateValue(GuiContainerCreative.class, null, 0, "tabPage");
		if (tabs.isEmpty()) {
			gui.setCurrentCreativeTab(CreativeTabs.tabInventory);
		} else {
			gui.setCurrentCreativeTab(tabs.get(0));
		}
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
		FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
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
			parentCategory.displayString = fitString(gui.mc.fontRendererObj, name, parentCategory.width - 6);
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
		if (mouseX < gui.guiLeft && mouseY >= gui.guiTop + RIBBON_START_Y_OFFSET) {
			int index = (mouseY - gui.guiTop - RIBBON_START_Y_OFFSET - 1) / (RIBBON_TILE_SIZE - 1);
			if (index < currentPageEntries.size() && gui.guiLeft - mouseX - 1 < ribbonWidths[index] + 6) {
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
		GlStateManager.disableLighting();
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
				parentCategory.xPosition = leftX - LEFT_BUTTON_WIDTH;
			}
			if (renderPages) {
				pagePrevious.xPosition = leftX - LEFT_BUTTON_WIDTH;
				FontRenderer font = gui.mc.fontRendererObj;
				String str = String.format("%d / %d", categoryPage + 1, pages + 1);
				if (pageNext.xPosition - pagePrevious.xPosition - pagePrevious.width - 5 < font.getStringWidth(str)) {
					str = Integer.toString(categoryPage + 1);
				}
				int w = font.getStringWidth(str);
				font.drawString(str, gui.guiLeft - leftWidth / 2 - w / 2 - 4, gui.guiTop + gui.ySize + 7, 0xFFFFFF);
			}
		}
	}

	@HookInvoked(callerClass = GuiContainerCreative.class, callerMethods = "drawScreen(IIF)V")
	public static void drawScreen(GuiContainerCreative gui, int mouseX, int mouseY) {
		CactiEntry entry = getCategoryAtCursor(gui, mouseX, mouseY);
		if (entry != null) {
			FontRenderer font = gui.mc.fontRendererObj;
			String name = entry.getDisplayName();
			String shown = fitString(font, name, getAvailableEntryWidth(font, gui.guiLeft - 3, name) - 4);
			if (!name.equals(shown)) {
				gui.drawCreativeTabHoveringText(entry.getDisplayName(), mouseX, mouseY);
			}
		}
	}

	private static void renderEntryRibbonTiles(GuiContainer gui, List<CactiEntry> entries, CactiEntry selected, Pass pass) {
		FontRenderer font = gui.mc.fontRendererObj;
		int rightSpace = gui.guiLeft - 3;
		for (int i = 0, size = entries.size(); i < size; i++) {
			CactiEntry entry = entries.get(i);
			String name = entry.getDisplayName();
			int width = getAvailableEntryWidth(font, rightSpace, name);
			name = fitString(font, name, width - 4);
			// ceil tiles
			int tiles = (width - RIBBON_TILE_SIZE + (RIBBON_TILE_SIZE + 1)) / RIBBON_TILE_SIZE;
			int offset = (width + 1) % RIBBON_TILE_SIZE;
			int x = gui.guiLeft - RIBBON_TILE_SIZE - offset + RIBBON_X_OFFSET;
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
		width -= 2;
		if (pass == Pass.TEXT) {
			font.drawStringWithShadow(name, gui.guiLeft - width - 1, y + 3, 0xFFFFFF);
			return;
		}
		x = gui.guiLeft - width - 4 + width % 2;
		int vOffset = isSelected ? 20 : 0;
		width = width / 2 + 1;
        gui.drawTexturedModalRect(x, y, 0, 46 + vOffset, width, 8);
        gui.drawTexturedModalRect(x, y + 8, 0, 46 + vOffset + 13, width, 7);
        gui.drawTexturedModalRect(x + width, y, 200 - width, 46 + vOffset, width, 8);
        gui.drawTexturedModalRect(x + width, y + 8, 200 - width, 46 + vOffset + 13, width, 7);
	}

	private static void renderRegularRibbon(GuiContainer gui, boolean isSelected, Pass pass, FontRenderer font, String name, int width, int tiles, int offset, int x, int y) {
		if (pass == Pass.TEXT) {
			font.drawString(name, gui.guiLeft - width + 4, y + 5, 0x404040);
			return;
		}
		int v;
		if (isSelected) {
			v = 0;
			GlStateManager.translate(0, 0, 1);
			gui.drawTexturedModalRect(gui.guiLeft, y, 51, 0, 4, RIBBON_TILE_SIZE);
			GlStateManager.translate(0, 0, -1);
		} else {
			v = 17;
		}
		if (tiles > 0) {
			gui.drawTexturedModalRect(gui.guiLeft - RIBBON_TILE_SIZE, y, 34, v, RIBBON_TILE_SIZE, RIBBON_TILE_SIZE);
		}
		for (int t = 1, lastTile = tiles - 1;; t++) {
			int w = t == 1 ? offset : RIBBON_TILE_SIZE;
			if (lastTile > 0) {
				gui.drawTexturedModalRect(x - t * RIBBON_TILE_SIZE, y, 17, v, w, RIBBON_TILE_SIZE);
			}
			if (t >= lastTile) {
				w = lastTile == 0 ? w : RIBBON_TILE_SIZE;
				gui.drawTexturedModalRect(x - tiles * RIBBON_TILE_SIZE, y, 0, v, w, RIBBON_TILE_SIZE);	
				break;
			}
		}
	}

	@HookInvoked(callerClass = InventoryEffectRenderer.class, callerMethods = "drawActivePotionEffects()V")
	public static boolean drawActivePotionEffects(InventoryEffectRenderer gui) {
		if (!(gui instanceof GuiContainerCreative)) {
			return true;
		}
		int x = gui.guiLeft + gui.xSize + 2;
		int y = gui.guiTop;
		final int texV = 166;
		final int texWidth = 140;
		final int widthPad = 16; // Got this value from trial and error...
		int leftSpace = gui.width - gui.guiLeft - gui.xSize + widthPad;
		int widthReduceAmount = Math.max(0, texWidth - leftSpace);
		int texSplitWidth = (texWidth - widthReduceAmount) / 2;
		Collection<PotionEffect> collection = gui.mc.thePlayer.getActivePotionEffects();
		FontRenderer font = gui.mc.fontRendererObj;
		if (!collection.isEmpty()) {
			GlStateManager.disableLighting();
			int yStride = 33;
			if (collection.size() > 5) {
				yStride = 132 / (collection.size() - 1);
			}
			for (PotionEffect effect : collection) {
				Potion potion = Potion.potionTypes[effect.getPotionID()];
				if (!potion.shouldRenderInvText(effect)) {
					continue;
				}
				GlStateManager.color(1, 1, 1);
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
				name = fitString(font, name, texWidth - widthReduceAmount - 53 - levelWidth) + level;
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
