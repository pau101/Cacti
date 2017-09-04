package com.pau101.cacti.gui;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.pau101.cacti.Cacti;
import com.pau101.cacti.api.CactiAPI;
import com.pau101.cacti.api.CactiEntry;
import com.pau101.cacti.api.CactiEntryCategory;
import com.pau101.cacti.api.CactiEntryTabGroup;
import com.pau101.cacti.config.Configurator;
import com.pau101.cacti.config.DisplaySide;

public final class GuiContainerCactiCreative extends GuiContainerCreative {
	private static final ResourceLocation WIDGETS_TEX = new ResourceLocation(Cacti.MODID, "textures/widgets.png");

	private static final ResourceLocation TABS_TEX = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

	private static final ResourceLocation BUTTON_TEX = new ResourceLocation("textures/gui/widgets.png");

	private static final String MINECRAFT_TAB_GROUP = "minecraft";

	private static final String MISC_TAB_GROUP = "misc";

	private static final String MODS_TAB_GROUP = "mods";

	private static final int ENTRIES_PER_PAGE = 8;

	private static final int RIBBON_TILE_SIZE = 17;

	private static final int RIBBON_HEIGHT = 16;

	private static final int RIBBON_START_Y_OFFSET = 3;

	private static final int RIBBON_X_OFFSET = 17;

	private static final int BUTTON_ID_PARENT_CATEGORY = 0x534D57;

	private static final int BUTTON_ID_PAGE_PREVIOUS = 0xC6E9A5EE;

	private static final int BUTTON_ID_PAGE_NEXT = 0x8F087F60;

	private static final int LEFT_BUTTON_WIDTH = 106;

	private static boolean shouldUseButtonRibbonRender;

	private static Multimap<ModContainer, CreativeTabs> creativeTabs = HashMultimap.create();

	private static CactiEntryCategory currentCategory = CactiAPI.categories();

	private static CactiEntryTabGroup currentTabGroup;

	private static int categoryPage;

	private static int[] ribbonWidths = new int[ENTRIES_PER_PAGE];

	private static List<CactiEntry> currentPageEntries;

	private static int entryCount;

	private SetTabAction setTabAction = SetTabAction.SET;

	private GuiButton parentCategory;

	private GuiButton pagePrevious;

	private GuiButton pageNext;

	public GuiContainerCactiCreative(EntityPlayer player) {
		super(player);
	}

	private void setSetTabAction(SetTabAction setTabAction) {
		this.setTabAction = setTabAction;
	}

	@Override
	public void initGui() {
		try {
			int selected = selectedTabIndex;
			setSetTabAction(SetTabAction.IGNORE);
			super.initGui();
			selectedTabIndex = selected;
			buttonList.clear();
		} finally {
			setSetTabAction(SetTabAction.SET);
		}
		init();
	}

	@Override
	protected void updateActivePotionEffects() {
		boolean hasVisibleEffect = false;
		for (PotionEffect effect : mc.player.getActivePotionEffects()) {
			if (effect.getPotion().shouldRender(effect)) {
				hasVisibleEffect = true;
				break;
			}
		}
		hasActivePotionEffects = hasVisibleEffect;
	}

	@Override
	public void setCurrentCreativeTab(CreativeTabs tab) {
		setTabAction.setCurrentCreativeTab(this, tab);
	}

	private void doSetCurrentCreativeTab(CreativeTabs tab) {
		super.setCurrentCreativeTab(tab);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float delta, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1);
		RenderHelper.enableGUIStandardItemLighting();
		drawBackground();
		super.drawGuiContainerBackgroundLayer(delta, mouseX, mouseY);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float delta) {
		super.drawScreen(mouseX, mouseY, delta);
		drawScreen(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		try {
			replaceTabs(null, NullCreativeTab.INSTANCE);
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} finally {
			replaceTabs(NullCreativeTab.INSTANCE, null);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		if (state == 0) {
			CactiEntry entry = getCategoryAtCursor(mouseX, mouseY);
			if (entry != null) {
				setCurrentEntry(entry);
			}
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		switch (button.id) {
			case BUTTON_ID_PAGE_PREVIOUS:
				categoryPage -= 2;
			case BUTTON_ID_PAGE_NEXT:
				categoryPage++;
				updatePageButtonEnabledStates();
				updatePageEntries();
				break;
			case BUTTON_ID_PARENT_CATEGORY:
				setCurrentCategory(currentCategory.getOwner(), 0);
		}
	}

	@Override
	public void drawActivePotionEffects() {
		final int texV = 166;
		final int texWidth = 120;
		int leftSpace = guiLeft - 4;
		int widthReduceAmount = Math.max(0, texWidth - leftSpace);
		int texFullWidth = texWidth - widthReduceAmount;
		int texSplitWidth = texFullWidth / 2;
		int x, y = guiTop;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			x = guiLeft + xSize + 2;
		} else {
			x = guiLeft - texFullWidth - 2;
		}
		Collection<PotionEffect> collection = mc.player.getActivePotionEffects();
		FontRenderer font = mc.fontRendererObj;
		if (!collection.isEmpty()) {
			GlStateManager.color(1, 1, 1);
			GlStateManager.disableLighting();
			int yStride = 33;
			if (collection.size() > 5) {
				yStride = 132 / (collection.size() - 1);
			}
			for (PotionEffect effect : Ordering.natural().sortedCopy(collection)) {
				Potion potion = effect.getPotion();
				if (!potion.shouldRender(effect)) {
					continue;
				}
				GlStateManager.color(1, 1, 1);
				mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
				drawTexturedModalRect(x, y, 0, texV, texSplitWidth, 32);
				drawTexturedModalRect(x + texSplitWidth, y, texWidth - texSplitWidth, texV, texSplitWidth, 32);
				if (potion.hasStatusIcon()) {
					int icon = potion.getStatusIconIndex();
					drawTexturedModalRect(x + 6, y + 7, icon % 8 * 18, 198 + icon / 8 * 18, 18, 18);
				}
				potion.renderInventoryEffect(x, y, effect, mc);
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
				String remainingTime = Potion.getPotionDurationString(effect, 1);
				font.drawStringWithShadow(remainingTime, x + 28, y + 16, 0x7F7F7F);
				y += yStride;
			}
		}
	}

	private void init() {
		int parentCategoryX, pagePreviousX, pageNextX;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			int left = guiLeft - 5;
			pagePreviousX = parentCategoryX = left - LEFT_BUTTON_WIDTH;
			pageNextX = left - 20;
		} else {
			int right = guiLeft + xSize + 5;
			pagePreviousX = parentCategoryX = right;
			pageNextX = right + LEFT_BUTTON_WIDTH - 20;
		}
		parentCategory = new GuiButton(BUTTON_ID_PARENT_CATEGORY, parentCategoryX, guiTop - 22, LEFT_BUTTON_WIDTH, 20, "");
		pagePrevious = new GuiButton(BUTTON_ID_PAGE_PREVIOUS, pagePreviousX, guiTop + ySize + 1, 20, 20, "<");
		pageNext = new GuiButton(BUTTON_ID_PAGE_NEXT, pageNextX, guiTop + ySize + 1, 20, 20, ">");
		buttonList.add(parentCategory);
		buttonList.add(pagePrevious);
		buttonList.add(pageNext);
		initializeCreativeTabs();
		if (!Configurator.rememberLastTab()) {
			selectedTabIndex = 0;
			currentCategory = CactiAPI.categories();
			currentTabGroup = null;
			categoryPage = 0;
		}
		updateSelected();
		String widgetsOwner = getResourceOwner(WIDGETS_TEX);
		String tabsOwner = getResourceOwner(TABS_TEX);
		String widgetsOwnerName = getName(widgetsOwner);
		String tabsOwnerName = getName(tabsOwner);
		String defaultPack = Minecraft.getMinecraft().mcDefaultResourcePack.getPackName();
		String devDefaultPack = ForgeModContainer.getInstance().getName();
		shouldUseButtonRibbonRender = widgetsOwner.isEmpty() || tabsOwner.isEmpty() || !(widgetsOwnerName.equals(Cacti.NAME) && (tabsOwnerName.equals(defaultPack) || tabsOwnerName.equals(devDefaultPack))) && !widgetsOwner.equals(tabsOwner);
	}

	private void initializeCreativeTabs() {
		creativeTabs.clear();
		for (CreativeTabs tab : CreativeTabs.CREATIVE_TAB_ARRAY) {
			if (tab != null) {
				creativeTabs.put(getOwner(tab), tab);
			}
		}
		for (ModContainer mod : creativeTabs.keySet()) {
			Collection<CreativeTabs> tabs = creativeTabs.get(mod);
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

	private void updateSelected() {
		int index = selectedTabIndex;
		selectedTabIndex = -1;
		CactiEntryTabGroup group = currentTabGroup;
		setCurrentCategory(currentCategory, categoryPage);
		if (group != null) {
			setCurrentTabGroup(group);
		}
		if (index >= 0 && index < CreativeTabs.CREATIVE_TAB_ARRAY.length) {
			CreativeTabs tab = CreativeTabs.CREATIVE_TAB_ARRAY[index];
			if (tab != null) {
				setCurrentCreativeTab(tab);
				setTabPage((index - 2) / 10);
			}
		}
	}

	private void setCurrentEntry(CactiEntry entry) {
		if (entry instanceof CactiEntryCategory) {
			setCurrentCategory((CactiEntryCategory) entry, 0);
		} else if (entry instanceof CactiEntryTabGroup) {
			setCurrentTabGroup((CactiEntryTabGroup) entry);
		}
	}

	private void setCurrentCategory(CactiEntryCategory category, int page) {
		currentCategory = category;
		categoryPage = page;
		ImmutableList<CactiEntry> entries = category.getEntries();
		entryCount = entries.size();
		updatePageEntries();
		updatePageButtons();
		updateParentCategoryButton();
	}

	private void setCurrentTabGroup(CactiEntryTabGroup group) {
		currentTabGroup = group;
		if (group == null) {
			updateTabs(ImmutableList.of());
		} else {
			updateTabs(group.getTabs());
		}
	}

	private void updateTabs(ImmutableList<CreativeTabs> tabs) {
		CreativeTabs[] tabsArray = new CreativeTabs[tabs.size()];
		int len = 0;
		for (CreativeTabs tab : tabs) {
			if (tab == CreativeTabs.INVENTORY || tab == CreativeTabs.SEARCH) {
				continue;
			}
			tabsArray[len++] = tab;
		}
		CreativeTabs[] arr = CreativeTabs.CREATIVE_TAB_ARRAY = new CreativeTabs[Math.max(len + 2, 12)];
		arr[CreativeTabs.SEARCH.tabIndex] = CreativeTabs.SEARCH;
		arr[CreativeTabs.INVENTORY.tabIndex] = CreativeTabs.INVENTORY;
		for (int i = 0, idx = 0; i < len; idx++) {
			if (arr[idx] == null) {
				(arr[idx] = tabsArray[i++]).tabIndex = idx;
			}
		}
		int maxPages;
		final int prevId = 101, nextId = 102;
		if (arr.length > 12) {
			boolean needPrev = true, needNext = true;
			for (GuiButton button : buttonList) {
				if (button.id == prevId) {
					needPrev = false;
				} else if (button.id == nextId) {
					needNext = false;
				}
			}
			if (needPrev) {
				buttonList.add(new GuiButton(prevId, guiLeft, guiTop - 50, 20, 20, "<"));
			}
			if (needNext) {
				buttonList.add(new GuiButton(nextId, guiLeft + xSize - 20, guiTop - 50, 20, 20, ">"));
			}
			maxPages = (arr.length - 3) / 10;
		} else {
			buttonList.removeIf(button -> button.id == prevId || button.id == nextId);
			maxPages = 0;
		}
		setMaxPages(maxPages);
		setTabPage(0);
		setCurrentCreativeTab(Iterables.getFirst(tabs, CreativeTabs.INVENTORY));
	}

	private void setMaxPages(int maxPages) {
		ReflectionHelper.setPrivateValue(GuiContainerCreative.class, this, maxPages, "maxPages");
	}

	private void setTabPage(int tabPage) {
		ReflectionHelper.setPrivateValue(GuiContainerCreative.class, null, tabPage, "tabPage");
	}

	private void updatePageEntries() {
		ImmutableList<CactiEntry> entries = currentCategory.getEntries();
		// Hide the Minecraft entry if there are no other entries
		if (currentCategory == CactiAPI.categories() && entries.size() == 1) {
			currentPageEntries = Collections.emptyList();
		} else {
			int start = categoryPage * ENTRIES_PER_PAGE;
			int end = Math.min(entries.size(), (categoryPage + 1) * ENTRIES_PER_PAGE);
			currentPageEntries = entries = entries.subList(start, end);
		}
		initTabGroup(entries);
		updateRibbonWidths();
	}

	private void initTabGroup(List<CactiEntry> entries) {
		CactiEntryTabGroup group = null;
		if (entries.size() > 0) {
			CactiEntry entry = entries.get(0);
			if (entry instanceof CactiEntryTabGroup) {
				group = (CactiEntryTabGroup) entry;
			}
		}
		setCurrentTabGroup(group);
	}

	private void updateRibbonWidths() {
		for (int i = 0; i < ribbonWidths.length; i++) {
			if (i < currentPageEntries.size()) {
				ribbonWidths[i] = fontRendererObj.getStringWidth(currentPageEntries.get(i).getDisplayName());
			} else {
				ribbonWidths[i] = 0;
			}
		}
	}

	private void updatePageButtons() {
		pagePrevious.visible = pageNext.visible = entryCount > ENTRIES_PER_PAGE;
		updatePageButtonEnabledStates();
	}

	private void updateParentCategoryButton() {
		CactiEntryCategory owner = currentCategory.getOwner();
		if (parentCategory.visible = owner != null) {
			parentCategory.displayString = owner.getDisplayName();
		}
	}

	private void updatePageButtonEnabledStates() {
		pagePrevious.enabled = categoryPage > 0;
		pageNext.enabled = categoryPage < (entryCount - 1) / ENTRIES_PER_PAGE;
	}

	private CactiEntry getCategoryAtCursor(int mouseX, int mouseY) {
		int xLeft, xRight, xAlong;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			xLeft = 0;
			xRight = guiLeft;
			xAlong = guiLeft - mouseX - 1;
		} else {
			xLeft = guiLeft + xSize;
			xRight = width;
			xAlong = mouseX - xLeft;
		}
		if (mouseX >= xLeft && mouseX < xRight && mouseY >= guiTop + RIBBON_START_Y_OFFSET) {
			int index = (mouseY - guiTop - RIBBON_START_Y_OFFSET - 1) / (RIBBON_TILE_SIZE - 1);
			if (index < currentPageEntries.size() && xAlong < ribbonWidths[index] + 6) {
				return currentPageEntries.get(index);
			}
		}
		return null;
	}

	private void drawBackground() {
		if (currentPageEntries.isEmpty()) {
			return;
		}
		GlStateManager.disableLighting();
		mc.getTextureManager().bindTexture(shouldUseButtonRibbonRender ? BUTTON_TEX : WIDGETS_TEX);
		renderEntryRibbonTiles(currentPageEntries, currentTabGroup, Pass.BACKGROUND);
		renderEntryRibbonTiles(currentPageEntries, currentTabGroup, Pass.TEXT);
		int pages = (entryCount - 1) / ENTRIES_PER_PAGE;
		boolean parentCategoryButtonVisible = parentCategory.visible;
		boolean renderPages = pages > 0;
		if (parentCategoryButtonVisible || renderPages) {
			int leftX = guiLeft - 5;
			int shift = Math.max(0, LEFT_BUTTON_WIDTH - leftX + 2);
			int leftWidth = LEFT_BUTTON_WIDTH - shift;
			leftX += shift;
			if (parentCategoryButtonVisible) {
				parentCategory.width = leftWidth;
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					parentCategory.xPosition = leftX - LEFT_BUTTON_WIDTH;
				}
				parentCategory.displayString = fitString(mc.fontRendererObj, currentCategory.getDisplayName(), parentCategory.width - 6);
			}
			if (renderPages) {
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					pagePrevious.xPosition = leftX - LEFT_BUTTON_WIDTH;
				} else {
					pageNext.xPosition = parentCategory.xPosition + leftWidth - pageNext.width;
				}
				FontRenderer font = mc.fontRendererObj;
				String str = String.format("%d / %d", categoryPage + 1, pages + 1);
				if (pageNext.xPosition - pagePrevious.xPosition - pagePrevious.width - 5 < font.getStringWidth(str)) {
					str = Integer.toString(categoryPage + 1);
				}
				int w = font.getStringWidth(str);
				font.drawString(str, (pagePrevious.xPosition + pagePrevious.width + pageNext.xPosition) / 2 - w / 2, guiTop + ySize + 7, 0xFFFFFF);
			}
		}
	}

	private void drawScreen(int mouseX, int mouseY) {
		CactiEntry entry = getCategoryAtCursor(mouseX, mouseY);
		if (entry != null) {
			FontRenderer font = mc.fontRendererObj;
			String name = entry.getDisplayName();
			String shown = fitString(font, name, getAvailableEntryWidth(font, guiLeft - 3, name) - 4);
			if (!name.equals(shown)) {
				drawCreativeTabHoveringText(entry.getDisplayName(), mouseX, mouseY);
			}
		}
		GlStateManager.disableLighting();
	}

	private void renderEntryRibbonTiles( List<CactiEntry> entries, CactiEntry selected, Pass pass) {
		FontRenderer font = mc.fontRendererObj;
		int rightSpace = guiLeft - 3;
		for (int i = 0, size = entries.size(); i < size; i++) {
			CactiEntry entry = entries.get(i);
			String name = entry.getDisplayName();
			int width = getAvailableEntryWidth(font, rightSpace, name);
			name = fitString(font, name, width - 4);
			// ceil tiles
			int tiles = (width - RIBBON_TILE_SIZE + (RIBBON_TILE_SIZE + 1)) / RIBBON_TILE_SIZE;
			int offset = (width + 1) % RIBBON_TILE_SIZE;
			int y = guiTop + RIBBON_START_Y_OFFSET + RIBBON_HEIGHT * i;
			boolean isSelected = entry == selected;
			if (shouldUseButtonRibbonRender) {
				renderButtonRibbon(isSelected, pass, font, name, width, y);
			} else {
				renderRegularRibbon(isSelected, pass, font, name, width, tiles, offset, y);
			}
		}
	}

	private void renderButtonRibbon(boolean isSelected, Pass pass, FontRenderer font, String name, int width, int y) {
		width -= 1;
		if (pass == Pass.TEXT) {
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				font.drawStringWithShadow(name, guiLeft - width - (width + 1) % 2, y + 3, 0xFFFFFF);
			} else {
				font.drawStringWithShadow(name, guiLeft + xSize + 5, y + 3, 0xFFFFFF);
			}
			return;
		}
		int x;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			x = guiLeft - width - 4 + width % 2;
		} else {
			x = guiLeft + xSize + 2;
		}
		int vOffset = isSelected ? 20 : 0;
		width = width / 2 + 1;
		drawTexturedModalRect(x, y, 0, 46 + vOffset, width, 8);
		drawTexturedModalRect(x, y + 8, 0, 46 + vOffset + 13, width, 7);
		drawTexturedModalRect(x + width, y, 200 - width, 46 + vOffset, width, 8);
		drawTexturedModalRect(x + width, y + 8, 200 - width, 46 + vOffset + 13, width, 7);
	}

	private void renderRegularRibbon(boolean isSelected, Pass pass, FontRenderer font, String name, int width, int tiles, int offset, int y) {
		if (pass == Pass.TEXT) {
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				font.drawString(name, guiLeft - width + 4, y + 5, 0x404040);
			} else {
				font.drawString(name, guiLeft + xSize + 2, y + 5, 0x404040);
			}
			return;
		}
		int v;
		if (isSelected) {
			v = 0;
			GlStateManager.translate(0, 0, 1);
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				drawTexturedModalRect(guiLeft, y, 51, 0, 4, RIBBON_TILE_SIZE);
			} else {
				drawTexturedModalRect(guiLeft + xSize - 4, y, 81, 0, 4, RIBBON_TILE_SIZE);
			}
			GlStateManager.translate(0, 0, -1);
		} else {
			v = 17;
		}
		if (tiles > 0) {
			if (Configurator.displaySide() == DisplaySide.LEFT) {
				drawTexturedModalRect(guiLeft - RIBBON_TILE_SIZE, y, 34, v, RIBBON_TILE_SIZE, RIBBON_TILE_SIZE);
			} else {
				drawTexturedModalRect(guiLeft + xSize, y, 34, v, RIBBON_TILE_SIZE, RIBBON_TILE_SIZE);
			}
		}
		int x;
		if (Configurator.displaySide() == DisplaySide.LEFT) {
			x = guiLeft - RIBBON_TILE_SIZE - offset + RIBBON_X_OFFSET;
		} else {
			x = guiLeft + xSize - RIBBON_TILE_SIZE * 2 + offset + RIBBON_X_OFFSET;
		}
		for (int t = 1, lastTile = tiles - 1;; t++) {
			int w = t == 1 ? offset : RIBBON_TILE_SIZE;
			if (lastTile > 0) {
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					drawTexturedModalRect(x - t * RIBBON_TILE_SIZE, y, 17, v, w, RIBBON_TILE_SIZE);
				} else {
					drawTexturedModalRect(x + t * RIBBON_TILE_SIZE + (t == 1 ? RIBBON_TILE_SIZE - offset : 0), y, 102, v, w, RIBBON_TILE_SIZE);
				}
			}
			if (t >= lastTile) {
				w = lastTile == 0 ? w : RIBBON_TILE_SIZE;
				if (Configurator.displaySide() == DisplaySide.LEFT) {
					drawTexturedModalRect(x - tiles * RIBBON_TILE_SIZE, y, 0, v, w, RIBBON_TILE_SIZE);
				} else {
					drawTexturedModalRect(x + tiles * RIBBON_TILE_SIZE, y, 119, v, w, RIBBON_TILE_SIZE);
				}
				break;
			}
		}
	}

	private static void replaceTabs(CreativeTabs replacee, CreativeTabs replacement) {
		CreativeTabs[] tabs = CreativeTabs.CREATIVE_TAB_ARRAY;
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] == replacee) {
				tabs[i] = replacement;
			}
		}
	}

	private static ModContainer getOwner(CreativeTabs tab) {
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
					String name1 = m1.getName().toLowerCase(Locale.ROOT);
					String name2 = m2.getName().toLowerCase(Locale.ROOT);
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
			mod = Loader.instance().getMinecraftModContainer();
		}
		return mod;
	}

	public static void ungroupSingleTabMods() {
		boolean checkAdjustTab = currentTabGroup != null && currentTabGroup.getId().equals(MODS_TAB_GROUP);
		if (CactiAPI.categories().hasTabGroup(MODS_TAB_GROUP)) {
			CactiAPI.categories().removeEntry(MODS_TAB_GROUP);
			for (ModContainer mod : creativeTabs.keySet()) {
				Collection<CreativeTabs> tabs = creativeTabs.get(mod);
				if (tabs.size() == 1) {
					CreativeTabs tab = Iterables.getOnlyElement(tabs);
					CactiAPI.categories().removeEntry(mod.getModId());
					CactiEntryTabGroup modTab = CactiAPI.categories()
						.withTabGroup(mod.getModId())
						.withTab(tab);
					modTab.setCustomName(mod.getName());
					if (checkAdjustTab && tab.tabIndex == selectedTabIndex) {
						currentTabGroup = modTab;
						currentCategory = CactiAPI.categories();
						selectedTabIndex = 0;
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
		for (ModContainer mod : creativeTabs.keySet()) {
			Collection<CreativeTabs> tabs = creativeTabs.get(mod);
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
					selectedTabIndex = tabId;
					checkAdjustTab = false;
				}
				groupedMods.withTab(Iterables.getOnlyElement(tabs));
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
		GuiScreen gui = Minecraft.getMinecraft().currentScreen;
		if (gui instanceof GuiContainerCactiCreative) {
			((GuiContainerCactiCreative) gui).updateSelected();
		}
	}

	private static String getResourceOwner(ResourceLocation location) {
		try {
			return Minecraft.getMinecraft().getResourceManager().getResource(location).getResourcePackName();
		} catch (IOException e) {
			// noop
		}
		return "";
	}

	private static String getName(String str) {
		return str.substring(str.indexOf(':') + 1);
	}

	private static int getAvailableEntryWidth(FontRenderer font, int rightSpace, String name) {
		int width = font.getStringWidth(name) + 5;
		return Math.max(10, width - Math.max(0, width - rightSpace));
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

	private enum Pass {
		BACKGROUND, TEXT
	}

	private enum SetTabAction {
		IGNORE((gui, t) -> {}),
		SET(GuiContainerCactiCreative::doSetCurrentCreativeTab);

		private final BiConsumer<GuiContainerCactiCreative, CreativeTabs> action;

		SetTabAction(BiConsumer<GuiContainerCactiCreative, CreativeTabs> action) {
			this.action = action;
		}

		public final void setCurrentCreativeTab(GuiContainerCactiCreative gui, CreativeTabs tab) {
			action.accept(gui, tab);
		}
	}

	private static final class NullCreativeTab extends CreativeTabs {
		private static final NullCreativeTab INSTANCE;

		static {
			try {
				Class<?> unsafeCls = Class.forName("sun.misc.Unsafe");
				Field theUnsafeFld = unsafeCls.getDeclaredField("theUnsafe");
				theUnsafeFld.setAccessible(true);
				Object unsafe = theUnsafeFld.get(null);
				Method allocateInstance = unsafeCls.getMethod("allocateInstance", Class.class);
				INSTANCE = (NullCreativeTab) allocateInstance.invoke(unsafe, NullCreativeTab.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private NullCreativeTab() {
			super(throwUnsupported());

		}

		private static String throwUnsupported() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getTabPage() {
			return -1;
		}

		@Override
		public ItemStack getIconItemStack() {
			return getTabIconItem();
		}

		@Override
		public ItemStack getTabIconItem() {
			return ItemStack.EMPTY;
		}
	}
}
